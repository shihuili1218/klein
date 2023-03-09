(require 'cemerick.pomegranate.aether)
(cemerick.pomegranate.aether/register-wagon-factory!
 "http" #(org.apache.maven.wagon.providers.http.HttpWagon.))

(defproject jepsen.klein "0.1.0-SNAPSHOT"
  :description "klein cache jepsen test"
  :url ""
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :main jepsen.klein
  :jvm-opts ["-Xms2g" "-Xmx2g" "-server"]
  :dependencies [[org.clojure/clojure "1.11.1"]
                 [org.clojure/data.fressian "1.0.0"]
                 [org.clojure/tools.logging "1.2.4"]
                 [org.clojure/tools.cli "1.0.214"]
                 [spootnik/unilog "0.7.31"]
                 [elle "0.1.6"]
                 [clj-time "0.15.2"]
                 [io.jepsen/history "0.1.0"]
                 [jepsen.txn "0.1.2"]
                 [knossos "0.3.9"]
                 [clj-ssh "0.5.14"]
                 [gnuplot "0.1.3"]
                 [http-kit "2.6.0"]
                 [ring "1.9.6"]
                 [com.hierynomus/sshj "0.34.0"]
                 [com.jcraft/jsch.agentproxy.connector-factory "0.0.9"]
                 [com.jcraft/jsch.agentproxy.sshj "0.0.9"
                  :exclusions [net.schmizz/sshj]]
                 [org.bouncycastle/bcprov-jdk15on "1.70"]
                 [hiccup "1.0.5"]
                 [metametadata/multiset "0.1.1"]
                 [byte-streams "0.2.5-alpha2"]
                 [slingshot "0.12.2"]
                 [org.clojure/data.codec "0.1.1"]
                 [fipp "0.6.26"]
                 [jepsen "0.3.1"]
                 [clj-ssh "0.5.14"]
                 [cider/cider-nrepl "0.17.0-SNAPSHOT"]
                 [org.clojure/tools.nrepl "0.2.13" :exclusions [org.clojure/clojure]]
                 [net.java.dev.jna/jna "4.5.1"]
                 [com.ofcoder.klein.jepsen.server/klein-jepsen-server "0.0.1"]
                 ]
  )
