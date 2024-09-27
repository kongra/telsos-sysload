(defproject telsos-sysload "0.1.0-SNAPSHOT"
  :description "Systems analysis and loading facility"
  :url "https://github.com/kongra/telsos-sysload"
  :license
  {:name "EPL-2.0 OR GPL-2.0-or-later WITH Classpath-exception-2.0"
   :url  "https://www.eclipse.org/legal/epl-2.0/"}

  :dependencies
  [[org.clojure/clojure         "1.12.0"]
   [org.clojure/tools.namespace  "1.5.0"]]

  :repl-options
  {:init-ns telsos-sysload.core}

  :aot :all

  :global-vars
  {*warn-on-reflection* false
   *print-length*       500})
