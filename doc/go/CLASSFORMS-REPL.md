# The Java forms at the REPL of the Go build

Status: built (branch `cf-repl`, 2026-10-09/10), the user's decision of 2026-10-09: Java forms
in the Go build's REPL. The amendments CF1-CF6 (§8) were accepted on 2026-10-10 and folded into
J2G-SPEC (§16, "Java forms at the REPL"), B1-PLAN (D6) and ORACLE.md. It reverses, for `defclass` and the code forms,
B1-PLAN.md's D6 ("a first REPL without the Java forms"); `gen-class` stays out.

On the JVM, `defclass` and the code forms ([SPEC §9.5](../classes/SPEC.md)) are compiled by the
one implementation `arbace.classes` into bytecode, at the REPL as in files (SPEC §10). The Go
build has no bytecode and no class loader. This document says how the Go build gives the class
forms their meaning anyway: the same analysis, classes made at run time
([J2G-SPEC §5.12](J2G-SPEC.md), `Dyn`), and an interpreter of the analyzed code.

Since 2026-10-12 (standalone S4, doc/STANDALONE-NOTES.md, "S4: the JVM removed") the JVM's back
end is gone from main and the Go build's way is the only one: `arbace/classes/` holds the front
end, `interp` and `go` (no longer variants: `arbace/classes/interp.ae`, `arbace/classes/go.ae`),
`native` requires `interp` itself, the access flags come from `arbace.classes.opcodes` (alias
`Opcodes`, values not a cut class), and `TypeReference` is `arbace.classes.go` on every host.
The variants `types`, `env`, `analyze` (generic signatures from the world, no source path lookup)
stayed, for the front end still runs on a JVM: repr/java and j2g's edge recording on the frozen jar.
Since 2026-10-13 (35255c7) they are merged into their bases: `arbace.classes.go/go-build?`
(`Compiler$CFGo` exists) selects the world's signatures on Go, reflection on the frozen JVM.
`test/classforms/jvm_check.ae` is gone with the back end. The sections below are as built.

Contents: 1 Measurements · 2 Design · 3 Alternatives considered · 4 What is built ·
5 Checks · 6 Results · 7 What remains · 8 Amendments.

## 1. Measurements (before the work)

`arbace.classes` (the Java forms compiler, 8,400 lines) was not embedded in the program (the
tools are left out, EVAL-NOTES.md "Loading"): `(defclass ...)` at the Go REPL failed at
`Compiler.classForms`' `require` of `arbace.classes.native`. Embedded and loaded by hand:

| namespace | lines | what it needs on Go | outcome |
|---|---:|---|---|
| `arbace.classes.types` | 325 | `Class.getPackage` (the runtime's package name) | fails: jrt has no `Package`; the name is a constant in the Go build |
| `arbace.classes.env` | 385 | `java.util.WeakHashMap` (reflection's cache); ASM's `ClassReader` (constant values from class files) | `WeakHashMap` is not in the world; `ClassReader` is reached only in a `try` (no class files: reflection's view is used) |
| `arbace.classes.parse`, `lower` | 535 | | load |
| `arbace.classes.analyze` | 4,293 | ASM's `Opcodes` (access flags, ~150 uses), `TypeReference` (type annotations, evaluated eagerly), `Type` (accessors only); generic reflection (`getTypeParameters`, `getGenericSuperclass`...) for javac's bridges; `ClassLoader.getResource` (source path lookup, §9.2) | `Opcodes` constants throw (a cut class's members throw); `TypeReference` throws; generic reflection is not in jrt; `getResource` is not |
| `arbace.classes.emit`, `compiler`, `shape`, `verify` | 2,500 | ASM's writers and readers | not needed: no bytecode |

Load time on the evaluator (amd64): 13.8 s for the five namespaces, of which 11.5 s is the
checking of `arbace.core`'s macro specs (`spec.alpha` is loaded by the REPL; every `defn`, `let`,
`fn` of the 5,500 lines is checked); 2.3 s without the checks (the JVM loads these namespaces
AOT-compiled, unchecked). Analysis of a small class once loaded: 17-22 ms (65 ms the first time).

With those differences handled (§4), the analysis runs on Go unchanged and gives the same typed
node trees as on the JVM.

## 2. Design

### 2.1 Overview

```
(defclass ...)  ──Compiler (translated)──▶ ClassFormsExpr ──▶ arbace.classes.native (Go variant)
     │                                                                │
     │                         arbace.classes.analyze (evaluated): declarations, members,
     │                         derivations, code ──▶ typed node trees (as for emit)
     │                                                                │
     │                         arbace.classes.interp (evaluated): Klass per class, field layout,
     │                         vtables; per method a tree of Compiler$CF nodes
     │                                                                │
     └──────────────▶ classes made at run time (Compiler$CFGo, natives over Dyn): Class objects,
                      member tables (reflection), Dyn slots (translated code's interface calls)
```

1. **The front end is arbace.classes's own.** The analysis namespaces are embedded in the program
   (`arbace.j2g.embed`), with four small Go-build namespace variants (`arbace/lang/go/ns/classes/`,
   J2G-SPEC §10.3, amendment M1): `types` (the runtime's package name), `env` (a `HashMap` for
   reflection's cache), `analyze` (ASM's `TypeReference` and the world's generic signatures from
   `arbace.classes.go`, no source path lookup yet), `native` (the classes interpreted, below).
   ASM's `Opcodes`, which the analysis reads ~150 times, is a cut class whose constants are now
   readable (§8, CF1). They are loaded on the first Java form, with the macro spec checks off
   for the load (the `Compiler` variant's `classForms`).
2. **The back end is an interpreter of the analyzed nodes.** `arbace.classes.native`'s boundary
   (`compile-top`, `compile-fn`, `compile-deftype`) is kept; in the Go build its
   `compile-and-load!` is `arbace.classes.interp`'s, which builds, from the node trees emit would
   compile, trees of interpreter nodes (`Compiler$CF$...`, Java forms in the `Compiler` variant
   `arbace/lang/go/ClassForms.ae`, translated by j2g like the evaluator), one per method, and
   makes the classes.
3. **Classes are classes made at run time.** A class extending `Object` (or another interpreted
   class) is a `Dyn` class (J2G-SPEC §5.12): its objects are `Dyn` values, their fields `Dyn`'s
   `F`, so translated code calls their methods through Go's interfaces (every interface of the
   world, by slot) and Clojure code through reflection (the member tables the natives fill, with
   the members' modifiers); interfaces of Java forms are interfaces made at run time; a Clojure
   fn's class (`fn` in a class body, a fn handed over by `compile-fn`) has `CF$FnObj` objects,
   a translated `RestFn` choosing the class's `invoke` by arity.

### 2.2 The interpreter

- **Nodes evaluate themselves, typed.** `Compiler$CF$Node` has `eval` (an object), `evalJ` (a
  `long`, for `int`, `long`, `short`, `byte`, `char`, `boolean`), `evalD` (a `double`, for
  `float` and `double`), `exec` (a statement) and `test` (Java's or Clojure's truth). Each node
  overrides the one of its kind; the others convert. So `int` arithmetic in a loop allocates
  nothing: `ArithJ` computes in `long` with Java's `int` operations (`unchecked-add-int` on the
  values cast to `int`: j2g's Go has Java's wrap-around, division and shift semantics), `ArithD`
  rounds each `float` result to `float`. About 70 node classes, one per kind of analyzed node
  (`:arith`, `:convert`, `:compare`, `:invoke`, `:new`, `:get-field`, `:switch`, `:lambda`...).
- **Frames** hold the slots of one invocation: references in `Object[]`, the rest in `long[]`
  (doubles by their bits); each local of a method has its own slot (the builder numbers them).
- **Jumps** (`return`, `break`, `continue`/`recur`) set the frame's pending jump (a code per target:
  its break or its recur) and value; every node that evaluates children checks it after each
  and unwinds; a `loop` restarts on its recur code, a `label` ends on its break code, `try`
  runs `finally` code keeping the pending jump unless the `finally` code jumps itself. No Go
  panic is involved, so `recur` costs a compare.
- **Calls.** Interpreted methods are `CF$Meth`: their frame layout, parameter slots and body. A
  static, private or super call of an interpreted method calls it directly, the arguments
  evaluated typed into the callee's frame; a virtual or interface call looks the receiver's class
  up (`klassOf`, inline-cached) and its vtable by name and descriptor, else calls by reflection
  (a method of the world on any object, or inherited from `Object`). Calls of the world's classes
  are reflective (`java.lang.reflect.Method.invoke` over jrt's member tables), resolved once,
  when the node is built.
- **Classes** are `CF$Klass`: the interpreted superclass (else the world's), interfaces, the
  instance field layout (the superclass's fields first, then its own, then `this$0` and the
  captured values `val$x` of inner, local and anonymous classes, stored before the constructor
  runs), static fields, the methods it declares, its vtable (inherited, interface defaults, its
  own), constructors, the static initializer, run on first use as JLS 12.4.2 says (superclass
  first; a failed one is `ExceptionInInitializerError`, then `NoClassDefFoundError`).
- **Lambdas and method references** are objects of a class per site (named as the JVM names its
  hidden classes, `Outer$$Lambda/N`), implementing the functional interface and the markers;
  their method is the lambda's body, its frame filled with the captured values (the enclosing
  instance first). Method references are lambdas whose body is the call, with
  `LambdaMetafactory`'s adaptations (boxing, unboxing, widening, casts).
- **What javac derives** that emit writes as bytecode is interpreted from the declarations:
  bridges (a virtual call of the target), record accessors, `equals`, `hashCode` and `toString`
  (as `ObjectMethods` makes them), and the analysis's `:clj-consts` (vars and quoted constants,
  read when the node is built). javac's accessors (`access$NNN`) are not made: the interpreter
  checks no access.
- **Exceptions** are Java's: a `try` node is a Java forms `try` (Go's panics and recovers through
  j2g), its clauses matched by `Class.isInstance` (an interpreted exception class by its
  `Klass`).

### 2.3 The run-time classes (Compiler$CFGo, `j2g_cf.go`)

The natives of `Compiler$CFGo` (`arbace.j2g.dyncf`):

- `defineClass(name, super, interfaces, flags, kind, klass)`: a `jrt.Class` made at run time
  (`jrt.DefineDynamic`, so that a later `defclass` of the name replaces it, as SPEC §10's
  generations do), `Dyn`'s `DynClass` with the interfaces of the class and of its superclasses
  (Dyn's nominal check) and the `CF$Klass` (a new field `CF` of `DynClass`); its `IsInstance`
  follows the superclass chain; kind 1 is an interface (`DynImplements`), kind 2 a Clojure fn's
  class.
- `alloc(class, values)`: a `Dyn` of the class with these field values.
- `setMethod(class, name, params, ret, flags, impl)`: a static method's member table entry, or for
  an instance method `Dyn`'s slot of its name and descriptor (translated code's interface calls)
  and the class's method by key, and a member table entry that calls the receiver's own method of
  that key (virtual, as `Method.invoke`).
- `addCtor`, `addField`, `addStatic`: constructors, fields with their types and modifiers (`Field.get`
  and `set` convert as the member tables' convention says), static fields (their class
  initialized first).

Interpreted classes are registered by name (`Class.forName`, `RT.classForName`), so Clojure code
and later Java forms name them; the analysis of later Java forms takes their declarations from
`env/defined`, as the JVM's compiler takes them from the classes it defined, and their code is
linked to the newest class of a name, as SPEC §10 says for the JVM.

## 3. Alternatives considered

- **Interpret JVM bytecode.** Run emit as on the JVM and interpret the class files: complete
  fidelity (whatever emit writes runs), no second meaning of the nodes. Rejected: ASM's writer
  and reader (and emit with its `COMPUTE_FRAMES`, class hierarchy queries included) would have to
  be translated into the program (ASM is erased from the world, J2G-SPEC §10.1, amendment E2),
  `invokedynamic`'s bootstraps (`LambdaMetafactory`, `StringConcatFactory`, `ObjectMethods`,
  `SwitchBootstraps`, Clojure's reflective call sites) reimplemented, and a stack machine boxes or
  needs a typed operand stack; two levels of indirection (nodes to bytecode to execution) where
  one suffices.
- **Interpret the analyzed node maps in Clojure.** No Java forms to translate: the interpreter
  would be a namespace evaluated by the evaluator. Rejected for speed: two levels of
  interpretation (the evaluator running an interpreter), each node a keyword lookup and an
  evaluated fn call, about a hundred times slower than nodes translated to Go. The builder
  (maps to node objects, once per method) is that namespace: it runs once.
- **Translate Java forms to Go at run time.** There is no Go compiler in the program (nor in the
  box, B1b). Rejected; AOT translation of Java forms in files is j2g's, not the REPL's.
- **Compile to Go closures** (each node a Go `func`, as EVAL-PLAN §6 proposed for the evaluator,
  built in step 7b, per method at its first call): faster dispatch, but closures cannot be written
  in Java forms; the node objects are its equivalent in Java forms (an interface call per
  node). Step 7 did not lower the interpreter further.
- **A carrier class per superclass of the world** (`CF$Ex extends RuntimeException` ...) instead of
  `Dyn`'s `DynSub_C`: rejected for the general case (each overridable method written by hand);
  used for Clojure fn classes only (`CF$FnObj`), whose superclass `RestFn` has one entry point.
- **Analysis in Java forms** (porting arbace.classes to the Java subset, so that j2g translates
  it): the analysis is 5,500 lines of Clojure; evaluated it costs 2.3 s once and 20 ms per class.
  Not worth a port now; step 6's image (the embedded namespaces pre-analyzed) is the remedy in
  place; AOT of namespaces to Go is not scheduled (as of 2026-10-10; since S6 the namespaces are
  compiled to Go by default, `ARBACE_AOT=1`, doc/STANDALONE-NOTES.md, "S6: Arbace compiled to Go").

## 4. What is built

As built (2026-10-10). Today `Compiler$CF` and `Compiler$CFGo` are in `arbace/lang/Compiler.ae`
(the variants merged, SA150, f862ece), `interp` and `go` in `arbace/classes/`, and the `.subst`
variants and `test/classforms/jvm_check.ae` are gone (S4, e831e8d; `types`, `env`, `analyze` 35255c7).

| where | what |
|---|---|
| `arbace/lang/go/ClassForms.ae` | the interpreter (`Compiler$CF`: `Rt`, `Host`, `Frame`, `Node` and its kinds, `Klass`, `Meth`, `MethodFn`, `CtorFn`, `FnObj`, the JVM's `TestHost`); the Go host (`Compiler$CFGo`: `GoHost`, the natives); `Compiler.classForms` loading the Java forms without spec checks |
| `arbace/lang/go/ns/classes/interp.ae` | `arbace.classes.interp`: the builder (analyzed nodes to interpreter nodes), the classes' layouts, vtables, publication to the host, `define!` and `compile-and-load!` |
| `arbace/lang/go/ns/classes/go.ae` | `arbace.classes.go`: ASM's `TypeReference` as the analysis computes it, the world's generic signatures |
| `arbace/lang/go/ns/classes/*.subst.ae` | the Go build's variants of `types`, `env`, `analyze`, `native` |
| `arbace/j2g/dyncf.ae` | the natives of `Compiler$CFGo` (`j2g_cf.go`); `arbace/classes/generics.edn` (the world's generic signatures, embedded) |
| `arbace/j2g/embed.ae` | the analysis namespaces embedded (not emit, compiler, shape, verify, build, boot) |
| `arbace/j2g/out.ae` | a cut class's constants are readable (§8, CF1) |
| `arbace/j2g/dyn.ae`, `main.ae` | `DynClass.CF`; `j2g_cf.go` and `generics.edn` written |
| `test/oracle/forms/defclass.ae` | the oracle's forms for `defclass` at the REPL |
| `test/classforms/jvm_check.ae` | the interpreter on the JVM against the compiled classes (static calls) |

## 5. Checks

- `bin/arbace-j test/classforms/jvm_check.ae [FILE...]`: on the JVM, every `defclass` of the file
  compiled and interpreted (over the test host, without Class objects), every form calling only
  static methods of the classes evaluated both ways and compared.
- `bin/oracle check '<go executable> -' forms/defclass`: the oracle's `defclass` forms (recorded on
  the JVM) on the Go build.

## 6. Results

On amd64 (this machine, 2026-10-09/10), the branch `cf-repl`:

- **The oracle's `defclass` forms** (`test/oracle/forms/defclass.ae`, 213 cases recorded on the
  JVM: arithmetic and conversions, control flow, strings, arrays, objects, static
  initialization (`ExceptionInInitializerError`, then `NoClassDefFoundError`), inheritance and
  interfaces (default and static methods, `Comparable` through `sort`), the world's interfaces
  implemented and called by the runtime (`Iterable`, `Counted`, `ILookup`), inner, nested, local
  and anonymous classes, lambdas and method references, `switch` (int, `String`, enum,
  patterns), records, enums (constants with bodies, `valueOf`, `values`, `getEnumConstants`
  constants), exceptions of Java forms (extending `Exception`, `IllegalStateException`),
  `AFn` subclasses as Clojure fns, Clojure in class bodies, code forms in fns and `deftype`
  handed over (`compile-fn`, `compile-deftype`), reflection, redefinition at the REPL): 212 of
  213 match on Go. The one difference: `(Base.)` of an abstract class is
  `InstantiationException` (the evaluator constructs by reflection) where the JVM's compiled
  `new` gives `InstantiationError`; recorded in `test/oracle/known-go-amd64.edn`. (Since the
  branch `gofix`, 2026-10-10, the evaluator's resolved `new` of an abstract class throws
  `InstantiationError` as compiled code does: EVAL-NOTES.md, "The suite's Java fixtures".)
- **The Java forms tests' corpus** (`test/oracle/forms/defclass_corpus.ae`, generated by
  `test/classforms/gen_corpus.ae` from `test/classes/*_test.ae`, both on `arbace-for-java-26` only since S4, the corpus kept as data: the 65 vectors of class
  forms the tests compile, each defined at the REPL by `defclasses`; 55 define, 10 are the
  tests' error cases, failing with the same messages): 131 of 131 match on Go. Every method of
  every class is built when the class is defined, so this checks the builder on all of them.
- **A class script through defclass** (`test/classforms/class_scripts.ae`): `arbace.lang.Murmur3`
  defined at the REPL from its Java forms (`arbace/lang/Murmur3.ae`, as `cf.lang.Murmur3`) and
  the oracle's `Murmur3.ae` script run on it: 189 of 189 steps match, on the JVM (compiled) and on
  Go (interpreted). The other scripts' classes are recognized by identity in the runtime or the
  driver, or extend classes without a DynSub type (the tool says which).
- **No regression**: the whole oracle on Go after merging main `b20b577` (`bin/oracle check ...
  --expected test/oracle/known-go-amd64.edn`): 20,909 of 20,944 cases, the 35 recorded mismatches
  (the case above among them), none new, none newly passing; `bin/oracle check jvm` passes
  (20,944 of 20,944); `bin/arbace-go --smoke` passes.
- **On the JVM**, `test/classforms/jvm_check.ae` runs the interpreter against the compiled classes
  (over the test host): 74 forms calling static methods match; the others need Class objects.

**Times** (amd64; `test/classforms/bench.ae`, second run; JVM warm, `bin/arbace-j`):

| | JVM, compiled | Go, interpreted | Go, the same as a Clojure fn (evaluator) |
|---|---:|---:|---:|
| `sumSq` 10⁶ (`int` loop, `long` sum) | 0.4 ms | 76 ms | 3,067 ms |
| `fib 22` (static recursion) | 0.1 ms | 8.6 ms | 120 ms |
| `sieve` 10⁶ (`boolean[]`) | 6.0 ms | 338 ms | |
| `objs` 10⁵ (`new`, a virtual call) | 0.4 ms | 76 ms | |
| `strs` 10⁵ (`java-str`, `StringBuilder`) | 11 ms | 54 ms | |

The interpreter is 14-40 times faster than the evaluator on the same work and 5-200 times slower
than HotSpot's compiled code. The first Java form of a session loads the analysis: 3.2 s (2.3 s
of it evaluating the namespaces); then a small class takes 10-60 ms to define.

**Size** (on `cf-repl`, before SZ1-SZ4, unstripped): the static amd64 executable is 74.0 MB against
63.1 MB for main at `b20b577` (built the same way, same day): +10.9 MB (+17%). Most of it is the
DynSub types of the nine new superclasses (about 0.5 MB each: `j2g_dyn.go`'s forms grow from 5.1 to
9.7 MB); then the interpreter and the host (the `Compiler` variant's forms grow by 350 KB,
`j2g_cf.go` 81 KB, and their member tables), the embedded analysis (360 KB of sources) and
`generics.edn` (106 KB, restricted to what an interpreted class can extend or implement; 674 KB for
the whole world). Since SZ1-SZ4 (stripped, the `DynSub_C` types sharing one `dynCore`; JRT-NOTES.md,
"Size: trimming the executable") the whole executable was 61.9 MB; about 63-65 MB since
java.security and java.text.

## 7. What remains

- **Superclasses of the world** beyond the list (`arbace.j2g.dyn/class-supers`: `Throwable`,
  `Exception`, `RuntimeException`, `Error`, `IllegalArgumentException`, `IllegalStateException`,
  `Enum`, `Record`, `AFn`, and the proxies' `Writer`, `Reader`, `PushbackReader`, `InputStream`,
  `OutputStream`, `APersistentMap`, `ThreadLocal`, `BufferedWriter`): a class extending another
  class of the world is refused when defined ("cannot extend ... yet"). Leaf classes
  (`UnsupportedOperationException`) cannot get a DynSub type without changing their translation.
  Each addition costs about 0.5 MB.
- **Stack traces** show no interpreted frames (the evaluator's do, EVAL-NOTES "Stack traces");
  `CF$Frame` would need the method and the line.
- **Reflection's view**: `getDeclaredMethods` of an interpreted class lists the inherited methods
  it was given for dispatch; protected and package-private members of classes of the world are
  not reachable (jrt's member tables list public ones), so Java forms using them fail when
  defined.
- Assertions are always disabled (the JVM's
  default); javac's accessors are not made (no access checks at run time); serializable lambdas
  cannot be deserialized; varargs method references; annotations of classes of the world
  (`getAnnotation`); `defmodule` and `defpackage` write nothing (no class files). The source path
  lookup (§9.2) works on Go since JC4 (doc/classes/JAVA-CONFORMANCE-SURVEY.md, "As built: JC4"):
  `p/C.ae` on ARBACE_PATH or among the embedded sources (`RT/resourceAsStream`), then the file
  being loaded.
- Fixed by JC4 (found by jdk26u's tests): a private instance method called on any receiver
  (`p.run()`, `this.run()`: the builder's `:invoke` of kind `:virtual`, javac's `invokevirtual`
  of a nestmate's private method, is a `CallI` of the method itself, as §2.2 says; before, a
  `CallV` found no private method in the vtable and ended in `AbstractMethodError`), `Object`'s
  methods called through an interface of the world (`Map.toString()`: `real-method` falls back
  to `Object`'s, JLS 9.2), and `Object.clone` of an interpreted `Cloneable` object (`Rt/cloneOf`:
  a shallow copy of its fields; `CloneNotSupportedException` for a class not `Cloneable`; a class
  with a superclass of the world still not, J2G-SPEC §5.12's `DynSub` part).
- **Speed**: frames are allocated per call and arguments of reflective calls boxed. Not covered
  by step 7: its remedies (direct invokers of the member tables, closures; step 7b) were done
  for the evaluator only (step 7a's runtime changes apply to all Go code); open.

## 8. Amendments

All accepted by the user on 2026-10-10 and folded where each says (size: JRT-NOTES.md, "Size:
trimming the executable", SZ1-SZ4):

- **CF1** A cut class's constants (static final primitive or `String` fields) are readable: its
  member table's getter returns the JVM's value instead of throwing (`arbace.j2g.out`). The class
  forms' analysis reads ASM's `Opcodes` about 150 times. Folded: J2G-SPEC §4.1.
- **CF2** The Java forms compiler's analysis is embedded in the Go program (`types`, `env`,
  `parse`, `lower`, `analyze`; not the back end) with the namespace variants of §4, and loaded on
  the first Java form without checking macro specs (`Compiler.classForms`' variant), as the JVM
  loads it compiled. Folded: J2G-SPEC §10.3.
- **CF3** The Java forms at the REPL are interpreted (§2): the interpreter is the `Compiler`
  variant's `Compiler$CF`, the host `Compiler$CFGo` with j2g's natives (`j2g_cf.go`,
  `arbace.j2g.dyncf`), the builder `arbace.classes.interp` (a Go-build namespace). This reverses
  B1-PLAN's D6 for `defclass` and the code forms; `gen-class` stays out. Folded: J2G-SPEC §10.4,
  B1-PLAN.md D6.
- **CF4** J2G-SPEC §5.12: `DynClass` gains a field `CF` (the interpreted class), and the
  DynSub types are made for `class-supers` too (so these classes can also be proxied); an
  interpreted class extending one has DynSub objects, with natives for allocation without
  constructor, the superclass's constructor on an allocated object, and its implementations
  (super calls). Folded: J2G-SPEC §5.12.
- **CF5** `j2g --program` embeds `arbace/classes/generics.edn`: the generic signatures of the
  world's interfaces and extendable classes, and the constructors of abstract ones, which jrt's
  reflection does not have (javac's bridges need them). Folded: J2G-SPEC §10.3.
- **CF6** ORACLE.md's exclusion of the Java forms is lifted: `forms/defclass.ae` and
  `forms/defclass_corpus.ae` (generated) check them; `test/classforms/` holds the JVM-side check,
  the class scripts through defclass and the timings. Folded: ORACLE.md (contents, exclusions).
