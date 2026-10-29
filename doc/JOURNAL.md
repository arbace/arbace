# Journal

An append-only log of important decisions and actions, oldest first. Never edit past entries; add
a new entry to correct or supersede one. The goal is that the progress could be recreated from this
log.

This journal starts over after the milestone `arbace-reaps-3-crops-v1`, by the user's decision (the
goal of 2026-10-24). It started over the same way at the two freezes and after the milestones
`arbace-inside-itself-v1` and `arbace-interprets-go1.27.1-v1`. The history before it is on the frozen branches, at two tags, and in
git history:

- 2026-10-06 to 2026-10-08 (the class forms, j2c, the vendored and self-hosting Clojure, the work
  for Java 26, the g2c survey): on
  [`arbace-for-java-26`](https://github.com/arbace/arbace/blob/arbace-for-java-26/doc/JOURNAL.md),
  tag `arbace-for-java-26-v1`;
- 2026-10-08 to 2026-10-11 (milestone B1a: g2c, c2g, jrt, the evaluator, the Go executable and
  its feature completion, the freeze of the Go build): on
  [`arbace-for-golang`](https://github.com/arbace/arbace/blob/arbace-for-golang/doc/JOURNAL.md),
  tag `arbace-for-go1.27.1-v1`;
- 2026-10-11 to 2026-10-15 (the `.ae` sources, c2a, B1b the box and its milestones
  `arbace-box-amd64-v1`, `arbace-box-arm64-v1`; the standalone Arbace S1-S8 and the milestones
  `arbace-seed-is-go-v1`, `arbace-inside-itself-v1`): at the tag
  [`arbace-inside-itself-v1`](https://github.com/arbace/arbace/blob/arbace-inside-itself-v1/doc/JOURNAL.md)
  (main's `e0ad476`);
- 2026-10-15 to 2026-10-20 (S9, Go's toolchain as forms; the docs' audit; the build's speed; the
  renames to repr/java, repr/go, repr/clojure, c2j and j2g; Futamura's F2-F7; repr/java print;
  repr/haskell R1-R6 and HR25; repr/rust RR1-RR6; goi GR0-GR10, the arm64 box check, GC81, and
  the milestone `arbace-interprets-go1.27.1-v1`): at the tag
  [`arbace-interprets-go1.27.1-v1`](https://github.com/arbace/arbace/blob/arbace-interprets-go1.27.1-v1/doc/JOURNAL.md)
  (main's `965cbed3`).
- 2026-10-20 to 2026-10-27 (the Go parser GP1-GP8 and its surveys; the Java front end's native parser,
  converter and attribution JN8-JN9d; Java's conformance check JC1-JC6b; goi after v1; the REAP survey and
  REAP1-REAP9, the three crops' matrix; three batched re-seeds; the parked sub-projects; the milestone
  `arbace-reaps-3-crops-v1`): at the tag
  [`arbace-reaps-3-crops-v1`](https://github.com/arbace/arbace/blob/arbace-reaps-3-crops-v1/doc/JOURNAL.md)
  (main's `83538e0b`).

## 2026-10-27: the journal starts over

- **The user's decision**, the goal of 2026-10-24 ("reach arbace-reaps-3-crops-v1 milestone, then reset the
  journal"): after the milestone `arbace-reaps-3-crops-v1` (a tag on main, not a freeze, so no branch) this
  journal starts over.
  - The earlier one, 2201 lines and 101 entries from 2026-10-20 to the tag's entry, is at the tag (linked
    above).
  - CLAUDE.md's Records paragraph, the agenda's header and README's documentation list name the five places
    of the history.
- **Where things stand** (doc/AGENDA.md): the REAP matrix at the tag, 12 of 12 cells checked, none failing,
  every non-pass in a recorded class (doc/FULL-SUPPORT-SURVEY.md, "REAP v1's divergences"); the seed
  `b4ac198d`; parked (the user, 2026-10-26): the box, repr/haskell, repr/rust, the macOS UI, the iOS app. Next,
  in the user's order: the scripts' sweep of `/dev/null` and process substitution; the milestone
  `arbace-reaps-3-crops-v2` (the efficient gate, REAP's small and medium leftovers); then C23 at parity with
  the three crops (prior art: go-whim's C front end). Subagents on Sonnet for as much as possible, Opus for the
  hard parts (the user, 2026-10-25/26). The user asked to settle after this entry, for a restart of the
  harness.

## 2026-10-27: the scripts' sweep started; then `arbace-reaps-3-crops-v2` (the user)

- The user: do the scripts' sweep, then start `arbace-reaps-3-crops-v2`. The sweep (decided 2026-10-24):
  `/dev/null`, `/dev/stdin`, `/dev/stdout`, `/dev/fd`, `/dev/zero` and process substitution out of the
  committed scripts, one commit per script, each script's check rerun. Scope: the scripts of the three crops
  and the build (what the gate runs), 29 files, about 90 uses, in three batches on Sonnet in parallel
  (`sweep-build`: arbace-go, seed and bin/lib's build helpers, alpine-env, x509roots-vendor; `sweep-java`:
  java-check, javac-vendor, jrt-convert, jrt, j2g-edge, repr-java and its lib; `sweep-go`: repr-go's lib,
  generated-check, diffcheck, clojure-tests, repr-clojure's lib). Left as they are, with the parked
  sub-projects (the user, 2026-10-26), to be swept when each is revisited: the box's scripts (`box-*`,
  `tamago-vendor`, `box-vendor`, `net-check`, `bin/lib/{store,tls}-checks.bash`), `repr-haskell` and
  `repr-rust` with their lib files (their checks need KVM or the host's GHC and rustc); `tools/rsdump/rsdump.rs`
  is Rust source.
- Corrected (the user, the same day): the parked sub-projects' scripts are swept too, a fourth batch
  (`sweep-parked`, on Sonnet): the box's 11 files and repr/haskell's and repr/rust's 5, checked where the
  host can run them (KVM tests, the box's smoke test and suite, `bin/tamago-vendor verify`, `bin/repr-haskell
  test`, `bin/repr-rust test`), the rest by syntax (the box's rebuild, the arm64 VM, the Mac bundle).
  `tools/rsdump/rsdump.rs`'s `<(` is Rust's syntax, not a process substitution.

## 2026-10-27: the scripts' sweep done; `arbace-reaps-3-crops-v2` started

- The scripts' sweep (four branches on Sonnet, merged): no `/dev/null`, `/dev/zero`, `/dev/stdin`,
  `/dev/stdout`, `/dev/fd` or process substitution left in `bin/` or the Makefile; 45 files, one commit each:
  `sweep-build` (12: arbace-go, seed, seed-go, host, tools, frozen, version, gocache, fresh, build-go/test,
  alpine-env, x509roots-vendor), `sweep-java` (10: java-check, javac-vendor, jrt-convert, jrt, j2g-edge,
  repr-java, its front-check, parse-check, roundtrip, javac-key), `sweep-go` (7: repr-go's check,
  native-corpus, plan-check, generated-check, diffcheck, clojure-tests, repr-clojure's check), `sweep-parked`
  (16: the box's eleven, repr-haskell and its test, repr-rust and its code-unit and run-program). The forms: a
  discarded stream to a scratch file under TMPDIR or the run's work directory, a missing file tested with
  `[ -f ]`, a `ls | head` a glob loop, `< /dev/null` an empty file, `head -c N /dev/zero` `printf '%*s' N '' |
  tr ' ' '\0'`, a `<(cmd)` a file written first (a `done < <(plan)` loop: a plan command's failure now an
  ordinary error), an awk `> "/dev/stderr"` a pipe to `cat 1>&2`. `/dev/kvm` kept (a device). Checks: each
  script's own (seed --check and --build, x509roots verify, alpine-env check; java-check essential and
  essential-compiled, j2g-edge verify, repr-java check, parse-check, front-check and roundtrip on the samples,
  jrt overlay --check; repr-go native-check --headers, plan-check, generated-check, repr-clojure check; the
  box's KVM tests, purego-check, tamago-vendor verify, the box built with its smoke test (112 ok) and two
  suite namespaces, repr-haskell test and a pretty round trip, repr-rust test and a UI program at the tree,
  behaviour and code levels); syntax only for box-arm64-vm, box-convert, box-rebuild, box-mac-bundle and
  net-check. jrt-convert and javac-vendor verify matched every forms file, their provenance differing only in
  the borrowed tool's key (REAP9's full gate passed both on a fresh tool). The essential gate on `938d67ce`
  passed (12m44s). The agents' slips: a `/dev/null` and a `<(...)` in throwaway wait loops, one stray
  `box-suite` run outside heavy (ended at once).
- `arbace-reaps-3-crops-v2` started (the user): first wave, the gate's survey (on Opus), JN9e (on Opus), JC4b
  (on Opus), REAP's small leftovers A (RPS32, RPS102 (b), RPS95 (b), JCS76; on Sonnet); a second wave after:
  Clojure's last deferred cases, RPS64 per kind, RPS86 (b), RPS80 (b), RPS81 (b), RPS97 (b) after the gate's
  design.

## 2026-10-27: the gate's survey merged; GTS1-GTS20 decided; GT1-GT3 started (the user)

- The gate's survey (branch `v2-gate-survey`, merged; on Opus): doc/GATE-SURVEY.md. Measured from REAP9's four
  full gates: the serial `--full` 227 min, the java chain 48% of it (`java-check-full` 71.7 min, its compiled
  level 45.2 in 7 shards), `repr-go-native` 51.0 (the tamago `cmd` and box corpora 31 of it), the fixpoint
  20.8; the concurrent chains no faster (each step 1.3-2.3 times slower, the java chain the critical path);
  nothing of `--full` ever skipped (94 of 185 commits since 2026-10-20 docs only); the program built three
  times from the same sources; `gate_key` wider than the build reads. Estimates after the plan (quiet host): a
  docs commit about 1 min, a classes file 2, repr/java 50-55, j2g 75-120; a tag's clean run about 280-290 min.
  Plan GT1-GT7, 8-12 days. Decided (the user): GTS1-GTS20 as recommended (GTS4 (b)/(c) in GT5, GTS13 (b) in GT6).
- Started: GT1-GT3 (the measure, the keyed steps and the planner, the shared builds; on Opus); GT4 and GT6
  after; GT5 after JC4b and JN9e merge; GT7 at the tag.

## 2026-10-27: REAP's small leftovers A merged

- Small leftovers A (branch `v2-small-a`, merged `7ff11474`; on Sonnet): RPS32 (a) (repr/go's printer,
  `arbace/repr/go/print_code.ae`, prints as written `make`/`new` without a type or with more arguments, a spread
  inside them, a method without or with several receivers: ddd1, makenew and bug217 byte for byte; their AOT
  results pass; `:printer-invalid` and their known entries gone: Go's AOT cell 1,688 / 149 / 0, no deferred
  case left in Go's row), RPS102 (b) (`--build-program`: a Go package goi's checker refuses for the program's
  table a build error; the interpreted fallback removed; `test/programs-fail/bad-go` expects the failure; the
  gate's `programs` step `mixed bad-go`), RPS95 (b) (`bin/seed --build` has stage 0 compile the checkout's
  namespaces into `target/seed/aot` with `aot.key` and `aot.runtime` when its runtime is the checkout's,
  `--no-aot` not, the gate's stage 0 `--no-aot`; `bin/arbace-go --compile-aot HOST OUT`; `program_base` takes
  stage 0: a fresh `target/` built every sample, 11 of 11), JCS76 (a) (`aot.runtime` beside `aot.key`, read by
  `bin/java-check`'s `aot_fresh`). Shipped code: none but the tool's printer (run from the checkout). The
  essential gate on `7ff11474` passed (31m03s); the matrix 12 of 12, 0 problems.

## 2026-10-27: JN9e and JC4b merged; JFS120-JFS129 and JCS110-JCS119 decided (the user)

- JN9e (branch `java-jn9e`, merged `d52c5509`; on Opus): repr/java reads Java without javac. `bin/repr-java read
  --no-javac` (and `read-jdk`) parses natively and converts with the native provider's answers alone; whatever
  would ask javac stops the unit and its file is read by javac (the fallback, counted by reason,
  `OUT/repr-java-nojavac.edn`). `bin/repr-java nojavac-check --corpus all`: 14,650 files, every one native, no
  fallback, the forms javac's byte for byte (2,315 s; peak 8.4 GB against javac's read's 38-40 GB on java.base).
  attr-check `--corpus all`: 14,503,922 facts equal, none different, none declined (JN9d.10: 171), no bridges
  (jdk26u's 13,053 before). JFS110 (b) (the declines ported: inference cycles, multi-catch lub, record components'
  annotations, ...), JFS112 (b), JFS113 (c), JFS117 (a), JFS118 (b) (`doc/classes/NATIVE-ATTR.md`). javac's tool
  changed (repr/java embedded); the shipped program did not. Survey: doc/classes/JAVA-FRONTEND-SURVEY.md,
  "As built: JN9e"; scratch `.tmp/jn9e/`. Decided (the user): JFS120-JFS129 as recommended: JN9f (the vendoring
  tools' read `--no-javac`, nojavac-check in `--full`, a jtreg read level without javac, the bridges emptied, then
  repr/java embedded in the shipped program, in v2 with its re-seed).
- JC4b (branch `java-check-jc4b`, merged `359cfe9b`; on Opus): the Java back ends' own limits (JCS98 (a), JCS109
  (a), JCS62's compiled half): 42 more world classes extendable by the interpreter (`arbace.j2g.model/class-supers`),
  their protected members, superinterfaces with default methods initialized (JLS 12.4.2), `Outer.super.m()` in
  j2g, constant labels over a box, the most specific default method, synchronized methods' monitors, a member
  class shadowing a local one, record patterns never matching null, bridges for reordered bounds; the JLS program
  `test/java/jls/BackEnds.java`. Java's interp 1,683 → 1,790, compiled 1,869 → 1,881, none lost (6 timeouts on
  the loaded host recorded `:flaky`). The executable +17.6 MB (174,010,528 bytes). Shipped files changed (for the
  re-seed): arbace/classes/{analyze,env,go,interp}.ae, arbace/j2g/{model,dyn,dyncf,code,decls,reach}.ae,
  arbace/lang/Compiler.ae. Survey: doc/classes/JAVA-CONFORMANCE-SURVEY.md, "As built: JC4b"; scratch
  `.tmp/jc4b/`. Decided (the user): JCS110-JCS119 as recommended: JC4c in v2 (JCS111-JCS113, JCS116-JCS118),
  JCS114/JCS115 in JN9f, the size kept and a shared dispatch later.
- The essential gate on `b60d3075` (both merged) passed (23m31s); the matrix 12 of 12, 0 problems: Java 7,739/3/93,
  1,790/329/1,202, 1,881/181/1,259, 2,584/637/99.
- The host's memory: JC4b's runs and GT1-GT3's `--full` overlapped (j2g's 17-20 GB beside each other), against
  the one-20-GiB-job rule. Since: a host lock `.tmp/big.lock` (flock) every job above about 8 GB holds, the agents'
  rules in `.tmp/agent-rules.md`; GT6 makes it the gate's own. GT1-GT3's first `--full` failed (276m31s:
  `jrt-convert`, `java-check-full`), its agent on it.
- Started beside GT1-GT3 (worktrees from `b60d3075`): JC4c and JN9f (on Opus), Clojure's last deferred cases
  (RPS41 (b), RPS42, RPS53, RPS54) and small leftovers B (RPS64 per kind, RPS86 (b), RPS80 (b), RPS81 (b)) on
  Sonnet.

## 2026-10-28: GT1-GT3 merged (the keyed gate); GTS21-GTS29 decided (the user)

- GT1-GT3 (branch `gate-gt1-3`, merged `b518a8f8`; on Opus; doc/GATE-SURVEY.md, "As built: GT1-GT3"): the gate's
  37 steps as data (`test/gate/steps.edn`: command, needs, declared inputs, hosts, pins, environment), the planner
  `test/gate/plan.ae` on stage 0 keying each check over them (never the commit) and skipping one passed at its key
  in the shared store (`ARBACE_GATE_STORE`, main's `.tmp/gate-pass/`); `--plan`, `--why STEP`, `--rerun`; each step
  measured (busybox `time -v`, a 5 s sampler, `.tmp/gate/NAME.mem`, `mem.tsv`); `gate_key` narrowed (repr/java,
  javac's world and the provenance files out); the fixpoint's G1 the gate's build (12:37, was 20:45), javac's tool
  once per `arbace_javac_key` (`bin/javac-vendor tool`, `check --from`), stage 0 kept by its pins; `--full` serial by
  default (`GATE_FULL_PARALLEL=1`). Runs: `--full` 1 failed (276m31s: `jrt-convert` killed for memory beside another
  agent's job; `java-check-full` on main's stale `:repr-java` provenance hashes, stale since the sweep's edit of
  `bin/repr-java`, and a runtime-key bug, `jdk/provenance.edn` now out of both keys); `--full` 2 passed (75m14s, 9
  run, 28 skipped); `--full` 3, nothing changed: 4 s. The essential gate on merged main: 24 s (3 run, 13 skipped).
  Heaviest steps (peak GiB): attr-check 40.1, java-check-full 35.0, front-check 28.8, roundtrip 28.1, repr-go-native
  27.7, diffcheck 26.3. Scratch: `.tmp/gt1-3/`. Decided (the user): GTS21-GTS29 as recommended, into GT4-GT6.

## 2026-10-28: Clojure's last deferred cases and small leftovers B merged; RPS130-RPS134, RPS150-RPS154 decided (the user)

- Clojure's deferred cases (branch `v2-clojure-deferred`; on Sonnet; doc/FULL-SUPPORT-SURVEY.md, "As built: Clojure's
  deferred cases"): RPS41 (b) (a compiled fn's class reflects its code object's closed-over fields, jrt's
  `AOTFnFields`), RPS42 (b) (c2j's field assignments and unboxed returns on their own lines), RPS53 (b) (the
  evaluator's field nodes and `invoke` set the frame's line, the template directive `(at-line line x)`), RPS54 (b)
  (`gen-interface` registers a `reify` adapter with `FISupport` for a `^FunctionalInterface`). The options (b) of
  RPS42, RPS53 and RPS54 taken from their recommendations ("(b) after the milestone"). Eval 20,566 → 20,576
  assertions, AOT 20,549 → 20,576; `clearing` at AOT 12 → 31 of 31; Clojure's deferred: eval 5 → 2, AOT 6 → 1.
  Shipped files (for the re-seed): arbace/lang/{Compiler,Reflector}.ae, arbace/genclass.ae,
  arbace/lang/go/CompilerNodes.ae, arbace/c2j/{emit,node}.ae, arbace/j2g/dyn.ae, go/arbace/jrt/class.ae,
  jdk/edge.edn (`Compiler$FISupport` public).
- Small leftovers B (branch `v2-small-b`; on Sonnet; "As built: v2 small leftovers B"): RPS64 per kind and RPS86 (b)
  in repr/java's printer (`arbace/repr/java/print.ae`: widened constants as the integer's literal, negated
  non-decimal literals, a static field through an expression qualified, `a[e] = a[e] + 1` written out, a `&&`
  chain with a pattern as one condition, ...; `:print-forms` split into `:print-shapes` and `:print-erasure` by
  design and 5 deferred); RPS80 (b) (`bin/jrt modules` generates `go/arbace/jrt/module_table.ae`, `Class.getModule`
  by package first); RPS81 (b) stopped at a design (RPS150: needs j2g support forms, JC4c's files). Java's print
  2,584 → 2,597, its deferred 99 → 41. Shipped: go/arbace/jrt/{module,module_table}.ae, go/arbace/jrt.ae.
- The essential gate on both merges passed (21m34s, 17 run; the gate's processes at most 43.4 GiB); the matrix 12 of
  12, 0 problems: Clojure 908/2/0, 21,052/80/2, 809/28/1, 1,031+/0/0; Java print 2,597/683/41.
- Decided (the user): RPS130-RPS134 and RPS150-RPS154 as recommended (RPS134 (b): `xml-stream` by design; RPS150
  after JC4c, in v2; RPS151, the converter's widening, in JN9f).
- JN9f filled the disk (100%) and lost its context; I trimmed Go's cache (14 GB → less) and sent it the remaining
  steps. A slip of mine: I committed `ee8ee304` (docs only) in main while its gate ran there.

## 2026-10-28: JC4c merged; JCS120-JCS123 decided; the C23 survey merged, CS1-CS24 decided

- JC4c (branch `java-jc4c`; on Opus; doc/classes/JAVA-CONFORMANCE-SURVEY.md, "As built: JC4c"): JC4b's follow-ups,
  JCS111-JCS113, JCS116-JCS118: jrt's hand-written leaves with class interfaces, protected statics, the capture
  chain through a prologue's class, lambda bridges, a program's own members in reflection (`names/own-class?`,
  diffcheck's Java forms in their `jf_` packages too), j2g's findings. Java interp 1,790 → 1,804 (deferred 1,202 →
  1,189), compiled 1,881 → 1,910 (deferred 1,259 → 1,232), none lost; the executable 177,287,328 bytes. Shipped
  files for v2's re-seed: those the section lists, `arbace/j2g/names.ae` among them. The essential gate on merged
  main passed (22m34s, 17 run; the gate's processes at most 44.8 GiB): the matrix 12 of 12, 0 problems; Java
  7,740/3/93, 1,804/329/1,189, 1,910/180/1,232, 2,597/683/41. Scratch: `.tmp/jc4c/`.
- Decided (the user): JCS120-JCS123 as recommended: JCS120 (b) after v2 (JCS110 (c)'s shared dispatch first),
  JCS121 (c) (V1 kept), JCS122 (b) (left), JCS123 (b) (j2g's two reflection fixes, with RPS150, before v2's
  re-seed).
- The C23 survey (branch `c23-survey`; on Opus; doc/C23-SURVEY.md, doc only): C forms, repr/c, ci and AOT at
  parity with the three crops, prior art go-whim (`../go-whim` at `80c49bf5`: `crefactor/cc`, modernc cc v4.29.7
  with a C23 delta; cemit; C-lisp; c23conf), musl, gcc 15.2, ccorpus2; the plan C1-C9, proposals CS1-CS24.
  Scratch: `.tmp/c23/`.
- Decided (the user): CS4 (b), not the recommended (a): cc ported to Arbace code rather than vendored as Go
  forms, as repr/java's native parser was ported from javac (JN8), cc kept as the oracle; the survey's (a) was
  vendoring through goi's table (GP1 → GP7), (c) a new parser, (d) clang's JSON as a helper. CS22 left open until
  C1 (the port is cc's derivative under BSD-3; go-whim's own parts need the user's statement when C1 brings them
  in). The rest as recommended: CS3 (a) (C23, the earlier standards, the GNU extensions musl and the corpora
  need), CS9 (a) (flat 64-bit address space), CS11 (a), CS12 (a) (musl 1.2.6's release tarball, pinned), CS14 (a)
  (gcc 15.2 `-O0` the judge), CS15 (a) (gcc 15.2's tarball fetched, pinned), CS2 (a) (`arbace-reaps-4-crops-v1`),
  CS5 (a), CS17 (a) (`arbace.repr.c`, ci, `crt`, cf2g), and CS1, CS6-CS8, CS10, CS13, CS16, CS18-CS21, CS23,
  CS24 (a). C23 starts after v2's tag.
- Running: JN9f (Opus), GT4 with GT6 (Sonnet), RPS97 (b) (Sonnet).

## 2026-10-28: JN9f merged; JFS130-JFS134 decided; C23's C1 merged, CS30-CS43 decided

- JN9f (branch `java-jn9f`; on Opus; doc/classes/JAVA-FRONTEND-SURVEY.md, "As built: JN9f"): JCS114 (a) and JCS115 (a)
  (a record component's type pattern unconditional by the record pattern's type arguments, its binding cast as
  javac's checkcast: the interpreter's `Pat` in `arbace/lang/Compiler.ae` gains `checked`, `arbace/classes/analyze.ae`
  marks `:cast`; a primitive for-each over another primitive's box); RPS151 (a) (the converter, `arbace/repr/java/convert.ae`'s
  `coerce`, writes a `long` constant widened to float/double, boxed into a non-`Long` reference, and a primitive
  conditional with a literal branch boxed whole; `:print-forms` removed; jdk/forms 3 of 982, jdk/javac/forms 3 of 903,
  `test/repr/java/expected/jdk26u.sha256` 30 of 13,566 regenerated, no value changed; javac's tool key `74dfa6c26ac6`);
  JFS121 (b) (`repr-java-nojavac` in `--full`'s java chain, `:mem-gib` 11, measured 10.8 GB on the jdk corpus; every
  corpus equal, 0 fallbacks); `bin/java-check --forms update` reports a failed read; JFS126 (b) designed (JN9f.8).
  `:record-pattern-findings`, `:boxed-foreach` removed from `test/java/divergences.edn`. The merge's conflicts
  (`test/java/divergences.edn`: each side had removed other classes, JC4c's `:lambda-bridges`, `:j2g-coercion`,
  `:j2g-findings`, JN9f's two; JAVA-CONFORMANCE-SURVEY.md's two appended sections) resolved by keeping both sides'
  removals and both sections. Shipped files for v2's re-seed: arbace/lang/Compiler.ae, arbace/classes/{analyze,interp}.ae,
  jdk/forms (3 files). The essential gate on merged main passed (22m28s, 17 run, at most 46.9 GiB); the matrix 12 of 12, 0
  problems: Java 7,740/3/93, 1,807/329/1,186, 1,913/180/1,229, 2,598/687/36. CLAUDE.md names the nojavac-check in
  `--full`. Scratch: `.tmp/jn9f/`.
- Decided (the user): JFS130-JFS134 as recommended; JFS133 (a)'s 6 differences started beside v2 (branch `java-jfs133`,
  on Sonnet, no big lock but its gate), JFS130-JFS132 as JN10 after v2.
- C23's C1 (branch `c23-c1`; on Opus; doc/C23-SURVEY.md, "C1: the plan after CS4 (b)"): `doc/c/SPEC.md` (C forms for
  C23 and the admitted GNU extensions; its 23 forms blocks read by the Clojure-compatible reader; six worked examples
  built by gcc 15.2), `test/c/corpora.edn` (21 corpora, 6,388 files, pinned: gcc-15.2.0.tar.xz SHA-256
  `438fd996826b0c82485a29da03a72d71d6e3541a83ec702df4271f6fe025d24e`, its SHA-512 against gcc.gnu.org's `sha512.sum`;
  musl-1.2.6.tar.gz `d585fd3b613c66151fc3249e8ed44f77020cb5e6c1e635a616d3f9f82460512a`, its SHA-512 against Alpine
  aports' APKBUILD 1.2.6-r6; the signatures fetched, not verified: no gpg on the host; ccorpus2 v1.6.0 by go.sum's
  `h1:`; go-whim at `80c49bf53cd6340487ac623f3d6d135b77ac1db6` by tree hashes). Findings: gcc's C23 tests are `c23-*`
  (no `c2x-*` since gcc 14); musl 1.2.6's headers lack C23's `nullptr_t`, `unreachable`, `<stdbit.h>`; `/root/musl` is
  not the release (34 files differ). The oracle: `tools/ccdump`, Arbace's own helper importing go-whim's cc through a
  generated `go.work`, not vendored. Plan after CS4 (b): 33-56 working days. The C1 merge's gate: 3 s (3 run, 13 skipped).
  Nothing of go-whim, musl or gcc in the tree. Scratch: `.tmp/c1/`.
- Decided (the user): CS37 (a), go-whim's own C parts (the C23 delta, cemit, clisp, c23conf, togo's rules if used)
  licensed to Arbace under the EPL-1.0, as D8 for the box (CS22 settled; cc and its dependencies keep BSD-3; LICENSE.md
  entries when the first of them enters the tree); CS35 not as recommended: `long double` is `double` in eval and AOT (no
  software x87), the runs it changes in a class `long-double`, C5 8-14 days (the recommended (a) was a software
  80-bit `long double`, C5 9-16); the rest (CS30-CS34, CS36, CS38-CS43) as recommended.
- The big-job lock: GT4/GT6's audit held it for one block of every remaining step (2 h 14 min, under strace),
  queuing main's gate and two agents; on my request it stopped the block, but its children kept the lock's fd until
  killed by PID; the audits now take the lock one step per hold.

## 2026-10-29: GT4 and GT6, JFS133 (a), RPS150 (b) with JCS123 (b) merged; the provenance files rewritten; a deadlock between the two host locks

- The gate's GT4 and GT6 (branch `gate-gt4-6`, merge 980d457d; doc/GATE-SURVEY.md, "As built: GT4, GT6"): the audit under
  strace, the canary, the runner library `bin/lib/gate-run.bash`, the heavy lock taken per `:heavy` step by the gate itself
  (`GATE_HEAVY_CMD`), measured peaks, CPU-time limits (`bin/lib/limit.bash`), `--prune`, the shared executables
  `ARBACE_GATE_EXES`, the essential step `provenance` (`bin/provenance-check`). Proposals GTS30-GTS37, which the user
  decided as recommended. GT5's branch records them in doc/GATE-SURVEY.md, "Decided (2026-10-28): GTS30-GTS37".
  The bin/java-check merge conflict was resolved by keeping both sides' functions in its `export -f` line.
  Scratch: `.tmp/gt4-6/`.
- JFS133 (a) (branch `java-jfs133`, merge 8d5e06d3, on Sonnet): the six differences of the read without javac, fixed.
  The nread count went from 3,276/38/6 to 3,282/39/0. Scratch: `.tmp/jfs133/`.
- RPS150 (b) and JCS123 (b) (branch `v2-rps150`, merge 8851006d): Module's getResourceAsStream and getPackages are now j2g
  support forms; an own interface's getMethods returns its methods in declaration order; an enum constant's empty body
  is read as a body. `:j2g-reflection` is by design. Shipped files for the re-seed: arbace/j2g/{out,main}.ae,
  arbace/classes/parse.ae. The conflict in FULL-SUPPORT-SURVEY.md was resolved by placing "As built: RPS150 and JCS123
  (b)" before the v2 record's draft. Scratch: `.tmp/rps150/`.
- Main's first gate on the three merges failed only at `provenance`. JFS133 had edited repr/java, so the four
  provenance files' reader hash was stale (5ec458cd against the reader's 0832ab00), and JC4c's `bin/j2g-edge update` had
  left `jdk/javac/provenance.edn`'s `:jdk-edge` stale (625ff84a against babddce8). This is the slip GTS36 (b) removes.
  Branch `v2-prov` (Sonnet, 25f3ee1c, merge d819e179) ran the writers' updates: a new javac tool (the old one's key
  predated JFS133), `bin/javac-vendor convert`, `bin/jrt-convert update`, `bin/clojure-tests --java-fixtures update`,
  `bin/java-check --forms update`. Only the four provenance files changed; no forms did. Scratch: `.tmp/prov/`.
- The essential gate on merged main d819e179 passed: 35m12s, 18 run, at most 25.2 GiB. The matrix is 12 of 12 cells with 0
  problems. Java AOT is 1,915/180/1,227; the other cells are as on 2026-10-28.
- A deadlock, 00:09 to 01:28. The RPS97 agent's branch predates GT6, and it ran `flock big.lock .tmp/heavy CMD` as the
  agent rules then said. That held big.lock while waiting for one of `.tmp/heavy`'s two slots. Both slots were held by
  the gates of `gate-gt5` and `v2-prov`, and their heavy steps were waiting for big.lock. I found the holders through
  /proc/*/fd and killed the two waiting commands by PID. The rule is now slot first, lock second (`.tmp/heavy flock
  big.lock CMD`); a GT6 gate does this by itself.

## 2026-10-29: C23's C2 merged; CS50-CS51 decided (the user)

- C23's C2 (branch `c23-c2`, on Opus, commits 943d01ce, d6a5a978, fea742ec; doc/C23-SURVEY.md, "As built: C2"): repr/c's
  lexer and preprocessor (`arbace.repr.c.lex`, `arbace.repr.c.pp`), ports of cc's, checked token for token against
  `tools/ccdump` (`bin/repr-c check --tokens`; `--judge-all` against gcc -E). repr/c is kept out of the embedded tree
  until C5 (CS36 (a), `arbace/j2g/embed.ae`). The ported lexer also reads digit separators in `#line`.
- Merge conflicts with GT4/GT6: `bin/arbace-go`'s `gate_key` and steps.edn's `:tools` take both sides' exclusions.
  GT4's removal of `arbace/lang/go/ns/` from `:tools` stays. GT4's planner then refused `!arbace/repr/c.ae`, which names
  no file until C5, so only `!arbace/repr/c/` is kept (96fc2195).
- The essential gate on merged main passed: 25m57s, 18 run, at most 23.8 GiB.
- Decided (the user): CS50 (a), the port follows gcc in C3 (trigraphs, UCNs in identifiers); CS51 (a), gcc -E's digest
  of every file recorded as data. Scratch: `.tmp/c2/`, `.tmp/repr-c-check/`.

## 2026-10-29: RPS97 (b) and RPS161 (b) merged; RPS160-RPS162 decided (the user)

- RPS97 (b) and RPS161 (b) (branch `v2-rps97`, on Sonnet; doc/FULL-SUPPORT-SURVEY.md, "As built: RPS97 (b)", "As built:
  RPS161 (b)").
  - `bin/lib/program.bash` is now the shared library for building programs.
  - `program_base` looks for a base in this order: `ARBACE_PROGRAM_BASE`, `target/arbace-go`, the gate's cache, stage 0's
    `target/seed`, and finally a `--build`. `bin/java-check`'s compiled level and `--essential-compiled` take their base
    from it; `JAVA_CHECK_AOT` still overrides.
  - `bin/clojure-tests --aot` keeps the gate's executables and stops unless their compiled namespaces are current.
- Checks:
  - `--essential-compiled`: 423 of 423.
  - The suite's AOT level: 20,576 of 20,580 assertions, no regressions.
  - The full compiled level: as recorded, 1 newly passing. t05445 `HugeToString` timed out under host load and
    passed alone. It is in the record's flaky-timeout class.
  - `--build-program --check`: 11 of 11.
- No shipped file changed.
- The branch's gate after merging 9c89817d passed (27m38s). Main's gate on the merge passed from the store
  (6 s, 4 run).
- Decided (the user): RPS160 (a), RPS161 (b) inside v2, RPS162 (a) as built. Scratch: `.tmp/rps97/`.

## 2026-10-29: REAP v2's re-seed

- `bin/arbace-go --fixpoint` on b6beefde. The fixpoint holds: G1 = G2 = G3 = a6a95bcb… (177,778,848 bytes), and the old
  stage 0 (45df0046…) differs from G1 as expected.
- G3 was packed by `ARBACE_SEED_GO_BUILD=target/arbace-go/fixpoint/g3 bin/seed --make` and committed as d173baf6. The
  new seed is 19,004,276 bytes, 2,102 files, SHA-256 `ad1aeae893ef460937c7825a713497ab365ea95c47ccc2aeeb97315dbc00a949`.
- It carries the shipped changes since the milestone v1's re-seed: JC4b and JC4c (names.ae among them), Clojure's
  deferred cases, small-B's jrt module and module_table, JN9f (Compiler.ae, analyze.ae, interp.ae, jdk/forms), RPS150
  (arbace/j2g/{out,main}.ae, arbace/classes/parse.ae) and C2's embedding list (arbace/j2g/embed.ae).
- `bin/seed --verify` passed in 1,351 s: printed again at b6beefde, the same tar byte for byte (8207e64d…) and the same
  archive, and stage 0 = G1 = G2 = G3.
- The essential gate on d173baf6 passed: 26m49s, 18 run, at most 24.7 GiB.
- GT5 changes a shipped file (arbace/lang/RT.ae, the loaded-namespaces log), so v2 re-seeds once more after GT5's merge.
  Scratch: `.tmp/reseed/`.

## 2026-10-29: GT5 merged; GTS40-GTS42 decided (the user); C23's C3 started

- GT5 (branch `gate-gt5`, on Opus; doc/GATE-SURVEY.md, "As built: GT5"):
  - **Parts:** `:parts` gives one check per corpus.
  - **Outputs and early cutoff:** `:outputs` keeps a step's output with its pass; `:uses` puts its producers' output
    digests in the consumer's key.
  - **Program hosts:** `:prog` and `:javac-prog` key a build by its units (j2g's Go forms by namespace,
    `bin/lib/prog-units.bash`), less a step's `:never-loads`.
  - **Java's check split per level:** `--save` runs the levels without judging and `--judge` only judges, so the
    judgment is a step of its own.
  - **The audit:** it logs the namespaces each step loads (`ARBACE_LOADED_LOG` in `RT.load`), and its reducer is
    `tools/gateaudit`.
  - **GTS31 (b):** the tools' own lock, `bin/lib/heavy.bash`.
  - **GTS32 (a):** the FIFO ticket lock, `bin/heavy-lock`.
  - **GTS34 (a):** `--prune` after every `--full`.
  - **GTS35 (a):** atomic caches.
  - **GTS36 (b):** `bin/j2g-edge update` rewrites `:jdk-edge`.
- GT5's demonstrations:
  - A classes file changed: only the judge and reap ran, 7 s.
  - A comment in arbace/j2g/out.ae: only the `arbace.j2g.out` unit moved, and the gate took 14m57s against 56m56s with
    every check run.
- The merge was clean. Its only shipped change is arbace/lang/RT.ae.
- Decided (the user): GTS40 (c), `go-build` and `fixpoint` are `:heavy true` again (50900e48); the per-process lock had
  queued a build's four heavy processes, about three hours. GTS41 (c) at GT7, (a) until then. GTS42 (a), at GT7.
- The essential gate on 50900e48 passed: 18m54s, 19 run, at most 45.0 GiB. Scratch: `.tmp/gt5/`.
- C23's C3 started beside v2: repr/c's parser, with CS50 (a) and CS51 (a). Branch `c23-c3`, on Opus, the user's choice.
  It changes no shipped file, stays light while `.tmp/gt7.quiet` exists, and merges after the tag.

## 2026-10-29: REAP v2's second re-seed; GT7's tooling started

- GT5 changed a shipped file, arbace/lang/RT.ae (the loaded-namespaces log), so v2 re-seeded a second time.
  - `bin/arbace-go --fixpoint` on c560b8ec: G1 = G2 = G3 = 9b117777… (177,778,848 bytes); the host, the first re-seed's
    stage 0 (a6a95bcb…), differs from G1.
  - The new seed (00ec2bf5) is 19,006,840 bytes, SHA-256
    `73b0d6372d651aa703931214600be44a8df5a285c3e140d85604528c4914cb40`.
  - `bin/seed --verify` passed in 1,412 s: the tar printed again at c560b8ec byte for byte (4aad6d0f…), the same archive,
    and stage 0 = G1 = G2 = G3.
  - The essential gate passed: 15m12s, 19 run, at most 36.5 GiB. Both re-seeds took the lock through `bin/heavy-lock`.
    Scratch: `.tmp/reseed2/`.
- GT7's tooling started: `bin/gate --clean` did not exist yet. Branch `gate-gt7`, on Opus, builds:
  - the clone with empty caches, every step audited, all parts (§7, GTS18 (a), GTS30 (a), GTS19 (a));
  - Java's levels with the forms cache forced (GTS41 (c));
  - the record `test/gate/clean-TAG.edn`;
  - GTS37 (a)'s concurrent `--full`;
  - GTS42 (a)'s demonstrations.
  It proves the tooling on a clean essential gate. The `--full --clean` run itself is main's, on a quiet host
  (`.tmp/gt7.quiet`).

## 2026-10-29: JCS120 (b)'s and JN10's plans merged, JCS124-JCS125 and JFS135-JFS138 decided; the v2 record's draft refreshed; t04863 classed

- Three light agents beside GT7's tooling (Sonnet, docs only, the user's choice):
  - **JCS120 (b)'s plan** (doc/classes/JAVA-CONFORMANCE-SURVEY.md, "JCS120 (b): the plan"): JCS110 (c)'s shared
    dispatch first, then the classes listed by the corpora's demand. About 14 `:interp-protected` candidates. 6-8 days.
  - **JN10's plan** (doc/classes/JAVA-FRONTEND-SURVEY.md, "JN10: the plan (JFS130-JFS132)"): the read closure apart
    from javac, the platform as an interface with a committed java.base pack, the shipped read and the REPL's
    `load-java`. 8 days, about 5.5 elapsed. The shipped program grows about 27 MB, so a re-seed is needed, batched
    with C23's.
  - **The v2 record's draft refreshed** (doc/FULL-SUPPORT-SURVEY.md): tables regenerated, the labels interp-clone,
    thread-order, flaky-timeout and the "moved between classes" notes checked test by test against v1's check files,
    and placeholders left for GT7. The refresh script moved to `.tmp/v2-record/`; the old one is kept in
    `.tmp/v2-record.old/`. Scratch: `.tmp/v2-record2/`.
- The record's finding: `:reflection`'s match took t04863 before the by-design `:j2g-reflection`. The user chose (b),
  the class moved first. The gate passed (1m33s; only the judge, reap and the canary ran: GT5's early cutoff).
  Java eval is now 1,807/330/1,185 and AOT 1,915/181/1,226.
- Decided (the user), all as recommended: JCS124 (b), JCS125 (a) with (d); JFS135 (a), JFS136 (a), JFS137 (b) for the
  gate and (a) at the tag, JFS138 (b) then (a).

## 2026-10-29: C23's C3 merged; the net/http survey; JCS124 measured; decisions (the user)

- **C23's C3** (branch `c23-c3`, on Opus; doc/C23-SURVEY.md, "As built: C3"), merged before the tag because it changes
  no shipped file:
  - `arbace/repr/c/parse.ae` ports cc's parser and go-whim's `c23.go`; `tree.ae` holds cc's 85 node types.
  - `parsecheck.ae` and `bin/repr-c parse-check`, in JN8.3's shape; `tools/ccdump`'s `tree` mode.
  - The parse check: of 6,387 files, 6,300 equal and 87 known; of 7,724 mutants, 7,679 equal and 45 known; none fail.
    Errors are compared in cc's order.
  - CS50 (a): trigraphs, UCNs, gcc's identifier spelling.
  - CS51 (a): `test/repr/c/pp-gcc.sha256`, gcc -E's digest of all 6,387 files. Judged by it: 6,349 agree, 31 both
    reject, 7 known.
  - The branch's gate passed at b2e3b763 (8m18s). Scratch: `.tmp/c3/`, `.tmp/repr-c-parse/`, `.tmp/repr-c-check/`.
- **The net/http survey** (doc/go/NET-HTTP-SURVEY.md, on Sonnet, the user's "consider completing our Golang
  implementation with net/http"):
  - net/http already imports interpreted (GR8a).
  - Listing it adds 19 packages, an estimated +4.6 to 5.4 MB. HTTP/2 is bundled in std.
  - Interpreted handlers go through GR9's stubs.
  - Plan NH0-NH5, 10-15 days. Scratch: `.tmp/nethttp/`.
- **JCS124 (b) measured** (doc/classes/JAVA-CONFORMANCE-SURVEY.md, "As measured: JCS124 (b)"): the shared dispatch's
  ceiling is 1.74 MB of 17.6, 9.9%, under the third, so it is not built. 97% of a DynSub type's 0.3 MB is Go's promoted
  wrappers of `dynCore`'s about 1,440 methods. Scratch: `.tmp/jcs124/`.
- **The docs sweep:** CLAUDE.md names RPS97 (b)'s `program_base` and RPS161 (b)'s base. Nothing else was stale.
- **Decided (the user):**
  - JCS126 (a): the cost accepted.
  - Java's coverage by module (java.base and the language at the run levels) is enough for now; JCS90-JCS96 are on
    hold.
  - NH1-NH10 as recommended.
  - CS60-CS63 as recommended.
  - During GT7's clean run, moderate work is allowed: no lock, ticket, slot or gate; at most 8 GB and 8 threads each;
    at most two agents. The clean run's wall times may run a little long, which its record will say.

## 2026-10-29: GT7's tooling merged; net/http's NH0 and NH1 merged; GTS50-GTS57 and NH11-NH14 decided (the user)

- **GT7's tooling** (branch `gate-gt7`, on Opus; doc/GATE-SURVEY.md, "As built: GT7 (tooling)"):
  - `bin/gate [--full] --clean [--tag TAG]`: a shared clone, empty caches, stage 0, the executables and the tool
    built fresh, serial, every step under strace, all parts, `JAVA_CHECK_FORMS_ONLY=1`, the CPU log; the record
    `test/gate/clean-TAG.edn`, and the passes published as `:clean`.
  - `--pins`, `--full --concurrent` (GTS37 (a)), `--plan --builds`, `bin/gate-demos` (GTS42 (a)).
  - Three clean essential runs. The first two found:
    - 2,160 files `go-build` reads that no key covered (`tamago/goroot/`, `tools/godump/`, goi's table's inputs; now in
      `gate_key` and the javac key);
    - `bin/heavy-lock` read by every `:inner` step;
    - `arbace.j2g.zlib` loaded by a goi step that declares it never loads j2g;
    - `$GOROOT/test`'s go-command programs on the host's go 1.27.2 (now TamaGo's on PATH);
    - `gcgort.go`'s CPU limit.

    The third passed, 86m06s, every audit ok, at most 25.8 GiB.
  - Main's gate on the merge passed (2m20s).
  - Scratch: `.tmp/gt7/`.
- **net/http's NH0 and NH1** (branch `nethttp-nh1`, on Sonnet; doc/go/NET-HTTP-SURVEY.md):
  - NH0, net/http interpreted on the shipped build: HTTP/2 over TLS works (60 concurrent streams); HTTP/1.1 fails on
    interpreted types' empty method sets and on exhausted stub slots. The import takes 9.5-11.6 s.
  - NH1: `test/nethttp/`, 60 offline programs, gc's runs as the reference, `bin/goi check --corpus nethttp`.
    gc passes 60; goi interpreted 10 pass, 49 fail, 1 error, in 4 deferred classes.
  - Main's gate on the merge passed (14m27s; `bin/goi` changed, so the executables were rebuilt).
  - Scratch: `.tmp/nh0/`, `.tmp/nh1/`, `.tmp/goi-check/nethttp/`.
- **Decided (the user):**
  - GTS50-GTS57 as recommended. GTS50, GTS52, GTS53 and GTS57 are built before the clean run (branch `gate-gts5x`,
    Sonnet).
  - NH11 (b), not as recommended: the empty method sets investigated first (branch `nethttp-nh11`, Opus).
  - NH12 (a), NH13 (b), NH14 (a).
