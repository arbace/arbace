# Coverage of Arbace's Go executables (doc/GATE-SURVEY.md, GT8.3, GTS206 (a)). Sourced (after
# bin/lib/gate-cache.bash). Defines:
#
#   cover_crops                  the crops whose code under check is measured: java go c clojure
#   cover_pkg_re CROP            an extended regex of the import paths of CROP's code under check, over the
#                                packages of the program j2g makes (module arbace): Java's, jrt (the Java
#                                runtime and the translated JDK), the translated runtime arbace/lang and the
#                                Java forms' front end and interpreter (arbace.classes.*); Go's, goi's back end
#                                go/arbace/goi and its namespaces (arbace.go, arbace.go.*); C's, ci's back end
#                                (arbace/ci, arbace/crt, arbace/cinterp), ci's namespaces (arbace.c, arbace.c.*:
#                                the checker, the builder, cf2g) and repr/c's (the front half, VM report 160's
#                                finding 2); Clojure's, arbace/lang and the vendored Clojure's namespaces with
#                                c2j (GTS205 (a): the runtime and c2j)
#   cover_pkgs PROG              the cover build's -coverpkg: the packages of PROG (j2g's program,
#                                PROG/go/arbace/...) that any crop's regex takes, comma-separated (one build
#                                for every crop; a crop's blocks are its packages' when the profiles are read)
#   cover_cache ROOT [KEY]       where the cover build of ROOT's gate_key is: ARBACE_COVER_CACHE when set, else
#                                ARBACE_GATE_EXES/cover-KEY20 (beside the gate's builds, KEY20/)
#   cover_exe ROOT [NAME]        the cover build's NAME (arbace, default, or arbace-tests) at ROOT's key: its path,
#                                status 1 with a note when it is not built (bin/arbace-go --gate-build --cover)
#   cover_env TOOL ID            when ARBACE_COVER_DIR is set: GOCOVERDIR=DIR for the run of test ID by TOOL (a
#                                check and its level, java-interp, goi-run, ...), DIR =
#                                ARBACE_COVER_DIR/TOOL/ID with / in ID as %, made, ID written into DIR/id and
#                                the build's meta-data file (ARBACE_COVER_META's, ARBACE_COVER_DIR/meta's) linked into it (Go writes a covered program's
#                                meta-data, tens of MB, into each GOCOVERDIR that lacks it: linked, a run writes
#                                only its counters); nothing when unset
#   cover_meta EXE               EXE's meta-data directory in the cover build: a tool that runs EXE exports it as
#                                ARBACE_COVER_META, whose covmeta files cover_env links
#   cover_settle TOOL ID         after the run: a meta-data file the run wrote moved to ARBACE_COVER_DIR/meta
#   cover_go_flags ROOT PROG WORK OVERLAY OUT
#                                bin/build-go's arguments for a cover build of the j2g program PROG
#
# A check run with ARBACE_COVER_DIR set runs the cover build's executable (cover_exe) in place of the gate's,
# each test with its GOCOVERDIR: bin/java-check's interp and compiled levels (the compiled level's programs
# built with the same -cover flags), bin/goi check (run, directory and errorcheck programs). bin/cover reads
# the directories (blocks, subset).

cover_crops() { echo java go c clojure; }

cover_pkg_re() {
  case $1 in
    java) echo '^arbace/(jrt|lang|aot_arbace_classes(_[a-z_]+)?)$' ;;
    go) echo '^arbace/(goi|aot_arbace_go(_[a-z_]+)?)$' ;;
    c) echo '^arbace/(ci|crt|cinterp|aot_arbace_c|aot_arbace_c_[a-z0-9_]+|aot_arbace_repr_c(_[a-z0-9_]+)?)$' ;;
    clojure) echo '^arbace/(lang|aot_arbace_(core|core_[a-z_]+|c2j_[a-z_]+|data|datafy|edn|genclass|gvec|inspector|instant|java_[a-z_]+|main|math|pprint|pprint_[a-z_]+|reflect|reflect_java|repl|set|spec_[a-z_]+|stacktrace|string|template|test|test_[a-z_]+|uuid|walk|xml|zip))$' ;;
    *) echo "cover: no crop $1 (java, go, c, clojure)" >&2; return 1 ;;
  esac
}

cover_pkgs() {
  local prog=$1 re c
  re=$(for c in $(cover_crops); do cover_pkg_re "$c"; done | paste -sd'|')
  (cd "$prog/go" && find arbace -mindepth 1 -maxdepth 1 -type d) | LC_ALL=C sort | grep -E "$re" | paste -sd,
}

cover_cache() {
  local root=$1 key=${2:-}
  [ -z "${ARBACE_COVER_CACHE:-}" ] || { echo "$ARBACE_COVER_CACHE"; return 0; }
  [ -n "$key" ] || key=$("$root/bin/arbace-go" --gate-key) || return 1
  echo "$(arbace_gate_exes "$root")/cover-${key:0:20}"
}

cover_exe() {
  local root=$1 n=${2:-arbace} c
  c=$(cover_cache "$root") || return 1
  [ -x "$c/amd64/$n" ] && [ -f "$c/key" ] && { echo "$c/amd64/$n"; return 0; }
  echo "cover: no cover build at the checkout's key ($c/amd64/$n; bin/arbace-go --gate-build --cover)" >&2
  return 1
}

cover_env() {
  [ -n "${ARBACE_COVER_DIR:-}" ] || return 0
  local d=$ARBACE_COVER_DIR/$1/${2//\//%} m
  mkdir -p "$d" "$ARBACE_COVER_DIR/meta" || return 1
  printf '%s\n' "$2" > "$d/id"
  for m in "$ARBACE_COVER_DIR"/meta/covmeta.* ${ARBACE_COVER_META:+"$ARBACE_COVER_META"/covmeta.*}; do
    [ -f "$m" ] && [ ! -e "$d/${m##*/}" ] && ln -s "$m" "$d/${m##*/}"
  done
  echo "GOCOVERDIR=$d"
}

# cover_settle TOOL ID: after the run of ID, a meta-data file it wrote itself (a program not linked yet: the
# compiled level's shard programs) moved into ARBACE_COVER_DIR/meta and linked back, so the next runs of that
# program link it
# cover_meta EXE: the cover build's meta-data directory of EXE (CACHE/amd64/NAME: CACHE/meta-NAME), for
# ARBACE_COVER_META
cover_meta() { echo "$(dirname "$(dirname "$1")")/meta-$(basename "$1")"; }

cover_settle() {
  [ -n "${ARBACE_COVER_DIR:-}" ] || return 0
  local d=$ARBACE_COVER_DIR/$1/${2//\//%} m
  for m in "$d"/covmeta.*; do
    [ -f "$m" ] && [ ! -L "$m" ] || continue
    mv -f "$m" "$ARBACE_COVER_DIR/meta/${m##*/}" && ln -s "$ARBACE_COVER_DIR/meta/${m##*/}" "$m"
  done
}

# cover_go_flags ROOT PROG WORK OVERLAY OUT: bin/build-go's arguments, one a line, for a cover build of the
# j2g program PROG printed into WORK with the overlay OVERLAY (bin/jrt overlay's): the overlay with the hook
# (tools/covsubset/hook/cover.go) added to WORK's main package, written to OUT, and the cover flags
cover_go_flags() {
  local root=$1 prog=$2 work=$3 overlay=$4 ov=$5 pkgs
  pkgs=$(cover_pkgs "$prog")
  [ -n "$pkgs" ] || { echo "cover: no package of $prog to cover" >&2; return 1; }
  sed '$s|}}[[:space:]]*$|,\n "'"$work/mod/cmd/arbace/zz_arbace_cover.go"'": "'"$root/tools/covsubset/hook/cover.go"'"}}|' "$overlay" > "$ov"
  grep -q zz_arbace_cover "$ov" || { echo "cover: could not add the hook to $overlay" >&2; return 1; }
  printf '%s\n' --overlay "$ov" --go-flag -cover --go-flag -covermode=set --go-flag "-coverpkg=$pkgs" --go-flag -tags=arbacecover
}
