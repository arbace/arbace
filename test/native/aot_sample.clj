(ns native.aot-sample
  "Compiled ahead of time by native.aot-test.")

(defclass ^:public Pair
  (field ^:public ^:final a)
  (field ^:public ^:final b)
  (constructor ^:public [this a b] (set! (.-a this) a) (set! (.-b this) b)))

(do
  (defclass ^:public Ping (method ^:public ^:static ping ^int [^int n] (if (== n 0) 0 (Pong/pong (unchecked-dec-int n)))))
  (defclass ^:public Pong (method ^:public ^:static pong ^int [^int n] (if (== n 0) 1 (Ping/ping (unchecked-dec-int n))))))

(defn kind [n] (switch (int n) 0 :zero (1 2 3) :small :big))

(def table (vec (map kind [0 2 9])))

(deftype Box [^long v]
  Object
  (toString [this] (label :l (when (neg? v) (break :l "negative")) (java-str "box" v))))

(defn result []
  [table (.-a (Pair. 1 2)) (Ping/ping 3) (class kind) (.getClassLoader (class kind))
   (str (->Box 3)) (.getClassLoader Box)])
