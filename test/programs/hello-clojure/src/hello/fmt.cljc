(ns hello.fmt
  "A namespace written in Clojure, as a .cljc file (RPS94): bin/arbace-go --build-program
  translates it into .ae with repr/clojure (the vendored Clojure's names renamed, the reader
  conditional resolved for :clj), then compiles it with the rest."
  (:require [clojure.string :as str]))

(def dialect #?(:clj "clj" :cljs "cljs" :default "other"))

(defn banner
  "s in upper case between rules, with the dialect the reader conditional chose."
  [s]
  (str "== " (str/upper-case s) " (" dialect ") =="))
