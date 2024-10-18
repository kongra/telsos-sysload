;; Created 2024-09-27
;; © 2024 Konrad Grzanek <kongra@gmail.com>
(ns telsos.sysload
  (:require
   [clojure.java.io                    :as               io]
   [clojure.set                        :as              set]
   [clojure.tools.namespace.dependency :as          ns-deps]
   [clojure.tools.namespace.file       :as          ns-file]
   [clojure.tools.namespace.find       :as          ns-find]
   [clojure.tools.namespace.parse      :as         ns-parse]
   [hashp.core                         :as       hashp-core]
   [nrepl.middleware                   :as nrepl-middleware])

  (:import (java.lang.management ManagementFactory RuntimeMXBean)))

(set! *warn-on-reflection*       true)
(set! *unchecked-math* :warn-on-boxed)

;; STATE NAMESPACE
(def ^:private STATE-NAMESPACE-SYMBOL 't3ls0s-sysl04d.c0r3)
(def ^:private STATE-ATOM-SYMBOL               'state-atom)

(defn ensure-state-ns!
  []
  (or (find-ns STATE-NAMESPACE-SYMBOL)
      (let [ns-              (create-ns   STATE-NAMESPACE-SYMBOL)
            runtime-mx-bean  (ManagementFactory/getRuntimeMXBean)
            start-time-msecs (RuntimeMXBean/.getStartTime runtime-mx-bean)]

        (intern ns- STATE-ATOM-SYMBOL (atom {:loadtime start-time-msecs}))

        ;; finally we return
        ns-)))

(defn state-atom
  []
  (-> (ensure-state-ns!) (ns-resolve STATE-ATOM-SYMBOL) deref))

(defn loadtime ^long
  []
  (:loadtime @(state-atom)))

(defn set-loadtime!
  [^long msecs]
  (swap! (state-atom) assoc :loadtime msecs))

(defn set-loadtime-current!
  []
  (set-loadtime! (System/currentTimeMillis)))

;; ANALYZING ns DEPENDENCIES IN SRC FOLDERS
(defn analyze-system-sources
  [source-dirs]
  (let [source-files
        (mapcat #(->> % str io/file ns-find/find-sources-in-dir) source-dirs)

        ns-decls
        (map ns-file/read-file-ns-decl source-files)

        namespace-names
        (into [] (map ns-parse/name-from-ns-decl) ns-decls)

        namespace-name?
        (set namespace-names)]

    {:source-files    source-files
     :namespace-names namespace-names

     :namespace-name->source-file
     (zipmap namespace-names source-files)

     :namespaces-graph
     (reduce
      (fn [graph decl]
        (let [nsname (ns-parse/name-from-ns-decl decl)]
          (reduce (fn [graph dep]
                    (if (namespace-name? dep)
                      (ns-deps/depend graph nsname dep)
                      graph))

                  graph (ns-parse/deps-from-ns-decl decl))))

      (ns-deps/graph) ns-decls)}))

;; ORDERING SYSTEM NAMESPACES TO BE (RE)LOADED
(defn- namespace-name-to-include-next?
  [namespaces-graph allowed-dependencies namespace-name]
  (set/subset?
   (set (ns-deps/immediate-dependencies namespaces-graph namespace-name))
   ;; this one ...

   ;; ... has to be a subset of:
   (set allowed-dependencies)))

(defn namespace-names-ordered
  [namespaces-graph namespace-names]
  (loop [results                   []
         allowed-dependencies      #{}
         namespace-names-remaining namespace-names]

    (if-not (seq namespace-names-remaining)
      (apply concat results)

      (let [namespace-names-next
            (filter (partial namespace-name-to-include-next?
                             namespaces-graph
                             allowed-dependencies)
                    namespace-names-remaining)

            namespace-names-next-set
            (set namespace-names-next)]

        (recur #_results
               (conj results (sort namespace-names-next))

               #_allowed-dependencies
               (set/union allowed-dependencies namespace-names-next-set)

               #_namespace-names-remaining
               (remove namespace-names-next-set namespace-names-remaining))))))

;; TIME MEASUREMENT
(defn- swatch-now
  []
  (System/nanoTime))

(defn- elapstr
  [swatch]
  (let [value (* (- (long (swatch-now)) (long swatch)) 0.000001)]
    (format "%.3f msecs" value)))

;; BOOT/SYNC IMPL.
(defn- load-src-file!
  [file]
  (let [file   (str file)
        _      (println "Loading" file "...")
        swatch (swatch-now)]

    (load-file (str file))
    (println "Done in" (elapstr swatch))))

(defn- ensure-file
  [namespace-name->source-file namespace-name]
  (let [file (namespace-name->source-file namespace-name)]
    (when-not file
      (throw (IllegalStateException.
              (str "Source file not found for namespace " namespace-name))))
    file))

(defn- out-of-sync?
  [namespace-name->source-file namespace-name]
  (> (->> (ensure-file namespace-name->source-file namespace-name)
          str io/file (.lastModified))

     (loadtime)))

(def ^:private NS-FINALIZE-SYMBOL 'ns-finalize)

(defn- remove-namespace!
  [namespace-name]
  ;; Let's perform a ns finalization if possible ...
  (when-let [ns- (find-ns namespace-name)]
    (when-let [finalize-ns (ns-resolve ns- NS-FINALIZE-SYMBOL)]
      (finalize-ns))

    ;; ... and the removal itself
    (remove-ns namespace-name)
    (println "Removed namespace" namespace-name)))

(defn- boot-impl!
  ([]
   (boot-impl! ["src/" "test/"]))

  ([source-dirs]
   (let [swatch (swatch-now)

         {:keys [namespace-names
                 namespace-name->source-file
                 namespaces-graph]} (analyze-system-sources source-dirs)

         namespace-names
         (namespace-names-ordered namespaces-graph namespace-names)]

     ;; We remove in reverse order because we think it's good for the consistency of
     ;; depenendent namespaces
     (doseq [namespace-name (reverse namespace-names)]
       (remove-namespace!  namespace-name))

     (doseq [namespace-name namespace-names]
       (load-src-file! (ensure-file namespace-name->source-file namespace-name)))

     (set-loadtime-current!)
     (println "telsos.sysload/boot! finished in" (elapstr swatch)))))

(defn- sync-impl!
  ([]
   (sync-impl! ["src/" "test/"]))

  ([source-dirs]
   (let [swatch (swatch-now)

         {:keys [namespace-names
                 namespace-name->source-file
                 namespaces-graph]} (analyze-system-sources source-dirs)

         namespace-names
         (namespace-names-ordered namespaces-graph namespace-names)

         dirty-namespace-names-set
         (->> namespace-names
              (filter (partial out-of-sync? namespace-name->source-file))
              set)]

     (when (seq dirty-namespace-names-set)
       (let [dirty-namespace-names
             (set/union
              ;; We take the dirty ones, plus ...
              dirty-namespace-names-set

              ;; ... all their dependents
              (ns-deps/transitive-dependents-set
               namespaces-graph
               dirty-namespace-names-set))

             ;; The ordering of the dirties has to be the same as in the original parsed
             ;; namespace-names
             dirty-namespace-names
             (sort-by (zipmap namespace-names (iterate inc 0)) dirty-namespace-names)]

         ;; We remove in reverse order because we think it's good for the consistency of
         ;; depenendent namespaces
         (doseq [namespace-name (reverse dirty-namespace-names)]
           (remove-namespace!  namespace-name))

         (doseq [namespace-name dirty-namespace-names]
           (load-src-file! (ensure-file namespace-name->source-file namespace-name)))))

     (set-loadtime-current!)
     (println "telsos.sysload/sync! finished in" (elapstr swatch)))))

;; BOOT/SYNC FACADE
(deftype ^:private Boot []
  clojure.lang.IDeref
  (deref [_this] (boot-impl!))

  clojure.lang.IFn
  (invoke [_this] (boot-impl!)))

(deftype ^:private Sync []
  clojure.lang.IDeref
  (deref [_this] (sync-impl!))

  clojure.lang.IFn
  (invoke [_this] (sync-impl!)))

(def boot! (Boot.))
(def sync! (Sync.))

;; nREPL MIDDLEWARE TO INTERN boot! AND sync! TO user NAMESPACE ON START
(defn- on-nrepl-start []
  (when-let [user-ns (find-ns 'user)]
    (intern user-ns 'boot! boot!)
    (intern user-ns 'sync! sync!)
    (println "boot! and sync! interned into user")

    #p :hashp-preloaded))

(defn middleware
  [handler]
  (on-nrepl-start)
  handler)

(nrepl-middleware/set-descriptor!
   #'middleware
   {:requires #{}
    :expects  #{}
    :handles  {}})
