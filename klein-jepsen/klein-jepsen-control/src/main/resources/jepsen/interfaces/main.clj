(ns jepsen.interfaces
  (:gen-class)
  (:require [clojure.java.io :as io]
            [clojure.core.reducers :as r]
            [clojure.tools.logging :refer :all]
            [knossos.model :as model]
            [knossos.op :as op]
            [jepsen.db :as db]
            [jepsen.cli :as cli]
            [jepsen.checker :as checker]
            [jepsen.client :as client]
            [jepsen.control :as control]
            [jepsen.generator :as gen]
            [jepsen.nemesis :as nemesis]
            [jepsen.tests :as tests]
            [jepsen.os.centos :as centos]
            [jepsen.control.util :as control-util]
            [jepsen.client :as client])
  (:import [com.ofcoder.klein.jepsen.control.jepsen.core Client]
	   [com.ofcoder.klein.jepsen.control.jepsen.core CheckerCallback]
	   [java.util HashMap])
)

(def testName (atom {:header "test"}))
(def userClient (atom {:client nil}))
(def checkers (atom {}))
(def enemy (atom {:nemesis "partition-random-halves"}))
(def database (atom {:db nil}))
(def nemesisOps (atom []))
(def nemesisCallbacks (atom {}))
(def clientOpGapTime (atom {:wait 1}))

(defn clientOp [_ _] 
    (let [op (-> (:client @userClient) (.generateOp))]
        {:type :invoke, :f op, :value (-> (:client @userClient) (.getValue op))})
)

(defn defaultGenerator [opts]
     (->> (gen/mix [clientOp]) ; this operation is just as the name suggests, chosen by the client
			       ; we will leave the operation selection to the user
          (gen/stagger (:wait @clientOpGapTime))
          (gen/nemesis (gen/seq (cycle @nemesisOps)))
          (gen/time-limit (:time-limit opts)))
)

(defn base-nemesis
  [nemesisCallback]
  (reify nemesis/Nemesis
    (setup! [this test] (-> nemesisCallback .setup) this)

    (invoke! [this test op] 
	(-> nemesisCallback (.invokeOp (:f op)))
	(assoc op :value (:f op)))

    (teardown! [this test]
      (->  nemesisCallback .teardown))))

(defn java-client "Method which returns client based on protocol"
  [args]
  (reify client/Client
	 (setup! [_ _] )
	 (open! [_ test node] (java-client (-> (:client @userClient) (.openClient node))))
	 (invoke! [client test op] 
	     (let [result (-> (:client @userClient) (.invokeClient args (:f op) (:value op)))]
	         (if (nil? result)
			(assoc op :type :fail :value 0)
			(assoc op :type :ok :value result) 
		))
	 )
         (teardown! [_ test]
             (-> (:client @userClient) (.teardownClient args))
         )))

(defn db [args] "Helps setup and teardown cluster of database being tested"
  (reify db/DB
         (setup! [_ test node]
                (-> (:db @database) (.setUpDatabase node)))
         (teardown! [_ test node]
                (-> (:db @database) (.teardownDatabase node)))))

(defn determineNemesis [nemesisName] 
      (case nemesisName
        "partition-majorities-ring" (nemesis/partition-majorities-ring)
        "partition-random-halves" (nemesis/partition-random-halves)
	nemesis/noop
      ))

(defn checkerBase [checkerCallback]
  (reify checker/Checker
     (check [_ test history _]
       (-> checkerCallback (.check test history))
       {:valid? true}
     )))

(defn getChecker [checkerName]
  (case checkerName
      "perf" (checker/perf)
      nil
  )
)

(defn baseGenerator [generatorCallback]
  (reify gen/Generator
    (op [generator test process]
	(let [opMap (-> generatorCallback (.generateOp generator test process) .getMap)
	      iter (-> opMap .entrySet .iterator)
	      res (atom {})]
		(while (.hasNext iter)
		    (let [entry (.next iter)
			  k (.getKey entry)
			  v (.getValue entry)]
		        (case k
			    "op-name" (swap! res assoc :f v)
			    "start" (swap! res assoc :f :start)
			    "stop" (swap! res assoc :f :stop)
			    "info" (swap! res assoc :type :info)
			    "invoke" (swap! res assoc :type :invoke)
			    "op-value" (swap! res assoc :value v)
			)   
		))
	     @res
	))))

(defn java-client-test [opts] "Test to be run"
   (merge tests/noop-test
         opts
         {:name (:header @testName)
          :client (java-client nil)
          :db (db nil)
	  :nemesis (nemesis/compose (assoc @nemesisCallbacks #{:start :stop} (determineNemesis (:nemesis @enemy)))) 
          :generator (defaultGenerator opts)
          :checker (checker/compose @checkers)})
)

(defn main [args]
  (cli/run! (merge (cli/single-test-cmd {:test-fn java-client-test})
                   (cli/serve-cmd)) args)
)

(defn setClient [localClient]
  (swap! userClient assoc :client localClient)
)

(defn setTestName [test_name]
  (swap! testName assoc :header test_name)
)

(defn setDatabase [userDb]
  (swap! database assoc :db userDb)
)

(defn setNemesis [chaosInjector]
  (swap! enemy assoc :nemesis chaosInjector)
)

(defn setCheckerCallbacks [callbacks]
  (if (nil? callbacks) (info "Not setting callbacks since provided argument is nil.")
  (let [iter (-> callbacks .keySet .iterator)]
     (while (.hasNext iter)
	(let [k (.next iter)
	      v (-> callbacks (.get k))
	      preImplementedChecker (getChecker k)]
	   (if (nil? preImplementedChecker)
                (swap! checkers assoc k (checkerBase v))
                (swap! checkers assoc k preImplementedChecker)
            )))
)))

(defn setNemesisOps [ops gap]
   (if (nil? ops) (info "Do nothing")
   	(let [iter (-> ops .iterator)]
             (while (.hasNext iter)
        	(let [op (.next iter)]
	(swap! nemesisOps conj (gen/sleep gap))
	(case op
	    "start" (swap! nemesisOps conj {:type :info, :f :start})
	    "stop" (swap! nemesisOps conj {:type :info, :f :stop})
            (swap! nemesisOps conj {:type :info, :f op})
	)
    ))))
)

(defn getOpsList [arrayList]
    (let [iter (.iterator arrayList)
	  convertedList (atom [])]
        (while (.hasNext iter)
	    (let [entry (.next iter)]
		(swap! convertedList conj entry)))
         (set @convertedList)))

(defn setNemesisCallbacks [callbacks]
   (if (nil? callbacks) (info "Doing nothing")
       	(let [iter (-> callbacks .iterator)]
	    (while (.hasNext iter)
	    	(let [entry (.next iter)]
		    (swap! nemesisCallbacks assoc (getOpsList (.getPossibleOps entry)) (base-nemesis entry))))
	)
   )
)

(defn setClientOpWaitTime [wait]
    (swap! clientOpGapTime assoc :wait wait)
)

(defn -main [& args] "Main method from which test is launched and also place from which Java will call this function." 
  (main args)
)
