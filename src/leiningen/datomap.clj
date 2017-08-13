(ns leiningen.datomap
  (:require [datomap.core :as dmap]
            [datomap.io :as dmap.io]
            [datomic.api :as d]))


(defn- parse-arg [[k v]]
  (let [kw (keyword (subs k 1))]
    [kw v]))

(defn- parse-args [args]
  (let [arg-count (count args)]
    (if (even? arg-count)
      (into {} (map parse-arg (apply hash-map args)))
      (throw
       (ex-info (str "Even number of forms required! got: " arg-count)
                {:args args
                 :causes #{:invalid-args}})))))


(defn render-graph
  [graph-type db]
  (case graph-type
    "tables" (dmap.io/show-schema-tables! db)
    "nodes" (dmap.io/show-schema-nodes! db)))

(defn save-graph
  [file-out db]
  (if file-out
    (dmap.io/save-schema-tables! db file-out)
    (throw (ex-info "No File Specified! pass in :file-out <file>"
                    {:causes #{:no-file-out}}))))

(defn dump-edn-schema
  [file-out db]
  (if file-out
    (dmap/schema->edn db file-out)
    (throw (ex-info "No File Specified! pass in :file-out <file>"
                    {:causes #{:no-file-out}}))))


(defn datomap
  [project & args]
  (let [{op :op
         uri :uri
         graph-type :graph-type
         file-out :file-out
         :as args
         :or {op "graph"
              graph-type "tables"}} (parse-args args)
        conn (d/connect uri)
        db (d/db conn)]
    (case op
      "graph" (render-graph graph-type db)
      "save-graph" (save-graph file-out db)
      "dump-schema" (dump-edn-schema file-out db))))
