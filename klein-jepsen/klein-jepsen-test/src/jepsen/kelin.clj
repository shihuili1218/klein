(ns jepsen.kelin
  (:import [com.ofcoder.klein.jepsen.server JepsenClient])
  (:require [clojure.tools.cli :refer [parse-opts]])
  (:require [clojure.tools.logging :refer :all]
            [clojure.string :as cstr]
            [jepsen.checker.timeline :as timeline]
            [jepsen
             [nemesis :as nemesis]
             [checker :as checker]
             [independent :as independent]
             [cli :as cli]
             [client :as client]
             [control :as c]
             [db :as db]
             [generator :as gen]
             [tests :as tests]]
            [knossos.model :as model]
            [jepsen.control.util :as cu]
            [jepsen.os :as os]))

(defn r   [_ _] {:type :invoke, :f :read, :value nil})
(defn w   [_ _] {:type :invoke, :f :write, :value (rand-int 5)})

(def fn-opts [[nil "--testfn TEST" "Test function name."]])

(defn- parse-long [s] (Long/parseLong s))
(defn- parse-boolean [s] (Boolean/parseBoolean s))

(def cli-opts
  "Additional command line options."
  [["-s"
    "--slots SLOTS"
    "Number of atomic server ranges."
    :default  1
    :parse-fn parse-long
    :validate [pos? "Must be a positive number"]]
   ["-q"
    "--quorum BOOL"
    "Whether to read from quorum."
    :default  false
    :parse-fn parse-boolean
    :validate [boolean? "Must be a boolean value."]]
   ["-r"
    "--rate HZ"
    "Approximate number of requests per second, per thread."
    :default  10
    :parse-fn read-string
    :validate [#(and (number? %) (pos? %)) "Must be a positive number"]]
   [nil
    "--ops-per-key NUM"
    "Maximum number of operations on any given key."
    :default  100
    :parse-fn parse-long
    :validate [pos? "Must be a positive integer."]]])

(defn- create-client0 [test] (doto (JepsenClient. "1:172.22.0.79:1218;2:172.22.0.80:1218;3:172.22.0.90:1218;4:172.22.0.91:1218;5:172.22.0.96:1218")))
(def create-client (memoize create-client0))

(defrecord Client [conn]
  client/Client
  (open! [this test node]
    (-> this
        (assoc :node node)
        (assoc  :conn (create-client test))))

  (setup! [this test])

  (invoke! [_ test op])

  (teardown! [this test])

  (close! [_ test]))

(defn -main
  "Handles command line arguments. Can either run a test, or a web server for
  browsing results."
  [& args]
  (ns jepsen.kelin)
  (println "Hello, klein")
  )
