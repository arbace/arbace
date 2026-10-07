# Class forms compiler: implementation notes

The implementation of [SPEC.md](SPEC.md) (agenda step 2): one plain-Clojure compiler of the class
forms (§12 question 17) that runs on the frozen baseline (stage 0) and emits bytecode with ASM.
These notes say how it is built, how to use and test it, what it covers (the §8 tables), and
where implementing the spec suggested changes to it (amendments, accepted and folded into
SPEC.md, last section).

## Layout

| file | namespace | role |
|---|---|---|
| `arbace/classes/boot.clj` | `arbace.classes.boot` | driver: at stage 0 loads the others mapping `arbace.*` to `clojure.*`, and loads `arbace/core_classes.clj` into `clojure.core`; from stage 1 on just requires them |
| `arbace/classes/types.clj` | `arbace.classes.types` | type forms (§4.3), descriptors, erasure, `Signature` strings |
| `arbace/classes/env.clj` | `arbace.classes.env` | class environment (§9.2): class infos from the compilation, from earlier definitions and by reflection (constants from class files); member lookup and access; package class loaders (§10) |
| `arbace/classes/parse.clj` | `arbace.classes.parse` | syntax of class forms and members, class access flags |
| `arbace/classes/analyze.clj` | `arbace.classes.analyze` | entering classes (headers, members, derived members), name resolution, code analysis into typed nodes |
| `arbace/classes/emit.clj` | `arbace.classes.emit` | bytecode for nodes and classes, `InnerClasses` by a post-pass over the constant pool |
| `arbace/classes/compiler.clj` | `arbace.classes.compiler` | one compilation: enter, analyze, emit; define and/or write the classes |
| `arbace/classes/native.clj` | `arbace.classes.native` | the boundary with `arbace.lang.Compiler` (stage 1 on): what the compiler calls for the class forms' special forms (SPEC §9.5, section "Native class forms" below) |
| `arbace/core_classes.clj` | `arbace.core` | the user-facing macros and operators, loaded by `arbace/core.clj` (formerly `arbace.classes.core`) |
| `arbace/classes/shape.clj` | `arbace.classes.shape` | class shapes (§3) as data, and their differences, for comparing with javac |
| `test/classes/*_test.clj` | `classes.*-test` | the tests; `test/classes/helpers.clj` compiles Java with javac in-process and compares shapes |
| `bin/class-forms-tests` | | runs the tests |

The compiler's sources name ASM `arbace.asm`: the boot driver rewrites those symbols while it
reads the files (also inside metadata such as type hints), so the same sources can later be
compiled into stage 1 against a vendored `arbace.asm`. Its other dependencies on the frozen
Clojure are ordinary `clojure.core` functions, `proxy` (for ASM's `ClassWriter` and visitors) and
`clojure.lang.DynamicClassLoader`/`RT` for the REPL loaders.

## Use

```
$ java -cp . clojure.main
user=> (require 'arbace.classes.boot)
user=> (defclass ^:public Counter
         (field ^:private ^int n)
         (method ^:public inc ^int [this] (set! n (unchecked-inc-int n))))
user.Counter
user=> (let [c (Counter.)] (.inc c) (.inc c))
2
```

- Loading `arbace.classes.boot` loads the compiler and interns the new names into `clojure.core`,
  so namespaces created afterwards refer them; `user` gets them referred directly
  (`(arbace.classes.boot/install! 'some.ns)` refers them into another existing namespace).
- `defclass` compiles at macroexpansion time, defines the classes in their package's loader,
  imports the top-level class and evaluates to it. Under `*compile-files*` (`compile`) the classes
  are also written to `*compile-path*`; the compiled namespace then imports them from the class
  path, and a JVM without the compiler can load it (tested).
- `defclasses` compiles several class forms as one compilation (mutually referring classes).
- `arbace.classes.compiler/compile-forms` returns the bytes without defining anything.
- Options (dynamic vars of `arbace.classes.emit`): `*version*`, the class file version (default:
  the running JDK's); `*string-concat*`, `:indy` (default) or `:inline` for javac's
  `-XDstringConcat=inline` (a `StringBuilder`, as the JDK build compiles `java.base` and a few other
  modules); `*method-parameters*`, `true` for javac's `-parameters`.

Tests: `bin/class-forms-tests [namespace...]`, for example `bin/class-forms-tests
classes.nested-test`. They write only under `.tmp/class-forms-tests/`. Most compare the classes
compiled from class forms with javac's for the equivalent Java (in-process `javax.tools`), by
shape: flags, supertypes, signatures, nest and inner class attributes, fields, methods,
`MethodParameters`, annotations and the symbolic content of code (member and class references,
call sites, constants). The others load and run the classes.

## How it works

1. **Parse** (`parse.clj`): a class form becomes a declaration map: kind, modifiers, options,
   members. Member macros are expanded and `do` spliced. Access flags (class file and
   `InnerClasses`) follow from the metadata and Java's implicit modifiers.
2. **Enter** (`analyze.clj`): every class of the top-level form gets its binary name (member
   classes `Outer$Inner`; local and anonymous ones are named when analysis meets them, javac's
   way: `Outer$1`, `Outer$1Local`, counted per enclosing class in textual order) and goes into
   the compilation set. Then headers (type parameters, supertypes, `Signature`), then members
   (descriptors, flags, `deftype`-style signature inference, and what javac derives: default
   constructors, enum and record members).
3. **Analyze**: the code of each class, in the textual order of its members, becomes typed nodes
   (`:type` is a descriptor, `:null` or `:none` for forms that do not complete). Field
   initializers and `initializer` bodies are analyzed once and emitted into each constructor that
   calls `super.`. Clojure macros are expanded with the frozen Clojure's macros; `and`, `or`,
   `locking`, comparisons, the operators and conversions are recognized by var (so shadowing
   works) and compiled to instructions.
4. **Emit** (`emit.clj`): ASM with `COMPUTE_FRAMES`, `getCommonSuperClass` answered from the
   class environment (classes being compiled are not loadable yet). `InnerClasses` is added by
   rereading the class's constant pool, as javac lists every nested class the file refers to.

Some mechanisms:

- **Frames and captures.** Every method, lambda and initializer body is a frame; local and
  anonymous classes and lambdas are capture boundaries. Resolving a local of an outer frame
  records it as captured by each boundary crossed. Emission loads captured locals from `val$x`
  fields (constructor parameters before the superclass constructor call, as javac). A receiver
  of an outer frame (`this` of the enclosing method, or `Outer/this`) becomes a chain of `this$0`
  reads, which marks each class on the way as using its outer instance; classes that never do
  get no `this$0` field (unless serializable), as javac since JDK 18.
- **Empty operand stack for jumps and handlers.** A node that is or contains `try`, `locking`,
  `break`, `continue`/`recur` or `return` is "unsafe": composite expressions evaluate the operands
  before an unsafe one into temporaries, so every jump target and handler starts with the stack
  it expects. So `try`, `loop` and `label` compile inline in any position (§5.7).
- **Cleanups.** Jumps out of `try` with `finally` and out of `locking` run the `finally` code or
  the `monitorexit` inline and split the protected ranges around it, as javac.
- **Constants.** Integer literals are `long` (Clojure) but marked as literals: they narrow to
  `int`, `short`, `byte`, `char` where the context needs it and the value fits (assignments,
  operands of `-int` operators, comparisons with `int`, branches whose other branches are `int`).
  A conditional whose values are all such literals is an `int` operand of every primitive
  operator, widened like one: `(unchecked-add d (if z 1 2))` loads `int` constants and `i2d`
  (§5.4).
  Constant expressions fold (`%` on `float` and `double` exactly as `frem`/`drem`); constant
  fields get `ConstantValue` and are inlined.

## Clojure in class bodies

Class bodies are Clojure (§5.13). Beyond the Java subset, at every stage:

- A call of a core operation (`+`, `=`, `int`, `aget`, `instance?`, ...) compiles to an
  instruction when its operands allow it, and otherwise as Clojure compiles it: the var's
  `:inline` expansion (`(+ a b)` on `Object`s is `Numbers.add(Object, Object)`, `(int x)` is
  `RT.intCast`), else a call of the var. The arguments are analyzed once (the expansion binds
  them as locals). In such an expansion a static call that no method is applicable to
  compiles as Clojure's compiler compiles it when the class has one method of that name and
  arity: that method, each argument converted as Clojure converts it (`RT.intCast`,
  `RT.floatCast`, ...). So `(unchecked-add-int x y)` or `(bit-and-int x y)` on `Object`s calls
  `Numbers.unchecked_int_add(int, int)` or `Numbers.andInt(int, int)`, without reflection.
- Any value can be called: locals, keywords, collections, fields, expressions (`IFn.invoke`).
- `fn*`, `letfn*`, `case*`, `def` and `var` are compiled by rewriting them
  (`arbace/classes/lower.clj`): a `fn` is an anonymous subclass of `AFunction` (`RestFn` when
  variadic) with an `invoke` (`doInvoke`, `getRequiredArity`) per arity, its parameters
  `Object` with their hints as local hints, `recur` to the top of the arity, the fn's name
  bound to the instance; with primitive signatures (`^long`, `^double`) it is a local class
  that also implements the `IFn$LL`... interfaces, with `invokePrim` and an `invoke` that
  converts with `RT.longCast`/`doubleCast`, as `arbace.lang.Compiler` makes them. `letfn`
  binds `Box`es, then the fns (which read the others from the boxes), then fills the boxes.
  `case*` is a `switch` on the hash (or int value) inside a `label`, each arm testing
  `Util.equiv` (or `identical?`) and leaving with its value, the default after the switch,
  as `CaseExpr`. `def` interns the var at compile time and calls `bindRoot`, `setMeta` (the
  evaluated metadata with the source position) and `setDynamic`.
- Any other constant (regex, ratio, big decimal, symbol, tagged literal) is read at class
  initialization, like keywords; `throw` of an `Object` is checked at run time; `set!` of a var
  is `Var.set`.
- `reify*` is a local class implementing the interfaces and `IObj` (`__meta`, `withMeta`
  making a copy), its methods' signatures inferred as for untyped methods (§4.6).
- `deftype*`, `monitor-enter`, `monitor-exit` and `import*` are errors there (use `defclass`;
  `locking` works).

## Stage-0 limits

- `anon`, `letclass`, `lambda`, `switch` and the other new code forms work inside class bodies
  only (the frozen compiler does not know their special forms). From stage 1 on they work in
  ordinary fns too (below).
- `defclass` must be evaluated (it compiles at macroexpansion time); a top-level `do` of class
  forms is seen form by form by the frozen compiler, hence `defclasses` (amendment 1). From
  stage 1 on a top-level `do` works.

## Native class forms (stage 1 on)

SPEC §9.5: the class forms' names are vars of `arbace.core` and their special forms are known to
`arbace.lang.Compiler`; there is still one implementation of the class forms, `arbace.classes`
(§12 question 17), which the compiler loads on first use. Nothing needs `arbace.classes.boot`
from stage 1 on: `java -cp target/stage1:. arbace.lang.Main` and `(defclass ...)`.

- **The names.** `arbace/core_classes.clj`, loaded at the end of `arbace/core.clj`, defines the
  macros (`defclass` ... `when-instance`) and the operators of §5.4 in `arbace.core`. At stage 0
  `arbace.classes.boot` loads the same file into `clojure.core` (reading `arbace.X` as
  `clojure.X`); there `defclass` and `defclasses` compile at macroexpansion time, as before,
  since the frozen compiler has no `class*` (the macros test `(contains? Compiler/specials
  'class*)`).
- **The operators.** The operators of §5.4 that Clojure lacks (`bit-and-int` ...
  `unsigned-bit-shift-right-int`, `unchecked-add-float` ... `unchecked-negate-float`,
  `unchecked-divide`, `unchecked-remainder`) are defined by `defop` with `:inline` expansions
  (and `:inline-arities`) to static methods of `arbace.lang.Numbers`, as Clojure's
  `unchecked-add-int` is: `andInt`, `orInt`, `xorInt`, `notInt`, `shiftLeftInt`,
  `shiftRightInt`, `unsignedShiftRightInt` (Clojure has these three), `unchecked_float_add`
  ... `unchecked_float_negate`, and `unchecked_divide`, `unchecked_remainder`. The `int` and
  `float` ones have one method each, `(int, int)` or `(float, float)`, so the compiler converts
  any argument as Clojure does for `unchecked-add-int` (`RT.intCast`, `RT.floatCast`; `long` to
  `int` checked, `double` to `float` by `d2f`), never reflecting; `unchecked_divide` and
  `unchecked_remainder` have the nine overloads of Clojure's `add` (`long`, `double`, `Object`
  pairs): `long` operands give `ldiv`/`lrem`, a `double` (or `float`) operand `ddiv`/`drem`,
  and the `Object` ones decide at run time (a `Double` or `Float` operand makes it `double`).
  `arbace.lang.Intrinsics` maps the primitive methods to their instructions, so in a primitive
  context (an operand of another operator, a `^long`/`^double` fn's result, a primitive local)
  the compiler emits `iand`, `fadd`, `ldiv` ... and no call; where the result is boxed it
  calls the method, as for Clojure's own operators (`test/native/inline_test.clj` reads the
  instructions of AOT-compiled fns). At stage 0 `clojure.lang.Numbers` lacks these methods:
  there `defop` defines plain functions, the fallback of class bodies for operands that fit no
  instruction.
- **Loading `arbace.classes`.** `bin/build-arbace` AOT-compiles the `arbace.classes`
  namespaces (`compile 'arbace.classes.boot`, not `arbace.j2c`) into each stage with that
  stage's runtime, after its class forms are built; the libraries they require (`arbace.set`,
  `arbace.string`, `arbace.java.io`) stay source. So a stage loads the class forms compiler from
  classes on first use (and the next stage is built by these classes); a source newer than its
  classes is still loaded from source, as Clojure's `load` decides. The output is
  reproducible, stage 3 equals stage 2 with them (2,100 files), after two fixes to the vendored
  runtime (`doc/VENDOR-NOTES.md`, "After vendoring", item 5): closed-over locals in a fixed
  order, and proxy constructors sorted. The first class form of a session (a `defclass` in a
  script on stage 1, the time of the `eval`) went from about 1,040 ms to about 255 ms; the
  second costs 3 ms. What remains is loading and initializing the 1,285 classes of the eight
  namespaces (`analyze` about 85 ms, `types` 40, `emit` 35) and the first compilation (30 to
  45 ms). Measured by timing each `require` (the libraries they use are loaded at startup
  already): before, compiling from source took nearly all of it, `analyze` 470 ms and `emit`
  290 ms; ASM and the class environment cost nothing measurable.
- **The special forms.** `class*`, `label*`, `break*`, `continue*`, `return*`, `switch*`,
  `lambda*`, `method-ref*`, `java-str*`, `java-assert*`, `for-each*`, `with-resources*` and
  `if-instance*` are in `Compiler/specials` (so `special-symbol?` holds and syntax-quote leaves
  them unqualified), all parsed by `Compiler$ClassFormsExpr$Parser`.
- **The boundary**, `arbace.classes.native`, called through `Compiler/classForms` (which
  requires the namespace):
  - `compile-top`: `(class* :top form)` and `(class* :tops forms)`, the expansions of `defclass`
    and `defclasses`, are compiled and defined (written under `*compile-files*`) while the
    compiler analyzes them, which then analyzes `(do (import* "p.C") C)` in their place (the
    class object as a constant, so classes of the unnamed package work too). AOT works: the
    classes are written to `*compile-path*` and the namespace's `__init` imports them.
  - Top-level `do`: `Compiler/eval` and `compile1` bind `Compiler/CLASS_FORM_SIBLINGS` to the
    class forms of the outermost top-level `do` (found through nested `do`s, by `class*` or by
    the head resolving to `arbace.core/defclass` or `defclasses`, without macroexpanding); each
    of them is compiled with the others entered as declarations only (like classes found by
    source lookup, §9.2), so mutually referring class forms work. `Compiler/load` resets the
    binding for the files it loads.
  - `compile-fn`: any other class form in code the compiler compiles itself, and the forms that
    are errors for Clojure but class forms for the spec (a primitive tag on a local with a
    primitive initializer, `set!` of a local of the method, `new` of an array class), throw `Compiler$ClassFormsExpr$Signal`. `FnExpr/parse` catches it
    and hands the whole fn over, unless the fn is one of the compiler's own `(fn* ^:once [] ...)`
    wrappers (loops and `try` in expression position, `lazy-seq` bodies) or a `letfn*`
    initializer (`CLASS_FORMS_NO_DELEGATE`): then the enclosing fn is handed over. At the top
    level (a `def`'s initializer) `eval` and `compile1` retry the form as `((fn* [] form))`.
    `compile-fn` makes the fn class the compiler would have made, with the compiler's name for
    it (`ns$f__123`), `public final`, extending `AFunction` or `RestFn` (and the `IFn$...`
    interfaces of primitive signatures), its arities compiled as for fns in class bodies (above),
    and with a field and constructor parameter per local of the compiler's environment that the
    fn uses (their primitive type or public tag class, else `Object`; found by the symbols of the
    form, plus any local a macro's expansion uses, by retrying). The compiler analyzes
    `(new C local...)` in place of the fn (with `with-meta` for the fn's metadata).
  - Code of handed-over fns has Clojure's meaning: everything of "Clojure in class bodies"
    above, and reflection where a member cannot be resolved (`Reflector`, with a warning under
    `*warn-on-reflection*`: `^{:reflection :clojure}` on the class), `Object` values converted
    by `RT.intCast` where an `int` is needed (array dimensions and indexes) and by
    `RT.longCast`... where a method returns a primitive, a `switch` with integer labels on a
    `long` or any other value (Clojure's integers) converting it by `RT.intCast`, hinted reference locals taking any
    reference from `recur` (checked), and the loop at the top of an arity is not a target of
    `break`.
- **Limits.** `recur` out of tail position and across `try` stay errors in code the compiler
  compiles (Clojure's test suite holds it to them; `continue` has that meaning). The class forms in `deftype` method
  bodies are not supported (the enclosing fn is handed over, and `deftype*` is an error there;
  `defclass` does what `deftype` does).
- **Tests**: `bin/native-tests [STAGE]` (`test/native/*_test.clj`, run by `bin/build-arbace` on
  stage 1): `defclass` with no boot step, a REPL session (`arbace.main/repl` on a string),
  `defclasses`, a top-level `do`, the code forms in fns, Clojure in handed-over fns and in class
  bodies, extended special forms, primitive signatures, and AOT compilation loaded by a fresh
  JVM from the class path. `test/classes/clojure_test.clj` covers Clojure in class bodies at
  stage 0.

## Checking converted code

`bin/class-forms-check DIR [PACKAGE-DIR]` compiles converted files (one file per Java file:
`in-ns`, `import`, `defclass` forms) without defining anything, and compares the shape of every
class with the class of the same name on the class path. For `clojure/lang` and `clojure/asm`
those are the baseline's javac classes. On 2026-10-06 the converter's output for the 183 baseline
files (read from its work area, not committed) first gave 138 of 139 `clojure/lang` files and
4 of 5 `clojure/asm/commons` ones shape-identical to javac (the others all), after patching two
converter issues by hand (notes for the converter, below); with the converter's later output all
files of `clojure/lang` (139), `clojure/asm` (36), `clojure/asm/commons` (5),
`clojure/asm/signature` (3) and `clojure/java/api` (1) are shape-identical, without patches.

### The converted JDK

The converter also converted all of jdk26u's modules (13,566 Java files; its work area, not
committed). The running JDK is built from the same sources, so its own classes are javac's
reference: `JAVA_OPTS='--add-modules ALL-SYSTEM' bin/class-forms-check <dir> <module>` per module,
with the javac options the JDK build uses for that module (`make/modules/*/Java.gmk`:
`CONCAT=inline` for `java.base`, `jdk.compiler`, `jdk.jfr`, `jdk.jartool`, `jdk.internal.vm.ci`;
`PARAMETERS=1 VERSION=69` for `jdk.internal.vm.ci`). The last complete run over all modules
(2026-10-06, 12,444 files with class forms) gave 11,351 files whose classes are all
shape-identical to the JDK's (91%), 233 compile errors and 860 files with differences; a later
partial run after more fixes gave 8,872 of 9,418 (94%). Each round of differences was triaged:
compiler issues were fixed (bridges, `this$N`, holder classes, outer instances, annotations,
lambda names, constant folding...), converter issues went into the notes for the converter
below. A complete rerun on 2026-10-07 (converter with the compiler's resolution for pins,
compiler with constructor-call param-tags, `anon :outer` and `int` literal conditionals; the
script in `.tmp/` of that worktree runs the modules in parallel) gave 11,753 of 12,444 files
shape-identical (94%), 174 compile errors, 517 files with differences; the same compiler on the
converter's earlier output (conservative pins) gave 11,745 / 177 / 522, and no file identical
there differs with the new output. What remains is mostly:

- converter output: mutable captures and catch parameters (note 9), inherited fields by simple
  name (note 7), generic varargs arrays (note 6);
- javac details the forms cannot express: the diamond in anonymous classes (note 10) and
  `InnerClasses` entries javac adds for classes named only in local variable signatures of its
  debug information;
- the converted `java.base` and a few other modules are compared with `-XDstringConcat=inline`
  and `-parameters` as the JDK build uses them; others need `--add-modules ALL-SYSTEM`.

### Running the converted runtime

The same converted files, compiled to class files (812 classes, as many as the baseline has),
replace the baseline's `clojure/**/*.class`: Clojure starts from them and loads `clojure.core`
with the converted `Compiler`. Clojure's upstream test suite on that runtime
(`CLOJURE_TESTS_RUN=stage0 CLOJURE_SRC=<classes>:<repo> bin/clojure-tests`) has no regressions
against the baseline: 83 namespaces, 809 tests, 20,718 of 20,750 assertions (the same 32
spec-message failures as the baseline), test.generative 27 of 27 (2026-10-06). The scratch steps,
in `.tmp/` of the worktree: copy the converter's output, compile every file with
`arbace.classes.compiler/compile-forms` and `write-classes!`, run the suite. The first run needed
two hand patches of the converter's output (notes 1 and 2 below); its later output needs none and
gives the same result.

## Coverage (§8)

Status: **done** (implemented and tested), ≡ (compared with javac's classes in the tests),
**partial**, **todo**.

### Tree kinds (§8.1)

| kinds | status |
|---|---|
| `COMPILATION_UNIT`, `PACKAGE`, `IMPORT` | done ≡: namespace package, `ns`/`import`, own-package and `java.lang` fallback, `defpackage` (`package-info.class`) |
| `MODULE`, `REQUIRES`, `EXPORTS`, `OPENS`, `USES`, `PROVIDES` | done ≡ (`defmodule`: `module-info.class`, `java.base` mandated, required system modules' versions) |
| `CLASS`, `INTERFACE`, `ENUM`, `RECORD`, `ANNOTATION_TYPE` | done ≡ (top-level, member, inner, local, anonymous; enum constant bodies; compact constructors) |
| `METHOD` | done ≡ (instance, static, abstract, native, default, private interface, varargs, throws, overloads, `deftype`-style untyped signatures) |
| `VARIABLE` | done ≡: fields, parameters, `let`/`loop` locals (`^:mutable`, `^:const`, primitive tags), catch parameters, resources, pattern bindings |
| `BLOCK` | done ≡: bodies, `initializer`, `static-initializer` |
| `MODIFIERS`, `ANNOTATION` | done ≡ (declaration annotations by retention, element values of every kind, defaults) |
| `TYPE_ANNOTATION`, `ANNOTATED_TYPE` | done ≡ in declarations (field, return, parameter, receiver, type parameters and bounds, supertypes, throws, with type paths; `TYPE_USE` annotations on declared names) and in code (local variables, `cast`, `instance?`, `new`, catch parameters); method reference type arguments todo |
| `TYPE_PARAMETER` | done ≡ (`Signature` of classes, methods, fields, record components) |
| `PRIMITIVE_TYPE`, `ARRAY_TYPE`, `PARAMETERIZED_TYPE`, wildcards, `INTERSECTION_TYPE`, `UNION_TYPE` | done ≡ |
| `IF`, `CONDITIONAL_EXPRESSION` | done ≡ |
| `WHILE_LOOP`, `DO_WHILE_LOOP`, `FOR_LOOP`, `ENHANCED_FOR_LOOP` | done ≡ (`while`, `loop`/`recur`, `dotimes`, `for-each` over arrays and `Iterable`s) |
| `LABELED_STATEMENT`, `BREAK`, `CONTINUE`, `RETURN`, `YIELD` | done ≡ (also through `finally` and `locking`) |
| `SWITCH`, `SWITCH_EXPRESSION`, `CASE`, case labels | done ≡ (int-like, `String`, enums with ordinals or `$SwitchMap$`, `nil`, `(nil :default)`, patterns with guards) |
| `ANY_PATTERN`, `BINDING_PATTERN`, `DECONSTRUCTION_PATTERN` | done ≡ (`switch`, `if-instance`, `when-instance`; record patterns with `MatchException` wrapping; consecutive record patterns of one record merged into a nested switch on the first component, as javac's `TransPatterns.processCases`) |
| `THROW`, `TRY`, `CATCH`, `SYNCHRONIZED`, `ASSERT` | done ≡ (`with-resources`, multi-catch, `locking`, `java-assert`, in interfaces through javac's holder class) |
| `IDENTIFIER`, `MEMBER_SELECT` | done ≡ (own and outer fields by name, `C/f`, `(.-f x)`, `Outer/this`, `super`, `Iface/super`) |
| `METHOD_INVOCATION`, `NEW_CLASS` | done ≡ (qualifying types per javac, `invokeinterface`, `super` calls, inner class creation, `(.new o Inner)`, `anon`, `(anon Inner [args] :outer o ...)`, signature polymorphic calls, `C/super` calls and javac's `access$` accessors, `(.super o args)`; param-tags on every call, constructor calls included: `(^[int] super. x)`, `(^[int] this. x)`, `(^[int] .super o x)`, `(^[int] .new o Inner x)`, `(anon C ^[int] [x] ...)`, `(NAME ^[int] [x])`) |
| `NEW_ARRAY`, `ARRAY_ACCESS` | done ≡ |
| `ASSIGNMENT`, compound assignments, increments | done ≡ |
| unary and binary operators | done ≡ (`-int`, long, `-float`, double; bit and shift operators; comparisons; `not`, `and`, `or`, `identical?`, `nil?`, `some?`) |
| `INSTANCE_OF`, `TYPE_CAST` | done ≡ |
| `LAMBDA_EXPRESSION`, `MEMBER_REFERENCE` | done ≡ (`lambda$m$n` methods, captures, instance lambdas, intersection targets with markers, static/bound/unbound/constructor references, `super` references); serializable lambdas and references (javac's hashed names, `altMetafactory` flags, `$deserializeLambda$`; a serialization round trip is tested), variable arity references through lambda methods |
| literals | done ≡ |

### Access flags (§8.2)

| flag | status |
|---|---|
| `ACC_PUBLIC`, `ACC_PRIVATE`, `ACC_PROTECTED`, `ACC_STATIC`, `ACC_FINAL`, `ACC_SUPER` | done ≡ (with Java's implicit modifiers and the `InnerClasses` mapping) |
| `ACC_SYNCHRONIZED`, `ACC_VOLATILE`, `ACC_TRANSIENT`, `ACC_VARARGS`, `ACC_NATIVE`, `ACC_ABSTRACT`, `ACC_INTERFACE` | done ≡ |
| `ACC_SYNTHETIC`, `ACC_BRIDGE` | done ≡ (`this$0`, `val$x`, `$VALUES`, `$values`, `$assertionsDisabled`, lambda methods, `$SwitchMap$` holders, bridges) |
| `ACC_ANNOTATION`, `ACC_ENUM`, `ACC_MANDATED` | done ≡ |
| `ACC_MODULE`, module flags | done ≡ |

### Attributes (§8.3)

| attribute | status |
|---|---|
| `ConstantValue` | done ≡ (static and instance final constant fields; constants of loaded classes read from class files) |
| `Code`, `StackMapTable`, `Exceptions` table | done ≡ |
| `Exceptions` | done ≡ (`:throws`; lambda methods get their interface method's) |
| `Signature` | done ≡ |
| `InnerClasses`, `EnclosingMethod`, `NestHost`, `NestMembers` | done ≡ |
| `PermittedSubclasses` | done ≡ (`:permits`, inferred, enums with constant bodies) |
| `Record` | done ≡ |
| `BootstrapMethods` | done ≡ (`StringConcatFactory`, `LambdaMetafactory`, `ObjectMethods`, `SwitchBootstraps`) |
| `Runtime(In)VisibleAnnotations`, parameter annotations, `AnnotationDefault` | done ≡ (record component annotations propagated by `@Target`) |
| `Runtime(In)VisibleTypeAnnotations` | done ≡ (declarations, and in `Code` for locals, casts, `instanceof`, `new`) |
| `MethodParameters` | done ≡ (inner, local, anonymous and enum constructors, `valueOf`, canonical record constructors) |
| `Deprecated` | done ≡ |
| `Module` | done ≡ |

### Other parts of the spec

| part | status |
|---|---|
| §9.1 one namespace per package, files loaded into it | done (converted files compile; `bin/class-forms-check`) |
| §9.2 class environment: current form, defined classes, class path, source path | done (a name that resolves to nothing is looked up as `p/C.clj` on the class path; its `in-ns`/`ns`/`import` forms are evaluated and its class forms entered as declarations only; tested with two files referring to each other) |
| §9.3 AOT | done (tested: a baseline JVM without the compiler loads the compiled namespace) |
| §10 REPL: package loaders, generations | done (tested) |
| §5.13 Clojure in class bodies | partial: vars (read and call), keywords, quoted data, vector/map/set literals (constants in private static synthetic `const__N` fields set in `<clinit>`, collections built with `RT.vector`/`map`/`set`); `fn`, `letfn`, `case` todo |
| §5.6 reflection is an error | done: unresolved members are compile errors; with `^{:reflection :warn}` on the class (or an enclosing one) unresolved instance method calls go through `clojure.lang.Reflector` with a warning |
| `access$NNN` accessors, `Outer/super` | done ≡ (protected members of a superclass in another package from nested classes and lambdas, `C/super` calls; javac's numbering) |

## Notes for the converter

Found by compiling the converter's output of the baseline (`bin/class-forms-check`):

1. `void.class` is `Void/TYPE`; a bare `void` is no value (`Compiler.java`).
2. Calls of signature polymorphic methods (`MethodHandle.invoke`, `invokeExact`, `VarHandle`
   methods) take the call site descriptor from the param-tags (or the arguments' types) and the
   result type from a tag on the call form: `^boolean (^[Method Object] MethodHandle/.invoke mh m
   target)` for Java's `(boolean) mh.invoke(m, target)` (`Reflector.java`).
3. A static member reached by simple name is qualified by javac with the current class when it is
   a member of it (inherited or not), and otherwise with its *declaring* class: `trimGenID(...)` in
   `Compiler.NewInstanceExpr.ReifyParser` is `Compiler$ObjExpr/trimGenID`, not
   `NewInstanceExpr/trimGenID` (checked with javac on a small example).
4. A cast of `null` is a `checkcast` in javac's code: keep `(cast String nil)` where Java has
   `(String) null`, also when param-tags already pick the overload (`GeneratorAdapter.box`).
5. Method references and lambdas: the instantiated types come from the forms only. Without a
   signature vector (and its tag) the functional interface method's erased types are used.
6. Variable arity calls of generic methods: javac creates the argument array with the inferred
   element type (`Arrays.asList("a", "b")` makes a `String[]`), the compiler with the erased one
   (`Object[]`). Where they differ, write the array: `(Arrays/asList (new String/1 ["a" "b"]))`,
   and for method references that need variable arity adaptation, the lambda javac makes.
7. A field read or assigned by simple name in a nested class resolves, in Java, to an inherited
   field before an enclosing class's field; in the forms only own and enclosing fields are in scope
   by name, so inherited ones must be written `(.-f this)` (seen in the converted JDK, e.g.
   `SingleNodeCounter`).
8. Overloads: integer literals are `long`, so where javac picks an `int` overload for a literal
   argument (`new Symbol(id, -1)` with `(int, int)` and `(int, Object)` constructors) the
   compiler picks another one. Done (2026-10-07): the converter asks the compiler's own
   resolution (`select-method` with `instance-candidates`, `static-candidates`,
   `ctor-candidates`) and pins where it differs, constructor calls included.
9. Captured locals must not be `^:mutable` (Java's effectively final); catch parameters that are
   assigned need `^:mutable`; locals must not shadow the macros the converted code uses (a local
   named `cond` or `when`). All seen in the converted JDK.
10. javac gives the constructor of an anonymous class of an interface that captures locals a
    `Signature` attribute, except when the Java source used the diamond (`new I<>() {...}`); the
    forms do not say which was written, and the compiler always emits it.

## Spec amendments (accepted)

All accepted by the user and folded into SPEC.md; the original texts are in git history
(commit 7229dd9).

1. **Top-level `do` at stage 0, `defclasses`.** Accepted (2026-10-07), folded into SPEC §9.2, §9.5, §10.
2. **Overload resolution as JLS 15.12.2 on erased types, with a literal narrowing phase.** Accepted (2026-10-07), folded into SPEC §5.6.
3. **Clojure's arithmetic in class bodies (`Math.*Exact`, `ldiv`/`lrem`, IEEE on doubles).** Accepted (2026-10-07), folded into SPEC §5.4, §5.13.
4. **Reference tags on `let` bindings are a `checkcast`.** Accepted (2026-10-07), folded into SPEC §5.3.
5. **Enum switches: ordinals only within the same top-level class.** Accepted (2026-10-07), folded into SPEC §5.8, §6.
6. **Integer literals in conditionals.** Accepted (2026-10-07), folded into SPEC §5.4.
7. **Branches of different primitive types boxed each by its own type.** Accepted (2026-10-07), folded into SPEC §5.5.
8. **Nested `java-str` flattened.** Accepted (2026-10-07), folded into SPEC §5.10.
9. **Qualifying types of statics by simple name.** Accepted (2026-10-07), folded into SPEC §7.1.
10. **Signature polymorphic calls.** Accepted (2026-10-07), folded into SPEC §5.6.
11. **`this` in instance initializers.** Accepted (2026-10-07), folded into SPEC §4.5, §4.7, §5.2.
12. **Anonymous subclasses of inner classes, `:outer`.** Accepted (2026-10-07), folded into SPEC §4.8.
13. **javac options.** Accepted (2026-10-07), folded into SPEC §3, §5.10.
