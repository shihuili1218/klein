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
  (try
    (c/cd (clojure.string/join "/" [klein-path ""])
          (c/exec :sh klein-start))
    (catch Exception e
      (info "Start node occur exception " (.getMessage e)))))

(defn stop! [node]
  (info "Stop" node)
  (try
    (c/cd (clojure.string/join "/" [klein-path ""])
          (c/exec :sh klein-stop))
    (catch Exception e
      (info "Stop node occur exception " (.getMessage e)))))

(defn db
  "klein DB for a particular version."
  [version]
  (reify
   db/DB
   (setup! [_ test node]
           (start! node)
           (Thread/sleep 10000))

   (teardown! [_ test node]
              (stop! node)
              (Thread/sleep 5000))))

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

(defn mostly-small-nonempty-subset
  "Returns a subset of the given collection, with a logarithmically decreasing
  probability of selecting more elements. Always selects at least one element.

      (->> #(mostly-small-nonempty-subset [1 2 3 4 5])
           repeatedly
           (map count)
           (take 10000)
           frequencies
           sort)
      ; => ([1 3824] [2 2340] [3 1595] [4 1266] [5 975])"
  [xs]
  (-> xs
      count
      inc
      Math/log
      rand
      Math/exp
      long
      (take (shuffle xs))))

(def crash-nemesis
  "A nemesis that crashes a random subset of nodes."
  (nemesis/node-start-stopper
   mostly-small-nonempty-subset
   (fn start [test node] (stop! node) [:killed node])
   (fn stop [test node] (start! node) [:restarted node])))

(defn recover
  "A generator which stops the nemesis and allows some time for recovery."
  []
  (gen/nemesis
   (gen/phases
    (gen/once {:type :info, :f :stop})
    (gen/sleep 20))))

(defn read-once
  "A generator which reads exactly once."
  []
  (gen/clients
   (gen/once {:type :invoke, :f :read})))

(defn klein-test
  "Given an options map from the command line runner (e.g. :nodes, :ssh, :concurrency, ...), constructs a test map."
  [opts]
  (info "opts: " opts)
  (merge tests/noop-test
         {:name      "klein"
          :os        os/noop
          :db        (db "0.0.1")
          :client    (Client. nil)
          :ssh    {:dummy? true}
          :model     (model/register 0)
          :nemesis   (nemesis/partition-random-halves)
          :checker   (checker/compose
                      {:perf     (checker/perf)
                       :timeline (timeline/html)
                       :linear   (checker/linearizable)})
          :generator (->>
                      (gen/mix [r w])
                      (gen/stagger 1/10)
                      (gen/delay 1/10)
                      (gen/nemesis
                       (gen/seq
                        (cycle
                         [(gen/sleep 10)
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
;lein run test --time-limit 40 --concurrency 10 --test-count 10 --ssh-private-key /root/.ssh/id_rsa
