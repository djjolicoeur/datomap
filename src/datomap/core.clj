(ns datomap.core
  (:require [clojure.java.io :as io]
            [datomic.api :as d]
            [loom.graph :as graph]))

(defn internal?
  [attr]
  (seq (re-matches #"(db|fressian).*" (namespace attr))))

(def user-defined? (complement internal?))

(defn all-attrs
  "Find all user defined attributes in db"
  [db]
  (d/q '[:find [?attr ...]
         :where
         [?e :db.install/attribute ?a]
         [?a :db/ident ?attr]
         [(datomap.core/user-defined? ?attr)]] db))

(def attr-pattern
  '[* {:db/valueType [:db/ident]
       :db/cardinality [:db/ident]
       :db/unique [:db/ident]}])

(defn normalize-idents
  "grab :db/ident values from pulled attributes"
  [attr-entity]
  (let [[valuetype
         cardinality
         unique] ((juxt :db/valueType
                        :db/cardinality
                        :db/unique) attr-entity)]
    (cond-> attr-entity
      valuetype   (update :db/valueType :db/ident)
      cardinality (update :db/cardinality :db/ident)
      unique      (update :db/unique :db/ident))))

(defn all-attr-entities
  "Grab all user defined entity attributes in the db and normalize"
  [db]
  (map (comp normalize-idents (partial d/pull db attr-pattern)) (all-attrs db)))

(defn entity->namespace
  "Get namespace of attribute entity"
  [attr-entity]
  (-> attr-entity :db/ident namespace keyword))

(defn group-attr-entities
  "Group schema by entity namespace"
  [attr-entities]
  (group-by entity->namespace attr-entities))

(defn entity->txable
  "Create a transactable version of entity attributes"
  [entity]
  (update entity
          :db/id
          (fn [e] (d/tempid :db.part/db (* -1  e)))))

(defn dump-schema
  "Dump entire schema grouped by entity namespace"
  [db]
  (-> db
      all-attr-entities
      group-attr-entities))

(defn schema->txable
  "Create a transactable copy of user defined schema"
  [db]
  (reduce
   (fn [coll [k v]]
     (into coll (map entity->txable v)))
   []
   (dump-schema db)))

(defn format-entity
  "Format entity to edn friendly format"
  [entity]
  (-> entity
      (update :db/id (comp symbol pr-str))
      clojure.pprint/pprint
      with-out-str
      (str "\n")))

(defn schema->edn
  "Dump transactable schema to edn file"
  [db file]
  (let [schema (schema->txable db)]
    (doseq [attr schema]
      (spit file (format-entity attr) :append true))))

(defn by-ident
  "Map attributes by :db/ident"
  [attr-entities]
  (reduce
   (fn [m e]
     (assoc m (:db/ident e) e))
   {}
   attr-entities))

(defn attr-ref-attrs
  "Find entity namespace of ref :db.type/ref references"
  [db attr]
  (d/q '[:find [?attr-name ...]
         :in $ ?attr
         :where
         [_ ?attr ?v]
         [?v ?e-attr _]
         [?e-attr :db/ident ?attr-name]]
       db attr))

(defn namespace-or-ident
  [k]
  (if (= :db/ident k) k
      (keyword (namespace k))))

(defn ref-attr->ref-namespaces
  "Get and format all reference target namespaces (or :db/ident for enums)"
  [db attr]
  (->> attr
       (attr-ref-attrs db)
       (map namespace-or-ident)
       set))

(defn by-value-type
  [db]
  (->> db
       all-attr-entities
       (group-by :db/valueType)))

(defn db-refs
  "Find all ref attributes, optionally scope by entity-namespaces"
  ([db]
   (:db.type/ref (by-value-type db)))
  ([db entity-namespaces]
   (let [ns-set (set entity-namespaces)]
     (filter
      #(ns-set (keyword (namespace (:db/ident %))))
      (db-refs db)))))

(defn db->ref-maps
  "Generate map of reference attr -> '(refernce target namespace) "
  ([db] (db->ref-maps db nil))
  ([db entity-namespaces]
   (let [refs (if (seq entity-namespaces)
                (map :db/ident (db-refs db entity-namespaces))
                (map :db/ident (db-refs db)))
         refs-to (map (partial ref-attr->ref-namespaces db) refs)
         global-namespaces (apply clojure.set/intersection refs-to)]
     (zipmap refs
             (map (fn [ref-to]
                    (remove global-namespaces ref-to)) refs-to)))))

(defn ref-map->ref-edges
  "Generate edge tuple for each relation"
  [[k v]]
  (map (fn [ns] [k ns]) v))

(defn db->ref-edges
  "Generate edge tuple for all references in the db"
  ([db]
   (mapcat ref-map->ref-edges (db->ref-maps db)))
  ([db entity-namespaces]
   (mapcat ref-map->ref-edges (db->ref-maps db entity-namespaces))))

(defn root->edge
  "Generate edge for entity namespace and attribute map"
  [[k v]]
  (map (fn [entity] [k (:db/ident entity)]) v))

(defn by-root->edges
  "Given a map of entity namespaces (root) to attribute maps,
  generate edges for each [namespace attribute-map] tuple"
  [by-root]
  (mapcat root->edge by-root))

(defn attr-map->edge
  "Generate edges for specific attributes to values, e.g.
  [:db/valueType :db.type/string]"
  [attr-map]
  (map (fn [[k v]] [(:db/ident attr-map) (pr-str [k v])])
       (dissoc attr-map :db/id :db/ident)))

(defn attr-maps->edges
  [attr-maps]
  (mapcat attr-map->edge attr-maps))

(defn schema->graph
  "Generates loom graph given a db value"
  ([db]
   (let [g (graph/digraph)
         by-root (dump-schema db)
         root-edges (by-root->edges by-root)
         all-attrs (by-ident (all-attr-entities db))
         attr-edges (attr-maps->edges (vals all-attrs))
         ref-edges (db->ref-edges db)
         g (apply graph/add-nodes (cons g (keys by-root)))
         g (apply graph/add-nodes (cons g (keys all-attrs)))
         g (apply graph/add-edges (cons g root-edges))
         g (apply graph/add-edges (cons g ref-edges))
         g (apply graph/add-edges (cons g attr-edges))]
     g))
  ([db entity-namespaces]
   (let [g (graph/digraph)
         by-root (select-keys (dump-schema db) entity-namespaces)
         root-edges (by-root->edges by-root)
         all-attrs (mapcat (fn [[k v]] v) by-root)
         attr-edges (attr-maps->edges all-attrs)
         ref-edges (db->ref-edges db entity-namespaces)
         g (apply graph/add-nodes (cons g entity-namespaces))
         g (apply graph/add-nodes (cons g (map :db/ident all-attrs)))
         g (apply graph/add-edges (cons g root-edges))
         g (apply graph/add-edges (cons g ref-edges))
         g (apply graph/add-edges (cons g attr-edges))]
     g)))
