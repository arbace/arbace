# The functions of a program's build (RPS97 (b), doc/FULL-SUPPORT-SURVEY.md, "As built: RPS97 (b)"):
# the base (the shipped build whose compiled namespaces a program reuses, and its key), the linker's
# flags, and the calls of bin/j2g --program and bin/build-go with the flags every program build
# shares. Sourced by bin/arbace-go (--build, --build-program), bin/clojure-tests (--aot) and
# bin/java-check (the compiled level); each keeps what is its own (the suite's hook, the shards, the
# refusals). The caller defines $root; the base's functions also use $dir (target/arbace-go),
# $programs, $discard, since and build, as bin/arbace-go defines them.

. "$root/bin/lib/version.bash"

# the linker's flags of the executables' final links (image, pgo_build), set in ldflags by
# link_flags: without the symbol table and DWARF (-s -w; amendment SZ1): Go's own tables
# (pclntab), which panics' traces, jrt's stack traces and profiles read, stay;
# ARBACE_GO_SYMBOLS=1 keeps them (go tool nm, debuggers). And Arbace's version
# (bin/lib/version.bash; J2G-SPEC §10.6): the main package's arbaceVersion, set with -X, read
# from the checkout (or VERSION in a tarball) at link time, so j2g's output and the image of
# prepared namespaces hold none. The first link (bin/build-go) leaves it empty: its executable
# only prepares the image
link_flags() {
  local v f
  v=$(arbace_version "$root") || return 1
  f="-X 'main.arbaceVersion=$v'"
  [ "${ARBACE_GO_SYMBOLS:-}" = 1 ] || f="-s -w $f"
  ldflags=("-ldflags=$f")
}

# aot_key: what the compiled namespaces' tree is a function of (standalone S6, SA144, SA157):
# the runtime key (bin/lib/version.bash; the program that compiles them has that runtime) and
# Arbace's sources (the namespaces compiled and the back end, arbace.c2j, which compiles them):
# bin/lib/version.bash's arbace_aot_key, the embedded ones since GT2 (doc/GATE-SURVEY.md, GTS5 (a))
aot_key() { arbace_aot_key "$root"; }

# aot_key_write DIR: records the key of the tree DIR just made, in DIR.key, and beside it the runtime
# key it was made with, in DIR.runtime (JCS76 (a): aot_key hashes the runtime with Arbace's
# sources, so it cannot be read back; a tree copied without its executable still names its
# runtime, which bin/java-check's compiled level reads)
aot_key_write() { aot_key > "$1.key"; arbace_runtime_key "$root" > "$1.runtime"; }

# seed_aot HOST OUT: stage 0's compiled namespaces (RPS95 (b)): HOST, an executable of the
# checkout's runtime, compiles the checkout's namespaces into OUT/aot and records OUT/aot.key and
# OUT/aot.runtime, so that OUT, bin/seed --build's directory, serves --build-program as a base
# (program_base). Status 1: HOST's runtime is not the checkout's (nothing made); 2: the compiling
# failed. One at a time (the log is shared)
seed_aot() {
  local host=$1 out=$2 variant=-seed
  mkdir -p "$dir" "$root/.tmp"
  (
    flock 9
    host_compiles "$host" 2>&1 || exit 1
    host_compile "$host" "$out/aot" || exit 2
    aot_key_write "$out/aot"
  ) 9> "$root/.tmp/seed-aot.lock"
}

# host_compile HOST OUT: the checkout's namespaces compiled to Java forms into OUT by HOST, when
# HOST's runtime is the checkout's (the :runtime of its version map, SA144): HOST prepares them
# from the checkout's sources (ARBACE_PATH_FIRST, as embedded ones, analyzed afresh: no compiled
# loader, no image), in the order the program's namespaces.txt lists them, then arbace.c2j
# compiles the units (host_compiles first: status 1 when HOST's runtime differs); status 2 when
# the compiling failed
host_compiles() {
  local host=$1 key hkey
  . "$root/bin/lib/version.bash"
  key=$(arbace_runtime_key "$root")
  hkey=$(env -u ARBACE_NO_AOT "$host" -e '(print (:runtime *arbace-version*))' 2>"$discard")
  # ARBACE_AOT_ONEPASS=force: one run whatever the host (measurements; not for a fixpoint)
  if [ "$key" != "$hkey" ] && [ "${ARBACE_AOT_ONEPASS:-}" != force ]; then
    echo "== the namespaces compiled after a first build (the host's runtime is not the checkout's)" >&2
    return 1
  fi
}
host_compile() {
  local host=$1 out=$2 nss t0
  t0=$(date +%s)
  echo "== the namespaces compiled by the host (arbace.c2j; its runtime is the checkout's)" >&2
  mkdir -p "$dir"
  # the embedded namespaces as j2g lists them (namespaces.txt)
  nss=$(cd "$root" && ARBACE_PATH=$root "$host" -e '(require (quote arbace.j2g.embed) (quote arbace.string))
(doseq [[p text] (sort (arbace.j2g.embed/sources "."))
        :when (and (arbace.string/starts-with? p "arbace/") (arbace.string/ends-with? p ".ae") (re-find #"(?m)^\(ns( |$)" text))]
  (println (-> (subs p 0 (- (count p) 3)) (arbace.string/replace "/" ".") (arbace.string/replace "_" "-"))))' | tr '
' ' ') || return 1
  rm -rf "$out"
  printf '(doseq [n (quote [%s])]
  (try (require n) (catch Throwable e (println "image: not prepared:" n (.getMessage e)))))
(require (quote arbace.c2j.main))
(let [r (arbace.c2j.main/run "%s" nil)]
  (println "aot:" (count (:compiled r)) "sources compiled," (:classes r) "classes;" (count (:failed r)) "failed")
  (doseq [[s m] (:failed r)] (println "aot: not compiled:" s m)))
' "$nss" "$out"     > "$dir/host-compile$variant.ae"
  (cd "$root" && GOGC=${GOGC:-400} ARBACE_COMPILE=1 ARBACE_NO_AOT=1 ARBACE_NO_IMAGE=1 ARBACE_PATH_FIRST=1 \
     ARBACE_PATH=$root ARBACE_AOT_ROOT=$root ARBACE_PREPARE=$dir/host-compile$variant.image \
     "$host" "$dir/host-compile$variant.ae") > "$dir/host-compile$variant.log" 2>&1 \
    || { tail -20 "$dir/host-compile$variant.log" >&2; echo "arbace-go: the host's compilation failed" >&2; return 2; }
  grep '^aot:' "$dir/host-compile$variant.log" >&2
  [ -f "$out/packages.edn" ] || { echo "arbace-go: the host compiled nothing ($dir/host-compile$variant.log)" >&2; return 2; }
  echo "   compiled: $(since "$t0")" >&2
}

host_arch() { case $(uname -m) in aarch64) echo arm64 ;; *) echo amd64 ;; esac; }

# program_base: the shipped build whose compiled namespaces the program reuses (sets base_exe,
# base_aot): ARBACE_PROGRAM_BASE (a --build's directory, or a directory holding arbace, aot/ and
# aot.key, as the gate's cache), else target/arbace-go, else the gate's cache .tmp/arbace-go-gate/
# amd64, then stage 0, target/seed (bin/seed --build keeps its compiled namespaces, RPS95 (b)), the
# first whose compiled namespaces' key is the checkout's (aot_key: the runtime and Arbace's
# sources) and whose executable's runtime is the checkout's; else stage 0's compiled again when its
# runtime is the checkout's (seed_aot), else a --build for the host's architecture first
program_base() {
  local k rk c e
  k=$(aot_key); rk=$(arbace_runtime_key "$root")
  for c in ${ARBACE_PROGRAM_BASE:-} "$dir" "$root/.tmp/arbace-go-gate/amd64" "$root/target/seed"; do
    e=$c/$(host_arch)/arbace; [ -x "$e" ] || e=$c/arbace
    [ -x "$e" ] && program_aot_current "$c/aot" || continue
    mkdir -p "$programs"
    [ "$(env -u ARBACE_NO_AOT "$e" -e '(print (:runtime *arbace-version*))' 2> "$programs/.runtime.err")" = "$rk" ] || continue
    base_exe=$e base_aot=$c/aot
    echo "== the shipped build's compiled namespaces: $base_aot (key ${k:0:12}; $base_exe)" >&2
    return 0
  done
  # stage 0 (bin/seed --build) of the checkout's runtime, its compiled namespaces made before the
  # sources of arbace/ changed or never (RPS95 (b)): compiled again by it, no --build first
  e=$root/target/seed/$(host_arch)/arbace
  if [ -x "$e" ] && [ "$(env -u ARBACE_NO_AOT "$e" -e '(print (:runtime *arbace-version*))' 2> "$programs/.runtime.err")" = "$rk" ]; then
    echo "== stage 0's compiled namespaces made (its runtime is the checkout's; $root/target/seed/aot)" >&2
    if (seed_aot "$e" "$root/target/seed") >&2; then
      base_exe=$e base_aot=$root/target/seed/aot
      return 0
    fi
  fi
  echo "== no shipped build of the checkout's compiled namespaces (key ${k:0:12}): --build for $(host_arch) first" >&2
  (arches=$(host_arch) build) || return 1
  base_exe=$dir/$(host_arch)/arbace base_aot=$dir/aot
}

# program_aot_current AOTDIR: AOTDIR holds compiled namespaces (packages.edn) whose key, AOTDIR.key,
# is the checkout's (aot_key: the runtime and Arbace's sources). The base's test, in one place
program_aot_current() {
  [ -f "$1/packages.edn" ] && [ -f "$1.key" ] && [ "$(cat "$1.key")" = "$(aot_key)" ]
}

# program_overlay: the patched Go runtime's overlay file (bin/jrt overlay), printed
program_overlay() { (cd "$root" && bin/jrt overlay | tail -1); }

# program_j2g EXE LOG OUT ARG...: bin/j2g --program ARG... --out OUT on the executable EXE (the tools'
# host of the run), output in LOG; the status is j2g's. $program_heavy, when set, prefixes the
# command (a heavy slot, bin/java-check's)
program_j2g() {
  local exe=$1 log=$2 out=$3
  shift 3
  (cd "$root" && J2G_GO_EXE=$exe REPR_GO_EXE=$exe ${program_heavy:-} bin/j2g --program "$@" --out "$out") > "$log" 2>&1
}

# program_build_go EXE ARCH WORK OUT LOG PROG [ARG...]: bin/build-go of j2g's program PROG (its out/prog)
# into the executable OUT, for ARCH, in the work module WORK, output in LOG: the flags every
# program build shares (the lines layout, repr/go's print cache, the overlay, x509roots), ARG... (a
# go flag, a root) beside them; the status is build-go's
program_build_go() {
  local exe=$1 arch=$2 work=$3 out=$4 log=$5 prog=$6 overlay
  shift 6
  overlay=$(program_overlay) || return 1
  (cd "$root" && REPR_GO_EXE=$exe ${program_heavy:-} bin/build-go --line-file --arch "$arch" --work "$work" -o "$out" \
     --print-cache "$root/.tmp/repr-go-print-cache" --overlay "$overlay" --root "$root/x509roots" "$@" "$prog") > "$log" 2>&1
}
