(ns telsos.sysload.human-readable)

(set! *warn-on-reflection*       true)
(set! *unchecked-math* :warn-on-boxed)

(defn human-readable
  [postfixes divisor x digits]
  (assert (nat-int? digits))

  (let [divisor (double             divisor)
        n       (alength ^objects postfixes)
        [index x]
        (loop [index 0 x (double x)]
          (if (or (= index n) (< x divisor))
            [index x]

            (recur (unchecked-inc index) (/ x divisor))))

        postfix (aget ^objects postfixes (long index))

        value
        (format (str "%."  digits "f") x)]

    (str value " " postfix)))

(def ^:private BYTES-MULTIPLIES-POSTFIXES
  (object-array ["byte(s)" "KB" "MB" "GB" "TB" "PB" "EB" "ZB" "YB"]))

(defn human-readable-bytes
  [b digits]
  (human-readable BYTES-MULTIPLIES-POSTFIXES 1024 b digits))

(def ^:private NANOSECS-MULTIPLIES-POSTFIXES
  (into-array ["ns" "µs" "ms" "s"]))

(defn human-readable-nanosecs
  [nanosecs digits]
  (human-readable NANOSECS-MULTIPLIES-POSTFIXES 1000 nanosecs digits))

(defn human-readable-msecs
  [msecs digits]
  (human-readable NANOSECS-MULTIPLIES-POSTFIXES 1000
                  (* 1000000 (double msecs)) digits))
