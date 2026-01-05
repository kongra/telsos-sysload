(ns build
  (:refer-clojure :exclude [compile])
  (:require
   [clojure.tools.build.api :as build-api]))

(def lib     'com.github.kongra/telsos-sysload)
(def basis   (build-api/create-basis {:project "deps.edn"}))
(def version (format "0.1.%s" (build-api/git-count-revs nil)))

;; SRC/RESOURCES
(def src-dirs           ["src/"])
(def src+resources-dirs ["src/" "resources/"])

;; TARGET(S)
(def target-dir  "target/")
(def classes-dir "target/classes/")

(def jar-file  (format "target/%s-%s.jar" (name lib) version))

;; MAVEN
(def pom-data
  [[:licenses
    [:license
     [:name "Eclipse Public License 2.0"]
     [:url "https://www.eclipse.org/legal/epl-2.0/"]
     [:distribution "repo"]
     [:comments "Eclipse Public License Version 2.0"]]]])

;; TASKS
(defn- prs [x] (with-out-str (pr x)))

(defn clean [_]
  (println "deleting" (prs target-dir))
  (time (build-api/delete {:path target-dir})))

(defn compile-clj [_]
  (println "compiling" (prs src-dirs) "into" (prs classes-dir))
  (time
    (build-api/compile-clj
      {:basis     basis
       :src-dirs  src-dirs
       :java-opts ["-XX:+UseStringDeduplication"
                   "-Dclojure.compiler.direct-linking=true"
                   "-Dclojure.assert=true"]

       :class-dir classes-dir})))

(defn jar [_]
  (println "writing pom for" lib version)
  (time
    (build-api/write-pom
      {:class-dir classes-dir
       :lib       lib
       :version   version
       :basis     basis
       :src-dirs  src-dirs
       :pom-data  pom-data}))

  (println "copying" (prs src+resources-dirs) "to" (prs classes-dir))
  (time
    (build-api/copy-dir
      {:src-dirs   src+resources-dirs
       :target-dir classes-dir}))

  (println "creating" jar-file)
  (time
    (build-api/jar
      {:class-dir classes-dir
       :jar-file  jar-file})))

(defn install [_]
  (build-api/install
    {:basis     basis
     :class-dir classes-dir
     :jar-file  jar-file
     :lib       lib
     :version   version})

  (println "installed" jar-file))
