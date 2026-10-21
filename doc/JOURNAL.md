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
