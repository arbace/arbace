;; c2g's fixtures (see FxCode.clj): VarHandles compiled statically (C2G-SPEC §8.5): handles made
;; in a static initializer and in field initializers by findVarHandle on a lookup (class
;; literals, Integer/TYPE and its kin, a final local bound to a class) and by
;; arrayElementVarHandle (MhUtil, the JDK's other way, is not exported to the fixtures),
;; on volatile and plain fields of every kind (the plain ones get the volatile representation in
;; Go) and on int, long and reference array elements; every access mode, the conversions of
;; arguments and results, the fences, and the exceptions of array handles.

(in-ns 'c2g.fixtures)

(import '(java.lang.invoke MethodHandles VarHandle))

(do

(defclass ^:public FxVhNode
  (field ^:volatile ^Object item)
  (field ^FxVhNode next)
  (constructor [this ^Object item] (set! (.-item this) item)))

(defclass ^:public FxVarHandles
  (field ^:volatile ^int i)
  (field ^long j)
  (field ^:volatile ^boolean z)
  (field ^float f)
  (field ^double d)
  (field ^:volatile ^Object o)
  (field ^String s)
  (field ^FxVhNode head)
  (field ^short sh)
  (field ^char ch)

  (field ^:static ^:final ^VarHandle I)
  (field ^:static ^:final ^VarHandle J)
  (field ^:static ^:final ^VarHandle Z)
  (field ^:static ^:final ^VarHandle F)
  (field ^:static ^:final ^VarHandle D)
  (field ^:static ^:final ^VarHandle O)
  (field ^:static ^:final ^VarHandle S)
  (field ^:static ^:final ^VarHandle HEAD)
  (field ^:static ^:final ^VarHandle SH)
  (field ^:static ^:final ^VarHandle CH)
  (field ^:static ^:final ^VarHandle ITEM)
  (field ^:static ^:final ^VarHandle NEXT
    (.findVarHandle (MethodHandles/lookup) FxVhNode "next" FxVhNode))
  (field ^:static ^:final ^VarHandle IA (MethodHandles/arrayElementVarHandle int/1))
  (field ^:static ^:final ^VarHandle LA (MethodHandles/arrayElementVarHandle long/1))
  (field ^:static ^:final ^VarHandle RA (MethodHandles/arrayElementVarHandle String/1))
  (field ^:static ^:final ^VarHandle OA (MethodHandles/arrayElementVarHandle Object/1))

  (static-initializer
    (try
      (let [l (MethodHandles/lookup)
            ^{:tag (Class ?)} self FxVarHandles]
        (set! I (.findVarHandle l self "i" Integer/TYPE))
        (set! J (.findVarHandle l FxVarHandles "j" Long/TYPE))
        (set! Z (.findVarHandle l FxVarHandles "z" Boolean/TYPE))
        (set! F (.findVarHandle l FxVarHandles "f" Float/TYPE))
        (set! D (.findVarHandle l FxVarHandles "d" Double/TYPE))
        (set! O (.findVarHandle l FxVarHandles "o" Object))
        (set! S (.findVarHandle l FxVarHandles "s" String))
        (set! HEAD (.findVarHandle l FxVarHandles "head" FxVhNode))
        (set! SH (.findVarHandle l FxVarHandles "sh" Short/TYPE))
        (set! CH (.findVarHandle l FxVarHandles "ch" Character/TYPE))
        (set! ITEM (.findVarHandle l FxVhNode "item" Object)))
      (catch ReflectiveOperationException e (throw (ExceptionInInitializerError. e)))))

  (method ^:public ^:static tInts ^String []
    (let [x (FxVarHandles.)]
      ^void (^[FxVarHandles int] VarHandle/.set I x 5)
      (java-str ^int (^[FxVarHandles] VarHandle/.get I x)
                " " ^boolean (^[FxVarHandles int int] VarHandle/.compareAndSet I x 4 9)
                " " ^boolean (^[FxVarHandles int int] VarHandle/.weakCompareAndSetPlain I x 5 9)
                " " ^int (^[FxVarHandles int int] VarHandle/.compareAndExchange I x 9 11)
                " " ^int (^[FxVarHandles int int] VarHandle/.compareAndExchangeAcquire I x 0 12)
                " " ^int (^[FxVarHandles int] VarHandle/.getAndAdd I x 3)
                " " ^int (^[FxVarHandles int] VarHandle/.getAndSet I x -1)
                " " ^int (^[FxVarHandles int] VarHandle/.getAndBitwiseAnd I x 0xff)
                " " ^int (^[FxVarHandles int] VarHandle/.getAndBitwiseOr I x 0x100)
                " " ^int (^[FxVarHandles int] VarHandle/.getAndBitwiseXor I x 0x1ff)
                " " ^int (^[FxVarHandles] VarHandle/.getAcquire I x)
                " " (.-i x))))

  (method ^:public ^:static tLongs ^String []
    (let [x (FxVarHandles.)]
      (set! (.-j x) Long/MAX_VALUE)
      (java-str ^long (^[FxVarHandles long] VarHandle/.getAndAdd J x 1)
                " " (.-j x)
                " " ^boolean (^[FxVarHandles long long] VarHandle/.compareAndSet J x Long/MIN_VALUE 7)
                " " ^long (^[FxVarHandles long long] VarHandle/.compareAndExchangeRelease J x 7 8)
                " " ^long (^[FxVarHandles long] VarHandle/.getAndBitwiseXor J x 15)
                " " ^long (^[FxVarHandles] VarHandle/.getOpaque J x)
                " " (do ^void (^[FxVarHandles long] VarHandle/.setRelease J x 42) (.-j x)))))

  (method ^:public ^:static tBooleans ^String []
    (let [x (FxVarHandles.)]
      (java-str ^boolean (^[FxVarHandles boolean boolean] VarHandle/.compareAndSet Z x false true)
                " " (.-z x)
                " " ^boolean (^[FxVarHandles boolean] VarHandle/.getAndSet Z x false)
                " " ^boolean (^[FxVarHandles boolean] VarHandle/.getAndBitwiseOr Z x true)
                " " ^boolean (^[FxVarHandles boolean] VarHandle/.getAndBitwiseAnd Z x false)
                " " ^boolean (^[FxVarHandles boolean] VarHandle/.getAndBitwiseXor Z x true)
                " " ^boolean (^[FxVarHandles boolean boolean] VarHandle/.compareAndExchange Z x true false)
                " " ^boolean (^[FxVarHandles] VarHandle/.getVolatile Z x))))

  (method ^:public ^:static tFloats ^String []
    (let [x (FxVarHandles.)]
      (set! (.-f x) (float 1.5))
      (set! (.-d x) -0.0)
      (java-str ^float (^[FxVarHandles float] VarHandle/.getAndAdd F x (float 2.25))
                " " (.-f x)
                " " ^boolean (^[FxVarHandles float float] VarHandle/.compareAndSet F x (float 3.75) Float/NaN)
                " " ^boolean (^[FxVarHandles float float] VarHandle/.compareAndSet F x Float/NaN (float 1.0))
                " " ^float (^[FxVarHandles float] VarHandle/.getAndSet F x (float 2.0))
                ;; doubles compare by their bits: 0.0 is not -0.0
                " " ^boolean (^[FxVarHandles double double] VarHandle/.compareAndSet D x 0.0 1.0)
                " " ^boolean (^[FxVarHandles double double] VarHandle/.compareAndSet D x -0.0 1.0)
                " " ^double (^[FxVarHandles double] VarHandle/.getAndAdd D x 0.5)
                " " ^double (^[FxVarHandles double double] VarHandle/.compareAndExchange D x 1.5 2.5)
                " " (.-d x))))

  (method ^:public ^:static tNarrow ^String []
    (let [x (FxVarHandles.)]
      ^void (^[FxVarHandles short] VarHandle/.set SH x (short -2))
      ^void (^[FxVarHandles char] VarHandle/.set CH x \A)
      (java-str ^short (^[FxVarHandles] VarHandle/.get SH x)
                " " ^boolean (^[FxVarHandles short short] VarHandle/.compareAndSet SH x (short -2) (short 300))
                " " (.-sh x)
                " " ^char (^[FxVarHandles char] VarHandle/.getAndSet CH x \z)
                " " ^char (^[FxVarHandles char char] VarHandle/.compareAndExchange CH x \z \q)
                " " (.-ch x))))

  (method ^:public ^:static tReferences ^String []
    (let [x (FxVarHandles.)
          a (String. "a")
          a2 (String. "a")
          n1 (FxVhNode. "one")
          n2 (FxVhNode. "two")]
      (java-str ^boolean (^[FxVarHandles Void String] VarHandle/.compareAndSet S x nil a)
                ;; identity, not equals
                " " ^boolean (^[FxVarHandles String String] VarHandle/.compareAndSet S x a2 "b")
                " " ^String (^[FxVarHandles String String] VarHandle/.compareAndExchange S x a "c")
                " " (^[FxVarHandles] VarHandle/.getAcquire S x)
                " " ^boolean (^[FxVarHandles Void Object] VarHandle/.compareAndSet O x nil n1)
                " " (.-item ^FxVhNode (^[FxVarHandles Object] VarHandle/.getAndSet O x n2))
                " " (.-item ^FxVhNode (.-o x))
                " " ^boolean (^[FxVarHandles Void FxVhNode] VarHandle/.compareAndSet HEAD x nil n1)
                " " ^boolean (^[FxVhNode Void FxVhNode] VarHandle/.compareAndSet NEXT n1 nil n2)
                " " (.-item (.-next (.-head x)))
                " " ^Object (^[FxVhNode String] VarHandle/.getAndSet ITEM n2 "deux")
                " " (.-item n2)
                " " (identical? n2 ^FxVhNode (^[FxVhNode FxVhNode FxVhNode] VarHandle/.compareAndExchange NEXT n1 n2 nil))
                " " (.-next n1))))

  (method ^:public ^:static tFences ^String []
    (VarHandle/fullFence)
    (VarHandle/acquireFence)
    (VarHandle/releaseFence)
    (VarHandle/loadLoadFence)
    (VarHandle/storeStoreFence)
    "fenced")

  (method ^:public ^:static tIntArray ^String []
    (let [a (new int/1 3)]
      ^void (^[int/1 int int] VarHandle/.set IA a 1 10)
      (java-str ^int (^[int/1 int] VarHandle/.getVolatile IA a 1)
                " " ^int (^[int/1 int int] VarHandle/.getAndAdd IA a 1 5)
                " " ^boolean (^[int/1 int int int] VarHandle/.compareAndSet IA a 1 15 7)
                " " ^int (^[int/1 int int int] VarHandle/.compareAndExchange IA a 2 1 3)
                " " ^int (^[int/1 int int] VarHandle/.getAndBitwiseOr IA a 0 6)
                " " ^int (^[int/1 int int] VarHandle/.getAndBitwiseXor IA a 0 3)
                " " ^int (^[int/1 int int] VarHandle/.getAndSet IA a 0 -1)
                " " (aget a 0) " " (aget a 1) " " (aget a 2))))

  (method ^:public ^:static tLongArray ^String []
    (let [a (new long/1 2)]
      (java-str ^long (^[long/1 int long] VarHandle/.getAndAdd LA a 0 Long/MAX_VALUE)
                " " ^long (^[long/1 int long] VarHandle/.getAndAdd LA a 0 1)
                " " ^boolean (^[long/1 int long long] VarHandle/.weakCompareAndSet LA a 1 0 9)
                " " ^long (^[long/1 int long] VarHandle/.getAndBitwiseAnd LA a 1 12)
                " " (aget a 0) " " (aget a 1))))

  (method ^:public ^:static tRefArray ^String []
    (let [a (new String/1 2)
          o (new Object/1 1)
          x (String. "x")]
      (java-str ^boolean (^[String/1 int Void String] VarHandle/.compareAndSet RA a 0 nil x)
                " " ^boolean (^[String/1 int String String] VarHandle/.compareAndSet RA a 0 (String. "x") "y")
                " " ^String (^[String/1 int String] VarHandle/.getAndSet RA a 1 "z")
                " " ^String (^[String/1 int String String] VarHandle/.compareAndExchange RA a 0 x "w")
                " " ^String (^[String/1 int] VarHandle/.getAcquire RA a 0)
                " " (aget a 1)
                " " (do ^void (^[Object/1 int Object] VarHandle/.setRelease OA o 0 (Integer/valueOf 4)) (aget o 0)))))

  (method ^:public ^:static tArrayErrors ^String []
    (let [sb (StringBuilder.)
          a (new int/1 2)
          ^Object/1 r (new String/1 1)]
      (try (^[int/1 int] VarHandle/.get IA a 2)
           (catch ArrayIndexOutOfBoundsException e (.append sb (.getMessage e))))
      (.append sb " | ")
      (try ^void (^[int/1 int int] VarHandle/.set IA nil 0 1)
           (catch NullPointerException e (.append sb "NPE")))
      (.append sb " | ")
      (try ^void (^[Object/1 int Object] VarHandle/.set OA r 0 (Integer/valueOf 1))
           (catch ArrayStoreException e (.append sb (.getName (.getClass e)))))
      (.toString sb)))

  (method ^:public ^:static tContended ^String []
    (let [x (FxVarHandles.)
          a (new long/1 1)
          ts (new Thread/1 8)]
      (loop [^int k 0]
        (when (< k 8)
          (aset ts k (Thread. (lambda Runnable ^void []
                                (loop [^int n 0]
                                  (when (< n 1000)
                                    (^[FxVarHandles int] VarHandle/.getAndAdd I x 1)
                                    (^[long/1 int long] VarHandle/.getAndAdd LA a 0 2)
                                    (loop []
                                      (let [^String cur (^[FxVarHandles] VarHandle/.get S x)
                                            ^String nxt (if (nil? cur) "." (.concat cur "."))]
                                        (when-not ^boolean (^[FxVarHandles String String] VarHandle/.compareAndSet S x cur nxt)
                                          (recur))))
                                    (recur (unchecked-inc-int n)))))))
          (.start (aget ts k))
          (recur (unchecked-inc-int k))))
      (loop [^int k 0]
        (when (< k 8)
          (.join (aget ts k))
          (recur (unchecked-inc-int k))))
      (java-str (.-i x) " " (aget a 0) " " (.length (.-s x))))))

)
