(ns atomic-server.core
  (:gen-class)
  (:import [com.alipay.sofa.jraft.test.atomic.server AtomicServer]
           [com.alipay.sofa.jraft.option CliOptions]
           [com.alipay.sofa.jraft
            JRaftUtils
            CliService
            Status
            RaftServiceFactory]))

(defonce group-id "atomic_0")

(defn- ^CliService get-cli-service []
  (RaftServiceFactory/createAndInitCliService (CliOptions.)))

(defn leave [node conf]
  (let [cli (get-cli-service)]
    (let [^Status status (.removePeer cli
                                      group-id
                                      (JRaftUtils/getConfiguration conf)
                                      (JRaftUtils/getPeerId node))]
      (when-not (.isOk status)
        (println "Leave node failed." {:node node :conf  conf :status status}))
      (System/exit 0))))

(defn join [node conf]
  (let [cli (get-cli-service)]
    (let [^Status status (.addPeer cli
                                      group-id
                                      (JRaftUtils/getConfiguration conf)
                                      (JRaftUtils/getPeerId node))]
      (when-not (.isOk status)
        (println "Join node failed." {:node node :conf  conf :status status}))
      (System/exit 0))))

(defn -main [& [cmd & rest]]
  (when (empty? cmd)
    (throw (ex-info "empty cmd" {:cmd cmd})))
  (case cmd
    "start" (AtomicServer/start (first rest))
    "leave" (leave (first rest) (second rest))
    "join" (join (first rest) (second rest))
    (throw (ex-info "Unknown cmd" {:cmd cmd}))))
