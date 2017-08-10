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
        attribute-name (format-keyword db-ident)]
    (println "ATTR NAME " attribute-name)
    (into [:tr {:port (kw->safe-port (keyword (name db-ident)))}]
         (->> ((apply juxt attribute-attributes) attribute)
              (mapv (fn [a] [:td (format-keyword a)]))))))

(defn attributes->rows
  [attributes]
  (mapv attribute->row attributes))

(defn entity->dot-node
  [[entity attributes]]
  (let [entity-name (format-keyword entity)]
    [(str (kw->safe-port entity) ":" (str (kw->safe-port entity)))
     {:label
      (html/html
       (into [:table {:port (kw->safe-port entity)
                      :border 0
                      :cellborder 1
                      :cellspacing 0}
              [:tr [:td entity-name]]
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
      {:arrowhead "crow"}])
   (dmap/db->ref-edges db)))

(defn schema->digraph
  [db]
  (let [grouped-entities (dmap/dump-schema db)
        relationships (relations db)]
    (println relationships)
   (dot/digraph
    (concat [(dot/node-attrs {:shape "plaintext"})]
            (entities->dot-nodes grouped-entities)
            [["db:ident"
              {:label
               (html/html
                [:table {:port "ident"
                         :border 0
                         :cellborder 1
                         :cellspacing 0}
                 [:tr [:td "db/ident"]]])}]]
            (relations db)))))

;; (defn update-digraph-node
;;   [node]
;;   (if (= "" (get-in node [:id :id]))
;;     (update-in node [:id :id] (constantly (get-in node [:id :port])))
;;     node))

;; (defn update-digraph
;;   [digraph]
;;   (update digraph
;;           :statements
;;           (fn [statements]
;;             (map update-digraph-node statements))))

(defn schema->dot
  [db]
  (dot/dot (schema->digraph db)))
