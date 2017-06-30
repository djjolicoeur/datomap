(defproject datomap "0.1.0-SNAPSHOT"
  :description "FIXME: write description"
  :url "http://example.com/FIXME"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.8.0"]]
  :profiles {:dev {:plugins [[lein-kibit "0.1.2"]]
                   :source-paths ["dev"]
                   :resource-paths ["dev-resources"]
                   :dependencies [[com.stuartsierra/component "0.2.3"]
                                  [reloaded.repl "0.2.1"]]}
             :provided {:dependencies [[org.clojure/clojure "1.8.0"]
                                       [com.datomic/datomic-free "0.9.5404"]]}})
