(ns leiningen.datomap
  (:require [leiningen.core.eval :refer (eval-in-project)]))


(defn datomap
  [project & args]
  (eval-in-project
   project
   `(datomap.plugin/plugin ~args)
   '(require 'datomap.plugin)))
