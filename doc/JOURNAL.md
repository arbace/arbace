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
