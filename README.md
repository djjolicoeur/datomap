# datomap

A Clojure library containing schema utilities and visualization tools.

## Usage

### Note!
Datomap only works if the schema is populated with data. Because datomic
schema is defined as attribute definitions, it is impossible to visualize
relations without those relations being present.

Mostly useful from the repl for dealing with the Datomic schema of
an application. Datomp provides two different visualization modes
(tables or nodes), the ability to write the "tables" graph to a file,
and the ability to "dump" a schema to a transactable edn map or write
the schema to a file.


```clojure
user> (require '[datomic.api :as d])
nil
user> (require '[datomap.core :as dmap])
nil
user> (require '[datomap.io :as dmap.io])
nil
user> (def dbval (d/db (:conn (:db system))))
#'user/dbval

;; Dump schema to REPL

user> (def schema (dmap.core/schema->txable dbval))
#'user/schema
user> (pprint schema)
[{:db/id {:part :db.part/db, :idx -71},
  :db/ident :user/email,
  :db/valueType :db.type/string,
  :db/cardinality :db.cardinality/one,
  :db/unique :db.unique/identity}
 {:db/id {:part :db.part/db, :idx -73},
  :db/ident :user/friend,
  :db/valueType :db.type/ref,
  :db/cardinality :db.cardinality/many,
  :db/index true}
 {:db/id {:part :db.part/db, :idx -69},
  :db/ident :user/last-name,
  :db/valueType :db.type/string,
  :db/cardinality :db.cardinality/one}
 {:db/id {:part :db.part/db, :idx -74},
  :db/ident :user/school,
  :db/valueType :db.type/ref,
  :db/cardinality :db.cardinality/many,
  :db/isComponent true}
 {:db/id {:part :db.part/db, :idx -72},
  :db/ident :user/role,
  :db/valueType :db.type/ref,
  :db/cardinality :db.cardinality/many}
 {:db/id {:part :db.part/db, :idx -70},
  :db/ident :user/gender,
  :db/valueType :db.type/ref,
  :db/cardinality :db.cardinality/one}
 {:db/id {:part :db.part/db, :idx -68},
  :db/ident :user/first-name,
  :db/valueType :db.type/string,
  :db/cardinality :db.cardinality/one}
 {:db/id {:part :db.part/db, :idx -80},
  :db/ident :user.school/major,
  :db/valueType :db.type/ref,
  :db/cardinality :db.cardinality/one}
 {:db/id {:part :db.part/db, :idx -75},
  :db/ident :user.school/name,
  :db/valueType :db.type/string,
  :db/cardinality :db.cardinality/one}
 {:db/id {:part :db.part/db, :idx -81},
  :db/ident :user.school/mascot,
  :db/valueType :db.type/string,
  :db/cardinality :db.cardinality/one}]
nil
user>

```

## License

Copyright © 2017 FIXME

Distributed under the Eclipse Public License either version 1.0 or (at
your option) any later version.
