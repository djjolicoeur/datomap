(ns leiningen.datomap
  (:require [leiningen.core.eval :refer (eval-in-project)]))



(defn datomap
  "This should not be loaded when lein runs."
  [project & args]
  (eval-in-project project
                   `(datomap.plugin/plugin ~args)
                   '(require 'datomap.plugin)))
