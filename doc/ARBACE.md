# arbace/

What `arbace/` holds (all `.ae`, Arbace's source extension since 2026-10-11: Clojure plus the
class forms, which Clojure cannot evaluate; VENDOR-NOTES.md, hand change 19): Arbace's Clojure
code. Two kinds of things live here:

- Arbace's own tools: `classes/` (the class forms compiler, `arbace.classes.*`), `j2c/`
  (the Java to class forms converter, `arbace.j2c.*`), `g2c/` (Go to Arbace forms,
  `arbace.g2c.*`, with its Go helpers under `tools/godump/` and `tools/gocmp/`; doc/go/) and
  `c2g/` (the class forms to Go forms translator of the Go build, `arbace.c2g.*`;
  doc/go/C2G-SPEC.md).
- The vendored Clojure, renamed `clojure.*` → `arbace.*`, everything else: the namespaces
  `arbace.core`, `arbace.main`, `arbace.pprint`, ... (`*.ae`), and the Java classes as class
  forms, one namespace per Java package (`lang.ae`, `asm.ae`, `java/api.ae`) with one file per
  Java file (`lang/RT.ae`, `asm/ClassReader.ae`, ...). Clojure's main class `clojure.main` is
  `arbace.lang.Main` (`lang/Main.ae`). A few files among them are Arbace's own, not derived from
  Clojure, and say so in their header (`lang/KeywordInvokeSite.ae`,
  `lang/ReflectorCallSite.ae`). `lang/go/` holds the Go build's variants of some of these
  classes and namespaces (derived from the vendored files; read by c2g only, not by the JVM
  build).

Origin of the vendored Clojure: the frozen baseline `clojure/` this repository held until the
freeze (it remains on the branch `arbace-for-java-26`), that is
https://github.com/clojure/clojure at `98d735fab02f337cee654cb0629bddc09883a75a` with ASM
(https://gitlab.ow2.org/asm/asm at `0460f74ba64230e5b846967c81a52628b5dd0596`, repackaged), with
the two seed fixes recorded in the journal on that branch
([JOURNAL.md](https://github.com/arbace/arbace/blob/arbace-for-java-26/doc/JOURNAL.md),
2026-10-06). It was derived on 2026-10-07 by `bin/vendor-arbace` (on that branch; converter
`arbace.j2c`, renaming `arbace.j2c.rename`) and is maintained by hand since.

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
