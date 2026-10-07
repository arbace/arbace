;; The training workload of Arbace's JDK AOT cache (bin/build-arbace runs it as
;; `bin/arbace --aot-train < test/aot-training.clj`, which makes target/arbace.aot): a REPL
;; session over what a launch commonly touches, the common libraries, the class forms compiler,
;; deftype, defrecord, protocols, arbace.test, futures and agents, doc and source. The classes
;; it loads from the jar, and the method profiles it gathers, go into the cache.
(require '[arbace.string :as str] '[arbace.set :as set] '[arbace.walk :as walk]
         '[arbace.pprint :as pp] '[arbace.edn :as edn] '[arbace.java.io :as io]
         '[arbace.test :as t] '[arbace.data :as data] '[arbace.zip :as zip])
(def m (into {} (map (fn [i] [(keyword (str "k" i)) (vec (range i))]) (range 50))))
(pp/pprint (select-keys m [:k1 :k2 :k3]))
(str/join "," (map str (range 10)))
(set/union #{1 2} #{3})
(walk/postwalk identity {:a [1 2]})
(data/diff {:a 1} {:a 2})
(zip/root (zip/vector-zip [1 [2]]))
(defclass ^:public TrainingPing
  (method ^:public ^:static ping ^int [^int n] (if (== n 0) 0 (unchecked-dec-int n))))
(TrainingPing/ping 3)
(defrecord TrainingR [a b])
(deftype TrainingT [x])
(defprotocol TrainingP (tp [x]))
(extend-protocol TrainingP Object (tp [x] x))
(tp (->TrainingR 1 2))
(t/deftest training-test (t/is (= 1 1)))
(t/run-tests)
(edn/read-string "{:a [1 2 #{3} \"s\"]}")
(doc map)
(arbace.repl/source-fn 'map)
@(future (+ 1 1))
(reduce + (pmap inc (range 100)))
(let [a (agent 0)] (send a inc) (await a) @a)
(try (throw (ex-info "training" {})) (catch Exception e (ex-message e)))
(with-out-str (prn (range 3) "s" 1.5 \c))
(sort-by :a [{:a 2} {:a 1}])
(frequencies "abracadabra")
;; reflective calls (arbace.lang.ReflectorCallSite), each site called thrice so it links: an
;; instance method, overloaded and not, a no-argument member, a field, a static method, a
;; constructor
(let [f (fn [s x t] [(.indexOf s x) (.substring s (count x)) (.length s) (Math/abs (count x))
                     (java.util.ArrayList. (count x)) (.-x t)])]
  (dotimes [_ 3] (f "abc" "b" (TrainingT. 1)) (f (StringBuilder. "abc") "b" (TrainingT. 2))))
