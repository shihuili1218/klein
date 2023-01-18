(require 'cemerick.pomegranate.aether)

(cemerick.pomegranate.aether/register-wagon-factory!
 "http" #(org.apache.maven.wagon.providers.http.HttpWagon.))

(defproject atomic-server "0.1.0-SNAPSHOT"
  :description "atomic-server jepsen test"
  :url ""
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :main atomic-server.core
  :plugins [[lein-libdir "0.1.1"]]
  :dependencies [[org.clojure/clojure "1.9.0"]
                 [com.alipay.sofa/hessian "3.3.6"]
                 [com.alipay.sofa/jraft-test "1.2.3"]])
