# arbace/

What `arbace/` holds (all `.ae`, Arbace's source extension since 2026-10-11: Clojure plus the
Java forms, which Clojure cannot evaluate; VENDOR-NOTES.md, hand change 19): Arbace's Clojure
code. Three kinds of things live here:

- Arbace's own tools: `classes/` (the Java forms' analysis and the Go build's interpreter of
  them, `arbace.classes.*`; the bytecode back end is on the branch `arbace-for-java-26`), `repr/clojure.ae`
  and `repr/clojure/` (repr/clojure, Clojure source to Arbace's, `arbace.repr.clojure` and
  `arbace.repr.clojure.*`; doc/REPR-CLOJURE.md; c2a, `arbace.c2a.*`, until 2026-10-17), `repr/java.ae`
  and `repr/java/` (repr/java, the Java to Java forms converter, `arbace.repr.java` and
  `arbace.repr.java.*`), `repr/go.ae` and `repr/go/` (repr/go, Go to Go forms and back,
  `arbace.repr.go` and `arbace.repr.go.*`, with its Go helpers under `tools/godump/` and
  `tools/gocmp/`; doc/go/), `build/go.ae` (the build of a program held as Go forms,
  `arbace.build.go`; doc/go/BUILD.md; until 2026-10-17 j2a, g2a and `arbace.g2a.build`) and
  `j2g/` (the Java forms to Go forms translator of the Go build, `arbace.j2g.*`;
  doc/go/J2G-SPEC.md), `c2j/` (c2j, the back end compiling namespaces to Java forms, `arbace.c2j.*`;
  `arbace.aot.*` until 2026-10-17; doc/STANDALONE-NOTES.md, "S6: Arbace compiled to Go"),
  `go/check.ae` and `go/check/` (goi's checker, a type checker of Go forms written anew after
  go/types, `arbace.go.check` and `arbace.go.check.*`; doc/go/GOI-SPEC.md §3, GR2; evaluated
  from the checkout, not embedded, until the program carries goi).
- Arbace's own namespaces of the program, not derived from Clojure: `box.ae` and `box/`
  (`arbace.box`, the store, over its host layer `arbace.box.host`; the box's build driver
  `arbace.box.build`, its plan `arbace.box.plan`, the rebuild in the box `arbace.box.rebuild`
  with its printing steps `arbace.box.print`, the symbol table cut `arbace.box.elf`;
  doc/go/BOX-NOTES.md, S3, S7, S8), `tls.ae` and `tls/` (`arbace.tls`, TLS client connections,
  over `arbace.tls.host`) and `http.ae` (`arbace.http`, a small HTTP/1.1 client; BOX-NOTES.md,
  "TLS (option 2)").
- The vendored Clojure, renamed `clojure.*` → `arbace.*`, everything else: the namespaces
  `arbace.core`, `arbace.main`, `arbace.pprint`, ... (`*.ae`), and the Java classes as class
  forms, one namespace per Java package (`lang.ae`, `java/api.ae`) with one file per
  Java file (`lang/RT.ae`, `lang/Compiler.ae`, ...). Clojure's main class `clojure.main` is
  `arbace.lang.Main` (`lang/Main.ae`). A few files among them are Arbace's own, not derived from
  Clojure, and say so in their header (`lang/KeywordInvokeSite.ae`,
  `lang/ReflectorCallSite.ae`). `lang/go/` holds only the Go build's generated variants of `Compiler`,
  `CompilerOps.ae` (by `test/j2g/eval_ops.ae`) and `CompilerAOTFn.ae` (by `test/c2j/gen_fns.ae`);
  the hand-written variants it held of `RT`, `Main` and `Compiler` were merged into their bases
  (SA150).

ASM (`asm.ae`, `asm/`, Clojure's `clojure.asm` renamed `arbace.asm`), the JVM's bytecode back end
of `lang/Compiler.ae` and of the Java forms compiler (`classes/emit.ae` ...), `lang/Compile.ae`,
`lang/DynamicClassLoader.ae` and `lang/Intrinsics.ae` left main on 2026-10-12 with the JVM build
(doc/STANDALONE-NOTES.md, S4: the JVM removed); they remain on the branch `arbace-for-java-26`,
whose `doc/ARBACE.md` records ASM's origin (below). The namespaces `genclass.ae`,
`core_proxy.ae`, `reflect/java.ae`, `main.ae` and `core/server.ae` took the Go build's variants
then (they were under `lang/go/ns/`, since merged).

Origin of the vendored Clojure: the frozen baseline `clojure/` this repository held until the
freeze (it remains on the branch `arbace-for-java-26`), that is
https://github.com/clojure/clojure at `98d735fab02f337cee654cb0629bddc09883a75a` with ASM
(https://gitlab.ow2.org/asm/asm at `0460f74ba64230e5b846967c81a52628b5dd0596`, repackaged), with
the two seed fixes recorded in the journal on that branch
([JOURNAL.md](https://github.com/arbace/arbace/blob/arbace-for-java-26/doc/JOURNAL.md),
2026-10-06). It was derived on 2026-10-07 by `bin/vendor-arbace` (on that branch; converter
`arbace.j2c`, renaming `arbace.j2c.rename`; since renamed `arbace.j2a` (2026-10-11), then `arbace.repr.java` (2026-10-17), and `arbace.repr.clojure.rename`) and is maintained by hand since.

Also vendored, renamed alike: clojure.spec, the two libraries upstream Clojure at that revision
depends on (`pom.xml`), as `arbace.spec.alpha`, `arbace.spec.gen.alpha`, `arbace.spec.test.alpha`
(`spec/`, from https://github.com/clojure/spec.alpha at `v0.6.249`,
`3d1efb353b8a95c699b4051ba8273568c21f874e`) and `arbace.core.specs.alpha` (`core/specs/alpha.ae`,
from https://github.com/clojure/core.specs.alpha at `v0.6.133-alpha10`,
`75875946b7e827a1cec91ce645d5e1dcfc475e49`), derived on 2026-10-07 by `bin/vendor-spec` (on that branch) and
maintained by hand since; each file names its origin in a comment.

The converted files keep their Java file's notice as a comment; which license covers which file
is stated only in [LICENSE.md](../LICENSE.md). Details, the renaming rules and how to build and
run it: [VENDOR-NOTES.md](VENDOR-NOTES.md).
