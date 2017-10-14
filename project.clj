(defproject djjolicoeur/datomap "0.1.1-SNAPSHOT"
  :description "Datomic schema utilities and visualization tools"
  :url "http://github.com/djjolicoeur/datomap"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[aysylu/loom "1.0.0"]
                 [hiccup "1.0.5"]
                 [jcf/dorothy "0.0.7-SNAPSHOT"]]
  :profiles {:dev {:source-paths ["dev"]
                   :resource-paths ["dev-resources"]
                   :dependencies [[com.stuartsierra/component "0.2.3"]
                                  [reloaded.repl "0.2.1"]
                                  [org.clojure/clojure "1.8.0"]
                                  [com.datomic/datomic-free "0.9.5404"]]}})
