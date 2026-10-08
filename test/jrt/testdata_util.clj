(ns jrt.testdata-util
  "The encoding of jrt's test data files (jrt.testdata)."
  (:require [arbace.string :as str])
  (:import [java.io File]))

;; ---------------------------------------------------------------------------------------
;; Encoding

(defn esc
  "A Java string escaped for the test files."
  [^String s]
  (let [sb (StringBuilder.)]
    (dotimes [i (.length s)]
      (let [c (.charAt s i)]
        (cond
          (= c \\) (.append sb "\\\\")
          (= c \tab) (.append sb "\\t")
          (= c \newline) (.append sb "\\n")
          (<= 0x20 (int c) 0x7e) (.append sb c)
          :else (.append sb (format "\\u%04X" (int c))))))
    (.toString sb)))

(defn ex-text [^Throwable e]
  (if-let [m (.getMessage e)]
    (str "!" (.getName (class e)) ": " (esc m))
    (str "!" (.getName (class e)))))

(defmacro result
  "The value of body as a field (strings escaped, numbers and booleans printed), or the
  exception it throws."
  [& body]
  `(try
     (let [v# (do ~@body)]
       (cond
         (string? v#) (esc v#)
         (nil? v#) "null"
         :else (str v#)))
     (catch Throwable e# (ex-text e#))))

(defn line [& fields] (str (str/join "\t" fields) "\n"))

(defn write [^File dir name lines]
  (spit (File. dir ^String name) (apply str lines)))

