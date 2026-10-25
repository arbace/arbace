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

## 2026-10-22: GP7 started (the user: "start GP7")

- **What:** GP7, the native reader compiled (doc/go/GO-PARSER-SURVEY.md, §8): go/parser, go/scanner,
  go/ast and go/token in goi's table, the dumper as Go forms in `go/`, gc's syntax as a renamed copy,
  GPS3 (b) if measured worthwhile; exit: the parse over std under 1 s, the executable's growth
  recorded, the essential gate. On the branch `go-parser-gp7` from main `8677987a`, in
  `.tmp/worktrees/go-parser-gp7`, beside JN7.
- **The re-seed** GP7 needs is done on main after its merge, batched with GP6's and JN7's.

## 2026-10-22: JN7 merged; JFS43-JFS50 decided as built; re-seed with GP7 (the user)

- **What:** JN7, the generators on Arbace, and JFS40 (c) (branch `java-frontend-jn7`, `efc39ada`,
  `36dc57ba`, `09c38b3a`; doc/classes/JAVA-FRONTEND-SURVEY.md, "As built: JN7"), merged into main at
  `4e825839` without conflicts.
- **Built:**
  - The JDK build's tools (generatecharacter, spp, generateextraproperties, cldrconverter,
    generatelsrequivmaps, tzdb, generatecurrencydata, generatebreakiteratordata, makejavasecurity)
    read by `bin/javac-vendor` into `jdk/javac/forms` (module `buildtools`), with 221 java.xml and 6
    java.base files (`overlay/jdk/javac/closure.txt`); `bin/repr-java read --origins`.
  - zlib 1.3.2's deflate ported to Java (`overlay/jdk/javac/java.base/java/util/zip/Deflater.java`;
    zlib's license in LICENSE.md); tool-world variants (`ResourceBundle`, `Files.walk`, `Scanner`,
    java.xml's `SecuritySupport`, `JdkXmlConfig$CatalogHolder`).
  - j2g fixes: binary names for the tool's packages; a superinterface's static field initializes
    the interface (regression `test/j2g/fixtures/FxJn7.ae`); values and calls of classes outside the
    closed world.
  - `bin/jrt-convert generate` on javac's tool executable; `GEN_JDK` removed, the provenance's
    `:generators`.
  - JFS40 (c): `test/jrt/classfile.ae` (`jrt.classfile`), `jrt.java-surface` and `jrt.jrt-edge` on it,
    the edge step on the tools' host.
- **Results:** the generated sources and data equal the JDK build's (30 of 31 sources and 10 of 10
  data files; `JRT_LOCALES=all` 1,561 of 1,562), but CaseFolding, whose build ran on boot JDK 25 (28
  entries more; the tool's is JDK 26's): the build's file kept (JFS43 (a)), `jdk/forms` unchanged.
  The ported edge equals the frozen JVM's but 3 JFR members (JFS44). The generate step 44-62 s on the
  tool (36-42 s on the JDK); the tool 209 MB.
- **Trace:** `bin/jrt-convert verify` and `bin/repr-java roundtrip` start no JVM (JN6: 100 and 97);
  `javac-vendor classes` only the reference javac. On a JVM remain the oracle, the edges'
  recordings, `check --suite`'s back end, j2g's fixtures' results, the data generators, the JDK's
  javac.
- **Checks:** `jrt-convert update`/`verify`, `javac-vendor convert`/`verify`/`check`/`classes`,
  `--java-fixtures`, `j2g-edge verify`, `repr-java check` and `roundtrip --text`: all pass. The branch's
  essential gate passed (after diffcheck's FxVarHandles case renumbered); on main at `4e825839` it
  passed (15m35s).
- **The shipped program:** j2g's code changed (one call in `java.security.MessageDigest` a missing
  call, the same behaviour); `jdk/provenance.edn` moves the runtime key.
- **Rule slips:** five reports to /tmp before TMPDIR was set (one from `bin/j2g-check
  --record-fixtures`, whose JVM gets no `java.io.tmpdir`), deleted; `pgrep -n -x` in a wait loop;
  `jdk/README.md` edited during checks, reverted then re-applied; the system JDK used for three
  scratch measurements, recorded in the survey.
- **Decided (the user):** JFS43-JFS50 as built; over a tree of the tools' own and the generators' JDK
  kept as a reference. The re-seed batched with GP7 (GP6, JN7 and GP7 in one), over re-seeding now.

## 2026-10-22: the frozen jar's JVM writes its reports to $TMPDIR (the user: "do JN7's one-line fix")

- **What:** `bin/lib/frozen.bash`'s `frozen_java` passes `-Djava.io.tmpdir="$TMPDIR"` (when set, as
  `bin/lib/tmpdir.bash` sets it) before the caller's options, so every frozen-jar JVM's error reports
  (`arbace-*.edn`) land in `.tmp/tmp`, not /tmp; a caller's own `-Djava.io.tmpdir` (`bin/javac-vendor`,
  `bin/clojure-tests`) comes later and wins. JN7 found `bin/j2g-check --record-fixtures`'s JVM without
  it. Fixed in the one function rather than in `bin/j2g-check` alone, which covers every caller.
- **Checked:** a throwing `frozen_java jar -- -e ...` with `TMPDIR=.tmp/tmpdir-test` wrote its report
  there, none to /tmp. No gate needed: `bin/lib/frozen.bash` is in no build's key and the frozen jar's
  outputs do not depend on its temp directory.
- **A slip of mine:** the test's command line redirected a `source`'s stderr to /dev/null, against
  the standing rule; nothing was lost (the line printed nothing).

## 2026-10-22: JFS48 (b) started; the Go plan stops after GP7; no milestone yet (the user)

- **Decided (the user):**
  - Java: JFS48 (b) now, javac's generated sources (`make/langtools/tools`' `compileproperties`
    and `propertiesparser`) run on javac's tool executable by `bin/javac-vendor`, `FlagsEnum` still
    copied; over JN8 now, JFS48 (b) then JN8, or nothing yet.
  - Go: stop after GP7 (with its re-seed), over GP8 next or GP8 with the pragmas.
  - `bin/gate --full` and a tag later, over after GP8 or after GP7.
- JFS48 (b) on the branch `java-frontend-gensrc` from main, in `.tmp/worktrees/java-frontend-gensrc`,
  beside GP7.

## 2026-10-22: JFS48 (b) merged; JFS51-JFS54 decided as built (the user)

- **What:** javac's generated sources made by `make/langtools/tools`' compileproperties and
  propertiesparser run on javac's tool executable (branch `java-frontend-gensrc`, `121343ec`;
  doc/classes/JAVA-FRONTEND-SURVEY.md, "As built: JFS48 (b)"), merged into main at `e02b7485`.
- **Built:** the tools' 8 files read into `jdk/javac/forms` (module `buildtools`, KIND `langtools`);
  `bin/javac-vendor convert` generates first (`version.properties` from the build's `spec.gmk`,
  CompileProperties over 17 `.properties`, PropertiesParser over `compiler.properties` and
  `launcher.properties`), every output compared with the build tree's; KIND `gensrc` generated,
  `gensrc-build` (`FlagsEnum`) copied; `:generated` in the provenance; `JAVAC_VENDOR_BOOTSTRAP=1` for
  a tool without the generators (used once: the tree then a fixpoint). j2g's `names.ae` gains the two
  prefixes.
- **Results:** 20 of 20 generated files the build tree's byte for byte; `jdk/javac/edge.edn`
  unchanged; the tool 210,055,328 bytes; the generate step 1 s. A trace of `bin/javac-vendor verify`:
  only the frozen jar's 4 edge-recording execs, no `javac`.
- **Checks:** `javac-vendor convert`/`verify`/`check`/`classes` (4 corpora), `repr-java check`,
  `jrt-convert verify`, `make check-makefile`: pass. The branch's essential gate passed (31m03s); on
  main at `e02b7485` it passed (18m34s).
- **The shipped program:** j2g's compiled `names.ae` alone; the translated runtime cannot change. In
  the batched re-seed with GP7's.
- **Decided (the user):** JFS51 (a), JFS52 (a), JFS53 (a), JFS54 (a), as built; over flagsgenerator
  next and package jrt for the tools now.

## 2026-10-22: GP7 merged; GPS45-GPS49 decided as built (the user); the re-seed started

- **What:** GP7, the native reader compiled (branch `go-parser-gp7`, `b58e7c48`, `c33263e6`,
  `e73ef3e7`, `01cc886b`, `bbb84c11`; doc/go/GO-PARSER-SURVEY.md, "As built: GP7"), merged into main
  at `97e34a6a` without conflicts.
- **Built:**
  - `go/arbace/gotable.edn` lists go/ast, go/parser, go/scanner, go/token, `arbace/repr/go/godump`
    and `arbace/repr/go/gcsyntax`.
  - The dumper committed as generated Go forms (`go/arbace/repr/go/godump.ae`, `godump/`; GPS13
    (b)), written by `arbace.repr.go.parse generate`.
  - The syntax-errors driver moved into `go/arbace/repr/go/gcsyntax`.
  - gc's syntax as a renamed copy, `go/arbace/repr/go/internal/syntax` (17 files), from
    `tamago/cmd/go/cmd/compile/internal/syntax`: TamaGo's go1.27.1 `src/cmd/compile/internal/syntax`,
    only its namespace and import path changed (`arbace.repr.go.syntax/copy-texts`; LICENSE.md).
  - The readers' `load!` a `require`: compiled where the table lists the package, else interpreted
    from ARBACE_PATH.
  - `bin/generated-check` also compares repr/go's 19 generated files.
  - A prefetch of imported packages, bounded by `REPR_GO_JOBS` permits (`REPR_GO_PREFETCH=0`), and
    a phases line in `bin/repr-go read`.
- **Results:** std's parse 0.5 s on one thread (GP1 interpreted: 25.2 s), the dump 6.5 s; the
  executable 149,749,920 → 150,814,880 bytes (+1,064,960; go/parser's four packages 221,184 of it).
  `read fmt unicode/utf8` 33-45 s (GP5: 87 s; the helper 5.1 s); `roundtrip amd64` 183 s (GP5: 447 s);
  `native-check` 107 min (GP5: 110). The tools' host is stage 0, so the speed comes with the re-seed.
- **Checks:** `native-check` over the 22 corpora on the GP7 executable (E-tree 12,275, E-ann and
  E-forms 12,158 of 12,158); the reader's output unchanged; `roundtrip amd64` no regression; goi
  `check --essential` 797 of 797, `annotations`, `test`; `generated-check`. The branch's essential
  gate passed; on main at `97e34a6a` it passed (31m36s).
- **Slips:** the first native-check, with the prefetch unbounded, was killed by the memory limit (its
  own process); runs at 32-64 jobs on the shared machine.
- **Decided (the user):** GPS45-GPS49 as built; over the syntax renamed at build time, a smaller
  table and profiling the raw conversion next.
- **The re-seed** (GP6, JN7, JFS48 (b), GP7) follows on main.

## 2026-10-22: seed d7303cf8 (GP6, JN7, JFS48 (b), GP7)

- `bin/arbace-go --fixpoint` on main at `bdd61ecf` (host `c60d5659…` != G1; G1 = G2 = G3 =
  `0fdfb00d…`, 150,823,072 bytes), then `ARBACE_SEED_GO_BUILD=$PWD/target/arbace-go/fixpoint/g3
  bin/seed --make`: `d7303cf8…` (18,354,656 bytes, 1,984 files), committed at `3ed72c96`;
  `bin/seed --verify` passed: stage 0 = G1 = G2 = G3 = `0fdfb00d…`, the same tar. The tools' host
  (stage 0) now carries the compiled native reader (GP7) and GP6's checker.

## 2026-10-22: JFS52 (b) started; the Go plan keeps holding (the user)

- **What** (the user: "do JFS52 (b)", after the server's reboot): `FlagsEnum` generated by
  `make/langtools/tools/flagsgenerator` on javac's tool executable, with a tool-world `ToolProvider`
  variant returning javac's `JavacTool` and javac on Arbace's file manager, so that nothing of
  jdk.compiler's generated sources is copied from the build tree. On the branch
  `java-frontend-flags` from main, in `.tmp/worktrees/java-frontend-flags`.
- **Decided (the user):** the Go plan keeps holding after GP7, over GP8 now, profiling the raw
  conversion (GPS49 (c)) or noder's pragmas (GPS41 (c)); the Java step after JFS52 (b) decided after
  it reports, over JN8 queued.

## 2026-10-22: JFS52 (b) merged; JFS55-JFS57 decided as built; JFS58 (a) done (the user)

- **What:** `FlagsEnum` generated by flagsgenerator on javac's tool executable (branch
  `java-frontend-flags`, `04b2b078`, `685b64e4`; doc/classes/JAVA-FRONTEND-SURVEY.md, "As built:
  JFS52 (b)"), merged into main at `5b3fa825`.
- **Built:** `make/langtools/tools/flagsgenerator/FlagsGenerator.java` read into module `buildtools`
  and run in `convert`'s generate step with the build's command line; flagsgenerator compiles
  `Flags.java` through javax.tools, so `overlay/jdk/javac/jdk.compiler` gains
  `jdk.internal.jrt.javac.SystemCompiler` (a `JavaCompiler` over JavacTool's tasks, run by javac on
  Arbace's `Main`) and `StandardFileManager`, the platform from `jdk.internal.jrt.javac.platform`;
  `overlay/jdk/javac/variants/ToolProvider.ae`; j2g's `names.ae` gains `flagsgenerator/`; `convert`
  reads the tool's key before writing (JFS57 (b): the bootstrap had written `:host ""`).
- **Results:** 22 of 22 generated outputs the build tree's byte for byte, none copied; the tree a
  fixpoint (bootstrap, then a tool from the result, its conversion the same); the tool 211,210,400
  bytes. A trace of `bin/javac-vendor verify`: the tool for the three generators and 6 reads, `java`
  only for the frozen jar's 4 edge-recording execs, no `javac`.
- **Checks:** `javac-vendor convert`/`verify`/`check`/`classes` (4 corpora), `jrt-convert verify`,
  `repr-java check`, `make check-makefile`; the branch's essential gate (14m19s); on main at `5b3fa825`
  the essential gate passed (13m21s).
- **Slip** (the agent's): a wait loop with `pgrep -x heavy` and a `/dev/null` redirect, stopped by
  its task ID.
- **JFS58 (a)** (the user: "do JFS58 if appropriate"): checked first: `bin/j2g-edge verify` on main
  failed, `jdk/edge.edn`'s `:exists` lacking `java.util.concurrent.ExecutionException`, `FutureTask`,
  `Semaphore` and `java.util.concurrent.atomic.LongAdder`, which GP7's `arbace/repr/go/annotate.ae`
  asks for (GP7's merge ran neither verify: they are `--full`'s). `bin/j2g-edge update` (the 4 added),
  `bin/javac-vendor build` and `convert` (javac's edge loses its `:exists`; the provenance's
  `:jdk-edge` and `:host`), `build` again, `bin/javac-vendor verify` (918 files byte for byte) and
  `bin/j2g-edge verify`: pass; the essential gate passed (12m01s).
- **Decided (the user):** JFS55 (a), JFS56 (a), JFS57 (b) as built, over package jrt for the tools
  now; JFS58 (a).
- **The shipped program changed** (j2g's `names.ae`; `jdk/edge.edn`, j2g's data): a re-seed is due.

## 2026-10-22: seed 3ab3b603 (JFS52 (b), JFS58; the user: re-seed now)

- `bin/arbace-go --fixpoint` on main at `1a80bf75` (host `0fdfb00d…` != G1; G1 = G2 = G3 =
  `2e4ce3e9…`, 150,827,168 bytes), then `ARBACE_SEED_GO_BUILD=$PWD/target/arbace-go/fixpoint/g3
  bin/seed --make`: `3ab3b603…` (18,349,996 bytes, 1,984 files), committed at `c4f5c8ba`;
  `bin/seed --verify` passed: stage 0 = G1 = G2 = G3 = `2e4ce3e9…`, the same tar. The user chose
  now over batching with the next work.

## 2026-10-23: JN8 and GP8 started; bin/gate --full alongside (the user)

- **Decided (the user):**
  - Java: JN8, the native Java parser (doc/classes/JAVA-FRONTEND-SURVEY.md, JF8 = JN8:
    `arbace.repr.java.parse`, Java 26's lexer and parser in Arbace, javac's trees as data, checked
    against javac on Arbace over jdk26u's 68 modules, the corpora and javac's sources), over the
    cleanups for the build tree's leaving (JFS43 (c), JFS51 (c), JFS55 (c)) and holding.
  - Go: GP8, the box reads Go (doc/go/GO-PARSER-SURVEY.md, §8: `tamago/goroot`'s conversion in the
    box byte for byte the committed forms, on amd64 and the emulated arm64 VM), over profiling the
    raw conversion, noder's pragmas and holding.
  - `bin/gate --full` now on main, alongside the two, over before new work and later: its first run
    since the tag `arbace-interprets-go1.27.1-v1`.
- JN8 on the branch `java-frontend-jn8`, GP8 on `go-parser-gp8`, both from main after this entry, in
  `.tmp/worktrees/`; nothing is merged into main while its `--full` runs.

## 2026-10-23: bin/gate --full at a138539a failed; GP7's forms check fixed

- **The run** (the user: `--full` alongside JN8 and GP8): `bin/gate --full` on main at `a138539a`, its
  first since the tag `arbace-interprets-go1.27.1-v1`, failed in 73m38s. Passed: the essential steps,
  `j2g-edge`, `go-build-full`, `box-forms`, `box-smoke`, `box-oracle`, `box-suite`, `fixpoint`,
  `box-goi`, `repr-haskell`, `repr-rust`, `repr-clojure`. Failed:
  - `javac`: j2g's run of javac's tool build killed by the kernel's OOM killer (18 GB resident), so
    `repr-java`, `repr-java-roundtrip`, `jrt-convert` and `java-fixtures` did not run;
    `repr-go-native`: `native-check --fresh` killed likewise (21 GB), every corpus it finished equal.
    The machine (62 GB) was shared with JN8's and GP8's agents.
  - `goi`: `nosplit.go` timed out at 10 s (the load, as GP5 and GP6 recorded).
  - `repr-go` (`bin/box-vendor roundtrip`) and `toolchain` (`bin/toolchain-vendor verify`): a bug of
    GP7's. `arbace.repr.go.parse/forms-check` compared the dumper's committed files
    (`go/arbace/repr/go/godump`) by paths relative to the working directory; the vendoring tools read
    from their modules' directories, so the files were missing there, the check printed "NOT as
    generated" and `bin/repr-go read` exited 1 after a conversion with no failure. Deterministic (the
    box's round trip failed alone too); GP7's merge ran neither step (`--full`'s).
- **The fix:** `forms-check` takes the committed files under the checkout that holds its goroot forms
  (ROOT/tamago/goroot, which `bin/repr-go` passes absolute). `bin/box-vendor roundtrip` passes,
  `bin/toolchain-vendor verify` (`tamago/cmd` byte for byte) passes, and the essential gate passed
  (22m03s). The namespace is embedded: the next re-seed carries it.
- **Still to run:** the steps the OOM killer stopped and `goi`'s check, on a quieter machine;
  then `--full` again.

## 2026-10-23: JN8 merged; JFS59-JFS66 decided as built; JFS66 (b) next (the user)

- **What:** JN8, the native Java 26 parser (branch `java-frontend-jn8`, `60def626`, `bcbc7a8a`,
  `7062dc96`, `fdfb0fd5`, `35ef87e6`; doc/classes/JAVA-FRONTEND-SURVEY.md, "As built: JN8"), merged
  into main at `737d0dc7` without conflicts.
- **Built:** `arbace/repr/java/lex.ae` (533 lines) and `parse.ae` (2,881), a method-by-method port of
  jdk26u's javac `JavacParser`, `JavaTokenizer` and the parts of `TreeInfo` and `TreeMaker` they use
  (jdk26u at `baf63fbe42b8758448fee570a9d9bb914272d259`, `src/jdk.compiler/share/classes/com/sun/tools/javac/parser/`
  and `tree/`; Oracle's GPLv2 with the Classpath Exception, LICENSE.md, doc/ARBACE.md); the trees as
  maps with javac's tags, positions, end positions and fields; `arbace/repr/java/jtree.ae` (javac on
  Arbace's trees as the same data, the comparison, seeded mutants); `javac.ae`'s `parse-only`;
  `bin/repr-java parse`, `parse-check` (`test/repr/java/parse-check-known.edn`, empty); `parse-check`
  in `bin/gate --full`'s java chain.
- **Results:** equal trees, positions included: samples 14, baseline 183, jdk 838, javac's sources
  824, jdk26u's 68 modules 12,809, 8,000 mutants (66,598 javac errors), jdk26u's tests 37,797; six
  error-recovery behaviours fixed on the way. 199 s for the whole check. The parser compiled in the
  tool about 9 times javac on Arbace's time (java.base 50.5 s against 5.6 s), 17% of it in j2g's
  checked array casts.
- **Checks:** `parse-check` over all corpora, `bin/repr-java check`, the three updates (forms byte for
  byte, only the provenance's `:repr-java` and `:host` moved), `make check-makefile`; the branch's
  essential gate (16m41s); on main at `737d0dc7` the essential gate passed (14m11s).
- **The shipped program:** no code; `jdk/provenance.edn`'s `:repr-java` moves the runtime key (the
  version map). Batched into the next re-seed.
- **Slips** (the agent's): one `> /dev/null 2>&1` in a profiling command; two reports to /tmp before
  TMPDIR, deleted.
- **Decided (the user):** JFS59-JFS66 as built; over embedding the parser now, a smaller `--full`,
  JN9 straight away or stopping after JN8. Next on Java: JFS66 (b), over j2g's array casts and
  holding until `--full`.

## 2026-10-23: JFS66 (b) merged; JFS67-JFS71 decided (the user); JFS70 started

- **What:** the converter on an interface over the tree and an attribution interface keyed by
  position (branch `java-frontend-jfs66`, `3a94ee54`, `432d05a4`, `39771c4c`;
  doc/classes/JAVA-FRONTEND-SURVEY.md, "As built: JFS66 (b)"), merged into main at `0f84b77d`.
- **Built:** `arbace/repr/java/tree.ae` (the syntax over JCTrees or the native trees as data),
  `attr.ae` (19 attribution functions; for native trees `attr/index` pairs each node with javac's of
  the same tag and position), `split.ae` (the catalogue of 70 fact sites and the split); `convert.ae`
  on 575 tree and 159 attribution calls; `read`/`read-jdk --front javac|native|both`, `--split`;
  `bin/repr-java front-check` (`test/repr/java/front-check-known.edn`), in `--full`'s java chain.
- **Results:** the native trees with javac's attribution give the current converter's forms: samples
  17, baseline 189, jdk 911, javac's sources 1,828; jdk26u's 13,788 but 3 files, one param-tags pin
  each, which javac's own trees differ on across runs (class infos cached while a class is half
  completed: JFS70); 8,872,827 nodes paired. The split over jdk26u's 2,258,047 lists by the most
  demanding tier asked: syntax 0.6%, + scope 2.3%, + declarations 15.0%, + members 16.0%, + types
  68.7%, + overload 98.3%, + inference 100%; statically 79 of `convert.ae`'s 243 forms syntax only.
  JN9 re-estimated at 19-34 days (§7: 25-45).
- **Checks:** `front-check --corpus all`, `repr-java check`, `parse-check`, the three updates byte for
  byte (the provenance's `:repr-java` and `:host` moved); the branch's essential gate (17m37s); on
  main at `0f84b77d` it passed (13m29s).
- **The shipped program:** no code; the runtime key moves with `jdk/provenance.edn`. Batched.
- **Slips** (the agent's): a tool build ran while `convert.ae`, in its key, was being edited (it
  failed; later runs on settled sources); `/dev/stdin` passed as an input path once.
- **Decided (the user):** JFS67 (a), JFS68 (a), JFS69 (b) (JN9 by tiers, JN9a first; over stopping and
  the whole JN9 at once), JFS70 (b) with `tree.ae`'s notice, JFS71 within JN9a. JFS70 first (both
  touch repr/java), on the branch `java-frontend-jfs70` from main.

## 2026-10-23: JFS70 (b) merged; JFS72 (b) decided; JN9a after all settles (the user)

- **What:** `resolve.ae`'s param-tags pins made independent of the javac run's history (branch
  `java-frontend-jfs70`, `af0237e3`, `3150a539`, `01cb107e`, `aeb4ed47`, `ff7a68ba`, `5343df47`;
  doc/classes/JAVA-FRONTEND-SURVEY.md, "As built: JFS70"), merged into main at the merge after
  `ba707b05`.
- **Cause:** `(first (Symtab/getClassesForName n))` and a by-name registry for the whole run. javac's
  `ClassReader.readInnerClasses` enters a nested class's symbol for each other module whose class
  file names it; those have no class file, fail to complete and stay erroneous (no superclass, no
  members), and `getClassesForName` returns them in a HashMap order of module identity hashes. An
  info made from one made an enum constant no `Enum`, so the converter pinned javac's choice.
- **Fix:** `canonical` (the run's source class by `Check.getCompiled`, a local or anonymous class, else
  `Symtab.getClass` in its package's module); `find-class` (the run's source class in the caller's
  module first, else the symbols their modules own, the caller's module first); `class-info` only from
  a symbol that completes, cached per symbol; the registry removed. A first version that asked Symtab
  first pinned 7 files of `jdk/forms` (local classes of the patched java.base), fixed before the updates.
- **Results:** two fresh runs agree over all 68 modules; jdk.javadoc with 1, 4 and 12 chunks the same;
  `front-check-known.edn` `{}`. No committed form changed (the three updates byte for byte, only the
  provenance's `:repr-java` and `:host`); in jdk26u (not committed) about 10 wrong pins gone,
  6 of which front-check could not see (both conversions alike): hence JFS72.
- **tree.ae:** jdk26u's notice for `skip-parens`, `tree-name`, `full-name`, `diamond?`, `class-kind`
  (`TreeInfo.java`'s `skipParens`, `name`, `fullName`, `isDiamond`; `JCTree.java`'s
  `JCClassDecl.getKind`, at `baf63fbe42b8758448fee570a9d9bb914272d259`,
  `src/jdk.compiler/share/classes/com/sun/tools/javac/tree/`); LICENSE.md, doc/ARBACE.md.
- **Checks:** `check --jdk` (68 modules, 0 with failures), `front-check --corpus all`, `repr-java check`,
  `parse-check`; the branch's essential gate (17m59s); on main the essential gate passed (13m31s).
- **Slip** (the agent's): `cmp` against a process substitution (`/dev/fd/63`), then temp files.
- **Decided (the user):** JFS72 (b), jdk26u's forms as hashes compared by `check --jdk`, over a
  two-run comparison and neither; JN9a (with JFS71, JFS72) after GP8, `--full` and the re-seed have
  settled, over starting now.

## 2026-10-23: GP8 merged; GPS50-GPS56 decided as built; bin/gate --full at a1be9220 (the user)

- **What:** GP8, the box reads Go (branch `go-parser-gp8`, `5c86ea8d`, `5a0b7e16`, `45bab282`,
  `09fd87d0`, `3d58e820`, `7eb397b3`; doc/go/GO-PARSER-SURVEY.md, "As built: GP8"), merged into main
  at `a1be9220` without conflicts: the Go parser plan's last milestone.
- **Built:** `arbace/box/convert.ae` (`arbace.box.convert`, embedded): GOROOT/src's digest checked in
  the box against `bin/tamago-vendor`'s pin, two guests reading std natively for tamago/amd64 and
  tamago/arm64 at once, a third assembling and merging (a port of `bin/tamago-vendor`'s three Python
  programs, which the script now calls too); `bin/box-convert` (`prepare`, `box`, `host`, `compare`;
  `make box-convert`).
- **Results:** amd64 from empty stores: 166.7 s and 20.0 GB peak (16 vCPUs a read; the host 154.9 s),
  the tree `0445b850…`, 2,269 files, equal to the committed `tamago/goroot` and the host's. The emulated
  arm64 VM: the whole of std impractical (about 180 times the amd64 box, an estimated 17 h or more);
  eight packages for both configurations (33 files each) equal to the amd64 box's whole run, 12,429 s.
  `bin/tamago-vendor verify` on the port: byte for byte. The program +778,240 bytes.
- **Gates:** the branch's essential gate (12m18s); on main at `a1be9220` it passed (12m34s).
- **Decided (the user):** GPS50-GPS56 as built; over Python kept on the host, the whole tree in the VM
  and profiling the emulation first. The user: "do not start JN9a yet".
- **`bin/gate --full` at `a1be9220`** (no agents running) failed in 121m00s. Passed: the essential
  steps, `go-build-full`, `box-forms`, `box-smoke`, `box-oracle`, `box-suite`, `fixpoint`, `repr-go`,
  `repr-go-native` (80m23s: every corpus, E-forms), `box-goi`, repr/haskell, repr/rust, repr/clojure,
  `toolchain`: the forms-check fix holds. Failed:
  - `j2g-edge` and `javac`'s verify: `jdk/edge.edn`'s `:exists` lacked `java.nio.file.OpenOption`,
    which GP8's `arbace.box.convert` asks (JFS58 again: an embedded namespace's new JDK class, caught
    only by `--full`); the four repr/java steps behind them did not run. Fixed as JFS58 (a):
    `bin/j2g-edge update`, `bin/javac-vendor build`, `convert`, `build`, `verify` (918 files byte for
    byte), `bin/j2g-edge verify`; the essential gate passed (16m47s). Then the java chain's commands
    alone: `check --suite` (no regressions), `jrt-convert verify`, `--java-fixtures verify`,
    `parse-check`, `front-check`: pass; `roundtrip --corpus baseline,samples,jdk --text`: fails (below).
  - `goi`: `nosplit.go` over the check's 10 s again, the machine running only `--full`.
  - `roundtrip --text`, jdk's record level: mode `:full`'s forms of `java/util/ResourceBundle.ae`
    (overlay/jdk/java.base) lack 3 of its 22 members (both `getBundle` and `newBundle`, the static
    methods), mode `:lines`' have them; the text level passes (mode `:full` keeps the text). A
    regression of the reader since the references were recorded, first seen now: today's first
    `--full` did not reach repr/java's steps, and JN8, JFS66 (b) and JFS70 ran no `roundtrip --text`.
    To be bisected and fixed.

## 2026-10-24: bin/gate --full's remaining failures fixed; j2g-edge in the essential gate; JN9a started (the user)

- **A correction** of the entry "GP8 merged; GPS50-GPS56 decided as built; bin/gate --full at
  a1be9220": the round trip's record failure on `java/util/ResourceBundle.ae` was not a reader
  regression. `bin/lib/repr-java/roundtrip`'s `jdk_originals` built `.tmp/repr-java-roundtrip/jdk-originals`
  once (since `f8d03c78`) and never again; main's was made on 2026-10-20 before JN2's `f1f7889d` added
  both `getBundle` and `newBundle` to `overlay/jdk/java.base/java/util/ResourceBundle.java`, so the text
  level read the old file (and printed it back) while the record level compared it with the committed
  forms of the new one. JN8, JFS66 (b) and JFS70 were not involved (911 of 911 with fresh originals).
- **The fix** (branch `repr-java-full-members`, `0f7defc0`, merged at `bd3ba0bb`):
  `bin/lib/repr-java/originals.bash`, the jdk corpus's originals for the round trip, parse-check,
  front-check and `bin/javac-vendor classes jdk`, keyed (`bin/jrt-convert`, `test/jrt`,
  `doc/go/java-surface.edn`, `overlay/jdk/java.base`, jdk26u's commit and status, `JRT_LOCALES`),
  rebuilt under a lock when the key differs, and every copied file checked byte for byte against
  its source at each use. JFS73 (a).
- **`bin/gate --full` at `bd3ba0bb`** (no agents running), 121m56s: every step passed but `goi`:
  `nosplit.go` over 10 s with `--jobs 8` too. Measured alone on the idle machine: 8.7 s run directly,
  about 20 s inside the check (it runs the go command over many assembly snippets); GP6 had recorded it
  as a pass although it timed out alone. My question to the user had said it passes alone; corrected,
  the user chose a per-program limit (over recording it known and raising the default to 30 s):
  `test/goi/timeouts.txt` (`nosplit.go` 60, `fixedbugs/bug358.go` 30), read by `bin/goi`'s
  `run_program`. `--full`'s goi step alone then passed (1,604 of 1,837, no regression).
- **Decided (the user), 2026-10-23:** `bin/j2g-edge verify` in the essential gate (over agents
  running it and leaving it to `--full`): an embedded namespace's new JDK class made `jdk/edge.edn`
  stale twice (GP7's, GP8's), seen only by `--full`; it left `--full`'s jvm chain. `--full`'s
  `bin/goi check` with 8 jobs (as its reference was recorded). The essential gate with both passed
  (18m10s; `j2g-edge` 1m02s), and again with the timeouts (12m05s).
- **The re-seed** started: the fixpoint at `bd3ba0bb` in `.tmp/worktrees/reseed` (the uncommitted
  changes above are not the program's), to be packed and verified on main.
- **JN9a started** (the user: "start JN9a"), with JFS71 and JFS72 (b), on the branch
  `java-frontend-jn9a` from `bd3ba0bb`, in `.tmp/worktrees/java-frontend-jn9a`.

## 2026-10-24: the Go plan's optional work started (the user: "start the optional later work for go")

- GPS49 (c), the native read's raw conversion (and layout) profiled and made faster, outputs byte
  for byte: the branch `go-raw-speed` from `8a879ec0`, proposals from GPS57.
- GPS41 (c) with GPS39 (b), gc's noder pragmas (`noder.go`'s `pragma`, `checkUnusedDuringParse`,
  `lex.go`'s `pragcgo`) ported into the syntax driver and syntax's branch errors merged into goi's
  check in gc's words: the branch `go-pragmas` from `8a879ec0`, proposals from GPS60.
- GPS56 (c), the whole tree converted in the box on the Mac, waits for the user's next Mac session.
- Both beside the re-seed's fixpoint and JN9a, each at 8 jobs or fewer.

## 2026-10-24: seed 54f77c90

- The fixpoint at `bd3ba0bb` in `.tmp/worktrees/reseed` (host `2e4ce3e9…` != G1; G1 = G2 = G3 =
  `c17d6ce8…`, 151,605,408 bytes), beside the essential gate on main: the uncommitted gate changes are
  not the program's. `ARBACE_SEED_GO_BUILD=…/reseed/target/arbace-go/fixpoint/g3 bin/seed --make` on
  main: `54f77c90…` (18,419,044 bytes, 1,990 files), committed at `fde09d4c` on `950f16a4`.
  `bin/seed --verify` (at `950f16a4`) passed: the same tree (`acdbf054…`) and archive; stage 0 = G1 =
  G2 = G3 = `696be4b3…`. The executables' hashes differ from the worktree's G3 by the version map
  alone (`:describe`, `:commit`: built at another commit); the program printed is the same.
- It carries GP8's `arbace.box.convert`, GP7's forms-check fix, JFS58's and GP8's edges, and the
  provenance moved by JN8, JFS66 (b) and JFS70.

## 2026-10-24: the Mac bundle's conversion; GPS41 (c) merged, GPS60-GPS65 decided as built; frozen_view's race fixed

- **The Mac bundle, conversion only** (the user: "i am ready for my next Mac session"; a conversion-only
  bundle, its two reads at once): branch `box-mac-convert` (`45115525`, `ba5ecaf5`), merged into main
  at `b7b4b618`: `bin/box-mac-bundle --convert` (`make mac-bundle-convert`), `box/mac/mac.sh convert`
  (`CONVERT_JOBS`, `CONVERT_CPUS`, `CONVERT_RAM`, `CONVERT_SERIAL`, `CONVERT_DRY`),
  `box/mac/README-convert.txt`; `bin/box-convert`'s host step writes `host.verified`. The tarball
  `arbace-box-mac-arm64-convert.tar.gz` (81,200,804 bytes, sha256 `96052afa…`, built at `45115525`):
  budgets for an M2 Max (12 CPUs, 96 GB): 6 threads and 12 vCPUs a read, 13 GiB guests, both reads at
  once (about 28 GB). On Linux: the host step's digests (the tree `0445b850…`, the forms
  `76a4a414…` and `47a3c126…`), and the extracted bundle's own `mac.sh convert` with the amd64
  launcher and guest as stand-ins: PASS, 238 s, 19.8 GB at most. The essential gate passed on the
  branch and on main (11m34s). Too large to send (30 MiB limit): the user copies it from the worktree.
- **GPS41 (c) with GPS39 (b)** (branch `go-pragmas`, `d5ed2259`, `0668e8c8`; doc/go/GO-PARSER-SURVEY.md,
  "As built: GPS41 (c)"), merged at `eb1c7f70` (the survey's two new sections side by side): gc's
  noder pragmas (`pragma`, `checkUnusedDuringParse`; `lex.go`'s `pragcgo`, `pragmaFlag`, `pragmaFields`;
  from TamaGo's go1.27.1 `src/cmd/compile/internal/noder`, LICENSE.md) and the writer's declaration
  checks (`DeclErrors`) in the driver `go/arbace/repr/go/gcsyntax`, its `Revision` constant;
  `arbace.go.run --errorcheck --gc-front` merging gc's parse errors by position, goi's branch errors
  dropped there. `bin/goi check`: 1,610 of 1,837 pass (1,604), 140 unsupported, 69 known, none fail;
  the class "errors of gc's phases after type checking" 13 (the back end's); essential 801.
  Decided (the user): GPS60-GPS65 as built, over a build-recorded hash now and fixing `bug217.go`
  next. The shipped program changed (`arbace.go.run`, `arbace.repr.go.syntax`, the driver): the next
  re-seed.
- **`bin/lib/frozen.bash`'s race** (both agents' first gates on a fresh worktree: `mv: cannot stat
  .tmp/frozen-view/jar.list.new`): each kind's refresh holds its own lock but all wrote `jar.list.new`;
  now each writes its own `jar.list.new.$BASHPID`, named before the fork (a redirection expands
  in the child: the first try named the child's pid), renamed into place. Two views refreshed at once
  from no list: both made, the list as before. The essential gate on `eb1c7f70` with the fix passed
  (21m35s).

## 2026-10-24: GPS56 (c) on the Mac: tamago/goroot converted in the box byte for byte

- The user ran the conversion-only bundle (`arbace-box-mac-arm64-convert.tar.gz`, `45115525`) on an M2
  Max (12 CPUs, 96 GB, macOS 26.7.1) with `./mac.sh convert` and returned `mac-results.tar.gz`
  (`check`, `sign`, `convert`: status 0). PASS, 13 of 13: both reads at once in 12-vCPU, 13 GiB guests
  on Hypervisor.framework (the first 12-vCPU guests there), tamago/arm64 110.9 s and tamago/amd64
  111.0 s, assembled and merged 1.4 s: `tamago/goroot` `0445b850…` = the commit's, byte for byte, in
  112.5 s, 25,019 MiB resident at most; every digest equal to Linux's (the forms `47a3c126…`,
  `76a4a414…`; GOROOT/src's digest the pin `415d2301…`). GP8's arm64 criterion met on hardware
  (doc/go/GO-PARSER-SURVEY.md, "On the Mac (2026-10-24)").

## 2026-10-24: j2g's array casts and a Java conformance survey started (the user)

- Beside JN9a and GPS49 (c), the user chose (over Go text at the REPL, GPS11, and nothing more):
  - JFS65 (b), j2g's checked array casts as a type switch rather than `Class.IsInstance` (17% of the
    compiled Java parser's time, JN8.5; every compiled program's), on the branch `j2g-array-casts`
    from `317927c0`;
  - a survey of Java's conformance (no counterpart of `$GOROOT/test` for interpreting and compiling
    Java: a corpus, today's coverage by `arbace.classes.interp` and j2g, a plan), on the branch
    `java-conformance-survey` from `317927c0`.

## 2026-10-24: JN9a merged, JFS74-JFS78 decided (the user)

- JN9a (branch `java-frontend-jn9a` from `bd3ba0bb`, doc/classes/JAVA-FRONTEND-SURVEY.md, "As built:
  JN9a"): a native attribution provider (`arbace/repr/java/{classfile,decls,scope,nattr}.ae`, ports of
  javac's Check, Resolve, TypeEnter, MemberEnter, Enter, Annotate, ConstFold, Attr's environments and
  Flow's liveness from jdk26u `baf63fbe`) answers the scope and declarations tiers; every answer is
  compared with javac on Arbace's: 0 different over the samples, the baseline, jdk, javac and jdk26u
  (5,425,925 equal, 28,585 declined: `var` and implicit lambda parameters' types, diamonds), the forms
  equal; `bin/repr-java attr-check` (`--attr native`), in `--full`'s java chain as `repr-java-attr`.
  Within it JFS71 (facts asked in jdk26u 15.15M → 13.54M) and JFS72 (b) (jdk26u's forms as hashes,
  `test/repr/java/expected/jdk26u.sha256`, `check --jdk` compares them; unchanged by JFS71). The rest
  of JN9 re-estimated 7-14 days (was 15-27). No shipped code changed; no re-seed.
- At the merge (`350f1bab`): the conflicts kept both (the survey; `bin/gate`'s report list: main's
  essential `j2g-edge`, the branch's `repr-java-attr`), and `bin/lib/repr-java/host.bash`'s
  `repr_java_provenance_same` compares through temporary files instead of process substitution
  (the rule against `/dev/fd`; the line predated the branch). The agent reports its slips: repr/java's
  sources edited under its own running checks (every affected check redone clean), a process
  substitution and a `2>/dev/null` of its own. The essential gate on `350f1bab` passed (14m03s).
- Decided (the user): JFS74 (a), JFS75 (b) first, JFS76 (a), JFS77 (a) (JFS75 (b), then JN9b),
  JFS78 (a); JFS75 (b) started now, on the branch `java-model` from main.

## 2026-10-24: goi's items deferred from v1 started (the user)

- Beside the five running agents (Go conversion speed, j2g's array casts, the Java conformance
  survey, JFS75 (b)), the user chose (over Go text at the REPL, GPS11, and nothing more) goi's two
  items deferred from `arbace-interprets-go1.27.1-v1`: GC157 (b), `byte`, `rune` and aliases as
  written in the checker's messages (six errorcheck programs), and `go:embed`, GS14 (a) with
  GC161 (b) (`embedfunc.go`, `embedvers.go`; the files read from the package's forms directory), on
  the branch `goi-after-v1` from `d36a03c9`, in `arbace/go/` alone. The shipped program changes:
  the batched re-seed.

## 2026-10-24: GPS49 (c) merged, the native read about twice as fast; GPS57-GPS59, GPS66, GPS67 decided (the user)

- GPS49 (c) (branch `go-raw-speed` from `8a879ec0`; doc/go/GO-PARSER-SURVEY.md, "As built: GPS49 (c)"):
  Go CPU profiles (`ARBACE_CPUPROFILE`, TamaGo's `go tool pprof`) found the raw conversion's cost in
  mode `:full`'s `basic-lit-extents` (a lazy `tree-seq` walk, about 20 µs a node), `parse-pos`'s regex,
  and the layout's read-back `diff` (depth × size), forms prepared twice and `printable?`'s set rebuilt
  per code point. Changed in `arbace/repr/go{.ae,/convert,annotate,layout,lit,read}.ae`: eager walks,
  a hand scan of positions, the reads' raw conversions without the text (`native-check` keeps it), one
  preparation, one comparison, an ASCII fast path. std's read 139.0 → 72.0 s (1,284 → 568 CPU-s), a
  `fmt` read 46.8 → 27.1 s, `cmd-linux-amd64` 477 → 250 s; every tree, summary and report byte for
  byte as before; `native-check` over the 22 corpora all equal (117 raw only, as before);
  `tamago-vendor verify` 4:01, `toolchain-vendor verify` 13:04 (GP5: 8:43, 31:37). The shipped program
  changed (the six namespaces): the batched re-seed, stage 0 at the old speed meanwhile.
- At the merge (`114fa076`) the branch's proposals GPS60 and GPS61 were renumbered GPS66 and GPS67
  (GPS41 (c)'s GPS60-GPS65 merged first); the frozen view's race its first gate met was fixed on main
  meanwhile (`c555f949`). The agent reports its slips: a `pgrep -f` as a read, a `/dev/null` operand,
  a process substitution, and goi's checks at its default 16 jobs (`OMP_NUM_THREADS` does not cap
  them). The essential gate on `114fa076` passed (24m24s; goi 801 of 801).
- Decided (the user): GPS57 (a), GPS58 (a), GPS59 (a), GPS66 (a), GPS67 (a), all as built.

## 2026-10-24: the Java conformance survey merged, JCS1-JCS15 decided, JC1 started (the user)

- The survey (branch `java-conformance-survey` from `317927c0`, merged `0e381e12`, a document alone):
  doc/classes/JAVA-CONFORMANCE-SURVEY.md. Java forms are checked today only by cases Arbace chose
  (diffcheck's Java pair, j2g's fixtures, the suite's fixtures, the oracle's, javac on Arbace's class
  files); nothing runs third-party Java against the JDK's run as goi's check does with `$GOROOT/test`.
  jdk26u's `test/` (at the platform's pin) has 26,271 jtreg tests, 5,182 plain `@run main` usable now.
  A sample of 2,428 (`jdk/java/lang`, `jdk/java/util`, `langtools/tools/javac`, `hotspot/jtreg/compiler`),
  each in its own package: 2,121 pass on the JDK; all read by repr/java and their class files equal to
  the JDK javac's; interpreted 1,279 (60%), compiled by j2g as one program 1,427 (67%); golden
  diagnostics 1,116 of 1,211 by javac on Arbace (the JDK's 1,157; mostly `--release N`). Failures:
  505 of the interpreter's the world (jrt's missing classes), 73 interpreter bugs (a private method on
  another instance throwing `AbstractMethodError`, 58), 79 analysis gaps, 2 a repr/java bug (arrays of
  more than nine dimensions); j2g's own findings (an unknown annotation type stopping the run, Go file
  names colliding by case, simple names colliding in `lang`). Plan JC1-JC8, 18-29 days. The agent's
  slips: `go env GOCACHE` once (possibly the host's go, a query), a `pgrep -f`, `/dev/null` redirects.
  A document alone: the essential gate at `114fa076` stands for it.
- Decided (the user): JCS1-JCS15 as recommended (the survey's "Decided (2026-10-24)"); JC1 started on
  the branch `java-check-jc1` from main, then JC4, then JC2.
- Correction: removing the survey's worktree (`git worktree remove --force`) also removed its
  untracked `.tmp/jc/` (the measurement's scripts of the survey's Appendix A and its data). The
  survey records what each script did and every number; JC1 builds the check anew as
  `bin/java-check` (JCS7), so only the scratch is gone. A worktree's scratch named by a survey is
  to be kept (copied to main's `.tmp/`) before its worktree goes.

## 2026-10-24: JC4 started beside JC1 (the user)

- The user chose JC4 now, beside JC1 (JCS14 (a) had it after JC1): the interpreter's bugs of the
  conformance survey's §6.2 (a private method called on another instance throwing
  `AbstractMethodError`, an `Object` method through a world interface, `clone` of an interpreted
  `Cloneable`) and JCS8 (b)'s source lookup at the REPL on Go, from the survey's reductions and
  jdk26u's tests, measured with JC1's `bin/java-check` once it merges; on the branch
  `java-interp-jc4` from `549c5c49`, in `arbace/lang/` and `arbace/classes/`. A runtime change: the
  batched re-seed. Proposals: JC1's JCS16-JCS29, JC4's from JCS30.

## 2026-10-24: the next milestone's goal, full support for Clojure, Java and Go (the user)

- The user set the next milestone: full support, read, eval, AOT and print, for Clojure, Java and Go.
  Decided with it:
  - the criterion is goi's v1 rule (`arbace-interprets-go1.27.1-v1`): each language's conformance
    corpus at each level against its reference, none failing, every program that does not pass in
    a recorded class with its reason (by design, or deferred and named);
  - Java's read is javac on Arbace (complete today); JN9b-JN9d (a javac-free converter) go on beside
    the milestone and do not gate it;
  - Go's AOT is Arbace's build of Go forms (repr/go's print, Go's toolchain: `arbace.build.go`),
    checked over `$GOROOT/test` against gc as goi's check is;
  - on the host (linux/amd64) alone, of forms only: the box's subsets and each language's text at
    the REPL (GPS11 for Go) are not the milestone's.
  The state at the decision: Clojure's suite on the test build 20,487 assertions pass, 11 fail, 3
  errors, diffcheck's Clojure pair 21 known; Java (the conformance survey's sample) interpreted 60%,
  by j2g 67%; Go's goi 1,610 of 1,837, none failing; every bridge's round trip byte for byte. A survey
  of the milestone (the matrix, each cell's corpus, reference, check and gaps, the plan) next.
- The milestone's name (the user): REAP (Read, Eval, AOT, Print; after REPL), tagged
  `arbace-reaps-3-crops-v1`, an annotated tag naming the three crops and their versions (Clojure
  at its vendored revision, Java 26, Go 1.27.1); considered: `arbace-reaps-clojure-java-go-v1`,
  `arbace-reaps-java26-go1.27.1-v1`.

## 2026-10-24: goi's GC157 and go:embed merged; REAP's survey merged, RPS1-RPS17 decided (the user)

- goi (branch `goi-after-v1` from `d36a03c9`, merged `5b0df516`; GOI-SPEC, "As built: GC157 (b) and
  GS14 (a)"): GC157 (b) by go/types' approach, GP4's resolve-mode spellings (`*spelled*`, `canon`,
  `same?`) made in every mode, about 40 sites where a spelling meets its canonical type rather than
  GR10's estimated 700, the builder and the table given canonical types (`%T` of `[]byte` still
  `[]uint8`); go:embed: `embed` listed in the table, `arbace/go/embed.ae` porting gc's `parseGoEmbed`,
  `checkEmbed`, `embedKind`, `embedFileList` and the go command's `resolveEmbed`, the files read from
  the package's forms directory, `embed.FS` made by `go/arbace/goi/embed.ae` in gc's layout; a bug of
  main's found by the embed test (a closure passed to compiled code through a func type without a
  typed adapter panicked: compiled func types' descriptors without `Ins`/`Outs`, GC171). `bin/goi
  check` 1,618 of 1,837 pass (1,610), 0 fail, 138 unsupported, 63 known; `divergences` 219 in 16
  classes, all by design. Speed unchanged (the whole check 237 s against 238), the executable +0.39%.
  The agent's slips: a process substitution, one error report written to `/tmp` (removed). The
  essential gate on `5b0df516` passed (31m27s). Decided (the user): GC166-GC174 as built.
- REAP's survey (branch `full-support-survey` from `fcda820e`, merged): doc/FULL-SUPPORT-SURVEY.md, the
  matrix measured, Go's AOT for the first time (every run program of `$GOROOT/test` built from its
  forms by `bin/build-go`: 1,032 of 1,051 with the lines layout, the 6 failing in goi's recorded
  classes; errorcheck 388 of 407 judged), Clojure's suite on the evaluated build the same 20,506 of
  20,520 as the compiled runtime's. Its scratch kept in main's `.tmp/reap-survey-fs/`. Milestones
  REAP1-REAP9 (RP and FS taken), proposals RPS1-RPS17. Decided (the user): all as recommended; REAP1
  and REAP2 started.

## 2026-10-24: JC4 merged, JCS30-JCS33 decided (the user)

- JC4 (branch `java-interp-jc4` from `549c5c49`, merged `37422a67`; doc/classes/JAVA-CONFORMANCE-SURVEY.md,
  "As built: JC4"): a private instance call (kind `:virtual`, as javac's `invokevirtual` of a nestmate's
  private method) went to the vtable (`CallV`), where private methods rightly are not, then to
  reflection, which found nothing for an interpreted owner: `this.run()` failed as well as `p.run()`;
  now `CallI`, direct (CLASSFORMS-REPL §2.2), method references too. An `Object` method through a world
  interface (`Map.toString()`): `real-method` falls back to `Object`'s (JLS 9.2). `clone` of an interpreted
  `Cloneable`: `Rt/cloneOf` in `Compiler$CF`, a shallow copy, `CloneNotSupportedException` otherwise.
  JCS8 (b): `*source-lookup*` on everywhere but j2g, through `RT/resourceAsStream`, and the file being
  loaded searched for its later classes (`p/C.ae` alone recovered no test). Sample A (324 single-file
  plain run tests): 205 → 221 interpreted, none regressing; sample B (24 found by the patterns): 2 → 13.
  14 cases appended to the oracle's `defclass.ae` (recorded on the frozen jar), a smoke check. Changed:
  `arbace.lang.Compiler`, `arbace.classes.interp`, `arbace.classes.analyze` (the batched re-seed). Its
  scratch kept in main's `.tmp/jc4-scratch/`. The essential gate on `37422a67` passed (19m26s).
- Decided (the user): JCS30 (b), JCS31 (a), JCS32 (b), JCS33 (a), as recommended.
- Disk at 85% (the user): JC4's worktree removed after its cited scratch was kept; the agents asked to
  delete their intermediate builds (j2g-array-casts' worktree 14 GB → 1.1 GB).

## 2026-10-24: JFS75 (b) built: the converter's model behind an interface (branch `java-model`)

- On the branch `java-model` from main `d36a03c9` (doc/classes/JAVA-FRONTEND-SURVEY.md, "As built:
  JFS75 (b)"): `arbace/repr/java/model.ae`, `arbace.repr.java.model`, 90 reads of symbols, types and
  annotation values over javac's model (Symbol, Type, Types, Attribute) or the native one (`decls`'
  symbols and types); `convert.ae` and `resolve.ae` read the answers only through it. With the native
  provider the answers are no longer realized as javac objects (nattr's `rl-type`, `rl-compound`,
  `rl-value` removed); javac's symbols of the unit's declarations are their native twins'
  (`nattr/canon`), a relation of a native type with a javac one is answered natively (`nativize`),
  and a read the native tiers cannot answer finds javac's symbol of the same declaration (a bridge:
  a method's `isMemberOf`, inferred and union local types, local classes' javac names, a package's
  classes, a diamond's anonymous supertypes). Each read counted by model in attr-check (`attr.md`).
  Ported from jdk26u `baf63fbe` (`code/Symbol.java`, `code/Types.java`, `comp/ConstFold.java`):
  isStatic, outermostClass, isMemberOf of fields, isSubClass, isAccessibleIn, hiddenIn,
  isSignaturePolymorphic, getRetention, erasure's exceptions. `decls` gained a final local's constant
  (it had none), ConstFold's coercion of constant variables (a boolean field's was none) and the
  absence of `lushrl` folding (found by jdk26u's `MontgomeryIntegerPolynomialP256`).
- Every corpus's forms unchanged (attr-check `--corpus all`, check, `check --jdk`, roundtrip
  `--text`, front-check, parse-check; the three updates byte for byte but `:repr-java`, `:host`);
  in jdk26u 62.7% of the model's reads native, 99.4% of the native answers' reads; attr-check 5%
  slower. Proposals JFS79, JFS90-JFS94 (JFS80-JFS89 left to the j2g branch); JN9b re-estimated 2-3
  days. No shipped code changed; no re-seed.

## 2026-10-24: JFS75 (b) and JFS65 (b) merged; JFS79-JFS82, JFS90-JFS94 decided (the user)

- JFS75 (b) (branch `java-model`, merged `6663972b`; its own entry above, "JFS75 (b) built"): the
  converter's model behind `arbace.repr.java.model`; in jdk26u 62.7% of the converter's model reads
  native, 99.4% of those of the native tiers' answers; 6,660,391 facts equal, none different; the
  forms of every corpus byte for byte; attr-check 5% slower. No shipped code changed.
- JFS65 (b) (branch `j2g-array-casts` from `317927c0`, merged `407ed654`; doc/classes/JAVA-FRONTEND-SURVEY.md,
  "As built: JFS65 (b)"): j2g's checked casts and `instanceof` of array types as jrt's type switch
  (`go/arbace/jrt/array.ae`: `IntArray_Cast` and kin, `ObjectArray_Cast`, `RefArray_Cast` with
  `Class.isInstance` out of line for covariance; each fast path within gc's inline budget),
  `arbace.j2g.model/array-test`, `J2g_CastArray` removed; the native parser in javac's tool over
  java.base 49.0 → 37.3 s (−24%), hinted-array workloads through the evaluator −19% to −22%, general
  ones unchanged; the fixture `FxArrays` (21 array types, 18 values); `test/j2g/check.ae` requiring
  `arbace.pprint` (it failed to load on stage 0). The shipped program changed: the batched re-seed.
  The agents' slips: process substitutions, a `pgrep -f` in a wait loop, two error reports in `/tmp`
  (removed). Their cited scratch kept in main's `.tmp/jfs75-scratch/`, `.tmp/jfs65-scratch/`; the
  worktrees removed (the disk 82% → 68%). The essential gate on `407ed654` passed (30m04s).
- Decided (the user): JFS79 (b), JFS80 (a), JFS81 (a), JFS82 (a), JFS90 (b), JFS91 (a), JFS92 ((a) until
  a javac-free read is the aim), JFS93 (a), JFS94 (b). JN9b started (branch `java-jn9b` from
  `407ed654`), beside REAP3 (`reap3-clojure-aot`) and REAP4 (`reap4-clojure-eval`), the user's choice;
  RPS18 (b) added SL5 (the suite's `metadata` and `java.javadoc`) to REAP4.

## 2026-10-24: JC1, `bin/java-check`: Java's conformance check with its interpreted level

- Built on the branch `java-check-jc1` from main `549c5c49`, as planned in
  doc/classes/JAVA-CONFORMANCE-SURVEY.md (JCS1-JCS15 as decided); "As built: JC1" there has the details
  and the numbers. `bin/java-check` (bash: the processes, as `bin/goi check`) and the namespace
  `java.check` (`test/java/check.ae`, on the tools' host, JCS7 (a): jtreg's tags, the categories,
  the ids, the reference, the plans, the report and the classes); `test/java/run.ae`, the interpreted
  runs on the executable under test (a test's packages each defined as one top-level `do`, JCS8 (a)).
- The corpus (JCS2 (b)): jdk26u's jtreg tests read in place from `/root/jdk26u` at
  `baf63fbe42b8758448fee570a9d9bb914272d259` (the platform's pin; `test/`: `jdk/java/*` but `awt`,
  `langtools/tools/javac`, `hotspot/jtreg/compiler`, their run-plain, run-compile and run-jitopts
  tests, `test/java/corpora.edn`), nothing of them copied but the essential subset's converted forms;
  and eleven programs of Arbace's own in `test/java/jls/` (the hand-curated JLS corpus, started).
  The reference recorded with the system's JDK (OpenJDK 26.0.2.1 built from jdk26u):
  `test/java/ref-jdk26.edn`, 3,218 tests, 2,967 passing on the JDK (JCS5 (b): the check needs no JVM).
- Results, the gate's executable of `549c5c49` (`gate_key` `881effe6…`), javac's tool of 04:35
  (key `bf740cf0…`, the checkout's `22385f97…`: `REPR_JAVA_STALE_OK=1`, javac's world unchanged since,
  repr/java's namespaces loaded from the checkout where they differ): read 2,965, class files byte for
  byte 2,965, interpreted 1,364 (46.0%; on the survey's four areas as its §5.3); every test not
  passing in one of the 36 classes of `test/java/divergences.edn`, recorded as
  `test/java/check-{read,interp,classes}.edn`. New findings: three interpreter bugs from the JLS
  corpus (a `finally`'s `return` not replacing a thrown exception; an overload chosen by the run-time
  class instead of a cast's static type; a constant string expression not interned), repr/java's
  converter overflowing its stack on `UncommonTrapStackBang.java`, javac on Arbace refusing the
  charset alias `utf8`. Recorded, not fixed (JC4, JC5).
- The essential subset (JCS13 (a)): `test/java/essential.txt`, 428 tests passing in under a second,
  their forms committed in `test/java/forms/` (1.0 MB; GPL-2.0 as jdk26u's `test/`, LICENSE.md;
  `bin/java-check --forms verify|update`), so the essential gate needs no javac: `bin/java-check
  --essential` in `bin/gate`'s essential step (28 s), the whole check and `--forms verify` in
  `--full`'s java chain. Makefile targets `java-check`, `java-check-essential`, `java-check-ref`;
  CLAUDE.md's layout and gate paragraphs. The shipped program is unchanged (nothing under `arbace/`,
  `go/`, `overlay/`, `jdk/`).
- Alternatives considered: the forms read on every essential run (needs javac's tool in the
  essential gate: minutes and a heavy slot; JCS21 (b)); test ids by digest (JCS23 (b)); per-file loads
  instead of one `do` (JCS8 (c), the survey's 36 fewer passes).
- Proposals JCS16-JCS25 (the survey's "As built: JC1"); JCS30 on are JC4's.
- The agent's slips: one throwaway command used `pgrep -f` (as a no-op operand, nothing stopped),
  one used a process substitution `source <(...)` to try a function; neither in a committed script.
- The coordinator relayed the user's JCS33 (a) (JC4's): once JC1 and JC4 are both on main, the
  interpreted level loads a test file by file with JC4's source lookup, the one-`do` load kept for
  comparison. Built as a switch, `bin/java-check --load do|files` (`JAVA_CHECK_LOAD`), `do` the
  default until then; without the lookup `files` passes 412 of the essential 428.

## 2026-10-24: REAP1 built: `bin/reap`, the classes of every recorded cell (branch `reap1-classes`)

- What (doc/FULL-SUPPORT-SURVEY.md, "As built: REAP1"; RPS3, RPS13): `bin/reap` (and `make reap`,
  the essential gate's step `reap`, seconds) judges the REAP matrix from the checks' recorded results
  alone: `test/reap/cells.edn` holds each cell's classes file and sources (reader, files, the part
  taken, `:check` or `:beside`, `:until` for a check not recorded yet), `test/reap/main.ae` (`reap.main`)
  reads them and classifies each non-pass as `test/goi/divergences.edn` does. New classes files in
  that format: `test/clojure/divergences.edn` (eval: the suite's 40 non-passing or not-run tests and
  namespaces, the oracle's 53), `divergences-read.edn` (repr/clojure's 2 one-way rules),
  `divergences-aot.edn` (diffcheck's Clojure pair, 21), `divergences-print.edn` (none);
  `test/repr/go/divergences-read.edn` (native-check's 117 raw only, 14 the helper's failures, 73
  rejected), `divergences-print.edn` (the round trips' 141); `test/java/divergences-diffcheck.edn`
  (diffcheck's Java pair, 2). Go's read numbers, printed by native-check but recorded nowhere, written
  to `test/repr/go/native-check-results.edn` by `bin/reap record-native-check` from main `bd3ba0bb`'s
  full run's report. `bin/clojure-tests --divergences [RESULTS]` judges a suite run per test. The known
  files are kept unchanged; their entries are classed by their reasons.
- The matrix as printed: Clojure read 908 / 2 / 0, eval 21,026 / 79 / 14 (1 pending), AOT no check
  yet (REAP3), print 1,031+ / 0 / 0; Java no check yet (JC1, JC2, REAP5); Go read 12,158 / 204 / 0,
  eval 1,618 / 219 / 0, AOT no check yet (REAP2), print 2,584+ / 141 / 0 (pass / by design /
  deferred); 0 problems. JC1's committed data (`java-check-jc1` `1aae6470`) tried in a scratch root:
  the Java cells turn on (read 5,930 / 0 / 4, eval 1,364 / 278 / 1,325), 0 problems.
- Why so: RPS3 (a) and RPS13 (a) as decided; one file per language and level (goi's name for eval,
  `-read`, `-aot`, `-print` beside it, as REAP2's `divergences-aot.edn`); the units each check's own
  (the suite per test, RPS25). Alternatives considered: one classes file per language with a `:level`
  key (JC1's way; one per level keeps a level's owner its file's), counting the suite by assertions
  (skipped tests' assertions are not recorded per test), the matrix only over checks with totals.
- Found: `metadata` and `java.javadoc` are deferred by the record (EVAL-NOTES.md, SL5 "not taken for
  now"), not by design as the survey said; five classes known with reasons but decided nowhere (the
  oracle's `Method.invoke` message, diffcheck's vars interned at load and compiled `Compiler/LINE`,
  the Java pair's two interpreter limits), recorded `:decided :pending`. Proposals RPS18-RPS29.
- Sources: main `37422a67`; doc/go/B1-PLAN.md (D6), doc/go/EVAL-NOTES.md (2B, 2C, "The suite's last
  failures", the amendments of 2026-10-10), doc/go/EVAL-PLAN.md (Q1), doc/FUTAMURA-SURVEY.md (F2, F3.3),
  doc/go/GO-PARSER-SURVEY.md (GP3, GP6, GPS9, GPS17), doc/go/ROUNDTRIP.md, doc/go/PRINTER-NOTES.md
  (G2F6), doc/REPR-CLOJURE.md; `java-check-jc1`'s `test/java/check.ae` (its classify, its kinds).

## 2026-10-24: JC1 and REAP1 merged, RPS18-RPS29 and JCS16-JCS25 decided; JC2 and the first batched re-seed started (the user)

- JC1 (branch `java-check-jc1`, merged `cd66f652`; its own entry above): `bin/java-check`, jdk26u's
  jtreg tests and `test/java/jls` against the JDK's recorded runs (`test/java/ref-jdk26.edn`): of the
  2,967 passing on the JDK, read 2,965, class files 2,965, interpreted 1,364; the essential subset
  (428, its forms committed) in the essential gate. Decided (the user): JCS16 (b), JCS17 (b), JCS18 (c),
  JCS19 (b), JCS20-JCS23 (a), JCS24 (c), JCS25 (a); JCS16, JCS17, JCS19 and JCS33 (a) built with JC2.
- REAP1 (branch `reap1-classes`, merged `45c41a46`; its own entry above): `bin/reap`, the REAP matrix
  from the committed references, every non-pass in a class (`test/reap/cells.edn`, `test/reap/main.ae`;
  the classes files under `test/clojure/`, `test/java/`, `test/repr/go/`), in the essential gate.
  Decided (the user): RPS18 (b) (SL5 built in REAP4), RPS19 (a), RPS20 (a), RPS21 (a), RPS22 (c),
  RPS23-RPS29 (a); the four `:pending` classes now cite the decisions. The essential gate on `45c41a46`
  passed; the matrix: Clojure read 908 / 2 / 0, eval 21,026 / 79 / 14; Java read 5,930 / 0 / 4, eval
  1,364 / 278 / 1,325; Go read 12,158 / 204 / 0, eval 1,618 / 219 / 0, print 2,584+ / 141 / 0 (pass /
  by design / deferred); 8 of 12 cells checked, 0 problems.
- JC2 started (branch `java-check-jc2` from `45c41a46`): the compiled level, j2g's findings, and JC1's
  decided follow-ups. The first batched re-seed started at `45c41a46` (the fixpoint in a detached
  worktree): GPS41 (c), GPS49 (c), GC157 and go:embed, JC4, JFS65 (b).
- Noticed: committed scripts of `bin/` still use `/dev/null` (123 places) and process substitution
  (about 40: `bin/seed`, `bin/repr-haskell`, `bin/repr-rust`, ...), against the working rule for
  commands; left as they are for now, a sweep offered to the user.
- Decided (the user): the scripts' sweep (`/dev/null` and process substitution replaced by files under
  TMPDIR, one commit per script, each script's check rerun) after the REAP milestone.

## 2026-10-24: the first batched re-seed; REAP2 merged, RPS30-RPS34 decided; JC3 started (the user)

- The first batched re-seed: `bin/arbace-go --fixpoint` at `45c41a46` in a detached worktree (host
  `696be4b3` != G1; G1 = G2 = G3 = `e1471263`, 152,404,128 bytes), packed by `ARBACE_SEED_GO_BUILD=<G3>
  bin/seed --make`: `seed/arbace-seed-go.tar.xz` 18,520,792 bytes, SHA-256 `916770d0` (was `54f77c90`),
  committed `cd0f1e24`, merged `0050e92a`. It carries GPS41 (c), GPS49 (c), GC157 and go:embed, JC4 and
  JFS65 (b). `bin/seed --verify` at `0050e92a`: printed again at `45c41a46`, the same tree (`34f35cf4`)
  and archive, stage 0 = G1 = G2 = G3 (`e1471263`), 1,746 s. Logs in `.tmp/reseed1-scratch/`.
- REAP2 (branch `reap2-goi-aot` from `37422a67`, merged `b348387c`): `bin/goi check --aot` (RPS10):
  goi's corpora built by `arbace.build.go` from the printed forms and run, the errorcheck programs
  printed from mode `:full` and compiled by gc, the directory tests built under the module (RPS11),
  against gc on the originals; 1,685 of 1,837 pass, 0 fail, 0 error (run 1,045 of 1,051, directory
  126 of 127, errorcheck 514 of 659, 137 of them invalid programs with no forms); 152 non-passes in 7
  classes (`test/goi/divergences-aot.edn`, known programs in `test/goi/check-aot-known.edn`, results
  `test/goi/check-aot-linux-amd64.edn`); `bin/goi divergences --aot`; the step `goi-aot` in `bin/gate
  --full`; Makefile `goi-check-aot`, `goi-divergences-aot`. goi's own reference switched to gc on the
  originals (RPS12): no program moved (1,618 pass, the results byte for byte). `arbace/build/go.ae` takes
  `--go-flag` (shipped code: the second re-seed). `bin/goi --record`'s backslash escaping fixed. The
  check takes 554-582 s at 8 jobs. Merge conflicts in the Makefile, `bin/gate` (the steps of both sides
  kept) and the survey (both sections kept). The essential gate on `b348387c` passed (19m19s); the
  matrix: 9 of 12 cells checked, 0 problems. The agent's slips: `/dev/null` as a grep argument twice,
  one `<(echo)`, a read outside heavy at 32 threads; nothing committed has them.
- Decided (the user): RPS30 (a), RPS31 (a), RPS32 (a), RPS33 (a), RPS34 (a), as recommended (doc/
  FULL-SUPPORT-SURVEY.md, "Decided (2026-10-24): RPS30-RPS34"); the two `:pending` classes cite them.
- JC3 started (branch `java-check-jc3` from `0050e92a`; the user): Java's front end's levels in
  `bin/java-check`, `diagnostics` beside JC1's `read` and `classes`; proposals JCS50-JCS59.

## 2026-10-25: JC3, JN9b, REAP4, JC2 and REAP3 merged; JCS40-JCS55, RPS40-RPS59 and JFS95-JFS101 decided; six milestones started (the user)

- JC3 (branch `java-check-jc3` from `0050e92a`, merged `3745075c`): `bin/java-check --levels diagnostics`,
  langtools' 1,212 golden-diagnostics tests (compile-fail-ref, the corpus `langtools-diagnostics`) run in
  place by javac on Arbace as jtreg runs them (@build and @compile in order, @library, @modules,
  @enablePreview, the substitutions), the output against the golden file line by line; the JDK's runs
  recorded in `test/java/ref-jdk26.edn` (1,194 pass on the JDK); javac on Arbace 1,101 of 1,194 (59 fail,
  34 differ: `--release` without ct.sym 44, processors from class files 25, doclint 8, the closed world 4,
  javac on Arbace's file manager 8 and command line 4), `test/java/check-diagnostics.edn`; in `bin/gate
  --full`'s java chain; `make java-check-diagnostics`; the Java read cell's third source. Numbers on a
  javac tool of an older key (javac's world unchanged since). Essential gate on `3745075c` passed (2m46s).
  Decided (the user): JCS50 (a) with (b) first, JCS51 (a), JCS52 (a), JCS53 (a), JCS54 (a), JCS55 (b).
- JN9b (branch `java-jn9b`, merged): the native attribution provider's members and types tiers,
  `arbace/repr/java/expr.ae` (from javac's Attr, Operators, Resolve) and `types.ae` (from Types), jdk26u at
  `baf63fbe`; a call's type computed after javac's overload choice (the lean, until JN9c); TypeEnter's
  generated members in `decls` (JFS79), `isMemberOf` native (JFS90), the bridges' known list (JFS94).
  `attr-check --corpus all`: 11,440,693 facts equal, 0 different, 84,239 declined; lists written alone
  63.4% (was 15.2%), the model's reads native 97.4%; bridges 48,519 (was 257,451), all known. Forms
  unchanged; javac's tool 224,006,304 bytes; the shipped program's code unchanged. Decided (the user):
  JFS95-JFS99 (a), JFS100 (b), JFS101 (a).
- REAP4 (branch `reap4-clojure-eval`, merged `8124b0be`): the boxed NaN's identity: the cause was the
  evaluator's constants (`Evaluator.constant` kept a constant collection's numbers as the same objects,
  where the JVM's `emitValue` boxes each anew; the reader's `##NaN` is shared on the JVM too), fixed by
  boxing each Integer, Long, Double and Character anew; 15 oracle forms added (`numbers.ae`, 837 cases),
  the known mismatches 53 to 52. `java.util.BitSet` among j2g's proxy superclasses (J2G-SPEC §5.12). SL5
  (RPS18 (b)): `arbace.inspector`, `arbace.xml`, `arbace.java.browse`, `arbace.java.javadoc` restored from
  `ac66b276^` and embedded (VENDOR-NOTES hand change 30), `org.xml.sax` and `org.w3c.dom` among the cut
  packages, `jdk/edge.edn` updated; metadata 53 of 53, java.javadoc 2 of 5. The suite 20,564 of 20,580
  assertions (78 namespaces). Survey conflict resolved by keeping both; Clojure's eval classes updated
  (`:proxy-bitset`, `:nan-identity`, `:d6-namespaces-cut` dropped; `:javadoc-url` added).
- JC2 (branch `java-check-jc2` from `45c41a46`, merged `71ed1d18`): `bin/java-check --levels compiled`, the
  tests translated by j2g in shards (`bin/j2g --program` with the build's compiled namespaces,
  `bin/build-go`), refused tests taken out with j2g's or Go's reason; 1,632 of 2,722 (60.0%) against
  interpreted 1,426; j2g's fixes (user classes named `J_<binary name>_J`, a class's failure in `fis` its
  own, a refused forms file its own, run-time exceptions of constant indices and divisors, record patterns,
  `instanceof null`, empty records); JC1's follow-ups JCS16 (b) (`jdk/java/beans` excluded), JCS17 (b)
  (identity hashes masked), JCS19 (b) (each test's own directory), JCS33 (a) (`--load files`), the
  references re-recorded; `test/java/jls/RuntimeConstants.java`; the Java AOT cell. Merge conflicts in nine
  files: both levels kept in `bin/java-check` and `test/java/check.ae`; the compiled level made to skip the
  golden-diagnostics tests (they would have counted as unread); the id `t06426` taken by both branches, so
  JC3's golden-diagnostics tests renumbered `t06427`-`t07638` (ids only: they run in place, unrenamed).
  Decided (the user): JCS40 (a), JCS41 (a), JCS42 (b), JCS43-JCS47 (a), JCS48 (a) and (b), JCS49 (a).
- REAP3 (branch `reap3-clojure-aot` from `407ed654`, merged `e9aa12af`): `bin/clojure-tests --aot`, the
  suite and its libraries compiled by c2j with the runtime's namespaces they load (156 sources, 15,219
  classes), j2g and `bin/build-go` making the program with the Java fixtures, each namespace loaded through
  its compiled loader (`test/suite_aot_hook.ae`); 20,489 of 20,520 assertions (`test/arbace-go-aot-results.edn`,
  recorded before REAP4's merge); the two levels' disagreements listed; `suite-aot` in `bin/gate --full`.
  F2.4's limit fixed (RPS6 (a)): the Java forms compiler records the vars its forms name, c2j writes
  `(c2j/refer-vars ...)`, j2g interns them; diffcheck's 11 F2.4 cases pass. c2j's fixes found by the suite
  (record and deftype literals, fn constants, qualified methods as values, a deftype defined twice, Go
  package cycles merged, file names unique ignoring case...). `bin/arbace-go --fixpoint` held on the branch.
  Its classes appended to `test/clojure/divergences-aot.edn` (RPS45 (a)), `:f2.4-limit` dropped. Decided
  (the user): RPS40 (b), RPS41-RPS47 (a), RPS48 (a) then (b), RPS49 (a); REAP4's RPS50-RPS59 (a).
- The essential gate on `566f549d` (the four merges) passed (21m07s): the suite no regressions, the oracle
  52 known, diffcheck 12,789 of 12,801 (12 known), goi 801 of 801, Java 428 of 428, `j2g-edge verify`;
  the matrix 11 of 12 cells checked, 0 problems; no class pending.
- The agents' slips (none committed): `/dev/null` and `/dev/zero` in throwaway commands (JC2, REAP3),
  `<(...)` once each (JC2, REAP3), `pgrep -f` once as a read (REAP4, JN9b), `ps | grep` (JC2), editing
  j2g's sources during a j2g run (JC2), small checks outside heavy (JN9b); mine: one `2>/dev/null` in a
  status check (2026-10-24).
- Started (the user), from this commit: REAP5 (Java's print level), JC5 (the analysis' gaps), REAP8 (the
  essential subsets), JC2b (JCS42 (b)-JCS48), RPS55 (jrt's `java.lang.Module`), JN9c (overload resolution).

## 2026-10-25: RPS55 merged, RPS80-RPS82 decided (the user)

- RPS55 (a) (branch `jrt-module-rps55` from `840e514b`, merged): jrt's `java.lang.Module`
  (`go/arbace/jrt/module.ae`, a hand-written final class of the manifest: `getName`, `isNamed`,
  `getClassLoader`, `toString` with the JDK's texts; two instances, java.base and the app loader's
  unnamed module) and `Class.getModule` by `classPlace`'s rule (java.base for primitives and
  `java.`/`javax.`/`jdk.`/`sun.` classes, exact for the shipped world's 839 java.base sources; the unnamed
  module otherwise; an array its element's); `getDescriptor` j2g's stub. `bin/jrt manifest` regenerated
  (`manifest.edn`, `manifest_test.ae`, `reflect_tables.ae`), `TestClassLoaders` checks the modules;
  `jdk/edge.edn` unchanged (`bin/j2g-edge update`, `verify`). The suite 20,566 of 20,580 assertions (was
  20,564; errors 5 to 3), java.javadoc 4 of 5, re-recorded; `:javadoc-url` narrowed to the java.sql
  assertion, by design (D6); the oracle and diffcheck unchanged; the executable 8 KB larger. The essential
  gate passed on the branch head (25m06s, then 2m31s), whose tree the merge is. Shipped code (the second
  re-seed): `go/arbace/jrt.ae`, `jrt/module.ae`, `manifest.edn`, `manifest_test.ae`, `reflect_tables.ae`.
  The agent's slips: `bin/jrt manifest` (the frozen JVM, a few cores) and seconds-long reads outside heavy.
- Decided (the user): RPS80 (a), RPS81 (a), RPS82 (a) (java.javadoc at the AOT level recorded at REAP8's
  re-record), as recommended.

## 2026-10-25: C23 the next language of REAP (the user)

- Decided (the user): after the milestone `arbace-reaps-3-crops-v1`, the next language to join Arbace's
  REAP (read, eval, AOT, print) is C23 (ISO/IEC 9899:2024). Not started; no survey yet. Haskell and Rust
  stay bridges (repr/haskell, repr/rust: read and print on the host's GHC and rustc), outside the matrix.

## 2026-10-25: go-whim, the prior art of C23's REAP (the user)

- The user: go-whim (`../go-whim`, at `80c49bf5`; the pilot Arbace vendored the box from and no longer
  depends on) holds most of what C23 needs for REAP's read and print. Studied, not vendored yet:
  `crefactor/cc`, a fork of modernc.org/cc/v4 v4.29.7 (BSD-3-Clause, The CC Authors) with C23's
  productions added (`c23.go`: `[[...]]` attributes in every position, `constexpr`, `_BitInt(N)`,
  `typeof_unqual`, `static_assert` without a message, digit separators, `u8'a'`; hooks marked `go-whim`),
  and a `Host` given instead of asked of a compiler (`host.go`, for a program with no compiler);
  `crefactor/cemit`, a canonical printer (one spelling per construct); `crefactor/clisp`, C-lisp, C23 as
  s-expressions and back, byte for byte on canonical text (`ToC(ToLisp(x)) == cemit.Canonical(x)`;
  `crefactor/clisp/SPEC.md`, doc/C-LISP.md); `crefactor/c23conf`, a conformance test of one file per
  feature (49, N3220's subclauses) judged in six stages against gcc 15.2's `-std=c23` (accepted, parsed,
  printed and accepted, a fixed point, the same tokens and the same objects at `-O0`, C-lisp's round
  trip), 49 of 49 (doc/C23.md); the preprocessor out (whim's texts have only `#include`). No evaluator of C
  forms seen there: C's eval and AOT are open questions for C23's survey after the milestone.

## 2026-10-25: after the milestone, C23 at parity the main effort (the user)

- Decided (the user): after `arbace-reaps-3-crops-v1`, the main effort is bringing C23 to parity with
  Clojure, Java and Go: read, eval, AOT and print, each cell meeting the milestone's rule (none fails,
  every non-pass in a recorded class). Its survey first (the prior art go-whim's, entry above; the open
  questions C's eval and AOT on TamaGo's toolchain alone); the scripts' sweep stays queued beside it.

## 2026-10-25: REAP8, REAP5 and JC2b merged, the matrix 12 of 12; RPS60-RPS78 and JCS70-JCS79 decided; REAP5b, REAP7 and the harness follow-ups started (the user)

- REAP8 (branch `reap8-essential` from `840e514b`, main merged into it for RPS55; merged `de5cb07a`): the
  essential subsets in the essential gate: `bin/goi check --aot --essential` (`test/goi/essential-aot.txt`,
  248 programs by the rule in its header: every passing directory test, the 13 run programs with gc's
  flags, every second of the 124 errorcheck programs goi sets apart, every fourth of the 41 known run
  programs, 36 of goi's essential list; 67-71 s at 8 jobs), `bin/java-check --essential-compiled` (the
  essential tests `check-compiled.edn` passes, 420 of 428, from their committed forms in one program cached
  by key; the compiled namespaces' `aot.key` checked; a timeout run again at twice the limit), `bin/clojure-tests
  --aot --essential` (`test/clojure/essential-aot.txt`, 15 namespaces compiled alone: 7,243 of 7,273
  assertions as the reference's); `J2G_LOCK`, one whole-program j2g (about 20 GiB) at a time in the
  essential phase; `--gate-build` keeps `aot/` and `aot.key`; RPS48 (b): every embedded namespace prepared and
  compiled into the whole `--aot`'s program (235 sources, 22,838 classes, no test moved). The AOT reference
  re-recorded on the merged head: 785 tests, 20,580 assertions, 20,549 pass (BitSet's proxy, SL5's metadata
  53 of 53 and java.javadoc 4 of 5 now run); `:proxy-bitset` dropped, `:d6-namespaces-cut` replaced by a
  by-design `:javadoc-url` (RPS82 (a)). The essential gate before/after: cold 20m51s/21m23s, cached
  2m29s/2m42s. No shipped code. The gate on `de5cb07a` passed (17m04s). Decided: RPS70-RPS73 (a), RPS74
  (b), RPS75 (a), RPS76 (a), RPS77 (b), RPS78 (a).
- REAP5 (branch `reap5-java-print` from `840e514b`, merged `678e15ba`): `bin/java-check --levels print`,
  per test five steps (the read level's forms printed in the lines layout with `--index jdk/forms`, javac
  on Arbace accepting the printed Java, read again to the same forms, mode `:full` printed back byte for
  byte, the class files against the original's, judged by `jrt.classfile`, no JVM), namespace `java.print`
  (`test/java/print.ae`), `test/java/check-print.edn`; 2,104 of 2,722 pass (javac refuses 93 printed files,
  48 read to other forms, 504 class files differ: 472 by design, RPS9 (a)); in `bin/gate --full`'s java chain;
  `make java-check-print`. It found printer bugs (an array creation as an operand, a switch expression or
  lambda as a receiver, `int[]::clone`, local and anonymous class names, qualified inner creation) and a cast
  the converter loses (`v((Object) "s")` read as `v("s")`, another overload). The matrix: 12 of 12 cells
  checked. No shipped code. Decided: RPS60-RPS65 (a), RPS66 (a) then (b), RPS67-RPS69 (a).
- JC2b (branch `java-check-jc2b` from `840e514b`, merged `6d8b8e27`): JCS42 (b) (j2g refuses a subclass of
  `PrintStream`/`PrintWriter`, its support code's leaves, `arbace.j2g.checks`), JCS43 (a) (`_f` on a static
  field or enum constant meeting a derived suffix), JCS44 (a) (refusals name the import: `:j2g-world` 420 to
  11, the rest into the package classes), JCS45 (a) (`test/java/levels-known.edn`), JCS46 (a) (the runtime of
  the compiled namespaces checked), JCS47 (a), JCS48 (a, b) (shards of 500, a shard whose j2g dies split, to
  25); the shipped program's translation unchanged (checked against main's `target/arbace-go/j2g`); interp
  1,462 (was 1,426: 38 timeouts pass on the retry), compiled 1,636 (was 1,632). Merged cleanly after REAP5.
  The gate on `6d8b8e27` passed (16m28s; all subsets). Decided: JCS70 (a), JCS71 (a), JCS72 (b), JCS73-JCS77
  (a), JCS78 (c), JCS79 (a).
- The agents' slips (none committed): `pgrep -x` in a wait loop (REAP5), `<(...)` once (JC2b), a scratch draft
  with `<(...)` never run (REAP8), stage 0 built in a worktree (REAP5), edits to scripts while own runs were
  queued, not running (REAP8); `2>/dev/null` in several agents' wait loops.
- Started (the user): REAP5b (the printer: RPS60-RPS63), REAP7 (`bin/arbace-go --build-program`), the
  harness follow-ups (JCS70, JCS71, RPS74 (b), RPS77 (b)); the catch-all triage left to JC6.

## 2026-10-25: JN9c merged, JFS102-JFS109 decided; JN9d started (the user)

- JN9c (branch `java-jn9c` from `840e514b`, merged `7b6ed2f1`; beside REAP, not gating it): overload
  resolution in the native attribution provider, `arbace/repr/java/overload.ae` ported from jdk26u's Resolve
  at `baf63fbe` (the three phases, most specific, mergeAbstracts, findMethod's interface phases, findFun
  over enclosing classes and static imports, the access checks; errors as javac's kinds); `expr.ae`'s
  choice native with javac's as the lean's fallback; the facts `overload/method`, `overload/constructor`,
  `overload/anon-super-ctor`, `overload/arity` native and checked; ClassReader's default methods and inner
  constructors' descriptors in `decls.ae`; JFS100 (b) (the declines' reasons summed in `attr.md`). Declined,
  javac's still (JN9d's): poly arguments, generic candidates passing the arity check (21,525 calls),
  explicit type arguments, diamonds, signature-polymorphic calls, qualified creations. `attr-check --corpus
  all`: 13,193,153 facts equal, none different, the forms byte for byte as javac's trees'; leaned answers in
  jdk26u 903,905 to 53,316; lists written alone 63.4% to 95.9% (baseline 99.1%, javac 93.0%); 2,378 s (was
  2,056). `front-check`, `parse-check`, `check`, `check --jdk`, `roundtrip --text` as recorded; the
  provenance files' `:repr-java` and `:host` moved (reader hash `a2daae6b`); javac's tool 226,652,320 bytes;
  the shipped program's code unchanged. Merged cleanly; the essential gate on `7b6ed2f1` passed (16m11s).
- Decided (the user): JFS102 (b), JFS103 (b), JFS104 (b), JFS105 (a), JFS106 (b), JFS107 (b), JFS108 (b),
  JFS109 (a); JN9d started now, beside the REAP work.

## 2026-10-25: the harness follow-ups merged, JCS80-JCS84 decided (the user)

- The harness follow-ups (branch `harness-followups` from `6f3a93ad`, merged): JCS70 (a) (`mask_dirs`: the
  test's working directory, its temp directory, the check's scratch and the checkout masked in stdout on
  both sides before the digest; the JDK's reference re-recorded for the 22 tests whose stdout names them, by
  splicing their lines from a full `--record-ref` run into the committed file, the other 4,158 lines byte for
  byte, as `--only` would write), JCS71 (a) (a would-be regression run again once; a flip `flaky`, not a
  regression; class `:flaky` naming Test8267042 and List/LockStep at the compiled level), RPS74 (b)
  (`test/clojure/essential-aot-skipped.edn`: `seq-and-transducer` skipped in the compiled suite's subset only;
  the cached step 1m56s to 36 s), RPS77 (b) (the subset's work and j2g trees removed after its build, 366 MB
  less). Java interp 1,464, compiled 1,638 (CheckTempDir and UserDirChangedTest pass). No shipped code. The
  essential gate passed on the branch (4m41s, then 2m50s cached) and on the merge. Decided (the user):
  JCS80 (a), JCS81 (b), JCS82 (a), JCS83 (b), JCS84 (a). The agent's slips: one `2>/dev/null` in a wait
  loop; main's gate cache copied into the worktree.

## 2026-10-25: REAP7 merged, RPS90-RPS99 decided; JC5's merge sent back; REAP7b started (the user)

- REAP7 (branch `reap7-build-program` from `6f3a93ad`, merged `79136f28`): `bin/arbace-go --build-program DIR`,
  a user's program: `program.edn` (`:main NS` for a Clojure program, `:main-class CLASS` for a Java forms
  program, neither for a Go program; `:name`, `:module`, `:go`), `src/`, `java/`, `go/`, `runs/`; the base's
  compiled namespaces reused (`ARBACE_PROGRAM_BASE`, `target/arbace-go`, the gate's cache, by `aot.key` and
  `:runtime`, else a `--build`), goi's table, c2j of the program's own units on the base, `bin/j2g --program`
  (new options `--entry`, `--entry-arg`, `--resources`; `arbace.j2g.embed/user-resources`), `bin/build-go`;
  a Go program by `bin/build-go` alone; Go forms in an Arbace program embedded and interpreted by goi; output
  `target/arbace-go/programs/NAME/ARCH/NAME`, cached by key; `--check` over `test/programs/` (hello-clojure,
  hello-java, hello-go, mixed: 8 of 8 runs; about 3-4 minutes a program, j2g about 20 GiB); `make program`,
  `make programs-check`; doc/go/BUILD.md "A user's program". Shipped code: `arbace/j2g/main.ae`, `embed.ae`
  (the shipped translation unchanged but j2g's own classes). On the branch the gate's `suite-aot-essential`
  j2g run was killed for memory beside another heavy slot's (rerun alone: passed); on the merge the gate
  passed whole (16m47s). Decided (the user): RPS90 (a), RPS91 (a), RPS92 (b) now (for the milestone),
  RPS93 (c), RPS94 (c), RPS95 (a) then (b), RPS96 (a), RPS97 (a) then (b), RPS98 (c), RPS99 (a).
- JC5's merge into main conflicted in seven files (the three provenance files, `check-interp.edn`,
  `check-compiled.edn`, `divergences.edn`, the survey): both sides re-recorded the levels on different code.
  Aborted; JC5 merges main into its branch, reruns the provenance updates on a tool of the combined
  checkout and re-records the levels there. Decided (the user): JCS60-JCS64 (a), JCS65 (a), JCS66 (a),
  JCS67 (a) and (c), JCS68 (a), JCS69 (a) then (b).
- Started (the user): REAP7b (RPS92 (b), RPS93 (c), RPS94 (c), RPS98 (c)).

## 2026-10-25: after the milestone, the scripts' sweep before any C23 work (the user)

- Decided (the user): after `arbace-reaps-3-crops-v1`, the scripts' sweep of `/dev/null` and process
  substitution comes first; any C23 work (its survey included) starts after it. This corrects the entry
  "after the milestone, C23 at parity the main effort" above, which had the sweep queued beside C23.

## 2026-10-25: the milestone's plan kept whole (the user)

- Considered: a shortcut to the tag (JC6 cut to naming the "other, to triage" catch-alls and recording the
  world's classes deferred, the corpus widening of RPS7 moved after the tag; about half a day to a day
  sooner). Decided (the user): the plan kept as decided, JC6 whole (the corpus widened, the world ranked and
  decided area by area, the decided items scheduled with it), so that the three crops are complete at
  `arbace-reaps-3-crops-v1`: after the milestone the work opens a new topic, C23's support (after the
  scripts' sweep), and should not have to come back to them.
