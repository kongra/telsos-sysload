(ns telsos.sysload-test
  (:require
   [clojure.test :refer :all]
   [telsos.sysload :as sysload :refer [analyze-system-sources ensure-state-ns! loadtime namespace-names-ordered set-loadtime! set-loadtime-current! state-atom]]))

(deftest test-state-namespace
  (testing "ensure-state-ns!"
    (is (instance? clojure.lang.Namespace (ensure-state-ns!))))

  (testing "state-atom"
    (is (instance? clojure.lang.Atom (state-atom))))

  (testing "loadtime"
    (is (integer? (loadtime)))
    (is (= {:loadtime 100} (set-loadtime! 100)))
    (set-loadtime-current!)
    (is (integer? (loadtime)))))

(deftest test-analyze-system-sources
  (testing "analyze-system-sources"
    (let [{:keys [source-files
                  namespace-names
                  namespace-name->source-file
                  namespaces-graph]}
          (analyze-system-sources ["src/" "test/"])]

      (is (sequential?         source-files))
      (is (sequential?      namespace-names))
      (is (map? namespace-name->source-file))

      (is (instance? clojure.tools.namespace.dependency.MapDependencyGraph
                     namespaces-graph)))))

(deftest test-namespace-names-ordered
  (testing "namespace-names-ordered"
    (let [{:keys [namespaces-graph
                  namespace-names]} (analyze-system-sources ["src/" "test/"])

          namespace-names-1
          (namespace-names-ordered namespaces-graph namespace-names)]

      (is (seq namespace-names))
      (is (seq namespace-names-1))

      (is (= (set namespace-names)
             (set namespace-names-1))))))
