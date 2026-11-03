# Coverage of Arbace's Go executables (doc/GATE-SURVEY.md, GT8.3, GTS206 (a)). Sourced (after
# bin/lib/gate-cache.bash). Defines:
#
#   cover_crops                  the crops whose code under check is measured: java go c clojure
#   cover_pkg_re CROP            an extended regex of the import paths of CROP's code under check, over the
#                                packages of the program j2g makes (module arbace): Java's, jrt (the Java
#                                runtime and the translated JDK) and the Java forms' front end and interpreter
#                                (arbace.classes.*), GTS205 (a)'s "jrt and the interpreter" (not arbace/lang, the
#                                Clojure runtime, review 182 finding 2); Go's, goi's back end
#                                go/arbace/goi and its namespaces (arbace.go, arbace.go.*); C's, ci's back end
#                                (arbace/ci, arbace/crt, arbace/cinterp), ci's namespaces (arbace.c, arbace.c.*:
#                                the checker, the builder, cf2g) and repr/c's (the front half, VM report 160's
#                                finding 2); Clojure's, arbace/lang and the vendored Clojure's namespaces with
#                                c2j (GTS205 (a): the runtime and c2j)
#   cover_pkgs PROG              the cover build's -coverpkg: the packages of PROG (j2g's program,
#                                PROG/go/arbace/...) that any crop's regex takes, comma-separated, and the main
#                                package arbace/cmd/arbace (one build for every crop; a crop's blocks are its packages' when the profiles are read)
#   cover_cache ROOT [KEY]       where the cover build of ROOT's gate_key is: ARBACE_COVER_CACHE when set, else
#                                ARBACE_GATE_EXES/cover-KEY20 (beside the gate's builds, KEY20/)
#   cover_exe ROOT [NAME]        the cover build's NAME (arbace, default, or arbace-tests) at ROOT's key: its path,
#                                status 1 with a note when it is not built (bin/arbace-go --gate-build --cover), or
#                                when ARBACE_COVER_DIR holds another build's data (cover_claim: RUN/key)
#   cover_env TOOL ID            when ARBACE_COVER_DIR is set: GOCOVERDIR=DIR for the run of test ID by TOOL (a
#                                check and its level, java-interp, goi, ...), DIR = ARBACE_COVER_DIR/TOOL/ID with /
#                                in ID as %, made, ID written into DIR/id; nothing when unset
#   cover_settle TOOL ID         after the run: the meta-data file the run wrote (about 15 MB, Go writes it into
#                                every GOCOVERDIR) kept once in ARBACE_COVER_DIR/meta, hard-linked from DIR
#   cover_exclude CROP           the source files of CROP's packages that are no code under check (bin/cover blocks)
#   cover_go_flags ROOT PROG WORK OVERLAY OUT
#                                bin/build-go's arguments for a cover build of the j2g program PROG
#
# A check run with ARBACE_COVER_DIR set runs the cover build's executable (cover_exe) in place of the gate's,
# each test with its GOCOVERDIR: bin/java-check's interp and compiled levels (the compiled level's programs
# built with the same -cover flags), bin/goi check (run, directory and errorcheck programs). bin/cover reads
# the directories (blocks, subset).

cover_crops() { echo java go c clojure; }

# a relative ARBACE_COVER_DIR made absolute once: the checks run their programs from other directories (goi
# from $GOROOT/test, java-check from the test's), review 182 finding 3
[ -z "${ARBACE_COVER_DIR:-}" ] || { ARBACE_COVER_DIR=$(realpath -m "$ARBACE_COVER_DIR"); export ARBACE_COVER_DIR; }

# cover_exclude CROP: an extended regex of the source files (textfmt's FILE) of CROP's packages that are not code
# under check: the user classes j2g puts in arbace/lang (J_..._J, names.ae's user-class?: the compiled level's
# tests, the fixtures), each reached by its own test alone, which would force every test into the cover (review
# 182 finding 2)
cover_exclude() { echo '^arbace/lang/J_'; }

cover_pkg_re() {
  case $1 in
    java) echo '^arbace/(jrt|aot_arbace_classes(_[a-z_]+)?)$' ;;
    go) echo '^arbace/(goi|aot_arbace_go(_[a-z_]+)?)$' ;;
    c) echo '^arbace/(ci|crt|cinterp|aot_arbace_c|aot_arbace_c_[a-z0-9_]+|aot_arbace_repr_c(_[a-z0-9_]+)?)$' ;;
    clojure) echo '^arbace/(lang|aot_arbace_(core|core_[a-z_]+|c2j_[a-z_]+|data|datafy|edn|genclass|gvec|inspector|instant|java_[a-z_]+|main|math|pprint|pprint_[a-z_]+|reflect|reflect_java|repl|set|spec_[a-z_]+|stacktrace|string|template|test|test_[a-z_]+|uuid|walk|xml|zip))$' ;;
    *) echo "cover: no crop $1 (java, go, c, clojure)" >&2; return 1 ;;
  esac
}

cover_pkgs() {
  local prog=$1 re c
  re=$(for c in $(cover_crops); do cover_pkg_re "$c"; done | paste -sd'|')
  # and the main package: Go initializes the coverage output only from an instrumented main (VM report 160); its
  # blocks are no crop's
  { (cd "$prog/go" && find arbace -mindepth 1 -maxdepth 1 -type d) | LC_ALL=C sort | grep -E "$re"; echo arbace/cmd/arbace; } | paste -sd,
}

cover_cache() {
  local root=$1 key=${2:-}
  [ -z "${ARBACE_COVER_CACHE:-}" ] || { echo "$ARBACE_COVER_CACHE"; return 0; }
  [ -n "$key" ] || key=$("$root/bin/arbace-go" --gate-key) || return 1
  echo "$(arbace_gate_exes "$root")/cover-${key:0:20}"
}

# cover_claim CACHE: ARBACE_COVER_DIR holds one cover build's data (review 182 L5): RUN/key names the build its
# counters come from (CACHE/key, the cover build's key; else the executable's digest), written by the first check
# run into it; a check run of another build is refused (status 1), so that no counters of two builds mix and bin/cover
# blocks reads the packages of the build that ran. Nothing without ARBACE_COVER_DIR
cover_claim() {
  [ -n "${ARBACE_COVER_DIR:-}" ] || return 0
  local k have t
  k=$(cat "$1/key" 2>"${TMPDIR:-.}/cover-key.err") || k=
  [ -n "$k" ] || k=exe-$(sha256sum < "$1/amd64/arbace" | cut -c1-64) || return 1
  mkdir -p "$ARBACE_COVER_DIR" || return 1
  if [ ! -e "$ARBACE_COVER_DIR/key" ]; then
    # (two checks starting at once: one link wins, the other compares)
    t=$ARBACE_COVER_DIR/.key.$$; echo "$k" > "$t" && { ln "$t" "$ARBACE_COVER_DIR/key" 2>"${TMPDIR:-.}/cover-key.err"; rm -f "$t"; }
  fi
  have=$(cat "$ARBACE_COVER_DIR/key" 2>"${TMPDIR:-.}/cover-key.err")
  [ "$have" = "$k" ] && return 0
  echo "cover: $ARBACE_COVER_DIR holds the data of another cover build (its key ${have:0:12}, this build's ${k:0:12}: $1): a directory of its own for each build" >&2
  return 1
}

cover_exe() {
  local root=$1 n=${2:-arbace} c k
  c=$(cover_cache "$root") || return 1
  # its key the checkout's cover key (the gate's, the hook, this file: review 182 finding 5); an
  # ARBACE_COVER_CACHE is taken as given, with a note
  if [ -n "${ARBACE_COVER_CACHE:-}" ]; then
    [ -x "$c/amd64/$n" ] && { echo "cover: $c/amd64/$n (ARBACE_COVER_CACHE), not compared with the checkout's key" >&2; cover_claim "$c" || return 1; echo "$c/amd64/$n"; return 0; }
  else
    k=$("$root/bin/arbace-go" --cover-key) || return 1
    [ -x "$c/amd64/$n" ] && [ "$(cat "$c/key" 2>"${TMPDIR:-.}/cover-key.err")" = "$k" ] && { cover_claim "$c" || return 1; echo "$c/amd64/$n"; return 0; }
  fi
  echo "cover: no cover build at the checkout's key ($c/amd64/$n; bin/arbace-go --gate-build --cover)" >&2
  return 1
}

cover_env() {
  [ -n "${ARBACE_COVER_DIR:-}" ] || return 0
  local d=$ARBACE_COVER_DIR/$1/${2//\//%}
  mkdir -p "$d" "$ARBACE_COVER_DIR/meta" || return 1
  printf '%s\n' "$2" > "$d/id"
  echo "GOCOVERDIR=$d"
}

# cover_settle TOOL ID: after the run of ID, the meta-data file it wrote (Go writes it into each GOCOVERDIR,
# whatever is there already: its size check never holds, go1.27.1's cfile.openMetaFile) kept once in
# ARBACE_COVER_DIR/meta and linked from the test's directory, where go tool covdata reads it
cover_settle() {
  [ -n "${ARBACE_COVER_DIR:-}" ] || return 0
  local d=$ARBACE_COVER_DIR/$1/${2//\//%} m s
  for m in "$d"/covmeta.*; do
    [ -f "$m" ] && [ ! -L "$m" ] || continue
    s=$ARBACE_COVER_DIR/meta/${m##*/}
    if [ -f "$s" ]; then rm -f "$m"; else mv -f "$m" "$s"; fi
    # a hard link (a run's directory copied with cp -a or tar keeps it, review 182 L7), else a symbolic one
    ln "$s" "$m" 2>"${TMPDIR:-.}/cover-ln.err" || ln -s "$s" "$m"
  done
}

# cover_hook_table: the hook's files, a line each, PACKAGE FILE: the Go package (under GOROOT/src) the cover builds'
# overlay adds FILE (relative to the checkout) to, as zz_arbace_cover.go: internal/coverage/cfile's hook, its
# os/signal part (the program's own signal registrations, review 182 L3) and its runtime part (whether a timer is
# pending, for the deadlock detector, review 182 L2)
cover_hook_table() {
  printf '%s\n' "internal/coverage/cfile tools/covsubset/hook/cover.go" "os/signal tools/covsubset/hook/signal/refs.go" \
    "runtime tools/covsubset/hook/runtime/timers.go"
}
# cover_hook_files: the hook's files (in the cover key, bin/arbace-go's cover_key, and the compiled level's program
# key, bin/java-check), relative to the checkout
cover_hook_files() { cover_hook_table | cut -d' ' -f2 | paste -sd' '; }

# cover_go_flags ROOT PROG WORK OVERLAY OUT: bin/build-go's arguments, one a line, for a cover build of the
# j2g program PROG printed into WORK with the overlay OVERLAY (bin/jrt overlay's): the overlay with the hook's
# files (cover_hook_table) added to their Go packages, written to OUT, and the cover flags
cover_go_flags() {
  local root=$1 prog=$2 work=$3 overlay=$4 ov=$5 pkgs g=${ARBACE_GOROOT:-/root/tamago-go}/src e="" p f
  pkgs=$(cover_pkgs "$prog")
  [ -n "$pkgs" ] || { echo "cover: no package of $prog to cover" >&2; return 1; }
  while read -r p f; do e="$e,\n \"$g/$p/zz_arbace_cover.go\": \"$root/$f\""; done <<< "$(cover_hook_table)"
  sed '$s|}}[[:space:]]*$|'"$e"'}}|' "$overlay" > "$ov"
  [ "$(grep -c zz_arbace_cover "$ov")" = "$(cover_hook_table | wc -l)" ] || { echo "cover: could not add the hook to $overlay" >&2; return 1; }
  printf '%s\n' --overlay "$ov" --go-flag -cover --go-flag -covermode=set --go-flag "-coverpkg=$pkgs" --go-flag -tags=arbacecover
}
