# Modernizing Arbace's compilers for Java 26: a survey

Research only; nothing here is implemented. This survey covers Arbace's Clojure compiler
(`arbace/lang/Compiler.clj`, converted from the frozen `clojure/lang/Compiler.java`) and the
class forms compiler (`arbace/classes/`). Both emit through the vendored ASM (`arbace/asm`). The
question is which classfile and platform features added since Java 8 they should use, with Java 26
as the target. Status claims were checked against the local JDK sources (`/root/jdk26u`, commit
`baf63fbe42b8`, `DEFAULT_VERSION_FEATURE=26`, classfile major 70) and the local JDK
(26.0.2.1). Survey date: 2026-10-07, on `0bb80fa`.

## 1. Executive summary

There are two different compilers to modernize, and they start from different places:

- **The class forms compiler is already modern.** It emits javac's shapes for the running JDK:
  major version 70, `invokedynamic` to `StringConcatFactory`, `LambdaMetafactory`,
  `ObjectMethods` and `SwitchBootstraps`, nestmates, records, sealed classes and stack map
  frames. Its job is to equal javac, so it follows javac's choices, and this survey has nothing to
  add to it beyond the ASM question (§4.10).
- **The Clojure compiler is Clojure 1.12/1.13-master's.** It emits class files of version 61
  (Java 17), not a pre-Java-9 version. It uses one `invokedynamic` construct, `LambdaMetafactory`
  for adapting fns to functional interfaces. Calls go through `Var.getRawRoot()` and
  `IFn.invoke`, keyword lookups through `ILookupThunk` sites, protocol calls through per-site
  cached classes, and reflection through `Reflector`. It makes one class per fn, initializes
  constants in `<clinit>`, and defines classes in a `DynamicClassLoader`.

Startup is where Arbace loses the most, and the cure for that is in the build, not the bytecode.
A stage-1 Arbace compiles `arbace.core` and the other namespaces from source at every launch,
because the stages hold only the class-form classes:

| `java ... arbace.lang.Main -e 1` (hyperfine, 10 runs, loaded 64-core machine) | mean |
|---|---:|
| stage 1 + sources (today's launch, `-cp target/stage1:.`) | 3.27 s |
| the same as a jar | 3.08 s |
| the same jar + JDK 26 AOT cache | 2.44 s |
| jar with the namespaces AOT-compiled by Arbace's compiler | 1.07 s |
| the same jar + JDK 26 AOT cache | **0.45 s** |
| for comparison: frozen `clojure/` from source (`clojure.main`) | 2.49 s |

A hand-written micro-benchmark found **no steady-state difference** between a `Var` call as
Clojure emits it, an `invokedynamic`-style constant call site, and a direct static call (all
about 3.2 ns per call in a monomorphic loop). The `invokedynamic` site cost about 18 µs more to
link the first time.

### Ranked recommendation

**Do first**

1. **AOT-compile Arbace's namespaces into the stages and ship a jar with an AOT cache** (JEP 483,
   514, 515, 516; all final in 26). This takes 3.3 s to 0.45 s with no change to emitted
   bytecode. The compile is deterministic: two runs gave byte-identical output for 2,147
   classes. So the fixpoint can grow to cover the namespaces too. It ties nothing to HotSpot
   except launch flags.
2. **Use the JDK's class-file verifier as a test oracle.** `ClassFile.verify` (JEP 484, final in
   24) accepts every class Arbace emits today: all 815 stage-1 classes and all 2,147 AOT
   namespace classes. Adding it to `bin/build-arbace` or the suite costs little and catches bad
   bytecode from any future emission change before the JVM does.
3. **Make the Clojure compiler's classfile version follow the running JDK**, as
   `arbace.classes` already does (`emit.clj`'s `*version*`). It is a one-line change (61 → 70).
   It gains no new classfile feature, since none has been added since Java 17 outside preview,
   but both compilers then agree.

**Do next, measured, one at a time**

4. **Constant dynamic (`ldc` of condy) for the compiler's constants**: keywords, vars, symbols
   and read-back literals. These are now 7,350 `RT.var` calls and many `Keyword.intern` calls
   across 2,130 `<clinit>`s, run when each fn class is loaded. Condy makes each lazy and drops
   the static fields. Ghadi Shayban's 2018 experiment on Clojure found no startup regression.
5. **`invokedynamic` for keyword invoke sites and for reflective calls.** Keyword sites replace
   `ILookupThunk` static-field juggling with a class-guarded call site (1,082 sites in the
   namespaces). Reflective calls today search for methods on every call. An inline-cached
   reflective call site, as Dynalink's bean linker does, would make un-hinted REPL code much
   faster. The method selection must stay exactly as `Reflector`'s.
6. **Virtual threads as an opt-in executor** for `send-off`, `future` and `pmap` (library
   level). `locking` no longer pins carriers since JEP 491 (24), and `LazySeq` and `Delay`
   already use `ReentrantLock`.

**Defer**

- `invokedynamic` for Var calls. It brings no steady-state gain at monomorphic sites, a link cost
  per site, and deoptimization on every `def`, `alter-var-root` or `with-redefs`. It becomes
  worth it only as "direct linking that survives redefinition", if Arbace ever wants direct
  linking by default.
- `invokedynamic` polymorphic inline caches for protocol dispatch. Worth measuring once item 5
  exists, because it reuses the same machinery.
- Replacing ASM with `java.lang.classfile`. It is final and good, but the replacement is large,
  changes every emitted byte once, and moves a dependency from Arbace's own code into the host
  JDK. A later variant is to convert the JDK's implementation with `arbace.j2c`, as was done for
  all of jdk26u.
- `StringConcatFactory` for `str`, VarHandles for `Atom`, JPMS modules and `jlink` images, FFM
  (an interop library, not the compiler).

**Avoid**

- `LambdaMetafactory` or hidden classes for Clojure fns. LMF can implement only one abstract
  method, while `IFn` has 22 `invoke` arities, metadata and `applyTo`. HotSpot marks every method
  of a hidden class as hidden and leaves its frames out of stack traces
  (`classFileParser.cpp:2677`, `javaClasses.cpp:2867`, unless `-XX:+ShowHiddenFrames`).
- Preview and incubator APIs in the runtime: Lazy Constants (JEP 526, second preview), Structured
  Concurrency (JEP 525, sixth preview), the Vector API (incubator) and primitive patterns
  (preview). They need `--enable-preview`, and they change from release to release.
- `ScopedValue` in place of Var thread bindings. `push-thread-bindings` and
  `pop-thread-bindings` are public and not lexically scoped, and the REPL and `Compiler.load`
  use them that way.
- JVM records and `ObjectMethods` for `defrecord` and `deftype`. Clojure's equality, hashing,
  `__extmap` and mutable hash caches do not fit records.

The native runtime planned for later points the same way. The baseline's mechanisms (a Var
indirection, an `IFn` interface call, class-per-fn, static fields) are cheap to implement in any
runtime. `invokedynamic`, method-handle combinators, condy, LMF and hidden classes require a
JSR 292 implementation, which is the most intricate part of HotSpot's runtime. The startup work
in item 1 is host configuration only: an own runtime would replace it with its own image or
snapshot, and nothing in Arbace's code would depend on it.

## 2. Baseline: what the compilers emit today

References are to the frozen `clojure/lang/Compiler.java` (9,647 lines); `arbace/lang/Compiler.clj`
(8,449 lines) is its conversion with the same structure.

### 2.1 Classfile version and frames

- `JVM_BYTECODE_VERSION = V17` (`Compiler.java:347`; `Compiler.clj:433`), so major 61. It is used
  for fn classes (`:4868`), deftype stubs (`:8343`) and compile stubs (`:8661`). The class files
  of the AOT-compiled namespaces have major version 61 (checked with `od` on `core__init.class`).
- Frames come from ASM's `COMPUTE_FRAMES | COMPUTE_MAXS`, with `getCommonSuperClass` always
  answering `java/lang/Object` (`Compiler.java:9632`; `Compiler.clj:8441`). That works because
  the compiler `checkcast`s values after merges. The JDK's verifier
  (`java.lang.classfile.ClassFile.verify`) accepts all 2,147 classes of the AOT-compiled
  namespaces.
- The class forms compiler emits the running JDK's version (`arbace/classes/emit.clj:10`:
  `(+ 44 (.feature (Runtime/version)))`, so 70), and its `getCommonSuperClass` uses the class
  environment (`emit.clj:940`). All 815 stage-1 classes have major version 70 and verify cleanly.
- Note: the frozen `clojure/*.class` are major version 70 too, because `javac -g` without
  `--release` targets the build JDK.

### 2.2 Calls

- **Var calls.** `(f x)` with `f` a non-dynamic var emits the Var constant (a `static final`
  field of the fn class), `Var.getRawRoot()`, `checkcast IFn` and `invokeinterface
  IFn.invoke(Object)` (`emitVarValue`, `Compiler.java:5740`). Dynamic vars use `Var.get()`, which
  looks up the thread's binding frame. The root is a `volatile` field, so the JIT cannot
  constant-fold the target, but it inlines the call through the receiver-type profile. In the
  AOT-compiled namespaces there are 12,525 `getRawRoot` and 395 `Var.get` call sites.
- **Arities.** `IFn` has `invoke` for 0 to 20 positional arguments plus an `Object[]` rest
  (`MAX_POSITIONAL_ARITY = 20`, `:145`). Variadic fns extend `RestFn`. Primitive signatures
  (`^long`, `^double`, up to 4 arguments) add `IFn$LO`-style interfaces with `invokePrim`.
- **Direct linking** (`:direct-linking`, Clojure 1.8). Each fn class has a static
  `invokeStatic`, and with direct linking on, calls of non-dynamic, non-`^:redef` vars become
  `invokestatic` (`StaticInvokeExpr`, `:3961`). Redefining the var then does not reach those
  callers. It is off by default; even off, every fn's `invoke` delegates to its `invokeStatic`
  (1,696 such calls in the namespaces).
- **Keyword invoke** `(:k m)`. A per-site pair of static fields, `__site__N` (`KeywordLookupSite`)
  and `__thunk__N` (`ILookupThunk`). The emitted code calls the thunk, compares its result to
  the thunk itself to detect a miss, and on a miss calls `fault`, stores the new thunk in the
  static field and calls again (`KeywordInvokeExpr`, `:3825-3847`). That is an inline cache
  written by hand: `defrecord` classes implement `ILookup` with a direct field read. There are
  1,082 such sites.
- **Protocol calls** (`InvokeExpr.emitProto`, `:4236`). A per-site `static Class
  __cached_class__N`. If the target's class equals it, the call goes through the protocol fn
  (Var root, `IFn.invoke`). If the target implements the protocol's interface, it is an
  `invokeinterface`. Otherwise the cache is updated and the protocol fn is called. The protocol
  fn dispatches through its own `MethodImplCache` (`core_deftype.clj:523, 590`).
- **Reflection.** Unresolved instance calls emit `Reflector.invokeInstanceMethod(target, name,
  args[])` and the like (`:2106-2108`, `:2350`, `:3146`). `Reflector` finds the candidate methods
  on every call and calls `Method.invoke`. Since JEP 416 (JDK 18) that is method-handle based, and
  the JDK spins a hidden `Reflector$$InjectedInvoker` class: it shows up in `-Xlog:class+load` of
  every Arbace start. There are 44 reflective call sites in the namespaces. User code at the REPL
  has many more.
- **Functional interfaces (Clojure 1.12).** Where a Java parameter or a hinted local has a
  `@FunctionalInterface` type, `maybeEmitFIAdapter` (`:1693`; `Compiler.clj:1703`) emits: the fn
  expression; if it is an `IFn` and not already the interface, an `invokedynamic` to
  `LambdaMetafactory.metafactory` that adapts a static `FnInvokers.invokeXX(IFn, ...)` method to
  the interface method (`emitInvokeDynamicAdapter`, `:1773`); then a `checkcast`. This is the
  compiler's only `invokedynamic`. None occur in the AOT-compiled namespaces.
- **Method values (1.12).** `String/toUpperCase` in value position compiles to a new fn class
  per occurrence, a "thunk" built by macroexpanding a `fn` form
  (`QualifiedMethodExpr.buildThunk`, `:1242`, with the comment "TBD: caching/reuse of thunks").

### 2.3 Constants and static initialization

- Every fn class has `static final` fields for its constants (`const__N`), vars and keywords,
  filled in `<clinit>` by `emitConstants` (`:5479`) and `emitValue` (`:5264`). Strings and
  numbers are pushed directly. Symbols, keywords and vars are interned (`Symbol.intern`,
  `Keyword.intern`, `RT.var`). Collections are built element by element. Anything else is printed
  with `*print-dup*` and read back with `RT.readString` at class initialization.
- In the AOT-compiled namespaces: 2,130 static initializers, 7,350 `RT.var` calls in them.
  Every fn class loaded runs its `<clinit>` when first instantiated, whether or not the
  constants are ever used.
- A namespace's `__init` class loads the namespace. Its `load` method is split into
  `__init0`, `__init1`, ... to stay under the 64 KB method limit.

### 2.4 Classes and loaders

- **One class per fn** (`FnExpr`, `:4468`), named `ns$name__1234`, `public final`, extending
  `AFunction` or `RestFn`. Closed-over locals are constructor arguments stored in fields.
  Nested fns are separate classes that refer to each other by name.
- At a stage-1 start from source, 2,273 classes are defined at run time (by
  `DynamicClassLoader`, `source: __JVM_DefineClass__`). In all, 4,168 classes load, 1,818 of
  them `arbace.core$...` fns.
- **`DynamicClassLoader`** (`clojure/lang/DynamicClassLoader.java`; `arbace/lang/DynamicClassLoader.clj`)
  is a `URLClassLoader`. `Compiler.load` and `eval` bind a fresh one (`RT.makeClassLoader()`,
  `:7692`, `:8167`). Defined classes go into a static `classCache` of `SoftReference`s that
  later lookups by name consult. That is how a redefined `deftype` becomes visible to code
  compiled afterwards. Classes unload when their loader becomes unreachable.
- The class forms keep **one loader per Java package** and start a new generation when a class is
  redefined (SPEC §10). Hidden classes and `Lookup.defineClass` were considered there and rejected
  (they cannot be named, or they cannot be redefined).

### 2.5 Locals clearing

The compiler nulls a local after its last use on each path (`LocalBindingExpr.shouldClear`,
`:6556`; `emitClearLocals`), so a held head of a lazy seq does not keep the realized seq
alive. `:disable-locals-clearing` (`:260`) turns it off for debuggers. It is still needed on
current JVMs: interpreted frames keep every reference local alive (HotSpot's interpreter oop
maps are not liveness-based), and so do frames under JVMTI debugging. Nothing in Java 9 to 26
changes that.

### 2.6 How the constraints apply

- **Fixpoint.** `bin/build-arbace` compares stage 2 and stage 3. Both are the output of
  `arbace.classes` for the class forms (Compiler included). The Clojure compiler's own emission
  is not in the stages today: the namespaces are compiled at run time. A change to Compiler's
  emission is still covered, but indirectly: stage 1's compiler compiles `arbace.classes`, which
  then builds stage 2. Once namespaces are AOT-compiled into the stages (item 1), the compiler's
  emission is in the fixpoint directly. Any new emission must stay deterministic: no hash order,
  no identity hashes in names, no time-dependent bootstraps.
- **One shape for handed-over fns.** `arbace.classes.native/compile-fn` makes "the fn class the
  compiler would have made" for fns that use code forms (COMPILER-NOTES, "Native class forms").
  It shares the name, superclass, constructor of closed-over locals and `IFn$..` interfaces, but
  not the constant mechanism. So constant or call-site changes need not be mirrored there.
  Changes to the fn class's shape must be.
- **Stage 0.** `Compiler.clj` is compiled by the frozen `clojure/` running `arbace.classes`, on
  JDK 26. So any JDK 26 API (`ConstantDynamic`, `java.lang.classfile`, `MutableCallSite`, ...)
  can be used in Arbace's sources at every stage.
- **The suite.** It checks behaviour, including error messages for reflection failures,
  `recur` errors and arity errors, and stack-trace contents in some places
  (`clojure.test-clojure.errors`, `clojure.test-clojure.compilation`). Anything that changes
  which frames appear or what `Reflector` throws needs care.

## 3. Measurements

All on 2026-10-07, JDK 26.0.2.1, a 64-core machine shared with two other agents (so absolute
numbers are noisy, ±10-15 %). The scripts are in `.tmp/survey/` (gitignored), not committed.

### 3.1 Startup

AOT compile, with Arbace's own compiler (stage 1):

```
java -Xss16m -Darbace.compile.path=.tmp/survey/aot -cp target/stage1:.:.tmp/survey/aot \
  arbace.lang.Compile arbace.core arbace.main arbace.core.server arbace.string arbace.walk \
  arbace.edn arbace.repl arbace.pprint
```

This takes 6.5 s and gives 2,147 classes (4.6 MB) of major version 61. A second run gives
identical bytes (`diff -rq`: no differences). Jars: `app.jar` holds stage 1, the AOT classes and
`arbace/` sources; `src.jar` holds stage 1 and the sources. AOT caches came from one training
run each (`java -XX:AOTCacheOutput=app.aot -cp app.jar arbace.lang.Main -e 1`, JEP 514's
one-step workflow; `app.aot` is 27 MB). The cache refuses non-empty directories on the class
path (`aotClassLocation.cpp:705`), hence the jars.

| variant | mean | min |
|---|---:|---:|
| `-cp target/stage1:.` (sources) | 3.27 s | 2.89 s |
| `src.jar` | 3.08 s | 2.53 s |
| `src.jar` + `-XX:AOTCache=src.aot` | 2.44 s | 2.02 s |
| `app.jar` (AOT-compiled namespaces) | 1.07 s | 0.91 s |
| `app.jar` + `-XX:AOTCache=app.aot` | 0.45 s | 0.40 s |
| frozen `clojure.main` from source | 2.49 s | 1.99 s |

Classes loaded (`-Xlog:class+load`): from source, 4,168 (2,273 defined at run time); `app.jar`,
3,139; `app.jar` with the cache, 3,186, of which 3,169 come from the cache. The cache cannot
help classes defined by `DynamicClassLoader`, because AOT linking covers only the built-in
loaders (`aotClassLinker.cpp:130`). That is why it gains only 0.6 s on the source launch.

### 3.2 Var call vs `invokedynamic` vs static call

`.tmp/survey/bench/VarBench.java` compares three calls of a one-argument `AFunction` (an array
load, no allocation) in a dependent chain of 10⁸ calls, after warm-up: through
`Var.getRawRoot()` as Clojure emits it, through a `static final MutableCallSite.dynamicInvoker()`
(C2 treats it as it treats an `invokedynamic` site bound to a `MutableCallSite`), and through
`invokeStatic` (direct linking).

| call | ns/call (5 runs) |
|---|---|
| Var + `IFn.invoke` | 3.15-3.32 |
| constant call site | 3.14-3.37 |
| static | 3.16-3.34 |

They are the same within noise. C2 already inlines the Var call through the profile, and the
volatile root read costs nothing measurable on x86. Retargeting the hot call site
(`setTarget`, which deoptimizes the compiled code that depends on it) cost about 6-7 µs per
retarget, plus recompilation later.

### 3.3 Linking cost of `invokedynamic`

`.tmp/survey/bench/LinkCost.java` uses `java.lang.classfile` to generate 2,000 static methods,
each with one call site, and calls each once. The sites are either Clojure-style Var calls or
`invokedynamic` with a custom bootstrap returning a `MutableCallSite`.

| site | first call | second call |
|---|---:|---:|
| Var-style | 12-14 µs | 0.6-1.2 µs |
| `invokedynamic` | 29-34 µs | 0.7-0.9 µs |

So about 18 µs more per site, once per site per run, for every site that runs. The namespaces
have 12,525 Var call sites; a start that runs a few thousand of them would pay tens of
milliseconds. The JDK 26 AOT cache pre-resolves only `LambdaMetafactory` and
`StringConcatFactory` call sites (`aotConstantPoolResolver.cpp:548`), so custom bootstraps pay
this cost at every launch.

### 3.4 Verification

`.tmp/survey/bench/VerifyAll.java` runs `ClassFile.of(...).verify(bytes)` over a tree. Both
`target/stage1` (815 classes) and the AOT-compiled namespaces (2,147 classes) verify with no
errors.

## 4. Candidates

Each entry: what it is and its status in 26; how the compilers would use it; pros; cons and
risks; effort; and what it means for the constraints and for an own runtime.

### 4.1 `invokedynamic` call sites

JSR 292, Java 7 (major 51). `java.lang.invoke` was extended by JEP 274 (Java 9). Final.

How the compiler would use it, by site kind:

- **Var calls.** `invokedynamic "invoke" (Object...)Object` with a bootstrap taking the var's
  namespace and name. The call site is a `MutableCallSite` per Var, or a `SwitchPoint`-guarded
  constant per var. `bindRoot` and `alterRoot` retarget it or invalidate it. Dynamic vars, and
  vars with `^:redef`, keep `Var.get()`.
- **Protocol dispatch.** A polymorphic inline cache: a chain of `guardWithTest(class == C)`
  ending in a lookup that adds an entry. The protocol holds a `SwitchPoint` that `extend`
  invalidates. This replaces `__cached_class__N` plus `MethodImplCache`.
- **Reflection.** A call site keyed on the method name and arity. It caches, per receiver class
  seen, the method handle `Reflector` would have chosen, guarded on the class, and falls back
  to `Reflector`'s algorithm. This is what Dynalink's `BeansLinker` does (JEP 276, module
  `jdk.dynalink`, still in jdk26u).
- **Keyword lookup.** A `guardWithTest(class == R)` call site on the record class, bound to its
  field getter, falling back to `ILookup.valAt`. This replaces `__site__N` and `__thunk__N`.

Prior art:

- Ghadi Shayban's `invokedynamic` branch of Clojure (2016,
  https://github.com/ghadishayban/clojure/tree/invokedynamic). In it, vars used as expressions,
  keywords and keyword invoke sites are indified; it passes the tests and is binary compatible.
  He expected identical performance, aimed it at startup of AOT code, and left protocol invokes
  and lazy var loading open. Clojure did not adopt it. Clojure's answer to call cost was direct
  linking (1.8) instead.
- JRuby: indy since 1.7 (2012). For years it was on only for simple operations, because
  LambdaForm spinning hurt startup ("LambdaForms have an enormous startup-time cost"). It is on
  by default in JRuby 10 (2025), and can still be turned off for development.
- Nashorn (JDK 8-14, removed by JEP 372) was indy throughout, via Dynalink. Its long warm-up was
  a known weakness.
- Groovy 4 dropped its call-site caching and is indy only.
- Kotlin and Scala use `invokedynamic` only for JDK-provided bootstraps (lambdas, string
  concatenation), not for their own call dispatch.

Pros:

- Redefinable direct linking: a var's target is a JIT constant, so callers get
  `invokestatic`-like inlining. Redefinition still works, at the cost of a deoptimization.
- Protocol and reflective sites gain real inline caches. Reflection is the largest win: today
  every reflective call searches the class's methods.
- Fewer static fields and less hand-written cache code in the emitted classes.

Cons and risks:

- No measured steady-state gain for Var calls (§3.2). Each site costs about 18 µs to link
  (§3.3), and custom bootstraps are not pre-resolved by the AOT cache.
- Each redefinition deoptimizes every compiled caller. `with-redefs` in tests and
  `alter-var-root` loops become deoptimization storms.
- Stack traces stay readable, since LambdaForm frames are hidden. Debuggers and profilers do
  show the method-handle machinery.
- Binding conveyance, `^:dynamic` and `set!` semantics must stay exactly as they are.
- Reflection: the suite tests `Reflector`'s choices and messages, so the call site must reuse
  `Reflector`'s method selection, not `MethodHandles.Lookup` resolution.

Effort: keyword sites, medium-low (one bootstrap class, `KeywordInvokeExpr`). Reflection,
medium (a bootstrap plus a cache around `Reflector`). Var calls, medium (`Var` changes, `def`
and `alter-var-root` paths, the boundary with direct linking). Protocols, high (`core_deftype.clj`
and extend invalidation).

Constraints: deterministic (bootstrap arguments are names), so fixpoint-safe. The suite is at
risk mostly for reflection errors. For an own runtime: a JVM-like runtime would need
`invokedynamic`, `CallSite`, and enough of `MethodHandle` (`bindTo`, `insertArguments`,
`guardWithTest`, `asType`) to run the bootstraps. That is a large part of a JVM. A Go-like
runtime has no such concept, and Var indirection plus hand-written caches map onto plain
pointers and interface calls. **Verdict:** keyword sites and reflection next (item 5); Var calls
and protocols deferred.

### 4.2 `LambdaMetafactory` for fns

Java 8 (JSR 335). `altMetafactory` adds marker interfaces and bridges. Final.

How it would be used: a fn body becomes a private static method of the namespace or enclosing
class, and the fn value an LMF proxy. Kotlin 2.0 and Scala 2.12+ compile lambdas this way. LMF
spins a hidden class per call site at run time.

Pros: fewer class files on disk. One class per namespace plus synthetic methods. The JDK's AOT
cache pre-resolves LMF sites.

Cons and risks:

- LMF implements one abstract method. A Clojure fn is an `AFunction` with up to 21 `invoke`
  methods, `applyTo`, `getRequiredArity` (`RestFn`), `withMeta` and the `IFn$LO` primitive
  interfaces. A generic holder dispatching to method handles would lose the per-class type
  profile that makes Clojure calls fast.
- The proxy classes are hidden (see 4.5). The bodies stay in visible synthetic methods, so
  stack traces show `ns$lambda$f$0` frames instead of `ns$f__123.invoke`. Clojure's
  `demunge`-based stack trace tools, `clojure.repl/pst` and error reporting all assume the
  latter. The suite checks some of it.
- The runtime class count stays the same, since LMF spins a class per site.

Effort: high (a new fn representation everywhere, including `arbace.classes.native/compile-fn`).
The fixpoint and the suite are both at risk. For an own runtime: it needs runtime class
spinning. **Verdict:** avoid for fns. Keep LMF where Clojure 1.12 uses it, for functional
interface adaptation. Method values (`String/toUpperCase`) in a functional-interface context
could adapt the method directly with LMF instead of building a thunk class; the compiler's own
comment at `:1695` sketches that optimization. That is small and optional.

### 4.3 Constant dynamic (condy)

JEP 309, Java 11 (major 55). `ConstantBootstraps` provides `invoke`, `getStaticFinal`,
`nullConstant` and others. ASM has `ConstantDynamic` (vendored as `arbace/asm/ConstantDynamic.clj`).
Final.

How it would be used: `emitConstant` becomes `ldc` of a dynamic constant. Keywords use
`ConstantBootstraps.invoke` with `Keyword.intern(String, String)`, vars use `RT.var(ns,
name)`, symbols `Symbol.intern`. The `*print-dup*`/`readString` fallback becomes a condy with the
string as a static argument. Collection literals of constants become condys over their
elements. The static fields and most of `<clinit>` disappear. The JVM resolves each constant at
its first `ldc`, once, and caches it in the constant pool.

Pros:

- Lazy: a constant costs only when its code first runs. Today every fn class's `<clinit>` runs
  all of them when the class initializes.
- Smaller classes and smaller static initializers (2,130 `<clinit>`s). The 64 KB limit on
  `__init` load methods is pushed back.
- Ghadi Shayban tried it on Clojure in 2018 (amber-dev, "Constant_Dynamic first experiences"):
  no startup regression and some `<clinit>` savings. Aggregate constants built through
  `asCollector` were slightly slower. So keep collections as they are, or build them in one
  static helper.

Cons and risks:

- Var roots cannot be condys, since they are mutable. Only the `Var` object can.
- Each bootstrap call goes through method-handle machinery, a little slower than a static field
  read the first time. The AOT cache does not pre-resolve app condys.
- `ldc` of a condy whose bootstrap throws gives a `BootstrapMethodError` wrapping the cause.
  Error messages for bad `*print-dup*` constants would change, and they surface later.

Effort: medium (`emitConstant`, `emitValue`, `emitConstants`, keyword and var emission, the
`__init` loader).

Constraints: deterministic, so fixpoint-safe. `compile-fn` reads its constants on its own and
need not follow. For an own runtime, condy is much simpler than indy: call a static method once
and cache the result per constant-pool slot. A JVM-like runtime can implement it without
`MethodHandle` combinators if it special-cases `ConstantBootstraps.invoke`. A Go-like runtime
would do the same with package-level `sync.Once` values. **Verdict:** do next (item 4), measured
against the AOT-cache startup.

### 4.4 Nestmates

JEP 181, Java 11 (major 55): `NestHost` and `NestMembers` attributes, private access between
nestmates without synthetic accessors. Final.

The class forms compiler already emits them as javac does (`emit.clj:1305-1308`). For the
Clojure compiler there is little to gain. Fns reach closed-over values through public or
package fields of their own class, and everything else through public members. Nesting a
namespace's fn classes would let fields be `private`, but every class of a nest must be in the
same loader and package. Clojure defines classes form by form in fresh loaders, so a nest
cannot span top-level forms. **Verdict:** nothing to do in the Clojure compiler. The concept is
cheap for an own runtime (one access-check rule).

### 4.5 Hidden classes

JEP 371, Java 15: `Lookup.defineHiddenClass`, with class data via
`defineHiddenClassWithClassData` and `MethodHandles.classData` (a condy). Final.

How it could be used: define fn classes as hidden classes of the namespace's lookup instead of
`DynamicClassLoader.defineClass`. Nested fns would be passed in as class data, since a hidden
class cannot be named. Each could unload on its own (unless `ClassOption.STRONG`). There would
be no loader per form and no `classCache`.

Pros: independent unloading, which matters for long REPL sessions with many `eval`s. No name
clashes. No `SoftReference` cache.

Cons and risks:

- **Stack traces.** HotSpot marks every method of a hidden class as hidden
  (`classFileParser.cpp:2677`), and `java_lang_Throwable::fill_in_stack_trace` skips hidden
  frames unless `-XX:+ShowHiddenFrames` (`javaClasses.cpp:2867`). Every Clojure fn frame would
  disappear from exceptions. That alone rules it out.
- They cannot be named, which breaks AOT (class files on disk), `deftype` and `defrecord`
  (named), `Class/forName` and the class forms' package loaders. SPEC §10 already rejected them
  for class forms.
- Hidden classes defined by user loaders are not AOT-cacheable.

**Verdict:** avoid. `DynamicClassLoader` stays. For an own runtime, the property worth taking
from hidden classes is unloading per class, and that can be designed in natively.

### 4.6 Sealed classes and records

Sealed: JEP 409, Java 17 (major 61, `PermittedSubclasses`). Records: JEP 395, Java 16 (major
60, `Record` attribute, `java.lang.Record`). Final. Record patterns (JEP 440) and pattern
`switch` (JEP 441) arrived in Java 21. The class forms compiler supports all of these (SPEC
§4.13, §4.11 and §5.8).

For the Clojure compiler:

- **`defrecord` as JVM records.** Records require all fields `private final`, a canonical
  constructor, `java.lang.Record` as superclass and accessor methods. Clojure records also have
  `__meta`, `__extmap` and the mutable caches `__hash` and `__hasheq`, and implement `IPersistentMap`
  with Clojure's equality (`=` across numeric types, map equality with any map). JVM record
  equality via `ObjectMethods` is per component with `Object.equals`. The two semantics differ.
  The only gain would be that Java code could deconstruct Clojure records in record patterns.
- **Sealed.** Clojure has no closed hierarchies: protocols and interfaces are open by design.
  It could apply to Arbace's own internal hierarchies (`Expr` in the compiler) through
  `defclass`, but that is a choice for the class forms, not for emission.

**Verdict:** avoid for `defrecord` and `deftype`. Records and sealed classes stay available to
Arbace through `defclass`. Both are metadata, cheap for any own runtime.

### 4.7 `StringConcatFactory`, `ObjectMethods`, `SwitchBootstraps`

- `StringConcatFactory` (JEP 280, Java 9). The class forms already emit it (`java-str`,
  `emit.clj:1356`). For the Clojure compiler it would be an intrinsic for `(str a b c)` at known
  arity. Clojure's `str` maps `nil` to `""` (Java's concatenation gives `"null"`) and calls
  `.toString`, so each argument needs a filter. The recipe could carry `nil` checks with constants
  folded in. The gain is small (`str` with a `StringBuilder` is already fast), but it is one of
  the two bootstraps the AOT cache pre-resolves. Defer.
- `ObjectMethods` (Java 16): record `equals`, `hashCode` and `toString`. Not applicable, see 4.6.
- `SwitchBootstraps.typeSwitch` and `enumSwitch` (JEP 441, Java 21). The class forms emit them.
  Clojure's `case*` is already a hash `tableswitch` or `lookupswitch` with equality checks
  (`CaseExpr`), and Clojure has no type switch. A `typeSwitch` could speed up `cond` chains of
  `instance?` tests and the fallback of protocol dispatch over many classes, but the PIC of
  §4.1 covers that better. Avoid for now.

Own runtime: each bootstrap is a well-specified library function. A runtime can provide
intrinsic implementations without general `invokedynamic`, as GraalVM Native Image does. The
class forms' `-XDstringConcat=inline` mode already avoids indy where wanted.

### 4.8 MethodHandles and VarHandles in the runtime

VarHandles: JEP 193, Java 9. Final.

- `Atom` holds a `final AtomicReference state` (`clojure/lang/Atom.java:18`). Replacing it with
  a `volatile` field updated through a static `VarHandle` (`compareAndSet`) removes one object
  (16 bytes) and one indirection per atom. `Volatile` is already a plain volatile field.
  `Agent`, `Ref` and `LockingTransaction` use their own atomics, and could get the same.
- `Reflector`'s field access (`Reflector.invokeNoArgInstanceMember`, `setInstanceField`) could
  use cached `VarHandle`s per class, as part of §4.1's reflective call sites.
- JEP 500 (Java 26) is relevant here. Mutating `final` fields through deep reflection now warns
  by default (`--illegal-final-field-mutation=warn`, `launcher.properties:76`) and will be denied
  later. Clojure's `Reflector.setInstanceField` on a final field already fails, since it does not
  call `setAccessible`, so nothing breaks today. Arbace should not start relying on it.

Effort: low for `Atom`; medium for reflective field caches. No risk to the fixpoint: these are
runtime class forms, compiled deterministically. Own runtime: VarHandles are a JDK API; an own
runtime would implement CAS on fields directly. **Verdict:** low priority, optional.

### 4.9 Classfile versions and stack map frames

- Classfile features by version: 51 brings `invokedynamic` (and requires stack maps, i.e. the
  type-checking verifier); 53 modules; 55 nestmates and condy; 60 records; 61 sealed classes.
  There has been nothing since 61 outside preview features. Valhalla's flags (`ACC_IDENTITY`,
  strict fields) are not in jdk26u's `java.lang.classfile` (grep finds nothing).
- The Clojure compiler at 61 can already use everything final. Raising it to 70 (the running
  JDK, as `arbace.classes` does) costs one line, and only makes the class files require JDK 26,
  which Arbace requires anyway. Recommended for uniformity (item 3).
- Frames: ASM computes them. The `getCommonSuperClass` → `Object` shortcut is sound for the
  compiler's code shape, and the JDK verifier agrees (§3.4). Nothing to change.

### 4.10 `java.lang.classfile` in place of ASM

JEP 484, final in Java 24 (preview in 22 and 23). `ClassFile.latestMajorVersion()` is 70
(`ClassFile.java:1039-1052`). It is an immutable model with builders, transforms, stack map
generation (with a `ClassHierarchyResolver`), a verifier (`ClassFile.verify`) and constant
descriptors (`java.lang.constant`). The JDK itself has moved off its internal ASM copy onto it.

How it would be used: the compiler's `GeneratorAdapter` code (thousands of calls in
`Compiler.clj`) and `arbace/classes/emit.clj` (2,038 lines) would be rewritten against
`CodeBuilder`. The vendored `arbace/asm` (45 classes in stage 1) would be dropped.

Pros:

- It follows the JDK release by release: new versions and attributes arrive with the JDK, so
  there is no vendored ASM to update.
- A cleaner API. Labels, exception tables and frames are handled by the library.
- A built-in verifier, usable now as a test oracle without any migration (item 2).

Cons and risks:

- A large rewrite of both emitters for no change in output semantics. Constant-pool order and
  frame encoding change, so every class's bytes change once. The fixpoint re-establishes
  itself, but stage 1 ≠ stage 2 during the transition (stage 0 runs the same emitter, so it is
  manageable).
- Arbace's classfile writer moves from Arbace's own code into the host JDK. For the own runtime,
  ASM-as-class-forms is already portable Arbace code. `java.lang.classfile` would have to be
  provided by that runtime: 200+ interfaces, sealed hierarchies, records.
- An alternative that avoids this: convert jdk26u's `java.lang.classfile` and
  `jdk.internal.classfile.impl` with `arbace.j2c`, as was already done for all of jdk26u
  (CONVERTER-NOTES: 12,730 files). That vendors it like ASM, at the cost of a much larger body
  of vendored code than ASM.

Effort: high. **Verdict:** defer. Use `ClassFile.verify` in tests now, and keep ASM (the
vendored ASM already knows `V26`, `arbace/asm/Opcodes.clj:105`).

### 4.11 JPMS modules and `jlink`

JEP 261, Java 9. Final. Arbace runs on the class path, and its runtime-defined classes live in
unnamed modules. Making `arbace.*` a named module would need `module-info`. The class forms can
write one with `defmodule` (SPEC §9.3). It would also need `Lookup`-based class definition for
the package loaders, or `--add-opens` for anything reflective. User code at the REPL would sit
in unnamed modules reading Arbace's.

The practical benefit, a small runtime image, does not need it. `jlink --add-modules
java.base,java.sql,...` builds a trimmed JDK image, and Arbace's jar stays on its class path.
The AOT cache works with both. **Verdict:** defer. If Arbace's own runtime is JVM-like, its
module story is its own decision.

### 4.12 Virtual threads, structured concurrency, scoped values

- **Virtual threads**: JEP 444, final in 21. JEP 491 (24) removed pinning in `synchronized`, so
  `locking` (`monitor-enter`) and `Var`'s `synchronized` methods no longer pin carriers. The
  baseline already moved `LazySeq` and `Delay` to `ReentrantLock` (`clojure/lang/LazySeq.java:32`,
  `Delay.java:28`). Agents use a fixed pool (`send`) and a cached pool (`send-off`, `future`)
  (`Agent.java:50-54`).
  - **Use:** an opt-in virtual-thread executor for `send-off`, `future` and `pmap` via
    `set-agent-send-off-executor!` (it exists today) or a system property. Do not make it the
    default. Virtual threads are always daemon threads (pool shutdown and `shutdown-agents`
    semantics change), thread names differ, and CPU-bound `send` gains nothing.
  - Var bindings (`ThreadLocal<Frame>`) work unchanged on virtual threads. Binding conveyance
    is explicit (`binding-conveyor-fn`), so it carries over.
  - Own runtime: this is the most portable item of all. A Go-like runtime has goroutines natively.
- **Structured concurrency**: JEP 525, **sixth preview** in 26 (`PreviewFeature.java`). Avoid in
  the runtime. A library can wrap it later.
- **Scoped values**: JEP 506, **final in 25** (`ScopedValue.java`, `@since 25`, no preview
  annotation). A natural fit at first sight: one `ScopedValue<Frame>` in place of the
  `ThreadLocal`, with `binding` as `ScopedValue.where(...).call(...)`, and `TBox`es inside the
  frame keeping `set!` working. But `Var.pushThreadBindings` and `popThreadBindings` are public,
  non-lexical operations. `with-bindings*`, the REPL (`arbace.main`), `Compiler.load` and
  tools like nREPL push in one place and pop in another, and `ScopedValue` cannot express that.
  Inheritance into `StructuredTaskScope` forks is nice but needs the preview API. **Verdict:**
  keep `ThreadLocal`. An own runtime would implement dynamic binding natively either way.

### 4.13 Foreign Function & Memory API

JEP 454, final in 22. It is not a compiler feature. A Clojure interop library on top of it
(`Linker`, `MemorySegment`, `Arena`; prior art: the `coffi` library) would give Arbace native
calls without JNI. It is relevant to the own-runtime plan in one way. If Arbace's interop to
native code goes through an Arbace-level API (`arbace.ffi`) rather than direct
`java.lang.foreign` calls, the same Clojure code ports to an own runtime with its own FFI (Go's
cgo is the analogue). **Verdict:** library work, outside this survey's scope.

### 4.14 Vector API

`jdk.incubator.vector` is still an **incubator** module in 26 (`package-info.java`, "{@incubating}").
It needs `--add-modules jdk.incubator.vector` and waits on Valhalla. **Verdict:** avoid.

### 4.15 Leyden: AOT cache, class loading and linking, method profiling

- JEP 483 (24): AOT class loading and linking (`-XX:AOTCache`, `AOTMode=record/create`).
- JEP 514 (25): one-step `-XX:AOTCacheOutput`.
- JEP 515 (25): method profiles in the cache, for faster warm-up.
- JEP 516 (26): AOT object caching with any GC (`AOTStreamableObjects`, `cds_globals.hpp:79`).

All final. AOT-compiled machine code is not in 26: `AOTAdapterCaching` and `AOTStubCaching`
are diagnostic flags, and there is no code cache flag.

How Arbace would use it:

- **Stages hold AOT-compiled namespaces.** Each stage would AOT-compile `arbace.core` and the
  rest with its own compiler, after building the class forms. This is deterministic (§3.1), so
  stage 2 = stage 3 can include them. The suite then runs against compiled namespaces, as
  upstream Clojure's does.
- **A jar plus a training run** produces `arbace.aot`. A launcher script uses it when present.

Measured (§3.1): 3.3 s → 1.07 s from AOT-compiled namespaces, → 0.45 s with the cache.

Cons and risks:

- The cache is tied to the exact JDK build and class path, and must be regenerated after each
  build. It covers only classes from jars loaded by built-in loaders, not REPL-defined classes.
- Training runs are another build step.
- Stage 1 bootstrapping order: the class-form stages must exist before the namespaces are
  compiled with them. That is a build-script matter.

Own runtime: a HotSpot-only mechanism, but nothing in the code depends on it. A Go-like own
runtime would compile ahead of time anyway; a JVM-like one would want its own snapshot or image
format. Doing this does not tie Arbace to HotSpot. **Verdict:** do first (item 1).

### 4.16 Lazy constants (formerly stable values)

JEP 502 introduced Stable Values in 25. In 26 they are **Lazy Constants, second preview** (JEP
526; `java/lang/LazyConstant.java` is `@PreviewFeature(LAZY_CONSTANTS)`). They are lazily
initialized values that the JIT treats as constants once set. They would fit the runtime's lazily
initialized statics, and maybe Var roots of vars that are never redefined. But they are preview,
and Var roots are mutable by contract. **Verdict:** avoid until final. Condy (§4.3) gives the
compiler-side laziness today.

### 4.17 Other items in 26

- **JEP 500, final means final (26)**: see 4.8. No change needed. Keep `Reflector` from mutating
  finals.
- **JEP 486 (24), Security Manager permanently disabled**: `RT`'s default imports still name
  `SecurityManager`. The class still exists in jdk26u, so nothing breaks, but it is a candidate
  for removal from the imports.
- **JEP 416 (18), core reflection on method handles**: already in effect (the
  `Reflector$$InjectedInvoker` hidden class). `Method.invoke` is faster than it was, which
  narrows the gap §4.1's reflective sites would close.
- **JEP 519 (25), compact object headers** (`-XX:+UseCompactObjectHeaders`, final): a free
  memory win for Clojure's many small objects (conses, boxed numbers, fns). Worth trying as a
  launcher default. No code change, no portability issue.
- **Primitive types in patterns** (JEP 530, fourth preview, `Preview.java` `PRIMITIVE_PATTERNS`):
  avoid.
- **Valhalla (JEP 401, value classes)**: not in 26. Looking ahead, value classes would suit
  Clojure's boxed numbers, `MapEntry` and small tuples, and a flattened `long`/`double` in
  collections. When it lands, the place to adopt it is `defclass` (a `^:value` flag) before the
  Clojure compiler.

## 5. Summary table

Abbreviations: P = portability to an own runtime (good: maps onto a simple runtime; medium:
needs a contained mechanism; poor: needs JSR 292 machinery or HotSpot internals).

| feature | JDK / status in 26 | benefit for Arbace | cost | risk to fixpoint / suite | P | verdict |
|---|---|---|---|---|---|---|
| AOT-compiled namespaces + AOT cache | JEP 483 (24), 514/515 (25), 516 (26); final | startup 3.3 s → 0.45 s | low-medium (build) | low; compile is deterministic | good (flags only) | **do first** |
| `ClassFile.verify` as test oracle | JEP 484 (24); final | catches bad bytecode early | low | none | n/a (tests) | **do first** |
| Compiler classfile version → running JDK | 61 → 70 | uniformity | trivial | none | good | **do first** |
| condy for constants | JEP 309 (11); final | lazy constants, smaller `<clinit>` | medium | low | medium | do next |
| indy keyword sites | JSR 292 (7); final | simpler code, modest speed | medium-low | low | poor | do next |
| indy reflective call sites | JSR 292, Dynalink idea | much faster un-hinted calls | medium | medium (messages, choice) | poor | do next |
| virtual-thread executor (opt-in) | JEP 444 (21), 491 (24); final | IO-bound agents and futures | low | low if opt-in | good | do next |
| indy Var calls | JSR 292; final | redefinable direct linking; no measured gain | medium | medium (redefs, deopt) | poor | defer |
| indy protocol PIC | JSR 292; final | faster polymorphic dispatch | high | medium | poor | defer |
| `java.lang.classfile` in place of ASM | JEP 484 (24); final | no vendored ASM to maintain | high | one-time byte churn | poor (unless vendored via j2c) | defer |
| `StringConcatFactory` for `str` | JEP 280 (9); final | small; AOT-resolvable | low-medium | low (`nil` semantics) | medium | defer |
| VarHandles in `Atom` and friends | JEP 193 (9); final | memory per atom | low | none | good (CAS) | optional |
| JPMS / `jlink` | JEP 261 (9); final | smaller images | medium | low | n/a | defer |
| compact object headers | JEP 519 (25); final | memory | none (flag) | none | n/a | try as default |
| FFM interop library | JEP 454 (22); final | native interop | medium (library) | none | good if abstracted | separate project |
| nestmates | JEP 181 (11); final | none for fns (class forms have them) | - | - | good | nothing to do |
| LMF for fns | Java 8; final | fewer class files | high | high (traces, `IFn` shape) | poor | **avoid** |
| hidden classes for fns | JEP 371 (15); final | per-class unloading | high | high (frames hidden) | medium | **avoid** |
| records / `ObjectMethods` for `defrecord` | JEP 395 (16); final | Java pattern interop | medium | high (equality) | good | **avoid** |
| sealed for Clojure types | JEP 409 (17); final | none (open by design) | - | - | good | avoid |
| `SwitchBootstraps` in `case` / `cond` | JEP 441 (21); final | marginal | medium | low | medium | avoid |
| `ScopedValue` for bindings | JEP 506 (25); final | cheaper inheritance | medium | high (push/pop API) | good | **avoid** |
| structured concurrency | JEP 525; 6th preview | - | - | preview | - | avoid |
| lazy constants | JEP 526; 2nd preview | JIT-constant lazy statics | - | preview | - | avoid until final |
| Vector API | incubator | - | - | incubator | - | avoid |
| primitive patterns | JEP 530; 4th preview | - | - | preview | - | avoid |
| Valhalla value classes | not in 26 | numbers, tuples | - | - | - | watch |

## 6. Sources

JDK sources, jdk26u at `baf63fbe42b8` (`/root/jdk26u`):

- `make/conf/version-numbers.conf`: feature 26, classfile major 70.
- `src/java.base/share/classes/jdk/internal/javac/PreviewFeature.java`: preview APIs in 26 are
  `STRUCTURED_CONCURRENCY` (JEP 525), `LAZY_CONSTANTS` (JEP 526) and `PEM_API` (JEP 524).
- `src/jdk.compiler/share/classes/com/sun/tools/javac/code/Preview.java`: the only preview
  language feature is `PRIMITIVE_PATTERNS`.
- `src/java.base/share/classes/java/lang/classfile/ClassFile.java`: `@since 24`; `verify` (758);
  `JAVA_26_VERSION = 70` (1039).
- `src/java.base/share/classes/java/lang/ScopedValue.java` (`@since 25`, final) and
  `java/lang/LazyConstant.java` (`@PreviewFeature`, 203).
- `src/java.base/share/classes/java/lang/invoke/` (`MutableCallSite`, `SwitchPoint`,
  `ConstantBootstraps`, `StringConcatFactory`, `LambdaMetafactory`) and
  `java/lang/runtime/` (`ObjectMethods`, `SwitchBootstraps`).
- `src/jdk.incubator.vector/share/classes/jdk/incubator/vector/package-info.java`: incubating.
- `src/jdk.dynalink/`: Dynalink, still shipped.
- `src/hotspot/share/cds/cds_globals.hpp`: `AOTMode`, `AOTCache`, `AOTCacheOutput`,
  `AOTClassLinking`, `AOTStreamableObjects`, training flags.
- `src/hotspot/share/cds/aotClassLinker.cpp:130`: AOT linking only for built-in loaders.
- `src/hotspot/share/cds/aotConstantPoolResolver.cpp:548`: only `StringConcatFactory` and
  `LambdaMetafactory` call sites are pre-resolved.
- `src/hotspot/share/cds/aotClassLocation.cpp:686-705`: no non-empty directories on the
  class path.
- `src/hotspot/share/classfile/classFileParser.cpp:2677` and `javaClasses.cpp:2867`: methods of
  hidden classes are hidden from stack traces (`ShowHiddenFrames`, `runtime/globals.hpp:1778`).
- `src/java.base/share/classes/sun/launcher/resources/launcher.properties:73-79`: JEP 500's
  `--enable-final-field-mutation` and `--illegal-final-field-mutation` (default `warn`).

Arbace and baseline code:

- `clojure/lang/Compiler.java` (frozen, Clojure `98d735fab02f`): `JVM_BYTECODE_VERSION` 347;
  FI adapter 1660-1806; method value thunks 1242; keyword sites 3825-3847; `StaticInvokeExpr`
  3961; `InvokeExpr.emitProto` 4236; `emitValue` 5264; `emitConstants` 5479; `emitVarValue`
  5740; locals clearing 6506-6596; `classWriter` 9632.
- `arbace/lang/Compiler.clj`: 433 (`V17`), 1703 (`maybeEmitFIAdapter`), 3418
  (`KeywordInvokeExpr`), 8441 (`COMPUTE_FRAMES`).
- `clojure/lang/DynamicClassLoader.java`, `Atom.java:18`, `LazySeq.java:32`, `Delay.java:28`,
  `Agent.java:50-54`, `core.clj:1652` (`locking`), `core_deftype.clj:523,590`.
- `arbace/classes/emit.clj`: 10 (`*version*`), 940 (`getCommonSuperClass`), 1149
  (`ObjectMethods`), 1305-1316 (nestmates, permitted subclasses, records), 1356
  (`StringConcatFactory`), 1501-1559 (`LambdaMetafactory`), 1894-1942 (`SwitchBootstraps`).
- `doc/classes/SPEC.md` §9 and §10; `doc/classes/COMPILER-NOTES.md`, "Native class forms".

JEPs (https://openjdk.org/jeps/N): 181 nestmates; 193 VarHandles; 261 modules; 274 enhanced
method handles; 276 Dynalink; 280 indified string concatenation; 309 dynamic class-file
constants; 371 hidden classes; 372 Nashorn removal; 395 records; 401 value classes (not in 26);
409 sealed classes; 416 reflection on method handles; 440 record patterns; 441 pattern
`switch`; 444 virtual threads; 454 FFM; 483 AOT class loading and linking; 484 Class-File API;
486 Security Manager disabled; 491 virtual threads without pinning; 500 final means final; 502
stable values; 506 scoped values; 514 AOT command-line ergonomics; 515 AOT method profiling; 516
AOT object caching with any GC; 519 compact object headers; 524 PEM (preview); 525 structured
concurrency (sixth preview); 526 lazy constants (second preview); 530 primitive patterns
(fourth preview).

Prior art:

- Ghadi Shayban, Clojure `invokedynamic` branch:
  https://github.com/ghadishayban/clojure/tree/invokedynamic. Described in #clojure-dev,
  2016-10-11:
  https://clojurians-log.clojureverse.org/clojure-dev/2016-10-11/1476200081.000614
- Ghadi Shayban, "Constant_Dynamic first experiences", amber-dev, July 2018:
  https://mail.openjdk.org/pipermail/amber-dev/2018-July/003246.html
- JRuby and invokedynamic, startup: https://github.com/cheister/jruby/wiki/PerformanceTuning,
  https://mail.openjdk.org/pipermail/mlvm-dev/2014-August/005866.html ("The Great Startup
  Problem"), https://www.infoq.com/news/2012/10/jruby-17, and JRuby 10 (indy on by default):
  https://www.devclass.com/development/2025/04/15/jruby-10-released-with-jump-to-ruby-34-java-21-despite-loss-of-red-hat-sponsorship/1621764
- Clojure 1.8 direct linking and 1.12 functional interfaces and method values: Clojure's
  `changes.md` (https://github.com/clojure/clojure/blob/master/changes.md) and
  https://clojure.org/reference/java_interop
- Kotlin (lambdas via `invokedynamic` by default since 2.0), Scala (LMF lambdas since 2.12),
  Groovy 4 (indy only): their release notes.
