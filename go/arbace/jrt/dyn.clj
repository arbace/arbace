;; jrt: the classes the evaluator makes at run time (C2G-SPEC §5.12, §10.4; doc/go/EVAL-PLAN.md):
;; deftype, defrecord and reify instances are values of one Go type, Dyn, which c2g generates
;; in package arbace/lang (c2g_dyn.go) with a method for every method of every interface of the
;; closed world. jrt knows them through the interface Dynamic, so that c2g's instanceof and
;; checkcast of an interface (in either package) add the nominal check: the Go assertion
;; succeeds for every Dyn, the run-time class must implement the interface.
(in-ns 'go.arbace.jrt)

(go/file "dyn.go" :imports [[strings "strings"]])

(go/type Dynamic
  "Dynamic is an object of a class made at run time (c2g's Dyn): DynImplements says whether
its class implements the interface c (directly or through its superinterfaces).\n"
  (interface
    (DynImplements ^bool [^{:tag (* Class)} c])))

(go/func dynNominal
  "dynNominal is the nominal check of jrt's hand-written interfaces (as c2g's instanceof and
checkcast make it for the translated ones): ok, the Go assertion's result, unless x is an
object of a class made at run time (Dyn implements every interface of the world), whose class
must implement c.\n"
  ^bool [^any x ^{:tag (* Class)} c ^bool ok]
  (when ok
    (let [(values d dyn) (assert Dynamic x)]
      (when dyn
        (return (.DynImplements d c)))))
  ok)

(go/var ^{:tag (map (* Class) bool)
          :doc "dynamicClasses are the classes DefineDynamic made (under registryMu).\n"}
  dynamicClasses (make (map (* Class) bool)))

(go/func DefineDynamic
  "DefineDynamic registers a class made at run time under its binary name and returns its
Class object. Unlike Define, it may replace a class DefineDynamic made before under the same
name (deftype evaluated again at the REPL); a class of the closed world cannot be replaced.\n"
  ^{:tag (* Class)} [^{:tag (* ClassInfo)} info]
  (let [c (addr (lit Class :info info))]
    (.Lock registryMu)
    (let [old (aget registry (.-Name info))]
      (when (and (!= old nil) (not (aget dynamicClasses old)))
        (.Unlock registryMu)
        (panic (Thrown (IllegalArgumentException_New_String
                         (Str (+ "duplicate class definition: " (.-Name info)))))))
      (when (!= old nil)
        (delete dynamicClasses old)))
    (aset registry (.-Name info) c)
    (aset dynamicClasses c true)
    (.Unlock registryMu)
    c))

(go/method Descriptor
  "Descriptor is the class's JVM descriptor: I, [Ljava/lang/String;, Ljava/lang/Object; (the
evaluator's method keys).\n"
  ^string [^{:tag (* Class)} c]
  (when (!= (.-desc c) 0)
    (return (conv string (conv rune (.-desc c)))))
  (when (strings/HasPrefix (.-Name (.-info c)) "[")
    (return (strings/ReplaceAll (.-Name (.-info c)) "." "/")))
  (+ "L" (strings/ReplaceAll (.-Name (.-info c)) "." "/") ";"))
