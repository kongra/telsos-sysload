(ns build
  (:refer-clojure :exclude [compile])
  (:require [clojure.tools.build.api :as build-api]))

(def basis   (build-api/create-basis {:project "deps.edn"}))
(def lib     'kongra/telsos-sysload)
(def version (format "0.1.%s" (build-api/git-count-revs nil)))

;; SRC/RESOURCES
(def src-dirs           ["src/"])
(def resources-dirs     ["resources/"])
(def src+resources-dirs ["src/" "resources/"])

;; TARGET(S)
(def build-folder                 "target/")
(def class-dir (str build-folder "classes/"))
(def jar-file  (format "target/%s-%s.jar" (name lib) version))

;; TASKS
(defn- prs [x] (with-out-str (pr x)))

(defn clean [_]
  (println "deleting" (prs build-folder))
  (time
   (build-api/delete {:path build-folder})))

(defn compile-clj [_]
  (println "compiling" (prs src-dirs) "into" (prs class-dir))
  (time
   (build-api/compile-clj
    {:basis     basis
     :src-dirs  src-dirs
     :java-opts ["-XX:+UseStringDeduplication"
                 "-Dclojure.compiler.direct-linking=true"
                 "-Dclojure.warn.on.reflection=false"
                 "-Dclojure.assert=true"]

     :class-dir class-dir})))

(defn jar [_]
  (println "writing pom for" lib version)
  (time
   (build-api/write-pom
    {:class-dir class-dir
     :lib       lib
     :version   version
     :basis     basis
     :src-dirs  src-dirs}))

  (println "copying" (prs src+resources-dirs) "to" (prs class-dir))
  (time
   (build-api/copy-dir
    {:src-dirs   src+resources-dirs
     :target-dir class-dir}))

  (println "creating" jar-file)
  (time
   (build-api/jar
    {:class-dir class-dir
     :jar-file  jar-file})))

(defn install [_]
  (build-api/install
   {:basis     basis
    :class-dir class-dir
    :jar-file  jar-file
    :lib       lib
    :version   version})

  (println "installed" jar-file))
