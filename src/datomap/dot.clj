(ns datomap.dot
  (:require [clojure.string :as str]
            [dorothy.core :as dot]
            [hiccup.core :as html]
            [datomap.core :as dmap]))


(def attribute-attributes
  [:db/ident
   :db/valueType
   :db/cardinality
   :db/unique
   :db/isComponent
   :db/index
   :db/doc])

(def attr-headings
  (into [:tr]
        (mapv (fn [a] [:td {:bgcolor "gray"} (pr-str a)]) attribute-attributes)))

(defn format-keyword
  [maybe-kw]
  (if (keyword? maybe-kw)
    (-> maybe-kw str (subs 1))
    maybe-kw))

(defn kw->safe-port
  [db-ident]
  (-> db-ident
      format-keyword
      (str/replace #"\/" "_")
      (str/replace #"\." "__")
      (str/replace #"-" "___")))

(defn attribute->row
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
  [attributes]
  (mapv attribute->row attributes))

(defn entity->dot-node
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
  [grouped-entities]
  (map entity->dot-node grouped-entities))

(defn format-port
  [kw]
  (if (namespace kw)
    (str (kw->safe-port (keyword (namespace kw)))
         ":"
         (kw->safe-port (keyword (name kw))))
    (str (kw->safe-port kw) ":" (kw->safe-port (keyword (name kw))))))

(defn relations
  [db]
  (mapv
   (fn [[root dest]]
     [(format-port root)
      (format-port dest)
      {:arrowhead "normal"}])
   (dmap/db->ref-edges db)))

(defn db->schema-digraph
  [db]
  (let [grouped-entities (dmap/dump-schema db)
        relationships (relations db)]
    (println relationships)
   (dot/digraph
    (concat [(dot/node-attrs {:shape "plaintext"})]
            (entities->dot-nodes grouped-entities)
            [["db"
              {:label
               (html/html
                [:table {:port "ident"
                         :border 0
                         :cellborder 1
                         :cellspacing 0}
                 [:tr [:td "db/ident"]]])}]]
            (relations db)))))


(defn db->dot-schema
  [db]
  (dot/dot (schema->digraph db)))

(defn show-schema!
  [db]
  (dot/show! (db->dot-schema db)))
