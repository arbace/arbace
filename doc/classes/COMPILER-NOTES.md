# Class forms compiler: implementation notes

The implementation of [SPEC.md](SPEC.md) (agenda step 2): one plain-Clojure compiler of the class
forms (§12 question 17) that runs on the frozen baseline (stage 0) and emits bytecode with ASM.
These notes say how it is built, how to use and test it, what it covers (the §8 tables), and
where implementing the spec suggested changes to it (proposed amendments, last section).

## Layout

| file | namespace | role |
|---|---|---|
| `arbace/classes/boot.clj` | `arbace.classes.boot` | stage-0 driver: loads the others mapping `arbace.asm.*` to `clojure.asm.*`, interns `arbace.classes.core` into `clojure.core` |
| `arbace/classes/types.clj` | `arbace.classes.types` | type forms (§4.3), descriptors, erasure, `Signature` strings |
| `arbace/classes/env.clj` | `arbace.classes.env` | class environment (§9.2): class infos from the compilation, from earlier definitions and by reflection (constants from class files); member lookup and access; package class loaders (§10) |
| `arbace/classes/parse.clj` | `arbace.classes.parse` | syntax of class forms and members, class access flags |
| `arbace/classes/analyze.clj` | `arbace.classes.analyze` | entering classes (headers, members, derived members), name resolution, code analysis into typed nodes |
| `arbace/classes/emit.clj` | `arbace.classes.emit` | bytecode for nodes and classes, `InnerClasses` by a post-pass over the constant pool |
| `arbace/classes/compiler.clj` | `arbace.classes.compiler` | one compilation: enter, analyze, emit; define and/or write the classes |
| `arbace/classes/core.clj` | `arbace.classes.core` | the user-facing macros and operators (what `arbace.core` will hold) |
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
  Constant expressions fold; constant fields get `ConstantValue` and are inlined.

## Stage-0 limits

- Class bodies compile the Java subset (§5.13) plus vars: a symbol naming a var reads it and a
  call of a var invokes its function (`clojure.lang.RT/var`, `IFn.invoke`). Keywords, collection
  literals, `fn`, `case`, `letfn` are errors in class bodies.
- `anon`, `letclass`, `lambda`, `switch` and the other new code forms work inside class bodies
  only (the frozen compiler does not know their special forms).
- `defclass` must be evaluated (it compiles at macroexpansion time); a top-level `do` of class
  forms is seen form by form by the frozen compiler, hence `defclasses` (amendment 1).

## Checking converted code

`bin/class-forms-check DIR [PACKAGE-DIR]` compiles converted files (one file per Java file:
`in-ns`, `import`, `defclass` forms) without defining anything, and compares the shape of every
class with the class of the same name on the class path. For `clojure/lang` and `clojure/asm`
those are the baseline's javac classes. On 2026-10-06 the converter's output for the 183 baseline
files (read from its work area, not committed) gave: `clojure/lang` 138 of 139 files
shape-identical to javac, `clojure/asm` 36 of 36, `clojure/asm/signature` 3 of 3,
`clojure/asm/commons` 4 of 5, `clojure/java/api` 1 of 1, after patching two converter issues by
hand (notes for the converter, below); the two remaining differences are converter issues too.

### Running the converted runtime

The same converted files, compiled to class files (812 classes, as many as the baseline has),
replace the baseline's `clojure/**/*.class`: Clojure starts from them and loads `clojure.core`
with the converted `Compiler`. Clojure's upstream test suite on that runtime
(`CLOJURE_TESTS_RUN=stage0 CLOJURE_SRC=<classes>:<repo> bin/clojure-tests`) has no regressions
against the baseline: 83 namespaces, 809 tests, 20,718 of 20,750 assertions (the same 32
spec-message failures as the baseline), test.generative 27 of 27 (2026-10-06). The scratch steps,
in `.tmp/` of the worktree: copy the converter's output, patch it as notes 1 and 2 below say, compile
every file with `arbace.classes.compiler/compile-forms` and `write-classes!`, run the suite.

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
| `TYPE_ANNOTATION`, `ANNOTATED_TYPE` | todo |
| `TYPE_PARAMETER` | done ≡ (`Signature` of classes, methods, fields, record components) |
| `PRIMITIVE_TYPE`, `ARRAY_TYPE`, `PARAMETERIZED_TYPE`, wildcards, `INTERSECTION_TYPE`, `UNION_TYPE` | done ≡ |
| `IF`, `CONDITIONAL_EXPRESSION` | done ≡ |
| `WHILE_LOOP`, `DO_WHILE_LOOP`, `FOR_LOOP`, `ENHANCED_FOR_LOOP` | done ≡ (`while`, `loop`/`recur`, `dotimes`, `for-each` over arrays and `Iterable`s) |
| `LABELED_STATEMENT`, `BREAK`, `CONTINUE`, `RETURN`, `YIELD` | done ≡ (also through `finally` and `locking`) |
| `SWITCH`, `SWITCH_EXPRESSION`, `CASE`, case labels | done ≡ (int-like, `String`, enums with ordinals or `$SwitchMap$`, `nil`, patterns with guards) |
| `ANY_PATTERN`, `BINDING_PATTERN`, `DECONSTRUCTION_PATTERN` | done ≡ (`switch`, `if-instance`, `when-instance`; record patterns with `MatchException` wrapping) |
| `THROW`, `TRY`, `CATCH`, `SYNCHRONIZED`, `ASSERT` | done ≡ (`with-resources`, multi-catch, `locking`, `java-assert`, in interfaces through javac's holder class) |
| `IDENTIFIER`, `MEMBER_SELECT` | done ≡ (own and outer fields by name, `C/f`, `(.-f x)`, `Outer/this`, `super`, `Iface/super`) |
| `METHOD_INVOCATION`, `NEW_CLASS` | done ≡ (qualifying types per javac, `invokeinterface`, `super` calls, inner class creation, `(.new o Inner)`, `anon`, signature polymorphic calls, `C/super` calls and javac's `access$` accessors); `(.super o args)` todo |
| `NEW_ARRAY`, `ARRAY_ACCESS` | done ≡ |
| `ASSIGNMENT`, compound assignments, increments | done ≡ |
| unary and binary operators | done ≡ (`-int`, long, `-float`, double; bit and shift operators; comparisons; `not`, `and`, `or`, `identical?`, `nil?`, `some?`) |
| `INSTANCE_OF`, `TYPE_CAST` | done ≡ |
| `LAMBDA_EXPRESSION`, `MEMBER_REFERENCE` | done ≡ (`lambda$m$n` methods, captures, instance lambdas, intersection targets with markers, static/bound/unbound/constructor references, `super` references); serializable lambdas (`$deserializeLambda$`) and variable arity method references todo |
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
| `Runtime(In)VisibleTypeAnnotations` | todo |
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
| §5.13 Clojure in class bodies | partial: vars (read and call); keywords, collection literals, `fn` todo |
| §5.6 reflection is an error | done (unresolved members are compile errors; there is no reflective fallback) |
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

## Proposed spec amendments

1. **Top-level `do` at stage 0 (§9.2).** The frozen compiler evaluates the forms of a top-level
   `do` one by one, so a `defclass` macro cannot see its siblings. Stage 0 adds
   `(defclasses (defclass A ...) (defclass B ...))`, which compiles the class forms together.
   Arbace's own compiler can treat a top-level `do` as the spec says; `defclasses` can stay as the
   explicit form.
2. **Overload resolution (§5.6).** Implemented as JLS 15.12.2 on erased types (phase 1 strict,
   phase 2 with boxing, phase 3 variable arity, each picking the most specific method), plus a
   last phase in which integer literals narrow to `int`/`short`/`byte`/`char` parameters. This
   is close to javac, so the converter needs fewer param-tags than with Clojure's own rules, and
   Clojure literals still choose `long` overloads first (`(.append sb 5)` is `append(long)`).
   Proposed text: "Overload resolution follows JLS 15.12.2 on erased types; integer literals
   are `long` and narrow to a smaller integral parameter only when no method applies otherwise."
3. **Clojure's arithmetic in class bodies (§5.4).** `+`, `-`, `*`, `inc`, `dec` on primitive
   `long`s compile to `Math.addExact` and friends and `quot`/`rem` to `ldiv`/`lrem` (Clojure's
   meaning, overflow throwing `ArithmeticException`); on `double`s to the IEEE instructions. This
   keeps hand-written class bodies free of the runtime without changing what these mean.
4. **Tags on `let` bindings with a reference type (§5.3).** When the initializer's static type
   is not assignable to the tag, the binding is a `checkcast` (Clojure's hint, made verifiable),
   rather than an error.
5. **Enum switches (§5.8, §6).** javac uses ordinals directly only for enums declared in the same
   top-level class as the switch (`Lower.mapForEnum`: the enum's tree must be in the class being
   translated), and the `$SwitchMap$` holder otherwise, even for enums compiled in the same javac
   run. The compiler does the same. Proposed text for §6: "ordinals directly for enums nested in
   the same top-level class, otherwise ...". The holder is named after all anonymous classes of
   the top-level class (javac creates it while lowering).
6. **Integer literals in conditionals (§5.4).** A conditional (`if`, `cond`, `switch`) whose
   values are all integer literals takes an `int` type where the context needs one (argument of an
   `int` parameter or `-int` operator, initializer of an `int` local), as Java's `c ? 1 : 0` does.
   The spec lists "a branch whose other branch is `int`"; this extends it to all-literal
   conditionals, which the converter produces (`(.substring s (if k 1 0))`).
7. **Branches of different primitive types (§5.5).** Following Clojure's `if`, branches of
   different primitive types are boxed each by its own type (`(if c 1.5 2)` gives `Double` or
   `Long`), not promoted. The converter writes Java's promotions explicitly, as §5.5 says.
8. **Nested `java-str` (§5.10).** Nested `java-str` forms are flattened into one call site, as
   javac flattens nested string concatenation.
9. **Qualifying types of statics by simple name (§7.1)**: see note 3 for the converter; the
   spec's "the current class for an inherited static called by simple name" holds only when the
   member belongs to the current class.
10. **Signature polymorphic calls (§5.6)**: see note 2; the spec does not say how they are written.
