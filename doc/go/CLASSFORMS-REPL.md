# The class forms at the REPL of the Go build

Status: design and first implementation (branch `cf-repl`, 2026-10-09), the user's decision of
2026-10-09: class forms in the Go build's REPL. It reverses, for `defclass` and the code forms,
B1-PLAN.md's D6 ("a first REPL without the class forms"); `gen-class` stays out.

On the JVM, `defclass` and the code forms ([SPEC §9.5](../classes/SPEC.md)) are compiled by the
one implementation `arbace.classes` into bytecode, at the REPL as in files (SPEC §10). The Go
build has no bytecode and no class loader. This document says how the Go build gives the class
forms their meaning anyway: the same analysis, classes made at run time
([C2G-SPEC §5.12](C2G-SPEC.md), `Dyn`), and an interpreter of the analyzed code.

Contents: 1 Measurements · 2 Design · 3 Alternatives considered · 4 What is built ·
5 Checks · 6 Results · 7 What remains · 8 Amendments.

## 1. Measurements (before the work)

`arbace.classes` (the class forms compiler, 8,400 lines) was not embedded in the program (the
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
   (`arbace.c2g.embed`), with four small Go-build namespace variants (`arbace/lang/go/ns/classes/`,
   C2G-SPEC §10.3, amendment M1): `types` (the runtime's package name), `env` (a `HashMap` for
   reflection's cache), `analyze` (ASM's `TypeReference` and the world's generic signatures from
   `arbace.classes.go`, no source path lookup yet), `native` (the classes interpreted, below).
   ASM's `Opcodes`, which the analysis reads ~150 times, is a cut class whose constants are now
   readable (§8, CF1). They are loaded on the first class form, with the macro spec checks off
   for the load (the `Compiler` variant's `classForms`).
2. **The back end is an interpreter of the analyzed nodes.** `arbace.classes.native`'s boundary
   (`compile-top`, `compile-fn`, `compile-deftype`) is kept; in the Go build its
   `compile-and-load!` is `arbace.classes.interp`'s, which builds, from the node trees emit would
   compile, trees of interpreter nodes (`Compiler$CF$...`, class forms in the `Compiler` variant
   `arbace/lang/go/ClassForms.clj`, translated by c2g like the evaluator), one per method, and
   makes the classes.
3. **Classes are classes made at run time.** A class extending `Object` (or another interpreted
   class) is a `Dyn` class (C2G-SPEC §5.12): its objects are `Dyn` values, their fields `Dyn`'s
   `F`, so translated code calls their methods through Go's interfaces (every interface of the
   world, by slot) and Clojure code through reflection (the member tables the natives fill, with
   the members' modifiers); interfaces of class forms are interfaces made at run time; a Clojure
   fn's class (`fn` in a class body, a fn handed over by `compile-fn`) has `CF$FnObj` objects,
   a translated `RestFn` choosing the class's `invoke` by arity.

### 2.2 The interpreter

- **Nodes evaluate themselves, typed.** `Compiler$CF$Node` has `eval` (an object), `evalJ` (a
  `long`, for `int`, `long`, `short`, `byte`, `char`, `boolean`), `evalD` (a `double`, for
  `float` and `double`), `exec` (a statement) and `test` (Java's or Clojure's truth). Each node
  overrides the one of its kind; the others convert. So `int` arithmetic in a loop allocates
  nothing: `ArithJ` computes in `long` with Java's `int` operations (`unchecked-add-int` on the
  values cast to `int`: c2g's Go has Java's wrap-around, division and shift semantics), `ArithD`
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
- **Exceptions** are Java's: a `try` node is a class forms `try` (Go's panics and recovers through
  c2g), its clauses matched by `Class.isInstance` (an interpreted exception class by its
  `Klass`).

### 2.3 The run-time classes (Compiler$CFGo, `c2g_cf.go`)

The natives of `Compiler$CFGo` (`arbace.c2g.dyncf`):

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
and later class forms name them; the analysis of later class forms takes their declarations from
`env/defined`, as the JVM's compiler takes them from the classes it defined, and their code is
linked to the newest class of a name, as SPEC §10 says for the JVM.

## 3. Alternatives considered

- **Interpret JVM bytecode.** Run emit as on the JVM and interpret the class files: complete
  fidelity (whatever emit writes runs), no second meaning of the nodes. Rejected: ASM's writer
  and reader (and emit with its `COMPUTE_FRAMES`, class hierarchy queries included) would have to
  be translated into the program (ASM is erased from the world, C2G-SPEC §10.1, amendment E2),
  `invokedynamic`'s bootstraps (`LambdaMetafactory`, `StringConcatFactory`, `ObjectMethods`,
  `SwitchBootstraps`, Clojure's reflective call sites) reimplemented, and a stack machine boxes or
  needs a typed operand stack; two levels of indirection (nodes to bytecode to execution) where
  one suffices.
- **Interpret the analyzed node maps in Clojure.** No class forms to translate: the interpreter
  would be a namespace evaluated by the evaluator. Rejected for speed: two levels of
  interpretation (the evaluator running an interpreter), each node a keyword lookup and an
  evaluated fn call, about a hundred times slower than nodes translated to Go. The builder
  (maps to node objects, once per method) is that namespace: it runs once.
- **Translate class forms to Go at run time.** There is no Go compiler in the program (nor in the
  box, B1b). Rejected; AOT translation of class forms in files is c2g's, not the REPL's.
- **Compile to Go closures** (each node a Go `func`, as EVAL-PLAN §6 proposes for the evaluator):
  faster dispatch, but closures cannot be written in class forms; the node objects are its
  equivalent in class forms (an interface call per node), and step 7 may lower them further.
- **A carrier class per superclass of the world** (`CF$Ex extends RuntimeException` ...) instead of
  `Dyn`'s `DynSub_C`: rejected for the general case (each overridable method written by hand);
  used for Clojure fn classes only (`CF$FnObj`), whose superclass `RestFn` has one entry point.
- **Analysis in class forms** (porting arbace.classes to the Java subset, so that c2g translates
  it): the analysis is 5,500 lines of Clojure; evaluated it costs 2.3 s once and 20 ms per class.
  Not worth a port now; AOT-compiling the embedded namespaces (step 6 and 7's pre-analysis) is
  the general remedy.

## 4. What is built

| where | what |
|---|---|
| `arbace/lang/go/ClassForms.clj` | the interpreter (`Compiler$CF`: `Rt`, `Host`, `Frame`, `Node` and its kinds, `Klass`, `Meth`, `MethodFn`, `CtorFn`, `FnObj`, the JVM's `TestHost`); the Go host (`Compiler$CFGo`: `GoHost`, the natives); `Compiler.classForms` loading the class forms without spec checks |
| `arbace/lang/go/ns/classes/interp.clj` | `arbace.classes.interp`: the builder (analyzed nodes to interpreter nodes), the classes' layouts, vtables, publication to the host, `define!` and `compile-and-load!` |
| `arbace/lang/go/ns/classes/go.clj` | `arbace.classes.go`: ASM's `TypeReference` as the analysis computes it, the world's generic signatures |
| `arbace/lang/go/ns/classes/*.subst.clj` | the Go build's variants of `types`, `env`, `analyze`, `native` |
| `arbace/c2g/dyncf.clj` | the natives of `Compiler$CFGo` (`c2g_cf.go`); `arbace/classes/generics.edn` (the world's generic signatures, embedded) |
| `arbace/c2g/embed.clj` | the analysis namespaces embedded (not emit, compiler, shape, verify, build, boot) |
| `arbace/c2g/out.clj` | a cut class's constants are readable (§8, CF1) |
| `arbace/c2g/dyn.clj`, `main.clj` | `DynClass.CF`; `c2g_cf.go` and `generics.edn` written |
| `test/oracle/forms/defclass.clj` | the oracle's forms for `defclass` at the REPL |
| `test/classforms/jvm_check.clj` | the interpreter on the JVM against the compiled classes (static calls) |

## 5. Checks

- `bin/arbace test/classforms/jvm_check.clj [FILE...]`: on the JVM, every `defclass` of the file
  compiled and interpreted (over the test host, without Class objects), every form calling only
  static methods of the classes evaluated both ways and compared.
- `bin/oracle check '<go executable> -' forms/defclass`: the oracle's `defclass` forms (recorded on
  the JVM) on the Go build.

## 6. Results

(Filled in as the work proceeds.)

## 7. What remains

(Filled in as the work proceeds.)

## 8. Amendments

(Filled in as the work proceeds.)
