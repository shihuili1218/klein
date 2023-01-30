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

(defonce klein-stop "stop.sh")
(defonce klein-start "start.sh")
(defonce klein-path "/root")

(defn- parse-long [s] (Long/parseLong s))
(defn- parse-boolean [s] (Boolean/parseBoolean s))

;;DB
(defn start! [node]
  (info "Start" node)

  (c/cd (clojure.string/join "/" [klein-path ""])
        (c/exec :sh klein-start)))

(defn stop! [node]
  (info "Stop" node)
  (c/cd (clojure.string/join "/" [klein-path ""])
        (c/exec :sh klein-stop)))

(defn db
  "klein DB for a particular version."
  [version]
  (reify
   db/DB
   (setup! [_ test node]
           (start! node)
           (Thread/sleep 15000))

   (teardown! [_ test node]
              (stop! node)
              (Thread/sleep 10000))))

;client
(defn r [_ _] {:type :invoke, :f :read, :value nil})
(defn w [_ _] {:type :invoke, :f :write, :value (rand-int 150)})

(defn- create-client0 [test]
  (doto
   (JepsenClient. test)))

(def create-client (memoize create-client0))

(defn- write
  "write a key/value to klein server"
  [client value]
  (info "write result: " (-> client :conn (.put value))))

(defn- read
  "read value by key from klein server"
  [client]
  (doto (-> client :conn (.get))))

(defrecord Client [conn]
  client/Client
  (open! [this test node]
    (-> this
        (assoc :conn (create-client node))))
  (setup! [this test])
  (invoke! [this test op]
    (try
      (case (:f op)
        :read  (assoc op :type :ok, :value (read this))
        :write (do
                 (write this (:value op))
                 (assoc op :type :ok)))
      (catch Exception e
        (let [^String msg (.getMessage e)]
          (cond
            (and msg (.contains msg "TIMEOUT")) (assoc op :type :fail, :error :timeout)
            :else
            (assoc op :type :fail :error (.getMessage e)))))))

  (teardown! [this test])

  (close! [_ test]))

(defn klein-test
  "Given an options map from the command line runner (e.g. :nodes, :ssh, :concurrency, ...), constructs a test map."
  [opts]
  (info "opts: " opts)
  (merge tests/noop-test
         {:pure-generators true
          :name            "klein"
          :os              os/noop
          :db              (db "0.0.1")
          :client          (Client. nil)
          :nemesis         (nemesis/partition-random-halves)
          :model           (model/register 0)
          :checker         (checker/compose
                            {:perf     (checker/perf)
                             :timeline (timeline/html)
                             :linear   (checker/linearizable)})
          :generator       (->> (gen/mix [r w])
                                (gen/stagger 1)
                                (gen/nemesis
                                 (gen/seq
                                  (cycle
                                   [(gen/sleep 5)
                                    {:type :info, :f :start}
                                    (gen/sleep 5)
                                    {:type :info, :f :stop}])))
                                (gen/time-limit (:time-limit opts)))}
         opts))

(defn -main
  "Handles command line arguments. Can either run a test, or a web server for
  browsing results."
  [& args]
  (info "hello klein: " args)
  (cli/run!
   (merge
    (cli/single-test-cmd
     {:test-fn klein-test})
    (cli/serve-cmd))
   args))

;d:/lein.bat run test --time-limit 40 --concurrency 10 --test-count 10 --nodes 1:172.22.0.79:1218,2:172.22.0.80:1218,3:172.22.0.90:1218,4:172.22.0.91:1218,5:172.22.0.96:1218 --username root --password 123456
;d:/lein.bat run test --time-limit 40 --concurrency 10 --test-count 10 --username root --password 123456
