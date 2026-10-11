;; Arbace's own (not converted from Clojure): the call site of a keyword invoke, (:k target),
;; which the Clojure compiler emits as invokedynamic (JSR 292) in place of Clojure's
;; KeywordLookupSite and the __site__N/__thunk__N static fields of the fn class. See
;; doc/VENDOR-NOTES.md.
;;
;; Its value is always (get target :k), as with Keyword.invoke. Like KeywordLookupSite it
;; caches, per class of target seen: the IKeywordLookup thunk of a record (its field read), the
;; ILookup valAt, or else RT.get. The cache is an inline cache of class guards
;; (MethodHandles.guardWithTest), at most MAX_CLASSES deep; at the next class the site becomes
;; RT.get, unguarded, for good. A nil target is never cached.
;;
;; The site links only once it has been called WARM_CALLS times, calling RT.get until then. So
;; the method handles, and the LambdaForm classes the JDK spins for them, are made only for
;; sites that run often; most sites run a few times at a launch, if at all. For the same reason
;; the call site is a plain MutableCallSite made with its target (this object's fallback), and
;; the method handles of linking are made on first use (KeywordInvokeSite$Handles).

(in-ns 'arbace.lang)

(import '(java.lang.invoke CallSite
                           MethodHandle
                           MethodHandles
                           MethodHandles$Lookup
                           MethodType
                           MutableCallSite))

(defclass ^:public ^:final KeywordInvokeSite
  (field ^:private ^:static ^:final ^int MAX_CLASSES 4)

  (field ^:private ^:static ^:final ^int WARM_CALLS 256)

  (field ^:static ^:final ^MethodType TYPE (MethodType/methodType Object Object))

  (field ^:private ^:static ^:final ^MethodHandle FALLBACK)

  (static-initializer
    (try
      (set! FALLBACK (.findVirtual (MethodHandles/lookup) KeywordInvokeSite "fallback" TYPE))
      (catch ReflectiveOperationException e (throw (ExceptionInInitializerError. e)))))

  ;; the method handles of linking
  (defclass ^:static ^:final Handles
    (field ^:static ^:final ^MethodHandle SAME_CLASS)

    (field ^:static ^:final ^MethodHandle THUNK_GET)

    (field ^:static ^:final ^MethodHandle VAL_AT)

    (field ^:static ^:final ^MethodHandle RT_GET)

    (static-initializer
      (let [l (MethodHandles/lookup)]
        (try
          (set! SAME_CLASS
                (.findStatic l
                             KeywordInvokeSite
                             "sameClass"
                             (MethodType/methodType Boolean/TYPE Class Object)))
          (set! THUNK_GET (.findVirtual l ILookupThunk "get" TYPE))
          (set! VAL_AT (.findVirtual l ILookup "valAt" TYPE))
          (set! RT_GET (.findStatic l RT "get" (MethodType/methodType Object Object Object)))
          (catch ReflectiveOperationException e (throw (ExceptionInInitializerError. e)))))))

  (field ^:private ^:final ^Keyword k)

  (field ^:private ^MutableCallSite site)

  ;; the number of calls of fallback (racy: only a threshold), and of class guards installed
  (field ^:private ^int calls)

  (field ^:private ^int classes)

  (constructor ^:private [this ^Keyword k] (set! (.-k this) k))

  (method ^:private ^:static callSite ^CallSite [^Keyword k]
    (let [s (KeywordInvokeSite. k)
          cs (MutableCallSite. (.bindTo FALLBACK s))]
      (set! (.-site s) cs)
      cs))

  ;; The bootstrap methods, for :name and for :ns/name. They are typed as the JVM invokes a
  ;; bootstrap method with one or two static arguments (java.lang.invoke.BootstrapMethodInvoker),
  ;; so no adaptation is made per class.
  (method ^:public ^:static bootstrap [^MethodHandles$Lookup lookup ^String name ^MethodType type
                                       kname]
    (KeywordInvokeSite/callSite (RT/keyword nil (cast String kname))))

  (method ^:public ^:static bootstrap [^MethodHandles$Lookup lookup ^String name ^MethodType type
                                       kns kname]
    (KeywordInvokeSite/callSite (RT/keyword (cast String kns) (cast String kname))))

  (method ^:private ^:static sameClass ^boolean [^Class c o]
    (and (some? o) (identical? (.getClass o) c)))

  ;; the target of the site until it is linked for target's class
  (method ^:private fallback [this target]
    (if (or (nil? target) (< calls WARM_CALLS))
        (do (set! calls (unchecked-inc-int calls)) (RT/get target k))
        (cond
          (instance? IKeywordLookup target)
            (let [t (.getLookupThunk (cast IKeywordLookup target) k)]
              (if (some? t)
                  (do
                    (.link this (.getClass target) (.bindTo Handles/THUNK_GET t))
                    (.get t target))
                  (do (.link this (.getClass target) (.valAt this)) (.valAt (cast ILookup target) k))))
          (instance? ILookup target)
            (do (.link this (.getClass target) (.valAt this)) (.valAt (cast ILookup target) k))
          :else (do (.link this (.getClass target) (.generic this)) (RT/get target k)))))

  ;; (.valAt target k), typed (Object)Object
  (method ^:private valAt ^MethodHandle [this]
    (.asType (MethodHandles/insertArguments Handles/VAL_AT 1 (new Object/1 [k]))
             TYPE))

  ;; (get target k): RT.get with k bound
  (method ^:private generic ^MethodHandle [this]
    (MethodHandles/insertArguments Handles/RT_GET 1 (new Object/1 [k])))

  (method ^:private ^:synchronized link ^void [this ^Class c ^MethodHandle h]
    (if (< classes MAX_CLASSES)
        (do
          (set! classes (unchecked-inc-int classes))
          (.setTarget site
                      (MethodHandles/guardWithTest
                        (MethodHandles/insertArguments Handles/SAME_CLASS
                                                       0
                                                       (new Object/1 [c]))
                        h
                        (.getTarget site))))
        (.setTarget site (.generic this)))))
