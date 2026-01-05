(ns telsos.sysload-test
  (:require
   [clojure.test :refer [deftest is testing]]
   [telsos.sysload :as sysload])
  (:import
   (clojure.lang PersistentVector)))

(set! *warn-on-reflection*       true)
(set! *unchecked-math* :warn-on-boxed)

(deftest test-state-namespace
  (testing "ensure-state-ns!"
    (is (instance? clojure.lang.Namespace (sysload/ensure-state-ns!))))

  (testing "state-atom"
    (is (instance? clojure.lang.Atom (sysload/state-atom))))

  (testing "loadtime"
    (is (integer? (sysload/loadtime)))
    (is (= {:loadtime 100} (sysload/set-loadtime! 100)))
    (sysload/set-loadtime-current!)
    (is (integer? (sysload/loadtime)))))

(deftest test-analyze-system-sources
  (testing "analyze-system-sources with valid directories"
    (let [{:keys [source-files
                  namespace-names
                  namespace-name->source-file
                  namespaces-graph]}
          (sysload/analyze-system-sources ["src/" "test/"])]

      (is (sequential? source-files))
      (is (sequential? namespace-names))
      (is (map? namespace-name->source-file))

      (is (instance? clojure.tools.namespace.dependency.MapDependencyGraph
                     namespaces-graph))

      ;; Verify we found this test namespace
      (is (contains? (set namespace-names) 'telsos.sysload-test))
      (is (contains? (set namespace-names) 'telsos.sysload))

      ;; Verify namespace-to-file mapping works
      (is (some? (namespace-name->source-file 'telsos.sysload-test)))))

  (testing "analyze-system-sources with single directory"
    (let [{:keys [namespace-names]} (sysload/analyze-system-sources ["src/"])]
      (is (seq namespace-names))
      (is (contains? (set namespace-names) 'telsos.sysload))
      ;; Should not contain test namespaces
      (is (not (contains? (set namespace-names) 'telsos.sysload-test))))))

(deftest test-namespace-names-ordered
  (testing "namespace-names-ordered preserves all namespaces"
    (let [{:keys [namespaces-graph
                  namespace-names]} (sysload/analyze-system-sources ["src/" "test/"])

          namespace-names-ordered-result
          (sysload/namespace-names-ordered namespaces-graph namespace-names)]

      (is (seq namespace-names))
      (is (seq namespace-names-ordered-result))

      ;; Same set of namespaces, just reordered
      (is (= (set namespace-names)
             (set namespace-names-ordered-result)))))

  (testing "namespace-names-ordered respects dependencies"
    (let [{:keys [namespaces-graph
                  namespace-names]}
          (sysload/analyze-system-sources ["src/" "test/"])

          ordered
          (vec (sysload/namespace-names-ordered namespaces-graph namespace-names))

          sysload-idx
          (PersistentVector/.indexOf ordered 'telsos.sysload)

          human-readable-idx
          (PersistentVector/.indexOf ordered 'telsos.sysload.human-readable)]

      ;; telsos.sysload depends on telsos.sysload.human-readable
      ;; so human-readable should come before sysload
      (when (and (>= sysload-idx 0) (>= human-readable-idx 0))
        (is (< human-readable-idx sysload-idx)
            "Dependencies should be loaded before dependents")))))

(deftest test-loadtime-operations
  (testing "loadtime is updated by set-loadtime-current!"
    (let [before (System/currentTimeMillis)]
      (Thread/sleep 10) ; Ensure time has passed
      (sysload/set-loadtime-current!)
      (let [current-loadtime (sysload/loadtime)]
        (is (>= current-loadtime before)
            "Loadtime should be greater than or equal to before timestamp"))))

  (testing "set-loadtime! accepts specific timestamp"
    (let [test-time 1234567890]
      (sysload/set-loadtime! test-time)
      (is (= test-time (sysload/loadtime)))))

  (testing "loadtime returns long"
    (is (instance? Long (sysload/loadtime)))))

(deftest test-room-and-gc
  (testing "room can be called"
    ;; Just verify it doesn't throw
    (is (nil? (sysload/room))))

  (testing "gc can be called"
    ;; Just verify it doesn't throw
    (is (nil? (sysload/gc)))))

(deftest test-boot-and-synch-types
  (testing "boot is both derefable and callable"
    (is (some? sysload/boot))
    (is (instance? clojure.lang.IDeref sysload/boot))
    (is (instance? clojure.lang.IFn sysload/boot))
    (is (ifn? sysload/boot)))

  (testing "synch is both derefable and callable"
    (is (some? sysload/synch))
    (is (instance? clojure.lang.IDeref sysload/synch))
    (is (instance? clojure.lang.IFn sysload/synch))
    (is (ifn? sysload/synch))))
