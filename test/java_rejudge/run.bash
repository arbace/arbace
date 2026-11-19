#!/usr/bin/env bash
# Queue 440 (2026-11-19): the morning's check of the night's Java re-judge against its prediction.
#
#   test/java_rejudge/run.bash [--latest-saved] [--no-fail] [RESULTS...]
#
# RESULTS: saved results (bin/java-check --save's results.tsv); --latest-saved adds the newest saved results of the gate's
# java-interp and java-compiled-full steps' parts in the store of passes (ARBACE_GATE_STORE, default the main checkout's
# .tmp/gate-pass/), the files bin/java-check --judge --latest-saved judges. Judged against test/java_rejudge/expected-1119.tsv
# by java-rejudge.expect (test/java_rejudge/expect.ae) on the tools' host: each level's pass count and score expected and
# actual, per class the rows predicted to remain and those that remain, the edits test/java/divergences.edn needs, and a
# FLAG line for each deviation (exit 1; --no-fail: 0). Run it before bin/java-check --judge --latest-saved --record (it reads
# no record, so after works too). Seconds, no run.
set -euo pipefail
root=$(cd "$(dirname "$0")/../.." && pwd)
. "$root/bin/lib/tmpdir.bash"
. "$root/bin/lib/tools.bash"
cd "$root"
latest="" files=() opts=()
while [ $# -gt 0 ]; do
  case $1 in
    --latest-saved) latest=1; shift ;;
    --no-fail) opts+=(--no-fail); shift ;;
    *) files+=("$1"); shift ;;
  esac
done
if [ -n "$latest" ]; then
  store=${ARBACE_GATE_STORE:-$(cd "$(git -C "$root" rev-parse --git-common-dir)/.." && pwd)/.tmp/gate-pass}
  for st in java-interp java-compiled-full; do
    for d in "$store/$st".*; do
      case $d in *.sub) continue ;; esac
      [ -d "$d" ] || continue
      f=$(ls -td "$d"/*.out 2>/dev/null | head -1)
      if [ -n "$f" ] && [ -f "$f/results.tsv" ]; then files+=("$f/results.tsv"); echo "java-rejudge: $f/results.tsv ($(date -r "$f/results.tsv" '+%F %T'))"; fi
    done
  done
fi
[ ${#files[@]} -gt 0 ] || { echo "java-rejudge: no results (RESULTS or --latest-saved)" >&2; exit 2; }
arbace_tool -p "$root/test" -m java-rejudge.expect expect "$root" "$root/test/java_rejudge/expected-1119.tsv" "${files[@]}" ${opts[@]+"${opts[@]}"}
