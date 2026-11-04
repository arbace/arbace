# The evaluator's speed (2026-11-04)

Branch `eval-speed` (from main at `01d3ce9f`): the cost of the operations evaluated code does
most, measured one by one, and the largest of them cut in the evaluator (`arbace/lang/Compiler.ae`,
its closure compiler: doc/go/SPEED-NOTES.md, "The evaluator") and below it. Every interpreted
crop pays these costs: the crops' readers and interpreters evaluated from the checkout (repr/ocaml,
repr/haskell, repr/scheme and si, repr/cpp, the Python crop's), and on stage 0, until a re-seed
carries this branch, goi's, ci's and the tools' namespaces that differ from the seed's embedded
copies.

## Method

- **Micro-benchmarks**: `test/bench/eval-micro.ae` (new; usage in its header): one operation in
  a loop of 100,000 (10,000 for the slower ones), its thunk run once (the evaluator compiles a
  method at its first call), the earlier benchmarks' garbage collected, then run again for at
  least `EVAL_MICRO_MS` (300 ms) and 3 runs; the best run's time per iteration, `:ns`, and the
  same less the empty loop's (`:net`; the empty loop costs 27-35 ns). 124 benchmarks in 8
  groups: calls of every arity (fixed, variadic, `apply`, a local, a closure, a compiled core
  fn), closures made, `recur`, boxed and primitive arithmetic, vars, atoms, volatiles, `case`,
  destructuring; `aget`/`aset`/`alength` on each primitive array type, hinted, with a boxed value,
  and with the array unhinted (reflective); `nth`/`get`/invoke on vectors and maps, `assoc`,
  `conj`, transients, `into`; `str`, `StringBuilder`, `String` methods hinted and not; `instance?`;
  protocols on a record, a deftype and an extended class, record fields, multimethods; `pr-str`;
  `ArrayList`, `HashMap`, static calls and fields, `new`. The older `test/bench/eval-bench.ae`
  (whole workloads) is run too.
- **Profiles**: `ARBACE_CPUPROFILE=FILE` and TamaGo's `go tool pprof -top` on the executable
  (stage 0's, the gate's); the evaluator's nodes (`Compiler_Code*`) show by name, so a
  benchmark's profile says which node, helper or jrt function the time is in.
- **Executables**: before, the gate's build of main at `01d3ce9f` (`.tmp/gate-exe/a6081fbf20de4d0261a3`,
  the checkout's runtime; stage 0, built from the older seed, measures the same within the noise);
  after, this branch's `bin/arbace-go --build`. The host (AMD EPYC 7763, 64 processors) was
  shared with other agents' builds (load 8 to 16): differences under 10-15% are noise.

## Where the time went (before)

The micro-benchmarks on main's executables (ns per operation, net of the loop's 27-35 ns; every
benchmark's time under "Measurements"):

- **A call of an evaluated fn: 85 ns of arity 0, then 25-30 ns more per argument (the boxing of
  a primitive argument included); 715-960 ns past 5 arguments**, 345 ns variadic, 425 ns through
  `apply`. The profile of `fib` (`(fib 22)`, boxed): the per-thread state's `ThreadLocal` (a map
  lookup in jrt's `Thread`) 8%, the frame's reset 7.5% (two interface calls for the method's line
  and source), the arity's compiled method found through the `FnMethod` array 5.6%, the binding of
  each parameter by kind 4.7%, the frame's release 5.6%, the `try`'s deferred `jrt.Catch` and
  `recover` 6.5%, the var's `deref` 5%; the arithmetic is the rest. Past 5 arguments EvalFn
  answered no `invoke`: `RestFn` made an `ArraySeq` and `doInvoke` walked it (`boundedLength`,
  the methods searched one by one).
- **`aget` on a `byte`, `short`, `char` or `float` array 230-245 ns, `aset` 290-600 ns, where an
  `int`, `long`, `double`, `boolean` or `Object` array takes 30-45**: the static calls the
  evaluator makes directly (`CodeOpN`, generated: test/c2j/gen_nodes.ae) were only those whose
  parameters and return are `long`, `int`, `double`, `boolean` or a reference; RT's `aget` and
  `aset` of the other arrays, and the casts `(byte x)`, `(char x)`, `(float x)`, went through the
  method's invoker (`CodeHostStatic`: an argument array, `Evaluator.typedArgs`, jrt's
  `convertArgs` copying it again, the result boxed).
- **A reflective call 2.7-14 µs**: `(aget a i)`, `(aset a i v)` and `(alength a)` of an unhinted
  array 11.6-13.2 µs, `(.append sb "x")` 10 µs, `(.length s)` 3.9 µs, `(.get al i)` 2.8 µs.
  Reflector looked the method up at every call: `Class.getMethods` makes a `Method` per public
  member (RT's 400 of them, `newMethod` and the collector two thirds of the time), then the
  matching. A boxed value into a hinted array is reflective when the analyzer does not know its
  class (`(aset ^longs a i o)`, o an Object to it: 12.8 µs); a `Long` into a `long[]` is
  resolved (50 ns), a `Byte` into a `byte[]` too but through the invoker (350 ns).
- **A resolved interop call 70-230 ns**: `(.length ^String s)` 70, `(.charAt ^String s i)` 165,
  `(.append ^StringBuilder sb "x")` with `setLength` 315, `(.get ^ArrayList l i)` 180,
  `(.get ^HashMap m k)` 145: `CodeHostInstance` makes the argument array, casts the target,
  converts the arguments (`typedArgs`), calls the member table's invoker through jrt's
  `invokeDirect` (`convertArgs` copies them again for a primitive parameter), boxes the result.
- **A protocol call 410-1,125 ns**: three evaluated calls (the protocol fn, the interface fn
  `-cache-protocol-fn` returns, the record's method), `MethodImplCache.fnFor` and the field
  `__methodImplCache` through their invokers, and, at each call of an interface made at run time,
  `dynConvert` (j2g's Dyn dispatch) building the return class's descriptor string
  (`Class.Descriptor`: `strings.ReplaceAll` and a concatenation) to learn it is not a primitive:
  13% of the call. A record's field by keyword, `(:r rec)`, 765 ns (its `valAt`, an evaluated
  method with a `case`).
- **`case` 125-390 ns**: `CodeCase` boxed its key into an `Integer` and asked three `HashMap`s
  (`containsKey`, then the test and the then).
- **`str`**: `(str i)` 150 ns, `(str "a" i "b")` 265 ns, of 13 pieces 965 ns: a quarter of it
  jrt's `Long.toString`, the JDK's `DecimalDigits` writing two digits at a time through `Unsafe`
  (a call per byte on Go).
- **`pr-str` 2.3-8.4 µs** (compiled code: `binding`'s `hash-map` and `pushThreadBindings` 30% of
  a vector's, the `print-method` multimethod, the writer): not the evaluator's.
- The empty loop (`(< i n)`, `(inc i)`, `recur`) 27-35 ns an iteration; `CodeRecur` handled one
  value as it handles many (the kinds by array, the temporaries' bookkeeping).

## The changes

Each keeps the semantics: the values, the exceptions and their messages, the reflection
warnings (the analysis is unchanged: the changes are in how the evaluator runs what it
analyzed), the boxing and the stack traces (jrt's mapping counts the frames of the call's `try`,
which every call still goes through; the new nodes are `Compiler$Code*`, internal to it), with
one exception, a correction: a `double` argument of a `float` parameter is narrowed as the
bytecode narrows it (item 5). The compiled path is untouched: c2j emits what it emitted (the
changed generator, `arbace.c2j.node`, only adds to the evaluator's half), and generated-check
regenerates `arbace/lang/go/CompilerNodes.ae` from the changed definitions.

1. **The thread's evaluator state in jrt's `Thread`** (`CodeRun.state`, `Evaluator.trace`): a
   field `EvalState` of jrt's `Thread` read and set by two natives (`go/arbace/jrt/natives.ae`:
   `Compiler_CodeRun_ThreadState__O_native`, `..._SetThreadState_O__V_native`; goi keeps its
   own the same way, `Thread.Interp`), in place of the `ThreadLocal` `STATE`, whose `get` is a map
   lookup in the thread's table. One state per thread as before.
2. **The call path** (`EvalFn`, `EvalMethod`, `Evaluator`, `CodeRun`, `Frame`, `CMethod`):
   - the call's own methods (the state, the frame, the call of the compiled method with its `try`,
     `invokeFn` and `invokeMethod`) are `CodeRun`'s, a class without a static initializer:
     `Evaluator`'s static methods each begin with its initialization guard, five of them a call,
     and gc's inlining of them is not to be relied on (the second build of this work, with more
     generated code in the package, was 10-20 ns a call slower than the first for it: the
     guards' `ClassInit.Done` no longer inlined); the push and pop are written out in them; jrt's
     stack-trace mapping (`isEvalCall`, `go/arbace/jrt/throwable.ae`) knows the `try`'s literal
     under `CodeRun`'s name too;
   - the compiled methods by arity cached on the `FnExpr` (`evalArities`, now a `CMethod[]`,
     filled at each arity's first call; its last element marks an arity without a fixed method),
     one load where it took the `FnMethod` array and the method's `evalCM`;
   - a `CMethod` keeps its method's line and its fn's source (`Frame.reset` read them through two
     interface calls per call), whether all its parameters are stored as they are (`plain`: the
     calls of arity 1-5 then store them directly, else `CodeRun.bind` by kind as before), and its
     rest parameter's slot (`restIdx`);
   - `EvalFn` answers `invoke` of 6 to 20 arguments: the values in an array, bound from it
     (`Evaluator.invokeArray`) where `RestFn` made an `ArraySeq` for `doInvoke`;
   - a call of an arity without a fixed method (a variadic fn of 1 to 20 arguments, or an arity
     error) binds the variadic method's fixed parameters from the arguments and its rest
     parameter to an `ArraySeq` of the others, at their offset (`Evaluator.invokeSpread`): the
     seq `doInvoke` made of them, and the one its `next`s reached, is that same `ArraySeq`; the
     `ArityException`'s count is the same; the seq path (`apply`, `doInvoke`) finds the method by
     the cached arity.
3. **`recur` of one or two values** (`CodeRecur1`, `CodeRecur2`): a value stored as a slot, a
   long or a double (store kinds 0, 2, 3), computed into Go's locals and then stored, without
   `CodeRecur`'s per-value dispatch over arrays and without frame temporaries; the values are
   computed in order before any is stored, as before; other recurs (a conversion by
   `Evaluator.prim`, compat mode's boxed locals, more values) are `CodeRecur`'s.
4. **`case`** (`CodeCase`): the keys in order in an `int[]` (the test map is a `SortedMap`),
   found by bisection, their tests, thens and skip checks in arrays beside them; an integer
   expression's key from its unboxed long (`runLong`); the choice is `Evaluator.caseKey`'s and
   `CaseExpr.evalIn`'s, test by test.
5. **Static calls of `byte`, `short`, `char` and `float`, and the JDK's most used static
   methods** (test/c2j/gen_nodes.ae's kinds and classes, `arbace.c2j.node`'s channels,
   `CodeCompiler.convert`, `CodeUnbox`): the listed methods are now 895 (579): RT's `aget` and
   `aset` of `byte[]`, `short[]`, `char[]`, `float[]`, the casts to and from them, Numbers' float
   operations, Math's float functions, and the static methods of `Character` (`isDigit`,
   `isWhitespace`, `digit`, `toChars` ...), `Long`, `Integer` and `Double` (`parseLong`,
   `parseInt`, `isNaN`, `toHexString` ...), `Boolean` and `String` (`valueOf`). A `byte`, `short` or `char`
   result is boxed in `run` and widened in `runLong`, a `float` widened in `runDouble`; an
   argument for such a parameter is narrowed from that channel when the argument is of the type
   (exact), a `double` to a `float` by Java's d2f (MethodExpr.emitTypedArgs'), else unboxed by
   RT's checked casts as `Evaluator.typedArgs` unboxes it (a `Character` as its code, then
   `RT.byteCast(Object)` ...: `CodeUnbox`'s modes 4 to 7). A `byte` or `short` argument of a
   `long` or `int` parameter, and a `float` one of a `double`, are passed in their channel (their
   value, as the boxed path gave it). A `char` argument is taken from `runLong` only from a
   direct call returning a `char` (its own channel), else from the boxed `Character`. The
   correction: a `double` argument of a `float` parameter (`(aset ^floats a i 1e300)`) is
   narrowed by d2f, as the bytecode (MethodExpr.emitTypedArgs) and c2j narrow it, where the
   method's invoker path checked it with `RT.floatCast(Object)`: before, the evaluator threw
   `IllegalArgumentException` ("Value out of range for float") where compiled code stored
   `##Inf`; now both store `##Inf` (no oracle or diffcheck case has it).
6. **Instance calls of the JDK's most used methods** (a new node kind, `instance-call`, defined
   in `arbace.c2j.emit` with `defnode` but evaluator-only, `:eval-only`: c2j compiles every
   resolved instance call already, `emit-instance-method`): 156 methods of `String`,
   `StringBuilder`, `CharSequence`, `Character`, `Number`, `ArrayList`, `HashMap`, `List`, `Map`,
   `Collection`, `Iterator`, `Set`, `HashSet`, `IdentityHashMap` and `ArrayDeque`, with
   `MethodImplCache.fnFor` (a protocol fn's dispatch) and `Volatile`'s `deref` and `reset`
   (`vswap!`, `vreset!`) (by name in test/c2j/gen_nodes.ae, those the class declares, public, not
   varargs, no checked exception jrt's reflection knows of, at most 3 parameters of usable kinds), a
   switch per arity (`CodeCall0`-`CodeCall3`) as the static calls': the target cast to the
   method's class, the arguments converted as the static calls' are, the call virtual, the value
   boxed as its return's or in its primitive channel. Their order is the compiled call's
   (InstanceMethodExpr.emit: the target's checkcast, then each argument converted in turn, then
   the call); `CodeHostInstance` cast the target after evaluating the arguments and converted them
   after the cast, an order no value depends on and an exception's choice only when both the
   target and an argument are wrong.
7. **Inline caches of reflective calls** (`CodeHostInstanceReflect`, `CodeHostStaticReflect`,
   the reflective branch of `CodeHostField`; `CodeReflectEntry`, `CodeMemberEntry`), after the
   JVM build's `ReflectorCallSite`: a site keeps, for up to 4 combinations of the receiver's class
   and, when Reflector chose among several methods, the arguments' classes, the method Reflector
   chose (`Reflector.selectMatchingMethod`, Reflector's own code, on the call that misses) and
   whether it widened the arguments; a hit calls it as `invokeMatchingMethod` calls the method it
   chooses (the arguments widened as `widenBoxedArgs` widens them, `boxArgs`, `prepRet`, the
   cause of an exception thrown as Reflector throws it). Reflector's choice is a function of those
   classes (its matching reads the classes, and jrt's `Method.canAccess` is true), so a hit is
   what Reflector would have done; a Go value (goi's `GoValue`), nil, an object of a class made
   at run time (whose members the REPL's Java forms may publish again) and a static call of
   `new` go through Reflector every time. The member reads (`(.-f x)`, and `(.m x)` with no
   arguments, which Reflector reads as a method else a field) cache the method or field by the
   receiver's class.
8. **The descriptor in `dynConvert`** (arbace/j2g/dyn.ae, j2g's Dyn dispatch: the return value
   of a method of an interface made at run time): a reference class returns the value at once,
   before its descriptor is built.
9. **Smaller ones**: a host call with no arguments shares `RT.EMPTY_ARRAY` (`CodeRun.argsOf`;
   the calls read their arguments, none writes or keeps the array); `CodeHostNew` finds once
   whether its class is abstract and the constructor's parameter types (a stub class, a deftype's
   being defined, still looks its class up at each call); a var that is not dynamic is read by
   `getRawRoot` (`CodeVarRoot`), as compiled code reads it (a var that is not dynamic has no
   thread binding: `pushThreadBindings` refuses one); `Long.toString(long)` and
   `Integer.toString(int)` are `String.valueOf`, jrt's (`overlay/jdk/variants/Long.ae`,
   `Integer.ae`: the same text, through Go's strconv).

## Checks (by day)

- The build itself: `bin/arbace-go --build`'s first program (the runtime changed, so the
  namespaces are compiled by a first build, not by the host) loads every embedded namespace from
  source on the new evaluator, `arbace.core`, spec, c2j, j2g, goi, ci and repr/c among them (157
  sources), and c2j compiles them: 157 compiled, 0 failed, as on main.
- Clojure's suite, every namespace of its list (78) run on the plain executables one by one, as
  `bin/clojure-tests` runs them (the main checkout's translated suite, `.tmp/clojure-tests/go`;
  the plain executable holds no Java fixtures, so the 8 namespaces that need them fail to load
  on both): the 78 result files byte for byte the same before and after.
- si's expander tests (`bin/si test`: psyntax evaluated from the checkout on the tools' host,
  `ARBACE_HOST_EXE` the executable): 60 of 64 pass, 4 known, the same lines.
- `bin/generated-check EXE` on the new executable: `CompilerNodes.ae` as test/c2j/gen_nodes.ae
  writes it, and the other generated files unchanged.
- Small cases against main's executable: arity errors (fixed, variadic, past 5 arguments, through
  `apply`), variadic calls of 0 to 3 extra arguments, `aset` of a byte, `.charAt`, an unhinted
  `.length`: the same values and messages. Stack traces of exceptions through evaluated fns,
  a deftype's method, an interface method, calls past 5 arguments, variadic and `apply` calls,
  `case`, `loop`, `aget` out of bounds, `.charAt` hinted and not, `(byte 300)`: the same frames,
  but for the lines of `Compiler.ae`'s own (its source changed); the exceptions' classes and
  messages of out-of-range casts, a nil or mistyped target, `aset` of an Object, `case` on a
  double: the same, but `(aset ^floats a 0 1e300)` (item 5's correction: `##Inf`, as the frozen
  JVM's v4 jar answers, where main's evaluator threw).
- `bin/j2g --slice arbace/lang/Compiler --root 'arbace/lang/Compiler$*'` (on the gate's
  executable, a minute) after each change: Compiler's classes, the generated nodes among them,
  analyzed and translated by j2g, no analysis failure, the same untranslated stubs as main's.
- The oracle on the new executable (`bin/oracle check "EXE -" --timeout 900 --expected
  test/oracle/known-go-amd64-frozen.edn`, `FROZEN_JAR` the main checkout's pinned jar):
  22,689 of 22,741 cases match, the 52 known mismatches as recorded, 0 new, 0 now passing.
- `bin/j2g-edge verify` (the frozen JVM compiles j2g's namespaces from the checkout): its first
  run failed, `MethodTooLargeException` compiling `arbace/j2g/dyn.clj`, `forms`' static
  initializer past the JVM's 64 KB with dynConvert's one more quoted form; dynConvert's form is
  now a fn of its own (`dyn-convert-form`), j2g's output byte for byte the same (a slice run's
  `gen/` tree compared); its second run passed: `jdk/edge.edn` is the JVM's answer.

## The night's proof

- `bin/gate` (the essential gate): the smoke test, the per-form differential check (the oracle's
  forms evaluated against compiled: the evaluator's semantics against c2j's), Clojure's suite on
  the test build, the oracle against the frozen JVM, goi's, Java's and C's essential checks, the
  generated variants, the programs.
- `bin/arbace-go --fixpoint --g1 "$(. bin/lib/gate-cache.bash; arbace_gate_cache "$PWD")"` (the
  `--full` step `fixpoint`, on the gate's build of this tree): G2 = G3, the tools hosted on the
  new evaluator where they are evaluated.
- By hand, any time: `bin/generated-check EXE`, `bin/si test` with `ARBACE_HOST_EXE=EXE`, and
  test/bench/eval-micro.ae against main's executable (doc's tables).

## Measurements

The executables: before, main's gate build (`01d3ce9f`); after, this branch's third build
(`bin/arbace-go --build`, amd64). Every number is the best of its runs; the host was shared
(load 4 to 10), so differences under 10-15% are noise (`aget-longs`, 62 against 83 in this run,
is 58-63 against 53-56 run again three times).

**The micro-benchmarks** (test/bench/eval-micro.ae, `EVAL_MICRO_MS=400`, each group in a
process of its own, the two executables in turn; ns per iteration, the loop's 24-29 ns
included):

| benchmark | before (ns) | after (ns) | speedup |
|---|---:|---:|---:|
| `call0` | 112 | 85 | 1.3x |
| `call1` | 144 | 110 | 1.3x |
| `call2` | 165 | 130 | 1.3x |
| `call3` | 192 | 152 | 1.3x |
| `call4` | 202 | 170 | 1.2x |
| `call5` | 232 | 200 | 1.2x |
| `call6` | 731 | 304 | 2.4x |
| `call8` | 915 | 370 | 2.5x |
| `call-variadic` | 376 | 199 | 1.9x |
| `call-apply` | 457 | 388 | 1.2x |
| `call-multi` | 164 | 144 | 1.1x |
| `call-long` | 154 | 121 | 1.3x |
| `call-double` | 145 | 111 | 1.3x |
| `call-local` | 141 | 115 | 1.2x |
| `call-closure` | 168 | 146 | 1.2x |
| `call-self` | 150 | 123 | 1.2x |
| `call-core` | 71 | 61 | 1.2x |
| `call-core2` | 55 | 54 | 1.0x |
| `make-closure` | 118 | 108 | 1.1x |
| `make-closure-call` | 249 | 220 | 1.1x |
| `letfn` | 223 | 204 | 1.1x |
| `kw-invoke` | 55 | 48 | 1.1x |
| `loop` | 29 | 24 | 1.2x |
| `loop-boxed` | 71 | 62 | 1.1x |
| `loop-2` | 48 | 38 | 1.3x |
| `loop-double` | 49 | 35 | 1.4x |
| `boxed-add` | 50 | 43 | 1.2x |
| `boxed-lt` | 41 | 39 | 1.1x |
| `long-arith` | 71 | 65 | 1.1x |
| `eq-kw` | 59 | 53 | 1.1x |
| `eq-long` | 41 | 35 | 1.2x |
| `var-deref` | 39 | 29 | 1.4x |
| `dyn-deref` | 37 | 34 | 1.1x |
| `atom-swap` | 149 | 144 | 1.0x |
| `vswap` | 246 | 75 | 3.3x |
| `case-kw` | 197 | 66 | 3.0x |
| `case-int` | 148 | 55 | 2.7x |
| `let-vec` | 114 | 103 | 1.1x |
| `let-map` | 142 | 131 | 1.1x |
| `instance` | 44 | 33 | 1.3x |
| `string?` | 60 | 49 | 1.2x |
| `class` | 62 | 58 | 1.1x |
| `nil?` | 44 | 35 | 1.3x |
| `aget-bytes` | 252 | 57 | 4.4x |
| `aget-shorts` | 258 | 60 | 4.3x |
| `aget-ints` | 61 | 58 | 1.1x |
| `aget-longs` | 62 | 83 | 0.7x |
| `aget-floats` | 280 | 67 | 4.2x |
| `aget-doubles` | 74 | 70 | 1.1x |
| `aget-chars` | 261 | 58 | 4.5x |
| `aget-booleans` | 58 | 57 | 1.0x |
| `aget-objects` | 58 | 56 | 1.1x |
| `aget-bytes-use` | 310 | 66 | 4.7x |
| `aset-bytes` | 490 | 61 | 8.1x |
| `aset-shorts` | 508 | 58 | 8.8x |
| `aset-ints` | 71 | 71 | 1.0x |
| `aset-longs` | 64 | 62 | 1.0x |
| `aset-floats` | 603 | 71 | 8.5x |
| `aset-doubles` | 67 | 66 | 1.0x |
| `aset-chars` | 341 | 71 | 4.8x |
| `aset-booleans` | 64 | 59 | 1.1x |
| `aset-objects` | 60 | 59 | 1.0x |
| `aset-longs-boxed` | 76 | 68 | 1.1x |
| `aset-ints-boxed` | 82 | 78 | 1.1x |
| `aset-bytes-boxed` | 377 | 62 | 6.1x |
| `aset-longs-object` (measured apart) | 12837 | 465 | 27.6x |
| `aget-unhinted-longs` | 12407 | 440 | 28.2x |
| `aget-unhinted-bytes` | 12182 | 357 | 34.1x |
| `aget-unhinted-objects` | 12540 | 339 | 37.0x |
| `aset-unhinted-longs` | 14991 | 530 | 28.3x |
| `aset-unhinted-objects` | 16681 | 496 | 33.6x |
| `alength-hinted` | 48 | 39 | 1.2x |
| `alength-unhinted` | 13894 | 225 | 61.7x |
| `vec-nth` | 61 | 60 | 1.0x |
| `vec-get` | 78 | 73 | 1.1x |
| `vec-invoke` | 67 | 57 | 1.2x |
| `map-get` | 63 | 53 | 1.2x |
| `map-invoke` | 52 | 48 | 1.1x |
| `hash-map-get` | 83 | 67 | 1.2x |
| `assoc` | 204 | 191 | 1.1x |
| `conj-vec` | 208 | 191 | 1.1x |
| `count-vec` | 58 | 50 | 1.2x |
| `first-next` | 108 | 93 | 1.2x |
| `transient-conj` | 119 | 93 | 1.3x |
| `transient-assoc` | 197 | 171 | 1.2x |
| `into-vec` | 664 | 661 | 1.0x |
| `seq-vec` | 95 | 92 | 1.0x |
| `str-1` | 179 | 103 | 1.7x |
| `str-3` | 293 | 224 | 1.3x |
| `str-13` | 1006 | 753 | 1.3x |
| `str-fn` | 428 | 433 | 1.0x |
| `sb-append` | 306 | 59 | 5.2x |
| `sb-append-unhinted` | 9808 | 436 | 22.5x |
| `string-length` | 98 | 38 | 2.6x |
| `string-length-unhinted` | 3946 | 107 | 36.8x |
| `string-charat` | 192 | 43 | 4.4x |
| `subs` | 113 | 112 | 1.0x |
| `string-from-bytes` | 382 | 335 | 1.1x |
| `protocol-record` | 960 | 524 | 1.8x |
| `protocol-deftype` | 1027 | 469 | 2.2x |
| `protocol-extended` | 433 | 254 | 1.7x |
| `protocol-2` | 1088 | 608 | 1.8x |
| `record-field` | 773 | 486 | 1.6x |
| `record-field-dot` | 76 | 62 | 1.2x |
| `deftype-field` | 74 | 74 | 1.0x |
| `multimethod` | 199 | 164 | 1.2x |
| `multimethod-class` | 189 | 166 | 1.1x |
| `pr-str-long` | 2528 | 2309 | 1.1x |
| `pr-str-string` | 2937 | 2844 | 1.0x |
| `pr-str-kw` | 2344 | 2245 | 1.0x |
| `pr-str-vec` | 5998 | 5742 | 1.0x |
| `pr-str-map` | 7640 | 7434 | 1.0x |
| `str-vec` | 4212 | 3840 | 1.1x |
| `arraylist-get` | 207 | 55 | 3.8x |
| `arraylist-get-unhinted` | 2950 | 281 | 10.5x |
| `arraylist-set` | 267 | 61 | 4.4x |
| `arraylist-add` | 188 | 73 | 2.6x |
| `arraylist-size` | 105 | 39 | 2.7x |
| `hashmap-get` | 171 | 61 | 2.8x |
| `static-call` | 45 | 45 | 1.0x |
| `static-call-other` | 161 | 48 | 3.4x |
| `static-field` | 33 | 30 | 1.1x |
| `new-object` | 113 | 50 | 2.3x |
| `new-sb` | 216 | 151 | 1.4x |

**The workloads** (test/bench/eval-bench.ae, ms, best run; its own code evaluated, arbace.core
compiled):

| benchmark | before | after | speedup |
|---|---:|---:|---:|
| `fib` | 7.8 | 6.0 | 1.3x |
| `fib-prim` | 8.6 | 7.1 | 1.2x |
| `loop-long` | 62.8 | 49.8 | 1.3x |
| `loop-double` | 12.3 | 9.2 | 1.3x |
| `dotimes-array` | 16.6 | 16.0 | 1.0x |
| `reduce-range` | 37.9 | 37.7 | 1.0x |
| `reduce-fn` | 29.8 | 27.0 | 1.1x |
| `vector-conj` | 26.7 | 23.6 | 1.1x |
| `map-assoc-get` | 24.3 | 23.0 | 1.1x |
| `keyword-map` | 18.9 | 14.5 | 1.3x |
| `str-build` | 11.9 | 7.6 | 1.6x |
| `seq-ops` | 33.8 | 33.7 | 1.0x |
| `lazy-walk` | 23.4 | 22.6 | 1.0x |
| `transduce` | 19.0 | 17.5 | 1.1x |
| `into-xform` | 11.7 | 11.8 | 1.0x |
| `protocol` | 123.7 | 68.0 | 1.8x |
| `multimethod` | 17.7 | 14.9 | 1.2x |
| `destructure` | 55.7 | 49.9 | 1.1x |
| `closures` | 45.9 | 39.6 | 1.2x |

**Whole runs**:

| run | before | after |
|---|---:|---:|
| si's expander tests, `bin/si test` (psyntax evaluated from the checkout on the tools' host) | 54.1 s | 30.8 s |
| Clojure's suite, its 78 namespaces 8 at a time (each loads every test namespace from source first), the processes' times summed | 670 s | 567-625 s (two runs) |
| start, `-e nil` (the namespaces compiled: the evaluator does little) | 118 ms | 119 ms |

## What is left

- **A call is still 60-85 ns of overhead** (arity 0 85 ns with the loop, from 112): the `try` of
  every call is j2g's translation of `try`/`finally`, a function literal with a deferred
  `jrt.Catch` that recovers and panics again (7-8 ns a call; an exception through N evaluated
  frames is N recovers and N panics); the frame's reset and release (some 20 stores); the IFn
  interface call and the type assertions. A `finally` alone could be Go's `defer` without a
  `recover` (a change to j2g's `translate-try`, and to jrt's `isEvalCall`, which knows the call
  by the literal's name); frames could skip storing what has not changed for the same method.
- **Protocols, 250-610 ns**: still three evaluated calls (the protocol fn, the interface fn,
  the method) and the interface method of a class made at run time found by its key in a Go map
  (`ByKey`, a string hashed at every call). The JVM's compiled call site
  (InvokeExpr.emitProto) calls the interface method directly when the target implements it; the
  evaluator could do the same for `InvokeExpr.isProtocol`, with the method's `EvalMethod` cached
  by the target's class. A record's field by keyword (`(:r rec)`, 485 ns) is its evaluated
  `valAt`; the JVM's keyword call site uses the record's `IKeywordLookup` thunk.
- **A reflective call is 225-530 ns on a hit** (Method.invoke's wrapping of exceptions,
  `boxArgs`' array, the guard over the classes): 20 to 60 times faster, still 5-10 times a hinted
  call: the reflection warnings stay the way to find them. A class made at run time is not
  cached (its members may be published again by the REPL's Java forms).
- **Interop not listed**: a resolved call of a method not in the instance-call or static-call
  lists goes through its invoker (70-230 ns); the lists are by name in test/c2j/gen_nodes.ae,
  easy to extend (a method j2g cannot reach from Java forms fails the build's j2g, and
  generated-check keeps the file in step).
- **`pr-str`, 2.2-7.4 µs** (compiled code, the runtime's): `binding`'s `hash-map` and
  `pushThreadBindings` (two binds in `pr-str` of a vector), jrt's `ThreadLocal` (a Go map lookup
  per get: every dynamic var's deref once a thread binds it, `Var.dvals`), the `print-method`
  multimethod, the writer. Not the evaluator's.
- **The empty loop, 24 ns** an iteration: the static call's switch per operation, the recur flag
  and `CodeLoop`'s test, two virtual calls per node.
- **Stage 0**: the crops evaluated on stage 0 (its seed predates this) get none of it until a
  re-seed; the executables built from the checkout (the gate's, `bin/arbace-go --build`'s) have
  it, and `ARBACE_HOST_EXE` can name one as the tools' host meanwhile.
