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
#
# The describe takes every tag (no --match): main descends from the freezes, so it reads
# <last freeze tag>-<commits since>-g<hash> until main has a tag of its own, which then wins as
# the nearest; the frozen branches' later tags (v2, v3 on arbace-for-java-26) are not ancestors
# of main and never show.

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
# a provenance rewritten keeps the gate's executables and their runtime the checkout's). Two programs of one runtime key
# analyze a checkout's namespaces alike, so a build's host of that key compiles them (one j2g
# run).
arbace_runtime_key() {
  local discard=${TMPDIR:-$1/.tmp/tmp}/version.discard
  mkdir -p "${discard%/*}"
  (cd "$1" && find arbace/lang go overlay/jdk jdk -type f ! -path jdk/edge.edn ! -path 'jdk/javac/*' ! -path jdk/provenance.edn \
       ! -path 'overlay/jdk/javac/*' 2>"$discard" \
     | LC_ALL=C sort | xargs -d '\n' sha256sum | sha256sum | cut -c1-64)
}

arbace_version() {
  local r top d c dirty=false discard=${TMPDIR:-$1/.tmp/tmp}/version.discard
  r=$(cd "$1" && pwd -P) || return 1
  mkdir -p "${discard%/*}"
  top=$(git -C "$r" rev-parse --show-toplevel 2>"$discard") && top=$(cd "$top" && pwd -P)
  if [ -n "$top" ] && [ "$top" = "$r" ] && c=$(git -C "$r" rev-parse HEAD 2>"$discard"); then
    d=$(git -C "$r" describe --tags --always --dirty) || return 1
    case $d in *-dirty) dirty=true ;; esac
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
# embeds and nothing of the build reads, repr/java, repr/haskell, repr/rust and repr/c (arbace/j2g/embed.ae's
# excluded; javac's tool embeds repr/java itself: bin/arbace-go's reader_key names it)
arbace_embedded_sources() {
  (cd "$1" && find arbace -type f ! -path arbace/repr/java.ae ! -path 'arbace/repr/java/*' \
       ! -path arbace/repr/haskell.ae ! -path 'arbace/repr/haskell/*' ! -path arbace/repr/rust.ae \
       ! -path 'arbace/repr/rust/*' ! -path arbace/repr/c.ae ! -path 'arbace/repr/c/*' | LC_ALL=C sort)
}

# arbace_aot_key ROOT: the key of a build's compiled namespaces (bin/arbace-go's aot_key, the
# shipped build's aot.key; bin/java-check's --essential-compiled compares it): the runtime key
# and the embedded sources (the namespaces compiled and c2j, which compiles them)
arbace_aot_key() {
  (cd "$1" && { arbace_runtime_key "$1"; arbace_embedded_sources "$1" | xargs -d '\n' sha256sum; } \
     | sha256sum | cut -c1-64)
}
