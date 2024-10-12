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
  :dependencies [
                  [org.clojure/clojure "1.10.0"]
                  [jepsen "0.1.19"]
                 ; [org.clojure/clojure "1.9.0"]
                 ; [jepsen "0.1.11"]
                 [clj-ssh "0.5.14"]
                 [cider/cider-nrepl "0.17.0-SNAPSHOT"]
                 [org.clojure/tools.nrepl "0.2.13" :exclusions [org.clojure/clojure]]
                 [net.java.dev.jna/jna "4.5.1"]
                 [javax.xml.bind/jaxb-api "2.3.1"]
                 [org.glassfish.jaxb/jaxb-runtime "2.3.1"]
                 [com.ofcoder.klein.jepsen.server/klein-jepsen-server "0.0.1"]
                 ]
  )
