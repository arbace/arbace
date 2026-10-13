# arbace/

What `arbace/` holds (all `.ae`, Arbace's source extension since 2026-10-11: Clojure plus the
class forms, which Clojure cannot evaluate; VENDOR-NOTES.md, hand change 19): Arbace's Clojure
code. Two kinds of things live here:

- Arbace's own tools: `classes/` (the class forms' analysis and the Go build's interpreter of
  them, `arbace.classes.*`; the bytecode back end is on the branch `arbace-for-java-26`), `c2a/` (the
  Clojure to Arbace translator, `arbace.c2a.*`; doc/C2A.md), `j2a/`
  (the Java to class forms converter, `arbace.j2a.*`), `g2a/` (Go to Arbace forms,
  `arbace.g2a.*`, with its Go helpers under `tools/godump/` and `tools/gocmp/`; doc/go/) and
  `a2g/` (the class forms to Go forms translator of the Go build, `arbace.a2g.*`;
  doc/go/A2G-SPEC.md).
- The vendored Clojure, renamed `clojure.*` → `arbace.*`, everything else: the namespaces
  `arbace.core`, `arbace.main`, `arbace.pprint`, ... (`*.ae`), and the Java classes as class
  forms, one namespace per Java package (`lang.ae`, `java/api.ae`) with one file per
  Java file (`lang/RT.ae`, `lang/Compiler.ae`, ...). Clojure's main class `clojure.main` is
  `arbace.lang.Main` (`lang/Main.ae`). A few files among them are Arbace's own, not derived from
  Clojure, and say so in their header (`lang/KeywordInvokeSite.ae`,
  `lang/ReflectorCallSite.ae`). `lang/go/` holds the Go build's variants of some of these
  classes and namespaces (derived from the vendored files; read by a2g only).

ASM (`asm.ae`, `asm/`, Clojure's `clojure.asm` renamed `arbace.asm`), the JVM's bytecode back end
of `lang/Compiler.ae` and of the class forms compiler (`classes/emit.ae` ...), `lang/Compile.ae`,
`lang/DynamicClassLoader.ae` and `lang/Intrinsics.ae` left main on 2026-10-12 with the JVM build
(doc/STANDALONE-NOTES.md, S4: the JVM removed); they remain on the branch `arbace-for-java-26`,
whose `doc/ARBACE.md` records ASM's origin (below). The namespaces `genclass.ae`,
`core_proxy.ae`, `reflect/java.ae`, `main.ae` and `core/server.ae` took the Go build's variants
then (`lang/go/ns/`).

Origin of the vendored Clojure: the frozen baseline `clojure/` this repository held until the
freeze (it remains on the branch `arbace-for-java-26`), that is
https://github.com/clojure/clojure at `98d735fab02f337cee654cb0629bddc09883a75a` with ASM
(https://gitlab.ow2.org/asm/asm at `0460f74ba64230e5b846967c81a52628b5dd0596`, repackaged), with
the two seed fixes recorded in the journal on that branch
([JOURNAL.md](https://github.com/arbace/arbace/blob/arbace-for-java-26/doc/JOURNAL.md),
2026-10-06). It was derived on 2026-10-07 by `bin/vendor-arbace` (on that branch; converter
`arbace.j2c`, renaming `arbace.j2c.rename`; since renamed `arbace.j2a` and `arbace.c2a.rename`) and is maintained by hand since.

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
