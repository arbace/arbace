# arbace/

Arbace's Clojure code. Two kinds of things live here:

- Arbace's own tools: `classes/` (the class forms compiler, `arbace.classes.*`), `j2c/` (the Java
  to class forms converter, `arbace.j2c.*`) and `javalisp/` (`arbace.javalisp.*`).
- The vendored Clojure, renamed `clojure.*` → `arbace.*`, everything else: the namespaces
  `arbace.core`, `arbace.main`, `arbace.pprint`, ... (`*.clj`), and the Java classes as class
  forms, one namespace per Java package (`lang.clj`, `asm.clj`, `java/api.clj`, `../arbace.clj`)
  with one file per Java file (`lang/RT.clj`, `asm/ClassReader.clj`, `main_class.clj`, ...).

Origin of the vendored Clojure: the frozen baseline `clojure/` of this repository, that is
https://github.com/clojure/clojure at `98d735fab02f337cee654cb0629bddc09883a75a` with ASM
(https://gitlab.ow2.org/asm/asm at `0460f74ba64230e5b846967c81a52628b5dd0596`, repackaged), with
the two seed fixes recorded in doc/JOURNAL.md. It was derived on 2026-10-07 by
`bin/vendor-arbace` (converter `arbace.j2c`, renaming `arbace.j2c.rename`) and is maintained by
hand since. Licenses: Eclipse Public License 1.0 for Clojure, BSD 3-Clause for ASM
(`asm/`, `asm.clj`), both in LICENSE.md. The converted files keep their Java file's notice as a
comment. Details, the renaming rules and how to build and run it: doc/VENDOR-NOTES.md.
