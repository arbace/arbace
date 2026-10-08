;; Go-build variant of arbace.lang.Compiler (C2G-SPEC §4.6, §10.1): read by c2g only. The Go
;; build has no bytecode back end (ASM is cut, D6) and no system properties for compiler
;; options; this first variant only lets Compiler initialize (its analyzer and munge, the
;; reader's syntax-quote). B1a step 5 replaces the back end with the evaluator.
(in-ns 'arbace.lang)

(c2g/variant Compiler
  ;; ARG_TYPES: ASM's types of the back end's method descriptors
  (c2g/cut (static-initializer 0))

  ;; *compiler-options* from the arbace.compiler.* system properties: none
  ^{:c2g/nth 1}
  (static-initializer
    (set! COMPILER_OPTIONS
          (.setDynamic (Var/intern (Namespace/findOrCreate (Symbol/intern "arbace.core"))
                                   (Symbol/intern "*compiler-options*")
                                   nil))))

  ;; the class file version the back end writes: Java 26's
  (field ^:public ^:static ^:final ^int JVM_BYTECODE_VERSION 70))
