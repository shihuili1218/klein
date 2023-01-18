(require 'cemerick.pomegranate.aether)
(cemerick.pomegranate.aether/register-wagon-factory!
 "http" #(org.apache.maven.wagon.providers.http.HttpWagon.))

(defproject jepsen.atomic "0.1.0-SNAPSHOT"
  :description "klein cache jepsen test"
  :url ""
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :main klein.core
  :jvm-opts ["-Xms6g" "-Xmx6g" "-server" "-XX:+UseG1GC"]
  :dependencies [[org.clojure/clojure "1.9.0"]
                 [jepsen "0.1.8"]
                 [clj-ssh "0.5.14"]
                 [cider/cider-nrepl "0.17.0-SNAPSHOT"]
                 [org.clojure/tools.nrepl "0.2.13" :exclusions [org.clojure/clojure]]
                 [net.java.dev.jna/jna "4.5.1"]
                 ]
  )
