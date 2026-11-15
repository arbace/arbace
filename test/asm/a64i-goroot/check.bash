#!/usr/bin/env bash
# a64i's $GOROOT/test check (AR24, queue 299; doc/asm/ARM64-SURVEY.md "As built: AR24"), run by bin/a64i goroot-check:
#   check.bash ROOT A64I [--record] [--only LIST|PROGRAM,...] [--from N --count M] [--jobs N] [--keep] [--resume]
# The corpus: the run, rundir, buildrundir and runindir programs of goi's linux record (test/goi/check-linux-amd64.edn), in its
# order; --only a list (a program a line) or names; --from/--count a slice of it (1-based), for runs in pieces. Each program
# built for linux/arm64 by TamaGo's go and run on this arm64 CPU and on a64i (one.bash), --jobs at once (default 3); the rows
# in .tmp/a64i/goroot/results.tsv, then judged (judge.ae) against test/asm/a64i-goroot-results-arm64.edn with the classes of
# test/asm/divergences-a64i-goroot.edn and the known programs of test/asm/a64i-goroot/known.edn; --record (a whole run)
# rewrites the record. Needs an arm64 host: the CPU's run is the reference.
set -uo pipefail
root=$1 a64i=$2; shift 2
rec= only= from=1 count= jobs=3 keep= resume=
while [ $# -gt 0 ]; do
  case $1 in
    --record) rec=1; shift ;;
    --only) only=$2; shift 2 ;;
    --from) from=$2; shift 2 ;;
    --count) count=$2; shift 2 ;;
    --jobs) jobs=$2; shift 2 ;;
    --keep) keep=1; shift ;;
    --resume) resume=1; shift ;;
    *) echo "a64i goroot-check: unknown argument $1" >&2; exit 2 ;;
  esac
done
case $(uname -m) in aarch64|arm64) ;; *) echo "a64i goroot-check: needs an arm64 host (this CPU's run is the reference)" >&2; exit 2 ;; esac
export GOROOT=${ARBACE_GOROOT:?TamaGo go1.27.1 (ARBACE_GOROOT)} A64I=$a64i
export W=$root/.tmp/a64i/goroot GOCACHE=${A64I_GOCACHE:-$root/.tmp/a64i/gocache}
mkdir -p "$W"
genv=(GOROOT="$GOROOT" GOTOOLCHAIN=local GOFLAGS= GOENV=off GOPROXY=off GOEXPERIMENT= CGO_ENABLED=0 GOOS=linux GOARCH=arm64 GOCACHE="$GOCACHE")
# std's export data as Go's test driver lists it (stdlibImportcfg), for linux/arm64
(cd "$W" && env "${genv[@]}" "$GOROOT/bin/go" list -export -f '{{if .Export}}packagefile {{.ImportPath}}={{.Export}}{{end}}' std) > "$W/importcfg.tmp" \
  && mv "$W/importcfg.tmp" "$W/importcfg" || { echo "FAIL: a64i goroot-check: go list -export std" >&2; exit 1; }
TAGS="$(cd "$W" && env "${genv[@]}" "$GOROOT/bin/go" list -f '{{join context.ReleaseTags " "}} {{join context.ToolTags " "}}' runtime) linux arm64 gc test_run"
export TAGS
# the built programs kept (W/built/KEY): KEY the hash of the Go tree's pin (seed/goroot.sha256, VERSION), the corpus's ($GOROOT/test's
# pin, goi's linux record) and one.bash, which builds them; other keys' directories removed; A64I_GOROOT_NOCACHE=1 builds them all
if [ -z "${A64I_GOROOT_NOCACHE:-}" ]; then
  key=$(cat "$root/seed/goroot.sha256" "$root/seed/goroot-test.sha256" "$GOROOT/VERSION" "$root/test/goi/check-linux-amd64.edn" "$root/test/asm/a64i-goroot/one.bash" | sha256sum | cut -c1-20)
  for o in "$W"/built/*; do [ -e "$o" ] && [ "$o" != "$W/built/$key" ] && rm -rf "$o"; done
  export A64I_GOROOT_BUILT=$W/built/$key
fi
# the programs known over a64i's budget (known.edn's :speed rows), not run on a64i
sed -n 's/^ *\["\([^"]*\)" :speed .*/\1/p' "$root/test/asm/a64i-goroot/known.edn" > "$W/speed.txt"
export A64I_GOROOT_SPEED=$W/speed.txt
# the corpus
sed -n 's/^ \["\([^"]*\)" :.*/\1/p' "$root/test/goi/check-linux-amd64.edn" | while read -r p; do
  k=$(awk 'NF==0{next} /^\/\/go:build/{next} /^\/\/ \+build/{next} {sub(/^\/\/ ?/, ""); print $1; exit}' "$GOROOT/test/$p" 2>/dev/null)
  case $k in run|rundir|buildrundir|runindir) echo "$p" ;; esac
done > "$W/corpus.txt"
if [ -n "$only" ]; then
  if [ -f "$only" ]; then grep -v '^#' "$only" | grep . > "$W/list.txt"; else tr ',' '\n' <<< "$only" > "$W/list.txt"; fi
else
  tail -n +"$from" "$W/corpus.txt" | head -n "${count:-1000000}" > "$W/list.txt"
fi
n=$(wc -l < "$W/list.txt")
echo "a64i goroot-check: $n of $(wc -l < "$W/corpus.txt") programs, $jobs at once"
# --resume: the rows of an earlier run of the same tree kept (a whole corpus run in pieces, then judged as one), the rest run
[ -n "$resume" ] || rm -rf "$W/res"
rm -rf "$W/b"; mkdir -p "$W/res"
t0=$SECONDS
while read -r p; do [ -f "$W/res/${p//\//__}.tsv" ] || echo "$p"; done < "$W/list.txt" > "$W/todo.txt"
echo "a64i goroot-check: $(wc -l < "$W/todo.txt") to run"
xargs -P "$jobs" -n 1 bash "$root/test/asm/a64i-goroot/one.bash" < "$W/todo.txt"
while read -r p; do f=$W/res/${p//\//__}.tsv; if [ -f "$f" ]; then cat "$f"; else printf '%s\tmissing\tno row\t\n' "$p"; fi; done < "$W/list.txt" > "$W/results.tsv"
[ -n "$keep" ] || rm -rf "$W/b"
echo "a64i goroot-check: $n programs in $((SECONDS - t0)) s"
partial=; { [ -n "$only" ] || [ "$from" != 1 ] || [ -n "$count" ]; } && partial=1
[ -z "$rec" ] || [ -z "$partial" ] || { echo "a64i goroot-check: --record needs the whole corpus" >&2; exit 2; }
. "$root/bin/lib/tools.bash"
arbace_tool -e "(do (load-file \"$root/test/asm/a64i-goroot/judge.ae\") nil)" \
  -e "(asm.a64i-goroot/main \"$root\" \"$W/results.tsv\" \"${rec:-}\" \"${partial:-}\")"
