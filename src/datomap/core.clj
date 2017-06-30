(ns datomap.core
  (:require [datomic.api :as d]
            [clojure.java.io :as io]))

(defn internal?
  [attr]
  (seq (re-matches #"(db|fressian).*" (namespace attr))))

(def user-defined? (complement internal?))

(defn all-attrs
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
  [db]
  (map (comp normalize-idents (partial d/pull db attr-pattern)) (all-attrs db)))

(defn entity->namespace
  [attr-entity]
  (-> attr-entity :db/ident namespace keyword))

(defn group-attr-entities
  [attr-entities]
  (group-by entity->namespace attr-entities))

(defn entity->txable
  [entity]
  (println entity)
  (update entity
          :db/id
          (fn [e] (d/tempid :db.part/db (* -1  e)))))

(defn dump-schema
  [db]
  (-> db
      all-attr-entities
      group-attr-entities))

(defn schema->txable
  [db]
  (reduce
   (fn [coll [k v]]
     (println k " " v)
     (into coll (map entity->txable v)))
   []
   (dump-schema db)))

(defn format-entity
  [entity]
  (-> entity
      (update :db/id (comp symbol pr-str))
      clojure.pprint/pprint
      with-out-str
      (str "\n")))

(defn schema->edn
  [db file]
  (let [schema (schema->txable db)]
    (doseq [attr schema]
      (spit file
            (format-entity attr)
            :append true))))


(defn by-ident
  [attr-entities]
  (reduce
   (fn [m e]
     (assoc m (:db/ident e) e))
   {}
   attr-entities))

(defn attr-ref-attrs
  [db attr]
  (d/q '[:find [?attr-name ...]
         :in $ ?attr
         :where
         [_ :attr ?v]
         [?v ?e-attr _]
         [?e-attr :db/ident ?attr-name]]
       db attr))

(defn ref-attr->ref-namespaces
  [db attr]
  ())
