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

## Coverage (§8)

Status: **done** (implemented and tested, javac comparison where marked ≡), **partial**, **todo**.

### Tree kinds (§8.1)

| kinds | status |
|---|---|
| `COMPILATION_UNIT`, `PACKAGE`, `IMPORT` | done: namespace package, `ns`/`import`, own-package and `java.lang` fallback; `defpackage` todo |
| `MODULE` ... | todo (`defmodule`) |
| `CLASS`, `INTERFACE` | done ≡ (top-level, member, inner, local, anonymous) |
| `ENUM`, `RECORD`, `ANNOTATION_TYPE` | todo |
| `METHOD` | done ≡ (instance, static, abstract, native, default, private interface, varargs, throws, overloads) |
| `VARIABLE` | done: fields ≡, parameters, `let`/`loop` locals (`^:mutable`, `^:const`, primitive tags), catch parameters |
| `BLOCK` | done ≡: bodies, `initializer`, `static-initializer` |
| `MODIFIERS`, `ANNOTATION` | done for modifiers ≡; annotations partial (declarations; type annotations todo) |
| `TYPE_PARAMETER` | partial: `:type-params` and `Signature` (class, method, field) |
| `PRIMITIVE_TYPE`, `ARRAY_TYPE`, `PARAMETERIZED_TYPE`, wildcards, `INTERSECTION_TYPE` | done in declarations; `UNION_TYPE` (multi-catch) done ≡ |
| `IF`, `CONDITIONAL_EXPRESSION` | done ≡ |
| `WHILE_LOOP`, `DO_WHILE_LOOP`, `FOR_LOOP` | done ≡ (`while`, `loop`/`recur`, `dotimes`) |
| `ENHANCED_FOR_LOOP` | todo (`for-each`) |
| `LABELED_STATEMENT`, `BREAK`, `CONTINUE`, `RETURN` | done ≡ (also through `finally` and `locking`) |
| `YIELD`, `SWITCH`, `SWITCH_EXPRESSION`, `CASE`, patterns | todo |
| `THROW`, `TRY`, `CATCH`, `SYNCHRONIZED` | done ≡; `with-resources` todo |
| `ASSERT` | todo (`java-assert`) |
| `IDENTIFIER`, `MEMBER_SELECT` | done ≡ (own and outer fields by name, `C/f`, `(.-f x)`, `Outer/this`, `super`) |
| `METHOD_INVOCATION`, `NEW_CLASS` | done ≡ (qualifying type per JLS 13.1, `invokeinterface`, `super` calls, `Iface/super`, inner class creation, `anon`); `(.new o Inner)` todo |
| `NEW_ARRAY`, `ARRAY_ACCESS` | done ≡ (`(new int/2 n)`, `(new int/1 [..])`, multi-index `aget`) |
| `ASSIGNMENT`, compound assignments, increments | done ≡ (`set!` of locals, own fields, `(.-f x)`, `C/f`, `aset`) |
| unary and binary operators | done ≡ (`-int`, long, `-float`, double; bit and shift operators; comparisons; `not`, `and`, `or`, `identical?`, `nil?`, `some?`) |
| `INSTANCE_OF`, `TYPE_CAST` | done (`instance?`, `cast` → `checkcast`, primitive `unchecked-*`); `if-instance` todo |
| `LAMBDA_EXPRESSION`, `MEMBER_REFERENCE` | todo |
| literals | done ≡ |

### Access flags (§8.2)

| flag | status |
|---|---|
| `ACC_PUBLIC`, `ACC_PRIVATE`, `ACC_PROTECTED`, `ACC_STATIC`, `ACC_FINAL`, `ACC_SUPER` | done ≡ (with Java's implicit modifiers and the `InnerClasses` mapping) |
| `ACC_SYNCHRONIZED`, `ACC_VOLATILE`, `ACC_TRANSIENT`, `ACC_VARARGS`, `ACC_NATIVE`, `ACC_ABSTRACT`, `ACC_INTERFACE` | done ≡ |
| `ACC_SYNTHETIC` | done for derived fields (`this$0`, `val$x`) ≡ |
| `ACC_BRIDGE` | todo (bridges) |
| `ACC_ANNOTATION`, `ACC_ENUM`, `ACC_MANDATED`, `ACC_MODULE` | flags computed; tests todo with enums, annotations, modules |

### Attributes (§8.3)

| attribute | status |
|---|---|
| `ConstantValue` | done ≡ (static and instance final constant fields) |
| `Code`, `StackMapTable`, `Exceptions` table | done ≡ |
| `Exceptions` | done ≡ |
| `Signature` | done for class, field, method and javac's constructor signatures for captured locals ≡; record components todo |
| `InnerClasses`, `EnclosingMethod`, `NestHost`, `NestMembers` | done ≡ |
| `PermittedSubclasses` | implemented, test todo |
| `Record` | implemented, test todo |
| `BootstrapMethods` | todo (lambdas, `java-str`, switches; records' `ObjectMethods` implemented) |
| `Runtime(In)VisibleAnnotations`, parameter annotations | implemented (retention by `@Retention`), test todo |
| type annotations | todo |
| `AnnotationDefault` | implemented, test todo |
| `MethodParameters` | done ≡ for inner, local and anonymous class constructors; enums and records todo |
| `Deprecated` | done ≡ |
| `Module` | todo |

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
