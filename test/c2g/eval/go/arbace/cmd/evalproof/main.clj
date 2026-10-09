;; The evaluator boundary's proof (doc/go/EVAL-PLAN.md, "The proof"): forms read by the
;; translated LispReader, analyzed by the translated Compiler into Expr trees, the trees shown
;; from Go, and evaluated (Expr.eval, and Compiler.eval through the evaluator's EvalFn).
;; Built by bin/c2g-evalproof; its output is compared with test/c2g/eval/expected.txt.
(in-ns 'go.arbace.cmd.evalproof)

(go/file "main.go" :imports [[fmt "fmt"] [strings "strings"] [jrt "arbace/jrt"] [lang "arbace/lang"]])

(go/func pr
  "pr prints v as RT.printString does (arbace.core is not loaded: RT's own printer)."
  ^string [^any v]
  (.String (lang/RT_PrintString_O__String v)))

(go/func cls "cls is the Java class name of v." ^string [^any v]
  (when (== v nil) (return "nil"))
  (.String (.GetName__String (jrt/GetClass v))))

(go/func items
  "items shows the Expr nodes of a vector of them."
  ^string [^lang/IPersistentVector v]
  (let [^{:tag (slice string)} parts nil]
    (when (!= v nil)
      (for [i (conv int32 0)] (< i (.Count__I v)) (inc! i)
        (set! parts (append parts (tree (.Nth_I__O v i))))))
    (strings/Join parts " ")))

(go/func locals
  "locals shows a seq of LocalBindings."
  ^string [^lang/ISeq s]
  (let [^{:tag (slice string)} parts nil]
    (for [] (!= s nil) (set! s (.Next__ISeq s))
      (set! parts (append parts (tree (.First__O s)))))
    (+ "[" (strings/Join parts " ") "]")))

(go/func tree
  "tree shows an Expr tree: the node kinds of the evaluator's first cases with their
children, any other node by its class."
  ^string [^any e]
  (when (== e nil) (return "nil"))
  (type-switch [x e]
    (case [(* lang/Compiler_NumberExpr)] (return (fmt/Sprintf "(Number %s)" (pr (.-F_n x)))))
    (case [(* lang/Compiler_ConstantExpr)] (return (fmt/Sprintf "(Constant %s)" (pr (.-F_v x)))))
    (case [(* lang/Compiler_NilExpr)] (return "(Nil)"))
    (case [(* lang/Compiler_BooleanExpr)] (return (fmt/Sprintf "(Boolean %v)" (.-F_val x))))
    (case [(* lang/Compiler_LocalBinding)]
      (return (fmt/Sprintf "%s#%d" (pr (.-F_sym x)) (.-F_idx x))))
    (case [(* lang/Compiler_LocalBindingExpr)]
      (return (fmt/Sprintf "(Local %s #%d)" (pr (.-F_sym (.-F_b x))) (.-F_idx (.-F_b x)))))
    (case [(* lang/Compiler_VarExpr)] (return (fmt/Sprintf "(Var %s)" (pr (.-F_var x)))))
    (case [(* lang/Compiler_TheVarExpr)] (return (fmt/Sprintf "(TheVar %s)" (pr (.-F_var x)))))
    (case [(* lang/Compiler_BodyExpr)] (return (fmt/Sprintf "(Body %s)" (items (.-F_exprs x)))))
    (case [(* lang/Compiler_IfExpr)]
      (return (fmt/Sprintf "(If %s %s %s)" (tree (.-F_testExpr x)) (tree (.-F_thenExpr x)) (tree (.-F_elseExpr x)))))
    (case [(* lang/Compiler_LetExpr)]
      (let [bis (.-F_bindingInits x)
            ^{:tag (slice string)} parts nil]
        (for [i (conv int32 0)] (< i (.Count__I bis)) (inc! i)
          (let [bi (assert (* lang/Compiler_BindingInit) (.Nth_I__O bis i))]
            (set! parts (append parts (fmt/Sprintf "%s#%d %s" (pr (.-F_sym (.-F_binding bi))) (.-F_idx (.-F_binding bi))
                                                   (tree (.-F_init bi)))))))
        (return (fmt/Sprintf "(%s [%s] %s)" (pick (.-F_isLoop x) "Loop" "Let") (strings/Join parts " ") (tree (.-F_body x))))))
    (case [(* lang/Compiler_RecurExpr)] (return (fmt/Sprintf "(Recur %s)" (items (.-F_args x)))))
    (case [(* lang/Compiler_StaticMethodExpr)]
      (return (fmt/Sprintf "(StaticMethod %s %s {%s} %s)" (cls_name (.-F_c x)) (.String (.-F_methodName x))
                           (method_text (.-F_method x))
                           (items (.-F_args x)))))
    (case [(* lang/Compiler_InstanceMethodExpr)]
      (return (fmt/Sprintf "(InstanceMethod %s %s %s)" (.String (.-F_methodName x)) (tree (.-F_target x)) (items (.-F_args x)))))
    (case [(* lang/Compiler_InvokeExpr)]
      (return (fmt/Sprintf "(Invoke %s %s)" (tree (.-F_fexpr x)) (items (.-F_args x)))))
    (case [(* lang/Compiler_DefExpr)]
      (return (fmt/Sprintf "(Def %s %s)" (pr (.-F_var x)) (tree (.-F_init x)))))
    (case [(* lang/Compiler_FnExpr)]
      (let [^{:tag (slice string)} parts nil]
        (for [s (lang/RT_Seq_O__ISeq (.-F_methods x))] (!= s nil) (set! s (.Next__ISeq s))
          (let [m (assert (* lang/Compiler_FnMethod) (.First__O s))]
            (set! parts (append parts (fmt/Sprintf "(Method %s %s)" (items (.-F_reqParms m)) (tree (.-F_body m)))))))
        (return (fmt/Sprintf "(Fn %s closes %s %s)" (.String (.-F_name x)) (locals (lang/RT_Keys_O__ISeq (.-F_closes x)))
                             (strings/Join parts " ")))))
    (default (return (fmt/Sprintf "(%s)" (cls e))))))

(go/func pick ^string [^bool b ^string x ^string y]
  (when b (return x))
  y)

(go/func method_text ^string [^{:tag (* jrt/Method)} m]
  (when (== m nil) (return "reflective"))
  (.String (.ToString__String m)))

(go/func msg ^string [^jrt/Throwable_I e]
  (let [m (.GetMessage__String e)]
    (when (== m nil) (return ""))
    (.String m)))

(go/func cls_name ^string [^{:tag (* jrt/Class)} c]
  (.String (.GetName__String c)))

(go/func try
  "try runs f, returning a Java exception it throws (with its causes, as text) instead."
  ^string [^{:tag (func [] [string])} f]
  (let [(values s exc) ((fn [] :results [^string s ^jrt/Throwable_I exc]
                          (defer (jrt/Catch (addr exc)))
                          (set! s (f))
                          (return)))]
    (when (!= exc nil)
      (let [b (lit strings/Builder)]
        (.WriteString b "throws")
        (for [n 0] (and (!= exc nil) (< n 6)) (inc! n)
          (.WriteString b (fmt/Sprintf " %s: %s;" (cls exc)
                                       (msg exc)))
          (set! exc (.GetCause__Throwable exc)))
        (return (.String b))))
    s))

(go/func show
  "show reads src, analyzes it as Compiler.eval would (C.EVAL), prints its tree and the value
of Compiler.eval."
  [^string src]
  (fmt/Printf "form   %s\n" src)
  (let [form (lang/RT_ReadString_String__O (jrt/Str src))]
    (fmt/Printf "read   %s (%s)\n" (try (fn ^string [] (pr form))) (cls form))
    (fmt/Printf "tree   %s\n" (try (fn ^string [] (tree (lang/Compiler_Analyze_Compiler_C_O__Compiler_Expr lang/Compiler_C_EVAL form)))))
    (fmt/Printf "eval   %s\n\n" (try (fn ^string [] (pr (lang/Compiler_Eval_O__O form)))))))

(go/func evalStr "evalStr evaluates the form src with Compiler.eval." ^any [^string src]
  (lang/Compiler_Eval_O__O (lang/RT_ReadString_String__O (jrt/Str src))))

(go/func goAssertsVector ^bool [^any o]
  (let [(values _ ok) (assert lang/IPersistentVector o)] ok))

(go/func dynDemo
  "dynDemo makes a class at run time as deftype will (C2G-SPEC §5.12): user.Foo implementing
Counted and ILookup, with a field a and count implemented by an evaluated fn; then uses an
instance from Go (translated code's interface calls and checks) and from evaluated forms
(reflection)."
  []
  (fmt/Println "== a class made at run time (Dyn)")
  (let [c (lang/Compiler_Dyn_DefineClass_String_Class1_String1__Class
            (jrt/Str "user.Foo")
            (jrt/RefArrayOf jrt/Class_class lang/Counted_class lang/ILookup_class)
            (jrt/RefArrayOf jrt/String_class (jrt/Str "a")))
        impl (lang/IFn_Cast (evalStr "(fn* [this] 42)"))]
    (lang/Compiler_Dyn_SetMethod_Class_String_Class1_Class_IFn__V c (jrt/Str "count") nil jrt/Prim_int impl)
    (lang/Compiler_Dyn_SetMethod_Class_String_Class1_Class_IFn__V
      c (jrt/Str "toString") nil jrt/String_class (lang/IFn_Cast (evalStr "(fn* [this] \"#<Foo>\")")))
    (let [o (lang/Compiler_Dyn_NewInstance_Class_O1__O c (jrt/RefArrayOf jrt/Object_class (jrt/Str "field a")))]
      (fmt/Printf "class               %s (forName: %v)\n" (cls o)
                  (== (jrt/Class_ForName_String__Class (jrt/Str "user.Foo")) c))
      (fmt/Printf "RT.count            %s\n" (try (fn ^string [] (fmt/Sprint (lang/RT_Count_O__I o)))))
      (fmt/Printf "toString            %s\n" (try (fn ^string [] (.String (jrt/ToString o)))))
      (fmt/Printf "instanceof Counted  %v\n" (lang/Counted_InstanceOf o))
      (fmt/Printf "instanceof ILookup  %v\n" (lang/ILookup_InstanceOf o))
      (fmt/Printf "instanceof IPersistentVector %v (the Go assertion alone: %v)\n"
                  (lang/IPersistentVector_InstanceOf o)
                  (goAssertsVector o))
      (fmt/Printf "cast to Seqable     %s\n" (try (fn ^string [] (lang/Seqable_Cast o) "ok")))
      (fmt/Printf "valAt (not set)     %s\n" (try (fn ^string [] (pr (.ValAt_O__O (lang/ILookup_Cast o) (jrt/Str "k"))))))
      (.BindRoot_O__V (lang/Var_Cast (evalStr "(def foo nil)")) o)))
  (range [_ src (lit (slice string)
                     "foo"
                     "(arbace.lang.RT/count foo)"
                     "(. foo count)"
                     "(. foo -a)"
                     "(let* [c arbace.lang.Counted] (. c (isInstance foo)))"
                     "(let* [c arbace.lang.IPersistentVector] (. c (isInstance foo)))"
                     "(. (. foo getClass) getName)")]
    (fmt/Printf "form   %s\neval   %s\n" src (try (fn ^string [] (pr (evalStr src))))))
  (fmt/Println))

(go/func fromFnDemo
  "fromFnDemo adapts an evaluated fn to java.util.function.Function through jrt.AdaptFn, as
Reflector's boxArg does (ClassInfo.FromFn, C2G-SPEC §7.11), and asks Function and Comparable
whether they are functional interfaces (isAnnotationPresent)."
  []
  (fmt/Println "== a fn as a functional interface (FromFn)")
  (let [f (evalStr "(fn* [x] (arbace.lang.Numbers/multiply x 2))")]
    (fmt/Printf "AdaptFn Function    %s\n"
                (try (fn ^string []
                       (let [g (jrt/Function_Cast (jrt/AdaptFn jrt/Function_class f))]
                         (fmt/Sprintf "%s, apply 21: %s" (cls g) (pr (.Apply_O__O g (jrt/Box (conv int64 21)))))))))
    (fmt/Printf "@FunctionalInterface Function %v, String %v\n"
                (.IsAnnotationPresent_Class__Z jrt/Function_class jrt/FunctionalInterface_class)
                (.IsAnnotationPresent_Class__Z jrt/String_class jrt/FunctionalInterface_class)))
  (fmt/Println))

(go/func main []
  (lang/Compiler_C_Init)
  (fmt/Println "== the translated reader and analyzer, Expr.eval, and the evaluator")
  ;; Expr.eval as Compiler has it: constants and a static call with constant arguments
  (let [form (lang/RT_ReadString_String__O (jrt/Str "(arbace.lang.Numbers/add 1 2)"))]
    (fmt/Printf "Expr.eval of %s: %s\n\n" (pr form)
                (try (fn ^string [] (pr (.Eval__O (lang/Compiler_Analyze_Compiler_C_O__Compiler_Expr lang/Compiler_C_EVAL form)))))))
  (range [_ src (lit (slice string)
                     "42"
                     "\"str\""
                     ":kw"
                     "[1 :a \"s\"]"
                     "(arbace.lang.Numbers/add 1 2)"
                     "(if nil 1 2)"
                     "(Math/abs -3)"
                     "(let* [x 1 y 2] (if x (arbace.lang.Numbers/add x y) nil))"
                     "(loop* [i 0 acc 0] (if (arbace.lang.Numbers/lt i 5) (recur (arbace.lang.Numbers/inc i) (arbace.lang.Numbers/add acc i)) acc))"
                     "(((fn* [x] (fn* [y] (arbace.lang.Numbers/add x y))) 40) 2)"
                     "(def plus (fn* [a b] (arbace.lang.Numbers/add a b)))"
                     "(plus 40 2)"
                     "(def twice (fn* [&form &env x] (arbace.lang.RT/list (quote arbace.lang.Numbers/multiply) x 2)))"
                     "(. (var twice) (setMacro))"
                     "(twice 21)"
                     "((fn* f [n] (if (arbace.lang.Numbers/lte n 1) 1 (arbace.lang.Numbers/multiply n (f (arbace.lang.Numbers/dec n))))) 10)"
                     "((fn* [& xs] xs) 1 2 3)"
                     "(nosuch 1)")]
    (show src))
  (dynDemo)
  (fromFnDemo))
