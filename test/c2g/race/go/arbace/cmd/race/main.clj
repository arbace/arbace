(in-ns 'go.arbace.cmd.race)
(go/file "main.go" :imports [[fmt "fmt"] [os "os"] [sync "sync"] [jrt "arbace/jrt"] [lang "arbace/lang"]])

(go/func work
  "work: what each thread does with the shared objects.\n"
  ^int64 [^int g ^lang/IPersistentVector v ^lang/IPersistentMap m ^{:tag (* lang/Atom)} a]
  (let [sum (conv int64 0)]
    (for [i 0] (< i 2000) (inc! i)
      ;; hash caches (_hasheq, _hash) computed by whichever thread comes first
      (set! sum (+ sum (conv int64 (lang/Util_Hasheq_O__I v)) (conv int64 (lang/Util_Hasheq_O__I m))))
      (set! sum (+ sum (conv int64 (jrt/HashCode v))))
      ;; interning: symbols and keywords (a ConcurrentHashMap and weak references)
      (let [k (lang/Keyword_Intern_String__Keyword (jrt/Str (fmt/Sprint "k" (% i 50))))]
        (set! sum (+ sum (conv int64 (.Hasheq__I k)))))
      (let [s (lang/Symbol_Intern_String__Symbol (jrt/Str (fmt/Sprint "s" (% i 50))))]
        (set! sum (+ sum (conv int64 (.Hasheq__I s)))))
      ;; namespaces
      (set! _ (lang/Namespace_FindOrCreate_Symbol__Namespace (lang/Symbol_Intern_String__Symbol (jrt/Str (fmt/Sprint "ns" (% i 5))))))
      ;; an atom: compare-and-set loops
      (while true
        (let [old (.Deref__O a)
              n (.LongValue__J (jrt/Long_Cast old))]
          (when (.CompareAndSet_O_O__Z a old (.Ref (jrt/Long_ValueOf_J__Long (+ n 1))))
            (break))))
      (set! sum (+ sum (conv int64 (lang/RT_NextID__I)))))
    sum))

(go/func main []
  (os/Exit
    (jrt/RunMain
      (fn []
        ;; statics are initialized lazily (C2G-SPEC §6.2): hand-written code runs the guards
        (lang/PersistentVector_Init)
        (lang/PersistentHashMap_Init)
        (let [^lang/IPersistentVector v lang/PersistentVector_EMPTY
              ^lang/IPersistentMap m lang/PersistentHashMap_EMPTY]
          (for [i 0] (< i 100) (inc! i)
            (set! v (.Cons_O__IPersistentVector v (.Ref (jrt/Long_ValueOf_J__Long (conv int64 i)))))
            (set! m (.Assoc_O_O__IPersistentMap m (.Ref (jrt/Long_ValueOf_J__Long (conv int64 i))) (.Ref (lang/Keyword_Intern_String__Keyword (jrt/Str "v"))))))
          (let [a (lang/Atom_New_O (.Ref (jrt/Long_ValueOf_J__Long 0)))
                ^{:tag sync/WaitGroup} wg (zero sync/WaitGroup)]
            (for [g 0] (< g 8) (inc! g)
              (.Add wg 1)
              (let [g g]
                (set! _ (jrt/Go (fmt/Sprint "worker-" g)
                                (fn [] (work g v m a) (.Done wg))))))
            (.Wait wg)
            (fmt/Println "atom" (.LongValue__J (jrt/Long_Cast (.Deref__O a))) "(expected 16000)")))))))
