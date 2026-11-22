# Arbace's version (doc/VENDOR-NOTES.md, hand change 20), for bin/arbace-go (the Go program's
# arbaceVersion, set with go build -ldflags -X) and bin/seed --make (the seed's SEED.edn; until
# S4 also the JVM stages' resource arbace/version.edn). Sourced. Defines:
#
#   arbace_version ROOT  prints the version map, read by *arbace-version*:
#                          {:describe "arbace-for-go1.27.1-v1-6-ge2a9f6b" :commit "<40 hex>" :dirty false
#                           :runtime "<64 hex>"} (:runtime: arbace_runtime_key, below)
#                        In a git checkout whose top level is ROOT: git describe --tags --always
#                        --dirty, git rev-parse HEAD, and :dirty when describe ends -dirty
#                        (tracked files modified; the gitignored target/ and .tmp/ do not
#                        count). Otherwise (a tarball, possibly unpacked inside another git
#                        repository, as abuild's src/ in aports) ROOT/VERSION as git archive
#                        filled it (export-subst): :dirty false, :describe the short commit
#                        when no tag describes it. Neither: {:describe "unknown" :commit nil
#                        :dirty nil}.
#                        ARBACE_VERSION set (bin/seed --verify sets it to the seed's own :version): printed
#                        instead of the computed map, when its :commit is HEAD, the tree is clean and its
#                        :runtime the checkout's; else refused (status 1). The describe abbreviates to 12
#                        (queue 037, finding 1: git's automatic length grows with the repository).
#
# The describe takes every tag (no --match): main descends from the freezes, so it reads
# <last freeze tag>-<commits since>-g<hash> until main has a tag of its own, which then wins as
# the nearest; the frozen branches' later tags (v2, v3 on arbace-for-java-26) are not ancestors
# of main and never show.

# The crops' files out of every key (PYS30 (a), the user 2026-11-04; doc/GATE-SURVEY.md, "As built: GT9 (write-ahead)", the keys
# commit): one list, find's -path patterns, that gate_key and gate_rest_key (bin/arbace-go), arbace_runtime_key,
# arbace_embedded_sources (here), arbace_javac_key and arbace_javac_rest_key (bin/lib/javac-key.bash) all leave out, so that a
# crop's commit (its namespaces, its Go forms, its scripts' libraries) rebuilds no executable, no javac tool and no compiled
# namespaces, and reruns no step on :gate, :prog, :javac or :javac-prog. In step with arbace/j2g/embed.ae's excluded (the
# namespaces no executable embeds) and test/gate/audit.ae's hosts (test/gate/gate-key-crops-test.bash checks the three).
# A crop that is embedded (its re-seed window) leaves the list. Not here, as they are C's (embedded since C9) or the four's:
# bin/lib/ci, bin/lib/repr-c (gate_key's own), repr/java (javac's tool embeds it), go/arbace/{ci,cinterp,crt}.
arbace_crop_paths=(
  # the crops' Arbace namespaces (embed.ae's excluded): repr/haskell, rust, ocaml; rsi's and llvmi's tools (W-P5 (a), W-D4 (a))
  arbace/repr/haskell.ae 'arbace/repr/haskell/*' arbace/repr/rust.ae 'arbace/repr/rust/*' arbace/repr/ocaml.ae 'arbace/repr/ocaml/*'
  arbace/rust.ae 'arbace/rust/*' 'arbace/llvm/*'
  # repr/asm, the assembly crops' bridge (doc/asm/SPEC.md; AX1)
  arbace/repr/asm.ae 'arbace/repr/asm/*'
  # the assembler of the assembly crops: TableGen, amd64's table generator, matcher, encoder and check (AX2)
  'arbace/asm/*'
  # repr/wasm, WebAssembly's bridge, and the Wasm crop's namespaces (arbace.wasm.*: the validator, w2g, the check; doc/wasm/WASM-SURVEY.md
  # §13.2, WAS24 (a), WA1)
  arbace/repr/wasm.ae 'arbace/repr/wasm/*' 'arbace/wasm/*'
  # W2 (doc/W-WINDOW.md, the keys at W2): repr/scheme, arbace/scheme, repr/cpp, repr/llvm, arbace/cpp, repr/python and
  # arbace/python embedded (CP180 (a), SC230 (a), PYS28 (a), W-D1 (a)), so they leave the list; not C++'s check tools that
  # compare with an oracle's dump (W-P4 (a))
  arbace/repr/cpp/ncheck.ae arbace/repr/cpp/tcheck.ae arbace/repr/llvm/mutate.ae
  # and the mutants' own verdicts, which only mutate.ae requires (review 341, D341-1 (a), the user 2026-11-18)
  arbace/repr/llvm/expect.ae
  # their Go forms: OCaml's C API (oicapi), pyi's lazy XML parser (expat, pyexpat), hsi's (hs). W2: oi (W-D5 (a), oi alone), si's
  # scm and scminterp, pyi's py and the libm port it imports are copied into the program and listed in goi's table, so they
  # leave the list (gate_key's and the javac key's; out of the runtime key by arbace_runtime_not, W-P3 (a))
  go/arbace/oicapi.ae 'go/arbace/oicapi/*' go/arbace/expat.ae 'go/arbace/expat/*' go/arbace/pyexpat.ae 'go/arbace/pyexpat/*'
  go/arbace/hs.ae 'go/arbace/hs/*'
  # x86i's (the x86-64 integer machine, AX5) and the frame of the machine emulators it runs on, a light driver (AXS51 (a))
  go/arbace/x86i.ae 'go/arbace/x86i/*' go/arbace/emu.ae 'go/arbace/emu/*' go/arbace/a64i.ae 'go/arbace/a64i/*'
  # wai's (the Wasm engine, a light driver; WAS6 (a), WA4)
  go/arbace/wai.ae 'go/arbace/wai/*'
  # pyi's lazy modules (queue 208, 209, 215: _bz2's, _lzma's, _zstd's, _ssl's and _sqlite3's glue; W-P6 (a)). Queue 521
  # (DECISION 521-2 (b)): the C library host go/arbace/clib (its worlds and their compiled packages) and _ctypes's package
  # go/arbace/pyctypes are listed in goi's table, so they leave the list (out of the runtime key by arbace_runtime_not)
  go/arbace/pybz2.ae 'go/arbace/pybz2/*' go/arbace/pylzma.ae 'go/arbace/pylzma/*'
  go/arbace/pyzstd.ae 'go/arbace/pyzstd/*' go/arbace/pyssl.ae 'go/arbace/pyssl/*' go/arbace/pysqlite3.ae 'go/arbace/pysqlite3/*'
  # OCaml's AOT runtime forms (o2j's Java, no executable's)
  'jdk/ocaml/*'
  # the crops' scripts' libraries
  'bin/lib/oi/*' 'bin/lib/repr-cpp/*' 'bin/lib/cppi/*' 'bin/lib/rsi/*' 'bin/lib/hsi/*' 'bin/lib/si/*' 'bin/lib/pyi/*'
  'bin/lib/repr-haskell/*' 'bin/lib/repr-rust/*' 'bin/lib/repr-ocaml/*' 'bin/lib/repr-scheme/*' 'bin/lib/repr-python/*'
  'bin/lib/repr-llvm/*' 'bin/lib/crops/*' 'bin/lib/repr-wasm/*' 'bin/lib/wai/*'
)
# W-P3 (a) (doc/W-WINDOW.md, PYS30 (a)'s reason): the embedded crops' Go forms, which c2j's compilation of the namespaces never
# depends on (it analyzes the runtime's classes, not their Go natives), stay out of arbace_runtime_key alone
arbace_runtime_not=(! -path go/arbace/oi.ae ! -path 'go/arbace/oi/*' ! -path go/arbace/scm.ae ! -path 'go/arbace/scm/*'
                    ! -path go/arbace/scminterp.ae ! -path 'go/arbace/scminterp/*' ! -path go/arbace/py.ae ! -path 'go/arbace/py/*'
                    ! -path go/arbace/libm.ae ! -path 'go/arbace/libm/*' ! -path go/arbace/clib.ae ! -path 'go/arbace/clib/*'
                    ! -path go/arbace/pyctypes.ae ! -path 'go/arbace/pyctypes/*')
arbace_not_crops=()
for _p in "${arbace_crop_paths[@]}"; do arbace_not_crops+=(! -path "$_p"); done
unset _p

# arbace_runtime_key ROOT: the SHA-256 of what a program's compilation of namespaces depends on
# in its runtime (standalone S6, doc/STANDALONE-NOTES.md, SA144, SA147): the runtime's class
# forms and their Go-build variants (the compiler, the classes it resolves members of), jrt
# (its classes and natives), the JDK's forms, data and jrt's own Java; not the namespaces'
# sources, nor j2g and the Java forms' front end (namespaces the compiling host loads from the
# checkout; their translation of the runtime shows in the next build, and the fixpoint's G2 =
# G3 proves it), nor j2g's edge (jdk/edge.edn: j2g's data, not the runtime's), nor javac's
# forms and the tool's own Java (jdk/javac, overlay/jdk/javac: the input of javac's tool build,
# not the program's, doc/classes/JAVA-FRONTEND-SURVEY.md, "As built: JN2"), Go's runtime
# patches, the roots and Go's pin; nor jdk/provenance.edn (since GT2: what repr/java wrote the forms
# with, its sources' hash and the tool, not the runtime; gate_key leaves it out likewise, so that
# a provenance rewritten keeps the gate's executables and their runtime the checkout's); nor jdk/platform/ (JN10
# part 2, the java.base pack of repr/java's shipped reader: no program embeds it until the read closure is
# embedded, doc/classes/JAVA-FRONTEND-SURVEY.md, "As built: JN10 (write-ahead)", JFS147); nor the crops' Go forms
# (go/arbace/{oi,scm,scminterp,py,libm}{.ae,/}: oi's, si's, pyi's and the libm port hsi and pyi import; the user, 2026-11-03,
# before the re-seed after the backlog: a crop's Go change reruns no compile of the runtime's namespaces; c2j analyzes
# the runtime's classes, not their Go natives; ci's, cinterp's and crt's stay, C's embedded runtime). Two programs of one runtime key
# analyze a checkout's namespaces alike, so a build's host of that key compiles them (one j2g
# run).
arbace_runtime_key() {
  local discard=${TMPDIR:-$1/.tmp/tmp}/version.discard
  mkdir -p "${discard%/*}"
  (cd "$1" && find arbace/lang go overlay/jdk jdk -type f ! -path jdk/edge.edn ! -path 'jdk/javac/*' ! -path jdk/provenance.edn \
       ! -path 'jdk/platform/*' "${arbace_not_crops[@]}" "${arbace_runtime_not[@]}" \
       ! -path 'overlay/jdk/javac/*' 2>"$discard" \
     | LC_ALL=C sort | xargs -d '\n' sha256sum | sha256sum | cut -c1-64)
}

arbace_version() {
  local r top d c dirty=false discard=${TMPDIR:-$1/.tmp/tmp}/version.discard
  r=$(cd "$1" && pwd -P) || return 1
  mkdir -p "${discard%/*}"
  top=$(git -C "$r" rev-parse --show-toplevel 2>"$discard") && top=$(cd "$top" && pwd -P)
  if [ -n "$top" ] && [ "$top" = "$r" ] && c=$(git -C "$r" rev-parse HEAD 2>"$discard"); then
    # --abbrev=12 (queue 037, finding 1): git's automatic length grows with the repository's objects
    d=$(git -C "$r" describe --tags --always --dirty --abbrev=12) || return 1
    case $d in *-dirty) dirty=true ;; esac
    if [ -n "${ARBACE_VERSION:-}" ]; then
      # a version map given (bin/seed --verify: the seed's own, so that a clone with other tags prints the
      # same seed; queue 037, finding 1, CS255 (a)): honoured only for this commit, clean, of this runtime
      local want="{:describe \"[^\"]*\" :commit \"$c\" :dirty false :runtime \"$(arbace_runtime_key "$r")\"}"
      if [ "$dirty" = true ] || ! printf '%s\n' "$ARBACE_VERSION" | grep -qx -- "$want"; then
        echo "arbace_version: ARBACE_VERSION $ARBACE_VERSION is not a version map of $r (commit $c, dirty $dirty, its runtime key)" >&2
        return 1
      fi
      printf '%s\n' "$ARBACE_VERSION"
      return 0
    fi
  elif [ -f "$r/VERSION" ] && ! grep -q '\$Format:' "$r/VERSION"; then
    c=$(sed -n 's/^commit //p' "$r/VERSION")
    d=$(sed -n 's/^describe //p' "$r/VERSION")
    [ -n "$d" ] || d=${c:0:7}
  else
    printf '{:describe "unknown" :commit nil :dirty nil}\n'
    return 0
  fi
  printf '{:describe "%s" :commit "%s" :dirty %s :runtime "%s"}\n' "$d" "$c" "$dirty" "$(arbace_runtime_key "$r")"
}

# arbace_embedded_sources ROOT: Arbace's sources a shipped build reads, one path a line, sorted
# (doc/GATE-SURVEY.md, GTS5 (a), "As built: GT1-GT3"): arbace/ less the namespaces no executable
# embeds and nothing of the build reads: repr/java (javac's tool embeds it itself: bin/arbace-go's reader_key names it) and the
# crops' namespaces of arbace_crop_paths (repr/haskell, rust, ocaml, rsi's arbace/rust, llvmi's arbace/llvm, C++'s oracle check tools;
# arbace/j2g/embed.ae's excluded; repr/c and ci's arbace/c are embedded since C9; repr/scheme and arbace/scheme, repr/cpp, repr/llvm
# and arbace/cpp, repr/python and arbace/python since W2, doc/W-WINDOW.md)
arbace_embedded_sources() {
  (cd "$1" && find arbace -type f ! -path arbace/repr/java.ae ! -path 'arbace/repr/java/*' "${arbace_not_crops[@]}" | LC_ALL=C sort)
}

# arbace_embedded_resources ROOT: the checkout's files a shipped build's generated resources are made from, one path a line,
# sorted (arbace/j2g/embed.ae's generated: si's psyntax.pp as Scheme text, arbace.scheme.resource, from
# scheme/psyntax/psyntax.pp.ae; doc/scheme/SCHEME-SURVEY.md, SC240, "As built: the embedding's gaps"): gate_key hashes them
# beside the sources (the generators are embedded sources), arbace_aot_key not (no namespace is compiled from them)
arbace_embedded_resources() {
  (cd "$1" && find scheme/psyntax/psyntax.pp.ae -type f | LC_ALL=C sort)
}

# arbace_aot_key ROOT: the key of a build's compiled namespaces (bin/arbace-go's aot_key, the
# shipped build's aot.key; bin/java-check's --essential-compiled compares it): the runtime key
# and the embedded sources (the namespaces compiled and c2j, which compiles them)
arbace_aot_key() {
  (cd "$1" && { arbace_runtime_key "$1"; arbace_embedded_sources "$1" | xargs -d '\n' sha256sum; } \
     | sha256sum | cut -c1-64)
}

# arbace_reader_runtime_key ROOT: the part of the runtime the compilation of repr/java's reader depends on (bin/arbace-go's
# reader_key, round 54, doc/classes/JAVA-FRONTEND-SURVEY.md, "As built / proposed: the reader's key by its load closure"):
# arbace_runtime_key's files less the Go natives that compilation never runs: C's (ci, cinterp, crt: CInterp's), goi's
# back end (goi: GoInterp's, for go/ namespaces alone), repr/go's Go packages (go/arbace/repr: goi's table's), goi's
# table itself, and the Markdown notes. c2j analyzes the runtime's classes; the natives it runs are jrt's and the
# program's own (go/arbace.ae, go/arbace/jrt, std, api, cmd), which stay
arbace_reader_runtime_key() {
  local discard=${TMPDIR:-$1/.tmp/tmp}/version.discard
  mkdir -p "${discard%/*}"
  (cd "$1" && find arbace/lang go overlay/jdk jdk -type f ! -path jdk/edge.edn ! -path 'jdk/javac/*' ! -path jdk/provenance.edn \
       ! -path 'jdk/platform/*' "${arbace_not_crops[@]}" "${arbace_runtime_not[@]}" ! -path 'overlay/jdk/javac/*' \
       ! -path go/arbace/ci.ae ! -path 'go/arbace/ci/*' ! -path go/arbace/cinterp.ae ! -path 'go/arbace/cinterp/*' \
       ! -path go/arbace/crt.ae ! -path 'go/arbace/crt/*' ! -path go/arbace/goi.ae ! -path 'go/arbace/goi/*' \
       ! -path 'go/arbace/repr/*' ! -path go/arbace/gotable.edn ! -name '*.md' 2>"$discard" \
     | LC_ALL=C sort | xargs -d '\n' sha256sum | sha256sum | cut -c1-64)
}
