(defcluster :klein
            :clients
            [{:host "klein-n1" :user "root"}
             {:host "klein-n2" :user "root"}
             {:host "klein-n3" :user "root"}
             {:host "klein-n4" :user "root"}
             {:host "klein-n5" :user "root"}])

(deftask :date "echo date on cluster" [] (ssh "date"))

(deftask :build [] (info "build klein"))

(deftask :deploy []
         (ssh
          (run
           (cd "~"
               (run "java -jar klein-jepsen-server.jar")))))
