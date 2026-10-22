# Journal

An append-only log of important decisions and actions, oldest first. Never edit past entries; add
a new entry to correct or supersede one. The goal is that the progress could be recreated from this
log.

This journal starts over after the milestone `arbace-interprets-go1.27.1-v1`, by the user's
decision (2026-10-20). It started over the same way at the two freezes and after the milestone
`arbace-inside-itself-v1`. The history before it is on the frozen branches, at two tags, and in
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

## 2026-10-20: the journal starts over

- **The user's decision**, after the milestone `arbace-interprets-go1.27.1-v1` (a tag on main, not
  a freeze, so no branch): this journal starts over.
  - The earlier one, 2,204 lines and 95 entries from 2026-10-15 to the tag's entry, is at the tag
    (linked above).
  - CLAUDE.md's Records paragraph, the agenda's header and README's documentation list now name the
    four places of the history.
- **The user cleared the goal** of 2026-10-19 ("reach arbace-interprets-go-v1, that is the next
  milestone"): the milestone is reached and tagged.
- **Still running at the reset:** goi's check in the emulated arm64 box over all of
  `test/goi/box.txt` (`GOI_BOX_ALL=1`, 584 programs), in a detached worktree at `ab8a41dc`.
  - It was started while the Mac run was awaited; the tag's claim counted the half, 291 of 292.
  - Its result is recorded here when it ends.

## 2026-10-20: the go-lisp, ghc-lisp and rust-lisp checkouts removed; their patches kept

- **Why:** Arbace's surveys and specs cite the three forks as prior art for repr/go, repr/haskell
  and repr/rust (G2C-SURVEY, REPR-HASKELL-SURVEY, REPR-RUST-SURVEY, doc/go/SPEC.md,
  doc/haskell/SPEC.md, doc/rust/SPEC.md). No script, build or check of Arbace reads them.
- **Checked first:** each local checkout equalled its GitHub origin (every branch, a clean tree, no
  tags or stashes), checked with `git ls-remote` without writing to them.
- **The patches** are kept in a new private repository, github.com/arbace/forks (`a21d047`), by the
  user's choice. Each fork's own commits are there as `git format-patch BASE..HEAD`:
  - go-lisp: 27 patches on golang/go `f043823a`, head `2483a496`;
  - ghc-lisp: 25 on GHC's `c35096f5`, head `db7adf93`;
  - rust-lisp: 11 on rust-lang/rust `602727f2`, head `f28e0de8`.
  It has no merges or binary files, about 1.4 MB.
  - Each series applied to its base (with `git apply --cached` in a throwaway bare clone) gives
    the head's tree exactly.
  - Each base is a commit of its upstream (GitHub's and GHC's GitLab's APIs).
  - A fresh clone of arbace/forks equals the local files byte for byte.
  The repository's README says how to restore a fork and under which upstream license each
  series is.
  - Alternatives considered: a release asset on GitHub (rejected: it lives only on GitHub's
    servers, outside any clone) and the patches inside Arbace (rejected: three more upstream
    licenses for files Arbace does not use).
- **Removed:** `/root/go-lisp`, `/root/ghc-lisp` and `/root/rust-lisp` (about 3.6 GB), at the
  user's word. The forks themselves stay on GitHub (go-lisp and ghc-lisp public, rust-lisp
  private). The specs' citations keep their commits; their local paths now name what was there.

## 2026-10-20: the forks deleted from GitHub; their record is arbace/forks

- The user deleted github.com/arbace/go-lisp, arbace/ghc-lisp and arbace/rust-lisp. They had no
  watchers, forks or stars. In the user's words, these projects were pathfinders for Arbace's
  forms: Go, Haskell and Rust written as s-expressions and compiled beside their ordinary source,
  the prior art of repr/go, repr/haskell and repr/rust.
- Their contributions are kept in the private repository github.com/arbace/forks (`a21d047`; the
  entry above): each fork's patch series on its upstream base, checked to give the fork head's
  tree exactly. Its README says how to restore a fork.
- The citations in Arbace's surveys and specs (doc/G2C-SURVEY.md, doc/REPR-HASKELL-SURVEY.md,
  doc/REPR-RUST-SURVEY.md, doc/go/SPEC.md, doc/go/ROUNDTRIP.md, doc/haskell/SPEC.md,
  doc/rust/SPEC.md) name the forks by repository, branch and commit. Those commits are now
  reachable only by applying arbace/forks' patches to the upstream bases: golang/go `f043823a`,
  GHC `c35096f5`, rust-lang/rust `602727f2`. The citations are left as written, a dated record.

## 2026-10-20: "written entirely in symbolic expressions"

The user found the one-line description imprecise, "a Clojure derivative written entirely in
Clojure", and chose "Arbace is a Clojure derivative written entirely in symbolic expressions". It is
more precise because Arbace's own code is Clojure plus the Java forms and the Go forms (jrt, goi,
`tamago/`, `box/`), all s-expressions but not all Clojure. The sentence changed in three places:
GitHub's repository description, README's first line and CLAUDE.md's.

## 2026-10-20: two surveys started: a native Go parser, a native Java front end

- **The user's request:** plans for an Arbace-native Go parser and an Arbace-native Java front
  end.
- **Why these two:** the readers of repr/go and repr/java are not Arbace's own.
  - repr/go reads through `tools/godump`, Go's go/parser and go/types in a Go helper.
  - repr/java reads through javac's internals on the frozen JVM jar: Arbace's last real JVM
    dependency besides the oracle's reference.
  - repr/clojure's reader is already Arbace's own.
- **Started:** two surveys, each in its own worktree from `9a6a36e9`:
  - `go-parser-survey`, writing doc/go/GO-PARSER-SURVEY.md (proposals GPS1-);
  - `java-frontend-survey`, writing doc/classes/JAVA-FRONTEND-SURVEY.md (proposals JFS1-).
- **What each weighs:**
  - prior art: go/parser and gc's syntax; javac, ECJ and JavaParser;
  - a hand-written Arbace reader against running the language's own front end from its forms:
    gc's parser is already Go forms in `tamago/cmd/`, and javac's Java could be converted by
    repr/java;
  - what Arbace already has: goi's checker, an Arbace go/types; `arbace.classes`, javac's analysis
    over Java forms;
  - the forms' equality with today's readers over every corpus, as the check of the switch;
  - milestones and effort.

## 2026-10-20: goi's check in the emulated arm64 box over all of box.txt

- **What:** `GOI_BOX_ALL=1 bin/box-arm64-vm goi` (no `--record`) in a detached worktree at
  `ab8a41dc`, with the program of the seed `fcc37393…`. It was started while the Mac run was
  awaited and was left out of the tag's claim, which counted the half: 291 of 292.
- **Result:** 584 programs: 582 pass, 0 fail, 0 error, 2 known. 9,690 s (2 h 41 min), 8 boxes of 2
  vCPUs, host load about 14. One program, `copy.go`, timed out in the parallel round and passed
  alone (`babde67d`'s retry).
- **Against the references:**
  - all 292 programs of the recorded half (`test/goi/check-box-arm64.edn`) as recorded;
  - against amd64's box reference, one difference: `fixedbugs/issue15277.go`, known on arm64
    because its build constraint is amd64.
  - The 2 known are in classes of `test/goi/divergences.edn`: `typeparam/issue50109.go` (the box's
    console) and issue15277 (another architecture's program).
  - It is the first emulated run to include issue15277. The Mac's run of all 584 gave the same 582
    and 2.
- **No file changed:** the half stays the recorded reference (GC75).

## 2026-10-20: the Java front end's survey merged (doc/classes/JAVA-FRONTEND-SURVEY.md)

- Branch `java-frontend-survey` (`145e64d8`, from `9a6a36e9`) merged. No JVM or Go build was run:
  the measurements are over the committed `jdk/forms`, the last `bin/gate --full`'s reports, jdk26u
  at `baf63fb` and shallow clones of the prior art.
- **Finding: it is not mostly a parser.**
  - 77% of `convert.ae`'s code lines read javac's attribution.
  - In jdk/forms, 25.0% of the lists have a head chosen by name and member resolution and 14.4% by
    expression types; 60.6% follow from syntax alone.
  - `arbace.classes` has no generic types in code (SPEC principle 4), so it covers about a third of
    the attribution. The rest is javac's own: about 47,000 lines of Attr, Resolve, Infer and
    Types, against about 9,300 for its parser.
  - Only javac makes javac's class files, which the round trip's classes level compares.
- **Prior art, measured:**
  - javac (`jdk.compiler`, 162,441 lines, GPLv2+CE, already in LICENSE.md);
  - ECJ (241,716, EPL-2.0);
  - JavaParser (99,905 plus 32,375 for its symbol solver);
  - tree-sitter-java (syntax only).
- **Recommended: (b), javac itself as Java forms, run by Arbace** (D12 (b) of STANDALONE-SURVEY),
  in a tool executable translated by j2g, with `convert.ae` on top unchanged.
  - repr/java converted `jdk.compiler` once already (2026-10-07: 342 of 347 files).
  - jrt lacks about 67 of the 238 java.base classes javac names (an over-count, to be measured).
  - A native front end, (a), is estimated at 30-50 days with an open tail, and cannot give the
    classes level.
- **Milestones:** JF1-JF6, the switch off the JVM, about 13-20 days; then JF7-JF9 later.
- **What stays on the JVM afterwards:** the oracle's reference, j2g's edge, `check --suite` and the
  data generators.
- **Proposals JFS1-JFS11** are open, for the user. JFS11 notes that the milestone prefix JF
  collides with PRINTER-NOTES' decided JF1-JF5, and recommends JN1-JN9 instead.

## 2026-10-20: the Go parser's survey merged (doc/go/GO-PARSER-SURVEY.md)

- Branch `go-parser-survey` (`91a6e888`, from `9a6a36e9`) merged.
- **Today:**
  - repr/go reads Go only through `tools/godump` (2,801 lines: `go list`, go/parser, go/types). The
    converter takes about 30 decisions from go/types.
  - The box cannot read Go: no go command, export data or helper.
  - 134 of goi's unsupported errorcheck programs come from it: 70 with syntax errors, and 64
    invalid programs the converter cannot convert (GC47).
  - The five annotation corpora hold 121,345 declarations (an earlier figure said about 111,000).
- **Measured:**
  - **E1:** gc's syntax package gives gc's errors on 66 of the 70 syntax-error programs, go/parser
    on 20.
  - **E2:** the two parsers disagree on 16 of `$GOROOT/test`'s 3,400 files.
  - **E3:** goi interpreting go/parser over std (1,742 files, 19.6 MB) took 26 s, 0.75 MB/s, 59-70×
    native. Its node counts equal native's file by file (4,844,260 nodes).
  - **E4:** byte loops in Arbace run at 1.4-1.6 MB/s, so a hand-written Arbace parser would be no
    faster.
  - **E5:** compiling go/parser in costs about +356 KB.
  - Of today's conversion, 7 min 54 s of CPU goes to reading back the helper's 252.7 MB of dump
    text, out of 13 min 9 s.
- **Recommended:**
  - Go's own go/parser from `tamago/goroot`'s forms, interpreted by goi first, compiled through
    goi's table later.
  - A dumper generated from go/ast's forms, building the dump's data directly.
  - Syntax errors in gc's words, from gc's syntax package run from `tamago/cmd`.
  - Annotations from `arbace.go.check`, which resolves the converter's raw forms in a resolve mode.
  - The helper stays as the oracle in `bin/gate --full`.
  - Rejected: gc's syntax for the tree (not go/ast's, no comments). A hand-written parser, about
    3,500 lines and 2-3 weeks, remains the alternative.
- **The proof of the switch:** equality with the helper over every corpus at three levels: trees,
  annotations, and `.ae` byte for byte in `:lines` and `:full`.
- **Milestones:** GP1-GP8, about 23-32 days, the last letting the box read Go.
- **Proposals GPS1-GPS11** are open, for the user.

## 2026-10-20: GPS1-GPS11 and JFS1-JFS11 decided as recommended (the user)

- **The Go parser** (doc/go/GO-PARSER-SURVEY.md, "Decided"):
  - Go's go/parser from `tamago/goroot`'s forms, run by goi, compiled at GP7;
  - gc's syntax for syntax errors;
  - goi's checker for the annotations, the converter unchanged;
  - the helper kept as the oracle in `bin/gate --full`;
  - the 64 invalid programs checked on their raw forms.
  The milestones are GP1-GP8.
- **The Java front end** (doc/classes/JAVA-FRONTEND-SURVEY.md, "Decided"):
  - javac itself as Java forms, in `jdk/javac/` (`bin/javac-vendor`), pinned to jdk26u `baf63fb`;
  - run by Arbace in a tool executable built on demand, with an Arbace `JavaFileManager`;
  - the platform's declarations compiled once and cached;
  - the fixtures' runtime printed from `arbace/lang`;
  - repr/java's `--full` steps moved to it;
  - no native front end now.
  The milestones are renamed JN1-JN9 (JFS11), since JF1-JF5 name PRINTER-NOTES' decided printer
  choices.
- Neither has started: the decisions record the plans.

## 2026-10-20: GP1 and JN1 started (the user)

- **GP1, the tree** (branch `go-parser-gp1`, from `26816f48`). Go's go/parser from
  `tamago/goroot`'s forms, run by goi in the tools' host; a dumper generated from go/ast's forms,
  building the dump's tree in the helper's shape; `bin/repr-go read --reader native` and a first
  `native-check`. Exit: E-tree (trees, comments, directives) equal to the helper's over every
  corpus; times over std.
- **JN1, the spike** (branch `java-frontend-jn1`, from `26816f48`). javac's sources (jdk26u
  `baf63fb`) read by repr/java into scratch; their closure in jrt measured, giving the real list of
  gaps and cuts; a minimal javac program translated by j2g into a tool executable; the platform's
  options tried. Exit: the samples parsed on Go equal the JVM's, as `Pretty` prints them; the
  tool's size and times.

## 2026-10-20: GP1 merged (the native Go parser's tree)

- **What:** branch `go-parser-gp1` (`a71f51c2`, from `26816f48`) merged.
  - **The parser:** `arbace.repr.go.parse` runs Go's go/parser in goi, in the tools' host, from
    the executable's digest. Each run checks first that the digest's go/parser, go/scanner, go/ast
    and go/token equal `tamago/goroot`'s forms declaration by declaration (135, 50, 291, 88; GPS12).
  - **The dumper** is generated in memory from `tamago/goroot`'s go/ast forms (56 node types, 194
    fields), with the helper's `:dot`, positions, comments, directives and `/*line*/` warnings
    written by hand beside it. It builds the dump's data directly, without annotations.
  - **New commands:** `bin/repr-go read --reader native` and `native-check [--fresh]`. The known
    differences are listed in `test/repr/go/native-known.edn`, which is empty.
    `bin/lib/repr-go/native-corpus` dumps the vendored corpora.
  - **The gate:** `bin/gate --full` gets a step `repr-go-native` after the round trips (GPS15). The
    Makefile gets `repr-go-native-check`.
- **E-tree:** the native trees, comments and directives equal the helper's dumps over all 22
  corpora: 12,275 of 12,275 dumps, 22,603 files, 25,763,858 node forms.
  - One real difference was found and fixed: `blank1.go` (`package _`), whose go/types package
    name is `""`.
  - The 73 errtest programs go/parser rejects have no helper dump; they are GP6's.
  - File lists come from the helper's headers until GP2 (GPS14).
- **Times** over std tamago/amd64 (19.6 MB, one thread): parse 25.2 s (0.78 MB/s), dump 27.3 s,
  52.5 s in all. Reading back the helper's dumps of the same files takes 96.7 s. The native dumps
  as text are 132 MB, against the helper's 252.7 MB.
- **Checks:** the branch's essential `bin/gate` passed (20m03s); `make check-makefile` passed.
  The change is to tools' namespaces only, evaluated from the checkout, so no re-seed is needed.
- **Open for the user:** GPS12-GPS15, each built as recommended (GOI-SPEC style, in the survey's
  "As built: GP1"):
  - GPS12: the forms from the digest, checked against `tamago/goroot`'s;
  - GPS13: the dumper generated in memory at each load, a committed file at GP7;
  - GPS14: file lists from the helper's headers until GP2;
  - GPS15: `native-check --fresh` sequential after repr-go in `--full`.
- **CLAUDE.md:** repr/go's verbs and `--full`'s list now name the native reader.
- **A rule slip:** `bin/repr-go` was edited while a run of it was executing. Its dumps were
  complete; the run's last line then failed.

## 2026-10-20: GPS12-GPS15 decided as built (the user)

GPS12-GPS15 (doc/go/GO-PARSER-SURVEY.md, "As built: GP1"), each (a), as built:
- the digest's forms of go/parser, checked against `tamago/goroot`'s;
- the dumper generated at each load until GP7;
- file lists from the helper's headers until GP2;
- `native-check --fresh` after the round trips.

## 2026-10-20: GP2 started (the user)

GP2, the loader (branch `go-parser-gp2`, from `c8287cf1`):
- `arbace.box.plan` generalized from the store's trees to directories as well: std and cmd,
  modules, test variants, the directory tests (`dirs.go`'s rules ported), embed globs, language
  versions. No export data is needed.
- The native reader takes its file lists and the dump's header from the loader, which ends
  GPS14's interim.
- Exit: E-tree's header equal to the helper's over every corpus; `tamago/goroot`'s and
  `tamago/cmd`'s lists equal to go list's; the box's planner kept working.

## 2026-10-20: JN1 merged (the spike: javac on Arbace)

- **What:** branch `java-frontend-jn1` (`322f7495`, from `26816f48`) merged. It records "As built:
  JN1" in doc/classes/JAVA-FRONTEND-SURVEY.md. It committed no code: the shipped program, jrt,
  `overlay/jdk`, j2g and `jdk/` are unchanged.
- **The conversion:** javac's sources from jdk26u `baf63fb` were converted on the frozen JVM:
  - jdk.compiler: 364 files, none failed, 18 s;
  - java.compiler: 135 files, none failed;
  - `CommandLine`: 1 file.
  That is 500 files, 6.7 MB of forms.
- **The closure:** beyond what jrt translates, javac needs only 19 small java.base classes and
  about ten members (stubbed).
  - Cut in javac: `ResourceBundle.getBundle`, `ServiceLoader`, `Runtime.version`, two overloads.
  - Avoided by the Arbace `JavaFileManager`: file systems, jar/zip, jmod, the module system.
  - `DataOutputStream`, which `ClassWriter` needs, is JN3's.
  - The edge needed javac's world recorded too: 85 failing classes down to 13, none reached.
- **The minimal program:** a driver (`jn1.Main`, `jn1.Files`), translated by j2g into a tool
  executable, gives output byte for byte equal to the frozen JVM's:
  - parsing the samples, the baseline, javac's own sources, java.base's closure (838 files);
  - attributing `Reduced`, the samples and the baseline (812 classes). The only difference is
    `capture#N` numbers, which also differ between two JVM runs.
  - Parsing runs at about the JVM's speed, attribution at about 1.7× the cold JVM.
- **The tool:** 183,373,984 bytes (+28% on the shipped executable); a rebuild takes about 5
  minutes.
- **The platform (JFS2 (a)) holds:** an exploded java.base works. Its sources on the source path
  do not.
- **What the spike needed, not committed:**
  - javac patched in 11 files (+94 −32);
  - jrt's `Files` and `Path` stubs;
  - j2g fixes: `Jv_` binary names for javac's packages (462 collisions), casts outside the world,
    `Object.wait(long)`, an unreached bound private method reference;
  - four workarounds: two j2g bugs and two bugs of `arbace.classes`' analysis.
  The diffs and drivers are kept in `.tmp/jn1/` (patches/, driver/, the scripts, the logs). The
  worktree's large build outputs were removed.
- **The estimates are revised:** JN2 to 1-2 days, JN3 to 3-4.
- **Open for the user:** JFS12-JFS17.
- **Rule slips:** five error reports went to `/tmp` (moved or deleted); `/dev/stdout` was used as
  an output path twice.
- **The disk** is at 73%: Go's build cache grew to 32 GB with the tool builds. It trims itself at
  the next builds.

## 2026-10-20: JFS12-JFS17 decided as recommended (the user)

From doc/classes/JAVA-FRONTEND-SURVEY.md, "As built: JN1":
- JFS12 (c): javac's sources unpatched; jrt grows the missing JDK API, and the workarounds
  become j2g and analysis fixes.
- JFS13 (a): binary names for the tool's modules.
- JFS14 (b): `jdk/javac/edge.edn`.
- JFS15 (b): the build tree's exploded platform as a pinned stand-in.
- JFS16 (b): the j2g fixes with JN2.
- JFS17 (a): javac's internals rooted for JN4, measured.

## 2026-10-20: JN2 started (the user)

JN2, javac's forms vendored (branch `java-frontend-jn2`, from `52d8c504`):
- javac's forms in `jdk/javac/` (forms, provenance, sources) with `bin/javac-vendor
  convert|verify|check`, and javac's edge in `jdk/javac/edge.edn`;
- no patches in javac (JFS12 (c)): jrt grows the missing JDK API, and JN1's workarounds become
  fixes in j2g and `arbace.classes`, each with a regression form in `bin/diffcheck`;
- LICENSE.md's GPLv2+CE list extended.
- Exit: `verify` byte for byte; j2g translates javac's world without errors; the essential gate;
  the shipped program changed only as intended (jrt's additions, `TimeUnit.timedWait`); the
  fixpoint; JN1's driver equal to the JVM's on the vendored, unpatched forms.

## 2026-10-20: GP2 merged (the native Go parser's loader)

- **What:** branch `go-parser-gp2` (`b42857a8`, from `c8287cf1`) merged.
  - **The box's planner,** `arbace/box/plan.ae` (346 → 1,020 lines), reads directories as well as
    store trees. It ports go/build's file matching, build constraints and header reader;
    cmd/go's embed resolution with globs; `go.mod`; std's and cmd's vendor resolution; go list's
    patterns and `-deps`.
  - **New, `arbace/repr/go/load.ae`:** the helper's three modes (packages with test variants,
    `-files`, `-dirs` with `dirs.go` ported) and each dump's whole header. It needs no go command
    and no export data.
  - **The native reader** (`read --reader native`) runs without the helper. `native-check`
    compares headers and the sets of dumps as well (`--headers`). Each corpus is a spec file.
  - **New:** `bin/lib/repr-go/plan-check [--base REV]` tests the box's planner without a box run.
- **The headers:** all 12,275 dumps over 22 corpora equal the helper's; `native-known.edn` stays
  empty. Equal headers mean go list's file lists match, for `tamago/goroot` and for
  `tamago/cmd`'s 105 packages, which the loader computes itself.
  - The native reader dumps 14 goos packages of other architectures that the helper cannot build
    (GPS17).
  - Three loader bugs were found and fixed: `builtin` and `runtime/cgo` under `std`, a test
    variant of a main package without tests, and `:imports` of a spec named `init`.
- **The box's plan is unchanged.** It is byte identical to the old planner's, `:src` hashes
  included: 325 packages on tamago/amd64, 324 on arm64, equal to tools/goplan's. It now takes 2.5
  s instead of 4.2 s.
  - Embedded files keep walk order (GPS18).
  - The planner now accepts glob `//go:embed` patterns and refuses `.cxx`, `.for`, `.f90`,
    `.swigcxx` and `.sx` files, none of which exist in the box's trees.
  - `bin/box-rebuild plan-check` was not run; it needs a box run.
- **The program changes:** `arbace.box.plan` and `arbace.repr.go.load` are embedded. Until the
  re-seed, the loader loads the checkout's `plan.ae` when it differs from the embedded copy
  (GPS16). The re-seed is batched with JN2's.
- **Times:** the loader takes about 66 s over all 22 corpora; `native-check --fresh` 19m51s;
  `--headers` 1m16s. The branch's essential `bin/gate` passed (18m52s).
- **Not ported**, listed in the survey: symlinks, `go.work`, module-cache requires, modules' own
  `vendor/`, go.mod `ignore`, cgo on, and go list's errors beyond go/build.
- **Open for the user:** GPS16-GPS20, each built as (a).

## 2026-10-20: GPS16-GPS20 decided as built (the user)

GPS16-GPS20 (doc/go/GO-PARSER-SURVEY.md, "As built: GP2"), each (a), as built.

## 2026-10-20: GP3 started (the user)

GP3, raw forms (branch `go-parser-gp3`, from `02d20ed1`): the converter's raw mode over the native
tree, with every decision that needs types left open and marked, for GP4's resolving checker. The
mode is private (GPS10 (a)) and documented. Exit:
- every corpus converted raw, read back, its open decisions counted per kind;
- GC47's invalid programs given raw forms where the parser accepts them;
- the normal conversion unchanged.

## 2026-10-20: JN2 merged (javac's forms vendored)

- **What:** branch `java-frontend-jn2` (`8b12c86a`, from `52d8c504`, main's GP2 merged in) merged.
- **The vendored forms:** `jdk/javac/forms` holds 561 files (6.7 MB) from 524 jdk26u sources at
  `baf63fb`:
  - jdk.compiler 368, of which 21 come from the build tree's gensrc;
  - java.compiler 135;
  - `CommandLine` 1;
  - 20 java.base classes for javac's world, among them an Arbace `ServiceLoader` that finds no
    provider (`overlay/jdk/javac/java.base`).
  `sources.txt`, `provenance.edn` and `jdk/javac/edge.edn` (35 classes beyond `jdk/edge.edn`) sit
  beside them.
- **The script:** `bin/javac-vendor convert|edge|verify|check|build`, with `bin/arbace-go
  --build-java` for the tool executable. The Makefile gets `javac-verify` (in `make verify`) and
  `build-java`. LICENSE.md's GPLv2+CE list covers `jdk/javac/`.
- **javac's sources are unpatched** (JFS12 (c)).
- **jrt's shipped additions:** `ResourceBundle.getBundle` (the JDK's candidate search),
  `Runtime$Version` and `Runtime.version()`, `printStackTrace(PrintWriter|PrintStream)`. They
  changed `jdk/edge.edn` in two lines, recorded by `bin/j2g-edge update` (JFS19).
- **Only in the tool build:** the 19 classes, `ServiceLoader`, and j2g variants for
  `InputStreamReader(InputStream, CharsetDecoder)`, `Path.getFileSystem` and ten throwing `Files`
  members (JFS18). j2g gets `--edge` (repeatable), `--variants`, `--qualify`.
- **The fixes,** each with a regression form in `test/j2g/fixtures/FxJavac.ae` and
  `FxJavacShapes.ae`:
  - `Jv_` binary names for the tool's modules;
  - casts outside the world;
  - `Object.wait(long)`;
  - a bound reference to a private method;
  - a lambda temporary typed `any`;
  - an outer instance read through an interface type;
  - an anonymous class in a constructor prologue;
  - a lambda returning an anonymous class type.
  The frozen jar cannot compile FxJavacShapes; its two results are kept by hand (JFS21).
- **The shipped program's diff,** function by function against main's build: `TimeUnit.timedWait`
  is translated instead of a stub, plus ResourceBundle's members, `Runtime_Version`, the cut list,
  and `ProcessImpl`'s `onExit` lambda without an `any` temporary.
- **Checks:**
  - `javac-vendor verify` byte for byte (564 files);
  - `javac-vendor check`: 16,668 classes analyzed, 0 failures; the tool is 186,527,904 bytes;
  - the branch's essential gate and fixpoint (G1 = G2 = G3);
  - JN1's driver equal to the JVM's on the unpatched forms;
  - on main after the merge, the essential `bin/gate` passed (20m28s).
- **The agent's leftover wait loop** (2 h on a finished build) was stopped by its PID.
- **Rule slips:** one `2>/dev/null`; a first baseline built while editing j2g, discarded.
- **The re-seed** waits for GP3: the program carries GP2's and JN2's changes.
- **Open for the user:** JFS18-JFS22.

## 2026-10-20: JFS18-JFS22 decided as built (the user)

JFS18-JFS22 (doc/classes/JAVA-FRONTEND-SURVEY.md, "As built: JN2"), as built:
- javac-only API in the tool's world;
- the shipped additions and their two `jdk/edge.edn` lines kept;
- the generated sources copied until JN7;
- two fixture results kept by hand;
- j2g for the tool on a program built from the checkout.


## 2026-10-21: GP3 merged (raw forms)

- **What:** branch `go-parser-gp3` (`5a6c822f`, from `02d20ed1`) merged. The converter has a raw
  mode (`arbace/repr/go/raw.ae`, new, 535 lines; `convert.ae`, `go.ae`). It reads no annotation
  and leaves every decision that needs types open, marked in the text and by a private `::open`
  key:
  - calls `(F a…)`, never classified;
  - `(go/select x f)`, `(go/index x i…)`, `(go/star x)`;
  - `true`/`false`/`nil` as `(go/id "…")`;
  - `^:go/define`, `^:go/key`, `^any` constraints, implicit import names `["path"]`;
  - three marks used only by invalid programs.
  It writes no `:val`, `:tag` on shifts, `:inst`, `:go/via` or `:init-order`. Raw forms always
  carry `:full`'s positions. They are private (GPS10 (a)), documented in doc/go/CONVERTER-NOTES.md,
  "Raw forms".
- **Measured** over all 22 corpora (`bin/repr-go read --reader native --raw`):
  - 12,289 dumps convert raw and read back, 22,674 files, 1,170.8 MB of forms;
  - 5,306,501 open decisions, the largest kinds select (1.89 M) and call (1.48 M);
  - all 586 error tests go/parser accepts get raw forms, GC47's 64 among them.
- **The check against the final forms:** `bin/repr-go raw-check` (Makefile `repr-go-raw-check`,
  `test/repr/go/raw-known.edn` empty) walks raw and final forms in parallel. They must be equal
  except at the open decisions, where the final form must be one of the kind's resolutions.
  - 12,158 dumps resolved, 0 differ.
  - 117 are raw only: GC47's 64, and 53 goos packages of other boards that the helper dumps with
    type errors.
- **The normal path is unchanged:** 28 conversions (14 corpora in `:lines` and `:full`), 62,878
  `.ae` files byte identical to main's; the round trips without regression.
- **A fix (GPS24):** the converter's statement lists loop instead of recursing. `bug385_64.go`
  (16,480 statements) overflowed the stack.
- **Times:** the raw read takes 42 min wall over the 22 corpora, `raw-check` 43 min, both
  evaluated from the checkout until the re-seed. The branch's essential gate passed (20m50s).
- **Open for the user:** GPS21-GPS26, each built as (a). GPS25 recommends correcting, at GP5, three
  places where the final forms are less precise than the raw ones.
- **A rule slip:** `convert.ae` was edited during a raw read over all corpora (a field named
  `true`). The run was stopped by its PIDs and all 22 corpora were done again with the final code.

## 2026-10-21: the seed remade after GP2, JN2 and GP3

- **What:** on main at `5f72b4ee` (GP3's merge), one chain: the essential `bin/gate` passed
  (27m24s), `bin/arbace-go --fixpoint` held (G2 = G3 byte for byte), then
  `ARBACE_SEED_GO_BUILD=$PWD/target/arbace-go/fixpoint/g3 bin/seed --make`, committed as
  `665fa0b6`, then `bin/seed --verify`.
- **The seed:** `aa4cb10b22fe51b3fe8d45359263383c1e2f86f9e7749a8b65292cbc9373dc89` (was
  `fcc37393…`), 18,316,556 bytes, 1,948 files, 120,922,551 bytes unpacked.
- **Verified:** printed again from its commit, the same archive; stage 0 = G1 = G2 = G3, byte for
  byte, `56604d7ea7239f9aa6fbc92c16aad29a98b3459da5373a65996455afcbf5cd20`.
- **Why batched:** GP2's loader and JN2's jrt and j2g changes changed the program; GP3 followed
  within the day, so one re-seed covers the three (decided at GP2's merge).

## 2026-10-21: GPS21-GPS26 decided as built (the user)

GPS21-GPS26 (doc/go/GO-PARSER-SURVEY.md, "As built: GP3"), each as recommended:
- marks of their own for the open decisions, `::open` in memory;
- raw calls unclassified;
- `raw-check` by hand now, merged into native-check at GP4;
- the statement lists loop;
- the final converter's three imprecisions corrected at GP5;
- GP4's checker takes the raw forms in memory.

## 2026-10-21: GP4 and JN3 started (the user: "start GP4 and JN3")

- **GP4, the annotator:** branch `go-parser-gp4` from main `982ae875`, in the worktree
  `.tmp/worktrees/go-parser-gp4`. Its work: goi's checker in a resolve mode over the raw forms in
  memory (GPS26), E-ann for every corpus, and `raw-check` merged into native-check (GPS23 (c)).
- **JN3, javac runs on Arbace:** branch `java-frontend-jn3` from main `982ae875`, in the worktree
  `.tmp/worktrees/java-frontend-jn3`. Its work: javac's class-file output on Arbace, byte for byte
  equal to the frozen JVM's javac over the baseline, the samples and java.base's closure
  (`-g:none`), on JFS15 (b)'s pinned platform.
- Each is built by an agent in its worktree. The merges, the journal, the agenda and the re-seed
  stay in the main session.

## 2026-10-21: JN3 merged (javac runs on Arbace)

- **What:** the branch `java-frontend-jn3` (`f805cbdd`, from `982ae875`) is merged.
  doc/classes/JAVA-FRONTEND-SURVEY.md, "As built: JN3" (JN3.1-JN3.10), has the details.
- **javac's command line on Arbace.** `jdk.internal.jrt.javac.Main` (170 lines) and `FileManager`
  (451 lines, a `java.io.File`-based `JavaFileManager`, JFS5 (b)) are jrt's own Java in
  `overlay/jdk/javac/jdk.compiler/`, converted with javac into `jdk/javac/forms` (JFS25 (a)).
  - Main handles `-d`, `-sourcepath`, `-cp`, `--patch-module`, `--module-source-path`,
    `--system none` and @argfiles.
  - javac's own Log prints the diagnostics.
- **Gaps filled:**
  - jdk26u's `DataOutput`, `DataOutputStream`, `UTFDataFormatException` and `ModifiedUtf` are
    converted unpatched into javac's world, for ClassWriter.
  - jrt's own `FileSystems` serves `PathFileObject.<clinit>`.
  - The annotation and processor paths stay stubs, since no corpus needs them.
  - No javac source is patched (JFS12 (c)).
- **The platform** is the build tree's exploded java.base, java.xml, java.logging, java.desktop,
  java.datatransfer and java.prefs. Each module's descriptor comes from `images/jmods` (JFS23 (b)),
  and the set is pinned by `jdk/javac/platform.sha256` (`bin/javac-vendor platform-pin`; `convert`
  writes the pin and `verify` checks it).
- **The check:** `bin/javac-vendor classes` compiles each corpus with the JVM's javac and with
  javac on Arbace (`-g:none`) and compares the class files and diagnostics. It has a Makefile target
  `javac-classes` and a `javac` step in `bin/gate --full`'s Go chain (JFS26 (a)).
  - All are byte for byte equal:

    | corpus | classes |
    |---|---|
    | baseline | 812 (183 files) |
    | samples | 32 |
    | java.base's closure | 2,224 (838 files) |
    | java.base, built the JDK build's way (JFS2 (a)) | 7,434 (3,404 files) |

  - java.base's classes also equal the build's `jdk/modules/java.base` in 7,433 of 7,434. The
    exception is `module-info.class`, which the build rewrites after javac (`AddPackagesAttribute`).
  - Wall time against the JVM's javac:

    | corpus | JVM's javac | javac on Arbace | ratio |
    |---|---|---|---|
    | baseline | 4.0 s | 5.5 s | 1.4× |
    | closure | 8.7 s | 18.0 s | 2.1× |
    | java.base | 21.1 s | 62.9 s | 3.0× |

    The samples are faster on Arbace, since there is no warm-up. Arbace's CPU time is lower
    throughout.
- **The tool:**
  - 16,777 classes analyzed, 0 failing, and 16,674 translated.
  - 187,433,120 bytes, built in 261-280 s.
  - `jdk/javac`: 531 sources, 570 form files (6.7 MB), an edge of 38 classes.
- **Shipped-program changes, for the next re-seed:**
  - jrt's `JavaLangAccess.countNonZeroAscii`, with a test. j2g's Dyn slot table has one more
    method (1,412).
  - `arbace.classes.env/known?`: a class compiled from source no longer counts as declared while
    `*declared*` is bound.
  - j2g's `tool-module-prefixes` gains `jdk/internal/jrt/javac/`.
  - `jdk/edge.edn`, `jdk/forms` and the java-fixtures are unchanged.
- **The branch's checks:**
  - `javac-vendor verify` (574 files) and `check`.
  - `j2g-edge verify`.
  - `bin/jrt test`.
  - The essential gate passed (15m34s).
- **Time:** about 1.5 h, against the 3-4 days estimated. JN1 had done the hardest part.
- **Rule slips:**
  - `bin/javac-vendor` was edited while a `convert` run was executing. That run died at its end
    and was re-run clean.
  - j2g's failure report went to `/tmp` (TMPDIR unset). It was removed, and TMPDIR was set to
    `.tmp/tmp` after that.
- **On main after the merge (`62d31a6a`):** the essential `bin/gate` passed every step but one
  (13m56s). The differential check had one new mismatch, `test/oracle/forms/processes.ae:178`:
  the compiled side's `(.get (.arguments info))` of a `sleep` just started gave "No value present".
  GP4's corpus runs were loading the machine at the time. `bin/diffcheck` run again alone passed
  (12,741 of 12,764, the 23 known, 0 new). The case is a timing flake: the child's `/proc`
  arguments were read before its exec. Left as is, to watch.
- **The re-seed** waits for GP4: the program changed (jrt, `arbace.classes.env`, j2g).
- **Open for the user:** JFS23-JFS26.

## 2026-10-21: JFS23-JFS26 decided as built (the user)

JFS23 (b), the exploded modules with the jmods' descriptors; JFS24 (a), the pinned build tree
stays the platform until JN6; JFS25 (a), the command line as jrt's Java in jdk.compiler's overlay;
JFS26 (a), the `javac` step in `bin/gate --full`.

## 2026-10-21: the re-seed batched, JN4 started, the processes form to be made robust (the user)

- **The re-seed:** JN3's changes to the program wait for GP4, for one re-seed (the user chose
  this over re-seeding now).
- **JN4, repr/java's `read` on Arbace:** started on the branch `java-frontend-jn4` from main
  `864a5de2`, in the worktree `.tmp/worktrees/java-frontend-jn4`, alongside GP4.
- **`test/oracle/forms/processes.ae:178`:** to be rewritten so it does not race the child's exec.
  The work is on the branch `oracle-processes-flake`; the user preferred this over recording it as
  known or leaving it.

## 2026-10-21: the processes form made robust (merged)

- **What:** the branch `oracle-processes-flake` (`de8388f2`) is merged. In
  `test/oracle/forms/processes.ae` (line 178), the child's `.info` is now polled every 10 ms, for at
  most 500 tries, until its arguments are present. The value is unchanged:
  `[true true ["5"] true true true true true true 143 false false]`.
  `test/oracle/expected/forms/processes.edn` was re-recorded from the frozen jar (129 cases; cases
  88-129 are 2 lines later).
- **Why in the form:** jrt's `ProcessHandle.Info` is a snapshot of `/proc/PID/cmdline` taken at
  `.info` time, as the JDK's is (`hostproc_linux.ae`'s `ProcInfo`). `.start` can return before the
  child's argument list is readable, which is the JDK's race too.
- **Checks:**
  - The old form failed 1 of 60 concurrent runs.
  - The new form passed 120 of 120 evaluated runs. Compiled, `bin/diffcheck forms/processes` passed
    20 of 20 under added load.
  - `bin/oracle check`: 22,658 of 22,711, the 53 known.
  - `bin/diffcheck`: 12,741 of 12,764, the 23 known.
  - The branch's essential gate passed.
- Nothing else keys on the file's line numbers: the known lists have no entries for it, and the
  box's subset excludes it.

## 2026-10-21: JN4 merged (repr/java's read on Arbace)

- **What:** the branch `java-frontend-jn4` (`664103b2`, from `864a5de2`) is merged. The survey's
  "As built: JN4" (JN4.1-JN4.11) has the details.
- **The reader on the tool.** `arbace/repr/java/javac.ae` hands JN3's
  `jdk.internal.jrt.javac.FileManager` to `getTask`, with the platform from `REPR_JAVA_PLATFORM`.
  - `bin/repr-java` takes `REPR_JAVA_HOST=jvm|arbace` and `REPR_JAVA_EXE`; the JVM stays the
    default.
  - `bin/javac-vendor platform DIR` makes the platform.
  - `bin/repr-java host-check [check jdk fixtures full]` is the check (Makefile
    `repr-java-host-check`).
  - The fixtures' runtime is Arbace's declarations, printed by `print --layout declarations`
    (bodies `throw null;`) and compiled by javac on Arbace: 141 files, 836 classes (JFS27 (b)).
  - The reader is compiled into the tool by c2j on a second pass of `--build-java`, kept by key in
    `target/arbace-go/aot-reader`, and embedded by j2g's new `--embed` (JFS28 (b)).
- **Equal to the JVM's reader, byte for byte:**

  | corpus | files |
  |---|---|
  | baseline | 189 |
  | samples | 11 |
  | module | 6 |
  | `jdk/forms` | 911 |
  | fixtures | 15 |
  | mode `:full` | 189 / 11 / 6 / 911 |

  `host-check` takes 393 s compiled and 609 s evaluated.
- **JFS17 (a) measured:** rooting javac's internals grows the tool by 0.6%
  (187,433,120 → 188,498,080 bytes). With the compiled reader it is 197,910,688 bytes.
- **Speed:**

  | corpus | the tool | the JVM |
  |---|---|---|
  | baseline | 24.1 s | 13.8 s |
  | jdk (12 threads) | 80.3 s | 25.3 s |

  The samples, module and fixtures take 0.3-0.75 s against 5-6 s, since no JVM starts. The jdk
  corpus peaks at 26.6 GB, about twice the JVM.
- **Fixes:**
  - j2g (`decls.ae`): a record component typed outside the world is now an operation-level stub.
  - c2j (`emit.ae`): a linked call resolves the callee's parameter tags in the callee's
    namespace; before, it depended on the order sources were compiled in.
  - The printer: `default` for an interface method with a body, and the declarations layout.
  - No diffcheck form is possible for these, for the reasons the survey gives.
- **Re-recorded:**
  - `jdk/javac/edge.edn`: 39 classes, `ModuleDescriptor` cut.
  - The three provenance files: the `:repr-java` hash only.
  - `jdk/edge.edn` and `jdk/forms` are unchanged.
- **The shipped program:** only four tool namespaces' compiled code changes (`arbace.j2g.main`,
  `.embed`, `.decls`, `arbace.c2j.emit`). The executable is 147,206,304 bytes.
- **Checks:**
  - `javac-vendor verify`, `check`, and `classes` on all four corpora.
  - `j2g-edge verify`.
  - `--java-fixtures verify`.
  - The branch's essential gate.
  - On main after the merge (`f824fc24`), the essential `bin/gate` passed (13m24s).
- **Time:** about 3 h, against the 2-4 days estimated.
- **Rule slips:** one wait loop used `pgrep -f build-prog.sh > /dev/null`. It only read; nothing
  was stopped.

## 2026-10-21: JFS27-JFS30 decided as built (the user)

- JFS27 (b): the fixtures' runtime from Arbace's declarations, compiled by javac on Arbace.
- JFS28 (b): the reader compiled into the tool, kept by key.
- JFS29 (a): `bin/repr-java host-check` ends `bin/gate --full`'s javac step until JN5.
- JFS30 (a) now and (b) at JN5: the provenance names the host that read, then the key of the
  tool's sources.

## 2026-10-21: GP4 merged (the annotator)

- **What:** the branch `go-parser-gp4` (`61905030`, from `982ae875`) is merged. The survey's "As
  built: GP4" has the details.
- **The checker's resolve mode** (`arbace.go.check`, doc/go/GOI-SPEC.md §3.10) checks GP3's raw
  forms in memory.
  - It resolves `go/select`, `go/index`, `go/star`, builtin-named calls through the scope,
    `^:go/define`, `^:go/key` and implicit imports.
  - It records answers by position: modes, types, values, objects, selections, fields, `:inst`,
    the init order.
  - go/types' spellings (`byte`, `rune`, aliases) are kept in this mode only (GPS27 (a)).
- **`arbace/repr/go/annotate.ae`** (new, 869 lines):
  - sessions per configuration;
  - external tests in go list -test's world (GPS29 (a));
  - the answers written into the native dump in the helper's shape;
  - E-ann: the annotated dump converted by the unchanged converter, compared with the helper's
    final forms with every key and position (GPS28 (a)).
- **Commands:**
  - `bin/repr-go native-check` runs E-tree, the raw check and E-ann in one read (GPS23 (c)).
    `raw-check` and `raw-known.edn` are removed.
  - New `read --reader native --forms [--full]`.
  - New `bin/goi annotations --forms DIR [--reference DIR]`.
- **Numbers:**
  - `native-check` over 22 corpora: 12,275 dumps, E-tree 12,275 equal, **E-ann 12,158 of 12,158
    equal**, `native-known.edn` empty. It took 70 min 57 s wall at a peak of 29.9 GB.
  - `bin/goi annotations` over the native conversions: zero errors.
  - The native forms of goi's five corpora are byte for byte the helper's (0 of 11,029 differ;
    GP5's E-forms already holds in `:lines`).
  - The 28 conversions are unchanged (62,878 files).
- **Checker fixes:**
  - Two apply in goi's normal mode too (GPS31 (a)): local multi-name constant specs, and
    `fallthrough` after a declaration.
  - The rest are fidelity fixes in the resolve mode, including seven rules for how go/types
    records invalid programs (GPS30 (a)).
- **Checks:** the branch's essential gate passed (21m27s). On merging, the Makefile's `.PHONY`
  line conflicted with JN4's: it was resolved with `repr-go-raw-check` removed and
  `repr-java-host-check` kept.
- **Rule slips:** a crash report went to /tmp before TMPDIR was set; it was deleted.
- **Open for the user:** GPS27-GPS31.
- **Next, the batched re-seed** for JN3, JN4 and GP4.

## 2026-10-21: GPS27-GPS31 decided as built (the user)

GPS27-GPS31 (doc/go/GO-PARSER-SURVEY.md, "As built: GP4"), each (a):
- go/types' spellings in the resolve mode only;
- E-ann compares the final forms, with every key and position;
- external tests in go list -test's world;
- go/types' rules for invalid programs in the resolve mode only, with (b) left for GP6;
- the two checker fixes in both modes.

## 2026-10-21: the seed remade after JN3, JN4 and GP4

- **What:** on main at `fd3aba59`, one chain: the essential `bin/gate` passed (26m59s),
  `bin/arbace-go --fixpoint` held (G2 = G3), then `bin/seed --make` from G3, committed as
  `66f8aaba`, then `bin/seed --verify`.
- **The seed:** `3dda04f62af832c90d5c7574ddc209607c9bcdd8401b44e6fb2428dc9762ef2e` (was
  `aa4cb10b…`), 18,488,584 bytes, 1,954 files, 123,303,354 bytes unpacked.
- **Verified:** the same archive printed again from its commit; stage 0 = G1 = G2 = G3, byte for
  byte, `4f32a6af6dc87a820cdf8b471a265e2893a6f43962d5228518d484678b7d9abd`.
- The gate at the start of the chain was also the first on GP4's merge (`7dd53bef`, its Makefile
  conflict resolved by hand).

## 2026-10-21: what next, the full gate, and /tmp (the user)

- **GP5 and JN5** start together, in worktrees off the re-seeded main.
- **`bin/gate --full`** waits for the next milestone, as the policy says. Its steps added since the
  tag (javac, `repr-java host-check`, native-check with E-ann) first run as gate steps then.
- **/tmp:** Arbace writes its error reports to `$TMPDIR` (else /tmp). 28 `arbace-*.edn` reports
  from 2026-10-20 and 2026-10-21 were deleted; some came from the re-seed's own gate as it ran. The
  scripts of `bin/` will set `TMPDIR` to `.tmp/tmp` when it is unset (the user's choice over
  deleting only).

## 2026-10-21: TMPDIR set by the scripts of bin/ (the user)

- **What:** a new `bin/lib/tmpdir.bash` sets `TMPDIR` to `.tmp/tmp` when it is unset. All 34
  scripts of `bin/` source it right after setting `$root`, and CLAUDE.md's working conventions say
  so.
- **Why:** Arbace writes its error reports (`arbace-*.edn`) to `$TMPDIR`, else /tmp. The gate's
  own runs left them in /tmp.
- **Alternatives:** an export in `bin/gate` alone would not cover scripts run directly. Changing
  where the program writes its reports would change the program's behaviour, which the JVM's
  `java.io.tmpdir` defines.
- **Checked:** the essential `bin/gate` passed (11m05s). It wrote nothing to /tmp: its two reports
  went to `.tmp/tmp`.

## 2026-10-21: JN5 merged (the checks moved to the tool)

- **What:** the branch `java-frontend-jn5` (`724eb171`, from `ad80344b`) is merged. The survey's
  "As built: JN5" has the details.
- **The fixpoint:** javac on Arbace reads javac's own sources into `jdk/javac/forms`, byte for
  byte.
- **The tool is repr/java's default host** (`REPR_JAVA_HOST=arbace`, JFS33 (b)), built by hand by
  `bin/javac-vendor build`. The JVM stays the reference until JN6, by `REPR_JAVA_HOST=jvm`.
- **The tool's key** (JFS30 (b), `bin/lib/javac-key.bash`) is written as `arbace-javac.key`. The
  provenance writers refuse a tool of another key; `REPR_JAVA_STALE_OK=1` overrides.
- **`bin/repr-java javac`** (new) runs javac's command line on the selected host. The round trip's
  classes level and `jrt-convert`'s `javac` step use it.
- **The platform** grows by jdk.compiler, java.compiler, jdk.internal.opt and jdk.zipfs (JFS31
  (a)): `jdk/javac/platform.sha256` has 10 lines.
- **The verifies** compare the provenance except `:host` (JFS32 (b)).
- **The round trip's corpora** run concurrently (JFS34 (b)).
- **`bin/gate --full`** (JFS9, JFS29):
  - the JVM chain keeps only `j2g-edge`;
  - the `javac` step runs check, build, the tool copied into `.tmp/gate-javac`, `verify` on the
    tool, then `classes`;
  - a java chain after it runs `repr-java`, `repr-java-roundtrip`, `jrt-convert` and
    `java-fixtures` on the tool;
  - `host-check` leaves the gate but stays a command (JFS36 (a)).
- **A fix:** `bin/jrt-convert verify` had failed since JN2 ("Only in jdk: javac"). It now leaves
  out `jdk/javac`.
- **Re-recorded:** the three provenance files, written on the tool. Only `:repr-java` and `:host`
  change; the forms, the edges and the round trip's references are unchanged.
- **Checks** on the tool, each command as the gate runs it:

  | check | time | result |
  |---|---|---|
  | `javac-vendor check` | 49 s | no failures |
  | `javac-vendor verify` | 94 s | 574 files |
  | `javac-vendor classes` (4 corpora) | 228 s | byte for byte |
  | `repr-java check --suite` | 178 s | as expected |
  | `roundtrip --corpus baseline,samples,jdk --text` | 543 s | every level as the references |
  | `jrt-convert verify` | 193 s | equal |
  | `--java-fixtures verify` | 20 s | equal |

  - The branch's essential gate passed (16m02s).
- **Speed:** the jdk reads are 3-3.6 times the JVM's. One process peaks at about 30 GB. The tool
  builds in 231 s with Go's cache warm, 1,006 s cold.
- **The shipped program:** no code change. `jdk/provenance.edn` changes the runtime key, so the
  version map changes; j2g's tree is identical.
- **On merging,** CLAUDE.md's conventions conflicted with the TMPDIR line and were merged by hand; the
  essential `bin/gate` on main at `e5923785` passed (14m36s).
- **What JN6 meets:** the edges' recordings, `check --suite`'s back end, `jrt-convert`'s sources'
  namespaces and the reference javac stay on the JVM. `check --jdk` still needs the JVM, since the
  platform holds 10 modules.

## 2026-10-21: JFS31-JFS36 decided as built (the user)

- JFS31 (a): one platform, grown by javac's four modules.
- JFS32 (b): the verifies compare all but `:host`.
- JFS33 (b): the tool is the default host, built by hand.
- JFS34 (b): the corpora run concurrently.
- JFS35: (a) now and (b) at JN6.
- JFS36 (a): `host-check` kept until JN6.

## 2026-10-21: JN6 started (the user: "start JN6 now")

- **What:** JN6, the JVM leaves repr/java, started on the branch `java-frontend-jn6` from main
  `02d2c34b`, in the worktree `.tmp/worktrees/java-frontend-jn6`, alongside GP5.
- **Its work:**
  - `bin/repr-java` without `bin/lib/frozen.bash`, and `frozen_view tools` without repr/java;
  - a trace showing that nothing of repr/java starts a JVM;
  - JFS35 (b);
  - each JVM-only remainder JN5 listed, either moved to the tool or kept with a reason.

## 2026-10-22: JN6 merged; JFS37-JFS42 decided as built (the user)

- **What:** JN6, the JVM leaves repr/java (branch `java-frontend-jn6`, `d173a4b6`, `78eb99ff`;
  doc/classes/JAVA-FRONTEND-SURVEY.md, "As built: JN6"), merged into main at `2b97f41e`.
- **Built:**
  - `bin/repr-java` no longer sources `bin/lib/frozen.bash`; the JVM's read and `host-check` are gone,
    `REPR_JAVA_HOST=jvm` stops with a message; the reader's JVM branch (`arbace/repr/java/javac.ae`)
    removed; `frozen_view tools` leaves out `arbace/repr/java`.
  - The provenance's helpers in `bin/lib/repr-java/host.bash`; `:jdk` names the platform by the hash
    of `jdk/javac/platform.sha256` (JFS35 (b)); the three provenance files rewritten on the tool,
    their forms unchanged.
  - `check --jdk` on the tool, the platform grown to the 68 modules of the build tree (JFS37):
    12,730 files, 0 failures, 14 min, 8.5 GB peak.
  - `bin/jrt-convert`'s source list, locales and report (`jrt.jrt-sources`) on the tools' host, the
    edge split into `test/jrt/jrt_edge.ae` on the frozen jar (JFS40).
  - Fixed: `test/repr/java/jdk.bash`'s root, wrong since `789b8534` (`check --jdk` could not run);
    `read --check`'s report for a module of `module-info.java` alone.
- **Trace:** `strace -f` over the affected `--full` steps' commands: no JVM exec under
  `bin/repr-java`; main's `REPR_JAVA_HOST=jvm` read as the control (flagged). What stays on the JVM:
  the frozen jar's oracle reference, edges, `check --suite`'s back end, `jrt.jrt-edge` and the data
  generators; a JDK's javac as the reference of `bin/javac-vendor classes`.
- **Checks** (each `--full` step's command alone, on the tool `811e563f`): `javac-vendor check`,
  `verify`, `classes` (4 corpora), `repr-java check --suite`, `roundtrip --text`, `jrt-convert
  verify`, `--java-fixtures verify`, `j2g-edge verify`, `check`, `check --jdk`: all pass. The
  essential `bin/gate` on main at `2b97f41e` passed (18m23s).
- **The shipped program:** no code change; `jdk/provenance.edn` moves the runtime key, so the
  version map. Re-seed batched with GP5's.
- **Rule slips:** a wait loop with `pgrep -f` (read-only, stopped by task ID); the control's JVM wrote
  one report to /tmp, deleted.
- **Decided (the user):** JFS37 (a), JFS38 (b), JFS39 (b), JFS40 (a) now and (c) with JN7, JFS41 (a),
  JFS42 (a); recommended over a second platform, an opt-in JVM reference, `check --jdk` in `--full`
  and hashes in place of the JDK's javac.

## 2026-10-22: GP5 merged; GPS32-GPS37 decided as built; re-seed now (the user)

- **What:** GP5, the native reader the default (branch `go-parser-gp5`, `aa9835ee`, `a20dad43`;
  doc/go/GO-PARSER-SURVEY.md, "As built: GP5"), merged into main at `a58eae7e` without conflicts.
- **Built:**
  - `bin/repr-go read` native by default (`--reader helper`, `REPR_GO_READER=helper`: the helper);
    both readers write the same forms, lines, report and `OUT.tsv` summary; `arbace.repr.go/report`
    shared; `--reader native --trees` the dumps alone.
  - `native-check`'s E-forms: both readers' `.ae` text byte for byte, `:full` and `:lines`; where the
    helper's conversion fails, the native one with the same message.
  - GPS25 (b): an instantiation's operand keeps its parentheses, an invalid conversion's `...` kept as
    `(conv T (spread x))`, `(spread x)` on its operand's line; `conform`'s three exceptions gone.
  - GPS20 (b): the goexperiment tool tags the target's, helper and loader (darwin/arm64 alone changes,
    no forms).
  - The vendoring tools and goi (`check`, `annotations`, the table's linux std) on the native reader;
    tamago-vendor's "every package converted" was vacuous, fixed.
  - goi's checker, resolve mode only: go/types' records of invalid programs (GPS36).
- **Results:** E-forms over 22 corpora: 12,275 dumps, E-tree 12,275, E-ann and E-forms 12,158 of
  12,158; `native-known.edn` empty; 64 failing error tests fail alike; 53 goos packages without an
  oracle. tamago-, toolchain-, x509roots-vendor verify byte for byte natively; box-vendor roundtrip
  41 of 41; every repr/go round trip without regression. goi: `ddd1.go` now passes (references
  updated); four run programs time out at 10 s under load, as main's executable does; divergences
  1,472 of 1,837 pass. The native path is 1.4-3 times the helper's (87 s for `fmt`).
- **Gates:** the branch's essential gate passed twice; on main at `a58eae7e` (after JN6) it passed
  (29m32s).
- **Rule slips:** `bin/repr-go` edited while a run of it executed (the run passed, bash then failed
  after it); a wait loop with `pgrep -f` (read-only, stopped by task ID).
- **Decided (the user):** GPS32-GPS37 as built (a), GPS36 (b) at GP6; over a committed package list,
  the 53 as known, a disk cache of std's sessions and the helper as default until GP7.
- **Re-seed now** (the user, over batching with GP6/JN7): GP5's converter and checker changed, and
  JN6's `jdk/provenance.edn` moved the runtime key.

## 2026-10-22: seed a8672e0d; GP6 and JN7 started (the user)

- **Re-seed** (the user: now, over batching): `bin/arbace-go --fixpoint` on main at `7315ca94`
  (host `4f32a6af…` != G1; G1 = G2 = G3 = `c60d5659…`, 149,332,128 bytes), then
  `ARBACE_SEED_GO_BUILD=$PWD/target/arbace-go/fixpoint/g3 bin/seed --make`: `a8672e0d…` (18,499,044
  bytes, 1,954 files), committed at `6e428346`; `bin/seed --verify` passed: stage 0 = G1 = G2 = G3 =
  `c60d5659…`, the same tar.
- **Decided (the user):**
  - next: GP6 (syntax errors in gc's words, invalid programs for goi) and JN7 (the generators: `bin/jrt-convert
    generate`'s tools read by repr/java on Arbace and run as Java forms, and JFS40 (c), a class-file reader
    of Arbace's own for `jrt-convert`'s edge) together, over GP6 then GP7, GP6 alone or a pause;
  - `bin/gate --full` at the next milestone, over now;
  - no milestone tag yet, over a tag after `--full`.
- GP6 on the branch `go-parser-gp6`, JN7 on `java-frontend-jn7`, both from main after this entry, in
  `.tmp/worktrees/`.

## 2026-10-22: GP6 merged; GPS38-GPS44 decided as built (the user)

- **What:** GP6, syntax errors in gc's words and invalid programs for goi (branch `go-parser-gp6`,
  `e351b51b`, `35ef6af5`, `f709bdcb`; doc/go/GO-PARSER-SURVEY.md, "As built: GP6"), merged into main
  at `475f84e3` without conflicts.
- **Built:**
  - `arbace.repr.go.syntax` (`arbace/repr/go/syntax.ae`): goi runs gc's `cmd/compile/internal/syntax`
    from `tamago/cmd`'s forms with a driver of Go forms parsing as gc's noder does, errors ordered and
    printed as gc's `-C`; `bin/repr-go syntax-errors`; the native read's summary gives gc's syntax
    errors for a file go/parser rejects.
  - `arbace.go.run --errorcheck --raw`: goi checks raw forms in the resolve mode; `raw-lines`
    rebuilds lines from mode `:full`'s positions; `(go/package _ ...)` in raw mode.
  - `bin/goi check`: error tests judged on gc's syntax errors first (GPS38); programs the converter
    cannot convert judged on their raw forms.
  - The checker: GPS36 (b), go/types' records of invalid programs in the normal mode too; new types2
    errors for GC47's programs; three `-lang` bugs found by GPS33's revisit fixed.
  - Records: `check-linux-amd64.edn` re-recorded, E1's 4 known, `divergences.edn` reclassed,
    `essential.txt` 797 programs.
- **Results:** `bin/goi check` 1,604 of 1,837 pass (was 1,472), 145 unsupported (was 281), 70 known,
  none fail; syntax errors 66 of 70 (E1's 4 known); GC47 63 of 64 (`weak.go` unsupported); the
  gc-phases class 18 (was 21). `native-check` over the five corpora concerned: 3,328 dumps, 0 differ.
  `annotations` as recorded.
- **Gates:** the branch's essential gate passed (14m58s); on main at `475f84e3` it passed (22m28s).
- **The shipped program changed** (`arbace.go.check`, `arbace.go.run`, `arbace.repr.go.annotate`,
  `.convert` in raw mode, the new `.syntax`): re-seed batched with JN7's.
- **Decided (the user):** GPS38-GPS44 as built: 38 (a), 39 (a) now and (b) with 41 (c), 40 (a), 41 (a)
  with (b) when needed and (c) as a step of its own, 42 (a), 43 (a), 44 (a); over porting the pragmas
  now and no raw read in the essential gate.
