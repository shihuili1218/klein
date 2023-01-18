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

(def fn-opts [[nil "--testfn TEST" "Test function name."]])

(defn- parse-long [s] (Long/parseLong s))
(defn- parse-boolean [s] (Boolean/parseBoolean s))

(def cli-opts
  "Additional command line options."
  [["-s"
    "--slots SLOTS"
    "Number of klein server ranges."
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

;;DB
(defn db
  "klein DB for a particular version."
  [version]
  (reify
   db/DB
   (setup! [_ test node]
           (info node "installing klein" version))

   (teardown! [_ test node]
              (info node "tearing down klein"))))

;client
(defn r   [_ _] {:type :invoke, :f :read, :value nil})
(defn w   [_ _] {:type :invoke, :f :write, :value (rand-int 5)})

(defn- create-client0 [test]
  (doto
   (JepsenClient. "1:172.22.0.79:1218;2:172.22.0.80:1218;3:172.22.0.90:1218;4:172.22.0.91:1218;5:172.22.0.96:1218")))

(def create-client (memoize create-client0))

(defn- write
  "write a key/value to klein server"
  [client key value]
  (when-not
    (-> client
        :conn
        (.put key value))
    (throw (ex-info "Fail to set" {:key key :value value}))))

(defrecord Client [conn]
  (reify client/Client
    (open! [this test node]
           (-> this
               (assoc :node node)
               (assoc :conn (create-client test))))

    (setup! [this test])

    (invoke! [this test op]
             (let [[kk v] (:value op)
                   k      (str kk)
                   crash  (if (= :read (:f op)) :fail :info)]
               (try
                 (case (:f op)
                   :write (do
                            (write this k v)
                            (assoc op :type :ok)))
                 (catch Exception e
                   (let [^String msg (.getMessage e)]
                     (cond
                       (and msg (.contains msg "TIMEOUT")) (assoc op :type crash, :error :timeout)
                       :else
                       (assoc op :type crash :error (.getMessage e))))))))

    (teardown! [this test])

    (close! [_ test])))

(defn klein-test
  "Given an options map from the command line runner (e.g. :nodes, :ssh,
  :concurrency, ...), constructs a test map."
  [opts]
  (merge tests/noop-test
         {:pure-generators true
          :name            "klein"
          :db              (db "0.0.1")
          :client          (Client nil)
          :generator       (->> (gen/mix [r w])
                                (gen/stagger 1)
                                (gen/nemesis nil)
                                (gen/time-limit 15))}
         opts))

(defn -main
  "Handles command line arguments. Can either run a test, or a web server for
  browsing results."
  [& args]
  (info "hello klein")
  (cli/run!
   (merge (cli/single-test-cmd {:test-fn klein-test})
          (cli/serve-cmd))
   args))

