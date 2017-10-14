(ns datomap.io
  (:require [clojure.string :as str]
            [hiccup.core :as html]
            [datomap.core :as dmap]))


;; Dorothy will throw if a windowing system is not enabled
;; Should probably fix in dorothy, but this will suffice for now.
(def dot-enabled?
  (try (require 'dorothy.core) true
       (catch Throwable _ false)))

(defmacro with-dot
  "Only execute form if dot is enabled"
  [form]
  `(if dot-enabled? ~form
       (throw (ex-info
               (clojure.string/join
                ["dorothy.core was unable to be required,"
                 " perhaps you are in a headless environment?"])
               {:causes #{:dorothy-not-required}}))))

;; loom.io will throw if a windowing system is not enabled
;; Should probably fix in dorothy, but this will suffice for now.
(def loom-io-enabled?
  (try (require 'loom.io) true
       (catch Throwable _ false)))

(defmacro with-loom.io
  "Only execute form if dot is enabled"
  [form]
  `(if loom-io-enabled? ~form
       (throw (ex-info
               (clojure.string/join
                ["loom.io was unable to be required,"
                 " perhaps you are in a headless environment?"])
               {:causes #{:loom.io-not-required}}))))


(def attribute-attributes
  "List of db schema attributes to pull for each attribute"
  [:db/ident
   :db/valueType
   :db/cardinality
   :db/unique
   :db/isComponent
   :db/index
   :db/doc])

(defn format-keyword
  [maybe-kw]
  (if (keyword? maybe-kw)
    (-> maybe-kw str (subs 1))
    maybe-kw))

(def attr-headings
  "Row of table headings"
  (into [:tr]
        (mapv (fn [a] [:td {:bgcolor "gray"} (format-keyword a)])
              attribute-attributes)))

(defn kw->safe-port
  "Some legal keyword symbols are illegal for dot port definitions, thus we
  convert to legal underscores"
  [db-ident]
  (-> db-ident
      format-keyword
      (str/replace #"\/" "_")
      (str/replace #"\." "__")
      (str/replace #"-" "___")))

(defn attribute->row
  "Generate a table row of schema attribute values per entity attribute"
  [attribute]
  (let [db-ident (:db/ident attribute)
        attribute-name (format-keyword db-ident)
        attrs ((apply juxt attribute-attributes) attribute)
        dims (into [[:td
                     {:port (kw->safe-port (keyword (name db-ident)))}
                     (format-keyword (first attrs))]]
                   (mapv (fn [a] [:td (format-keyword a)]) (rest attrs)))]
    (into [:tr] dims)))

(defn attributes->rows
  "Maps the above attribute->row over seq of attributes and generates
  a vector of table rows."
  [attributes]
  (mapv attribute->row attributes))

(defn entity->dot-node
  "Generates table consisting of entity name and rows for each entity attribute."
  [[entity attributes]]
  (let [entity-name (format-keyword entity)]
    [(kw->safe-port entity)
     {:label
      (html/html
       (into [:table {:border 0
                      :cellborder 1
                      :cellspacing 0}
              [:tr [:td {:port (kw->safe-port entity)} entity-name]]
              attr-headings]
             (attributes->rows attributes)))}]))

(defn entities->dot-nodes
  "Given a map of entity namespaces mapped to the attributes that fall under
  those namespaces, generate a table for each key value"
  [grouped-entities]
  (map entity->dot-node grouped-entities))

(defn format-port
  "Generates <entity namespace>:<attribute name> for given keyword"
  [kw]
  (if (namespace kw)
    (str (kw->safe-port (keyword (namespace kw)))
         ":"
         (kw->safe-port (keyword (name kw))))
    (str (kw->safe-port kw) ":" (kw->safe-port (keyword (name kw))))))

(defn relations
  "Generate dot edges for :db.type/ref attributes"
  [db]
  (mapv
   (fn [[root dest]]
     [(format-port root)
      (format-port dest)
      {:arrowhead "normal"}])
   (dmap/db->ref-edges db)))

(defn db->schema-digraph
  "Generate directed graph from db-value"
  [db]
  (with-dot
    (let [grouped-entities (dmap/dump-schema db)
          relationships (relations db)]
      (dorothy.core/digraph
       (concat [(dorothy.core/node-attrs {:shape "plaintext"})]
               (entities->dot-nodes grouped-entities)
               [["db"
                 {:label
                  (html/html
                   [:table {:port "ident"
                            :border 0
                            :cellborder 1
                            :cellspacing 0}
                    [:tr [:td "db/ident"]]])}]]
               (relations db))))))

(defn db->dot-schema
  "Generate dot string describing graph of db schema given db-value"
  [db]
  (with-dot
    (dorothy.core/dot (db->schema-digraph db))))

(defn show-schema-tables!
  "show DB schema as dot html tables"
  [db]
  (with-dot
    (dorothy.core/show! (db->dot-schema db))))

(defn save-schema-tables!
  "Save schema as PNG to file. defaults to .PNG"
  ([db file]
   (save-schema-tables! db file {:format :png}))
  ([db file opts]
   (with-dot
     (-> (db->schema-digraph db)
         dorothy.core/dot
         (dorothy.core/save! file opts)))))


;; Loom graph visualizations
(defn show-schema-nodes!
  "Shows schema as graph such that each namespace and attribute is an individual
  node"
  ([db]
   (with-loom.io (loom.io/view (dmap/schema->graph db))))
  ([db entity-namespaces]
   (with-loom.io (loom.io/view (dmap/schema->graph db entity-namespaces)))))
