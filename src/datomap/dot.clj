(ns datomap.dot
  (:require [dorothy.core :as dot]
            [hiccup.core :as html]))


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

(defn attribute->row
  [attribute]
  (let [attribute-name (pr-str (:db/ident attribute))]
   (into [:tr {:port attribute-name}]
         (->> ((apply juxt attribute-attributes) attribute)
              (mapv (fn [a] [:td (pr-str a)]))))))

(defn attributes->rows
  [attributes]
  (mapv attribute->row attributes))

(defn entity->dot-node
  [[entity attributes]]
  (let [entity-name (pr-str entity)]
    [entity-name
     {:label
      (html/html
       (into [:table {:port entity-name}
              [:tr [:td entity-name]]
              attr-headings]
             (attributes->rows attributes)))}]))

(defn entities->dot-nodes
  [grouped-entities]
  (map entity->dot-node grouped-entities))

(defn schema->digraph
  [grouped-entities]
  (dot/digraph
   (concat [(dot/node-attrs {:shape "plaintext"})]
           (entities->dot-nodes grouped-entities))))
