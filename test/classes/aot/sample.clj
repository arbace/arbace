(ns classes.aot.sample)

(defclass ^:public Greeter
  (field ^:private ^:final ^String greeting)
  (constructor ^:public [this ^String g] (set! greeting g))
  (method ^:public greet ^String [this ^String who]
    (.concat (.concat greeting ", ") who))
  (method ^:public ^:static make ^Greeter [] (Greeter. "Hello")))

(def answer (.greet (Greeter/make) "AOT"))
