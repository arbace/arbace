#!/usr/bin/env bash
# One program of a64i's $GOROOT/test check (AR24, queue 299; bin/a64i goroot-check, doc/asm/ARM64-SURVEY.md "As built: AR24"):
#   one.bash PROGRAM
# with A64I (a64i's light driver), GOROOT (TamaGo's go1.27.1, its test/ the corpus), W (the work directory: W/importcfg, std's
# export data for linux/arm64 as Go's test driver lists it) and A64I_GOROOT_TMO (a64i's budget in seconds, default 120) in the
# environment. PROGRAM (a run, rundir, buildrundir or runindir test) is built for linux/arm64 as cmd/internal/testdir builds it
# (go tool compile and link with the driver's flags; go build for a run with flags or arguments; go build . in a module for
# runindir), then run natively on this arm64 CPU (gc's run, the reference) and on a64i, both in $GOROOT/test (runindir: in its
# copy) with the driver's environment, stdin empty. Writes W/res/ID.tsv, one row PROGRAM<TAB>RAW<TAB>DETAIL<TAB>STATS:
#   pass        a64i's run equal to the CPU's (status, stdout byte for byte, stderr up to the goroutines' traces, a [signal]
#               line's addresses masked) and the CPU's passing the test (status 0, stdout and stderr the golden .out, or none)
#   ref         a64i's run equal to the CPU's, the CPU's not passing the test (DETAIL why: gc's own run on arm64)
#   differs     a64i's run not equal to the CPU's (DETAIL what)
#   timeout     a64i over its budget (the CPU's run within its own)
#   speed       not run on a64i: known over its budget, or the CPU's run over A64I_GOROOT_CPU_MAX ms
#   build       the build failed; constraint: the program's //go:build excludes linux/arm64 (the driver's shouldTest)
#   skip        the action sets -goexperiment or -godebug (as goi's check), or the CPU's run timed out
# STATS: the CPU's seconds, a64i's seconds and instructions.
set -uo pipefail
p=$1
g=$GOROOT/test/$p
id=${p//\//__}
t=$W/b/$id
rm -rf "$t"; mkdir -p "$t" "$W/res"
row() { printf '%s\t%s\t%s\t%s\n' "$p" "$1" "$2" "${3:-}" > "$W/res/$id.tsv"; rm -rf "$t"; exit 0; }
genv=(GOROOT="$GOROOT" GOTOOLCHAIN=local GOFLAGS= GOENV=off GOPROXY=off GOEXPERIMENT= CGO_ENABLED=0 GOOS=linux GOARCH=arm64 GOCACHE="$GOCACHE")
renv=(STDLIB_IMPORTCFG="$W/importcfg" GOENV=off GOFLAGS= GOROOT="$GOROOT" PATH="$GOROOT/bin:$PATH")

# the action line (cmd/internal/testdir: the first non-empty line not a build constraint, its // dropped), split as splitQuoted
act=$(awk 'NF==0{next} /^\/\/go:build/{next} /^\/\/ \+build/{next} {sub(/^\/\/ ?/, ""); print; exit}' "$g")
w=(); while IFS= read -r x; do w+=("$x"); done < <(printf '%s\n' "$act" | xargs printf '%s\n' 2>/dev/null)
kind=${w[0]:-}; set -- "${w[@]:1}"
tmo=10 flags=() single= modv=1.14
while [ $# -gt 0 ] && [ "${1#-}" != "$1" ]; do
  case $1 in
    -t) tmo=$2; shift 2 ;;
    -s) single=1; shift ;;
    -gomodversion) modv=$2; shift 2 ;;
    -goexperiment|-godebug) row skip "the action sets ${1#-}: $act" ;;
    *) flags+=("$1"); shift ;;
  esac
done
args=("$@")

# the driver's shouldTest: each //go:build line before the package clause, its tags those the driver's context matches for
# linux/arm64 without cgo (Go's release tags, the toolchain's goexperiment tags, linux, arm64, gc, test_run)
while IFS= read -r line; do
  e=${line#//go:build }
  ex=$(printf '%s' "$e" | sed -E 's/[A-Za-z0-9_.]+/ & /g' | awk -v tags=" $TAGS " '{ for (i = 1; i <= NF; i++) if ($i ~ /^[A-Za-z0-9_.]+$/) $i = (index(tags, " " $i " ") ? 1 : 0); print }')
  [ $(( ex )) = 1 ] || row constraint "$line"
done < <(awk '/^package /{exit} /^\/\/go:build /{print}' "$g")

dir=$GOROOT/test rdir=$GOROOT/test exe=$t/a.exe
case $kind in
  run)
    if [ ${#flags[@]} = 0 ] && [ ${#args[@]} = 0 ]; then
      (cd "$dir" && env "${genv[@]}" "$GOROOT/bin/go" tool compile -p=main -importcfg="$W/importcfg" -o "$t/pkg.a" "$p" &&
        env "${genv[@]}" "$GOROOT/bin/go" tool link -s -w -buildid=test -o "$exe" -importcfg="$W/importcfg" "$t/pkg.a") > "$t/build.log" 2>&1
    else
      # the driver's go run with the action's flags (GOPATH mode in $GOROOT/test, which has no go.mod)
      # go run's files: the program, then the arguments up to the first that is not a .go file (cmplxdivide.go's cmplxdivide1.go,
      # in the program's directory)
      gofiles=("$p"); while [ ${#args[@]} -gt 0 ] && [[ ${args[0]} == *.go ]]; do gofiles+=("$(dirname "$p")/${args[0]}"); args=("${args[@]:1}"); done
      (cd "$dir" && env "${genv[@]}" GO111MODULE=off "$GOROOT/bin/go" build ${flags[@]+"${flags[@]}"} -o "$exe" "${gofiles[@]}") > "$t/build.log" 2>&1
    fi ;;
  rundir)
    # each package of the .dir compiled in the files' order (one per file with -s), the last linked (its first file's object)
    d=$GOROOT/test/${p%.go}.dir gf=() lf=() seen=
    for i in "${!flags[@]}"; do [ "${flags[$i]}" = -ldflags ] && { lf=("${flags[@]:$((i + 1))}"); gf=("${flags[@]:0:$i}"); seen=1; break; }; done
    [ -n "$seen" ] || gf=(${flags[@]+"${flags[@]}"})
    pk=() pf=()
    for f in $(cd "$d" && ls -1 | LC_ALL=C sort | grep '\.go$'); do
      n=$(sed -nE 's/^package ([[:alnum:]_]+).*/\1/p' "$d/$f" | head -1)
      k=-1; if [ -z "$single" ]; then for j in "${!pk[@]}"; do [ "${pk[$j]}" = "$n" ] && k=$j; done; fi
      if [ $k = -1 ]; then pk+=("$n"); pf+=("$f"); else pf[$k]="${pf[$k]} $f"; fi
    done
    cp "$W/importcfg" "$t/importcfg"
    for j in "${!pk[@]}"; do set -- ${pf[$j]}; printf '\npackagefile test/%s=%s' "${1%.go}" "$t/test/${1%.go}.a" >> "$t/importcfg"; done
    mkdir -p "$t/test"; ok=1
    for j in "${!pk[@]}"; do
      set -- ${pf[$j]}; fs=(); for f in "$@"; do fs+=("$d/$f"); done
      if [ "${pk[$j]}" = main ]; then po=(-p=main); else po=(-o "test/${1%.go}.a" -p "test/${1%.go}"); fi
      (cd "$t" && env "${genv[@]}" "$GOROOT/bin/go" tool compile -e -D test -importcfg="$t/importcfg" "${po[@]}" ${gf[@]+"${gf[@]}"} "${fs[@]}") >> "$t/build.log" 2>&1 || { ok=; break; }
      if [ $j = $((${#pk[@]} - 1)) ]; then
        (cd "$t" && env "${genv[@]}" "$GOROOT/bin/go" tool link -s -w -buildid=test -o "$exe" -importcfg="$t/importcfg" ${lf[@]+"${lf[@]}"} "${1%.go}.o") >> "$t/build.log" 2>&1 || ok=
      fi
    done
    [ -n "$ok" ] ;;
  buildrundir)
    d=$GOROOT/test/${p%.go}.dir gos=() asms=()
    for f in $(cd "$d" && ls -1 | LC_ALL=C sort); do case $f in *.go) gos+=("$d/$f") ;; *.s) asms+=("$d/$f") ;; esac; done
    (cd "$t" && set -e
     ah=()
     if [ ${#asms[@]} -gt 0 ]; then : > go_asm.h; env "${genv[@]}" "$GOROOT/bin/go" tool asm -p=main -gensymabis -o symabis "${asms[@]}"; ah=(-asmhdr go_asm.h -symabis symabis); fi
     env "${genv[@]}" "$GOROOT/bin/go" tool compile -p=main -e -D . -importcfg="$W/importcfg" -o go.o ${ah[@]+"${ah[@]}"} "${gos[@]}"
     objs=(go.o)
     if [ ${#asms[@]} -gt 0 ]; then env "${genv[@]}" "$GOROOT/bin/go" tool asm -p=main -e -I . -o asm.o "${asms[@]}"; objs+=(asm.o); fi
     env "${genv[@]}" "$GOROOT/bin/go" tool pack c all.a "${objs[@]}"
     env "${genv[@]}" "$GOROOT/bin/go" tool link -s -w -buildid=test -o "$exe" -importcfg="$W/importcfg" all.a) > "$t/build.log" 2>&1 ;;
  runindir)
    d=$GOROOT/test/${p%.go}.dir m=$(basename "$d") rdir=$t/src/$(basename "$d")
    mkdir -p "$rdir"; cp -R "$d/." "$rdir/"; printf 'module %s\ngo %s\n' "$m" "$modv" > "$rdir/go.mod"
    (cd "$rdir" && env "${genv[@]}" GOPATH="$t" "$GOROOT/bin/go" build ${flags[@]+"${flags[@]}"} -o "$exe" .) > "$t/build.log" 2>&1 ;;
  *) row skip "not a run test: $kind" ;;
esac || row build "$(grep -v '^#' "$t/build.log" | head -2 | tr '\n' ' ' | cut -c1-240)"

head_err() { awk '/^goroutine /{exit} {print}' "$1" | sed -E 's/^\[signal ([A-Z]+): (.*) code=0x[0-9a-f]+ addr=0x[0-9a-f]+ pc=0x[0-9a-f]+\]$/[signal \1: \2 code=X addr=X pc=X]/'; }
s0=$(date +%s%N); cst=0
(cd "$rdir" && env "${renv[@]}" PWD="$rdir" timeout -k 5 $((tmo * 3)) "$exe" ${args[@]+"${args[@]}"} > "$t/c.out" 2> "$t/c.err" < /dev/null) || cst=$?
cs=$(( ($(date +%s%N) - s0) / 1000000 ))
case $cst in 124|137) row skip "the CPU's run timed out ($((tmo * 3)) s)" ;; esac
# speed (AR24): a program known over a64i's budget (known.edn's :speed, A64I_GOROOT_SPEED) is not run on a64i, nor one whose CPU
# run took over A64I_GOROOT_CPU_MAX ms (default 1000: a64i runs Go's code about a thousand times slower than the M2, so such a
# run is far over any budget); an a64i timeout is a failure until known.edn names it
if grep -qxF "$p" "${A64I_GOROOT_SPEED:-/dev/null}"; then row speed "known over a64i's budget (the CPU's run ${cs} ms)" "cpu ${cs}ms"; fi
[ "$cs" -le "${A64I_GOROOT_CPU_MAX:-1000}" ] || row speed "the CPU's run ${cs} ms, over ${A64I_GOROOT_CPU_MAX:-1000} ms: a64i not run" "cpu ${cs}ms"
s0=$(date +%s%N); ast=0
(cd "$rdir" && env "${renv[@]}" PWD="$rdir" timeout -k 5 "${A64I_GOROOT_TMO:-120}" "$A64I" run --stats "$exe" ${args[@]+"${args[@]}"} > "$t/a.out" 2> "$t/a.raw" < /dev/null) || ast=$?
as=$(( ($(date +%s%N) - s0) / 1000000 ))
# a64i's own line last on stderr (--stats): N instructions in ...
ins=$(tail -1 "$t/a.raw" | sed -nE 's/^a64i: ([0-9]+) instructions in .*/\1/p')
if [ -n "$ins" ]; then sed '$d' "$t/a.raw" > "$t/a.err"; else cp "$t/a.raw" "$t/a.err"; fi
st="cpu ${cs}ms a64i ${as}ms ${ins:-?} instructions"
# the runs' digests for the negative controls (judge.ae): a64i's and the CPU's status, stdout, stderr's head; the golden's
dg() { sha256sum | cut -c1-16; }
printf '%s\t%s\t%s\t%s\t%s\t%s\t%s\t%s\n' "$ast" "$(dg < "$t/a.out")" "$(head_err "$t/a.err" | dg)" "$cst" "$(dg < "$t/c.out")" \
  "$(head_err "$t/c.err" | dg)" "$(cat "$t/c.out" "$t/c.err" | dg)" "$(if [ -f "$GOROOT/test/${p%.go}.out" ]; then dg < "$GOROOT/test/${p%.go}.out"; else printf '' | dg; fi)" > "$W/res/$id.dig"
case $ast in 124|137) row timeout "a64i over its budget of ${A64I_GOROOT_TMO:-120} s (the CPU's run ${cs} ms)" "$st" ;; esac
why=
if [ "$ast" != "$cst" ]; then why="status $ast, the CPU's $cst: $(head_err "$t/a.err" | grep . | tail -1 | cut -c1-160)"
elif ! cmp -s "$t/a.out" "$t/c.out"; then why="stdout differs from the CPU's: $(diff "$t/c.out" "$t/a.out" | grep '^[<>]' | head -2 | tr '\n' ' ' | cut -c1-200)"
elif [ "$(head_err "$t/a.err")" != "$(head_err "$t/c.err")" ]; then why="stderr differs from the CPU's: $(diff <(head_err "$t/c.err") <(head_err "$t/a.err") | grep '^[<>]' | head -2 | tr '\n' ' ' | cut -c1-200)"
fi
if [ -n "$why" ]; then
  [ -n "$ins" ] || why="$why; a64i: $(grep '^a64i: ' "$t/a.raw" | tail -1 | cut -c1-160)"
  row differs "$why" "$st"
fi
# the CPU's run against the test (gc_ref_fails's rule: status 0; the golden .out, stdout then stderr, or no output; a golden
# that differs with both streams written taken as passing, their interleaving not known)
gold=$GOROOT/test/${p%.go}.out ref=
if [ "$cst" != 0 ]; then ref="status $cst"
elif [ -f "$gold" ]; then
  if ! cat "$t/c.out" "$t/c.err" | cmp -s - "$gold" && { [ ! -s "$t/c.out" ] || [ ! -s "$t/c.err" ]; }; then ref="its output differs from the golden .out"; fi
elif [ -s "$t/c.out" ] || [ -s "$t/c.err" ]; then ref="it writes output and the test has no golden .out"
fi
[ -z "$ref" ] || row ref "gc's own run on arm64: $ref" "$st"
row pass "" "$st"
