(ns user
  (:require [clojure.java.io :as io]
            [com.stuartsierra.component :as component]
            [reloaded.repl :refer [go init reset reset-all start stop system]]
            [datomic.api :as d]
            [datomap.core :as dmap]))




(defn datomic-readall [resource-name]
  (-> resource-name
      io/reader
      datomic.Util/readAll))

(defrecord DB [uri conn]
  component/Lifecycle
  (start [this]
    (if conn this
        (let [schema (datomic-readall "dev-resources/schema.edn")
              facts  (datomic-readall  "dev-resources/facts.edn")
              name   (java.util.UUID/randomUUID)
              uri    (str  "datomic:mem://" name)
              _      (d/create-database uri)
              conn   (d/connect uri)]
          @(d/transact conn schema)
          @(d/transact conn facts)
          (assoc this :conn conn :uri uri))))
  (stop [this]
    (if-not conn this
            (do
              (d/delete-database uri)
              (assoc this :conn nil :uri nil :merger nil)))))


(def dev-system
  (component/system-map
   :db (map->DB {})))




(reloaded.repl/set-init! (constantly dev-system))
