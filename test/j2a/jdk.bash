#!/bin/bash
# bin/j2a-check --jdk [MODULE...]: jdk26u's modules converted by j2a, forms-level (no class files
# compiled or compared with javac's since standalone S4, doc/STANDALONE-NOTES.md, "S4: the JVM
# removed", O2). Run through bin/j2a-check (from the repository root).
#
# Per module M of $JDK_SRC/*/share/classes (largest first): bin/j2a ... jdk --check, the
# module's Java files into class forms, $work/conv/M (no failures, no javac errors, every file
# read back by arbace.core/read); then the totals.
set -uo pipefail
root=$(cd "$(dirname "$0")/../.." && pwd)
cd "$root"
work=.tmp/j2a-jdk
src=${JDK_SRC:-/root/jdk26u/src}
steps=${JDK_STEPS:-convert}   # JDK_JOBS: parallel conversions (default 2, 10 GB each)
mkdir -p "$work"

if [ $# -gt 0 ]; then
  modules="$*"
else
  modules=$(for d in "$src"/*/share/classes; do
              m=${d#"$src"/}; m=${m%%/*}
              echo "$(find "$d" -name '*.java' | wc -l) $m"
            done | sort -rn | awk '{print $2}')
fi

has() { case ",$steps," in *",$1,"*) return 0 ;; esac; return 1; }

convert_one() {
  local m=$1
  rm -rf "$work/conv/$m"
  J2A_THREADS=8 J2A_JVM_OPTS=-Xmx10g bin/j2a -m arbace.j2a.main jdk --check "$work/conv/$m" "$m" \
    "$src/$m/share/classes" > "$work/log/convert-$m.log" 2>&1
  echo "convert $m: $(tail -1 "$work/log/convert-$m.log")"
}

export -f convert_one
export work src
mkdir -p "$work/log"

if has convert; then
  echo "== convert"
  printf '%s\n' $modules | xargs -P "${JDK_JOBS:-2}" -I{} bash -c 'convert_one {}'
fi
echo "== totals"
n=0 bad=0
for m in $modules; do
  l=$(tail -1 "$work/log/convert-$m.log" 2>/dev/null)
  n=$((n + 1))
  echo "$l" | grep -q " 0 failed, 0 javac errors, .* 0 unreadable" || { bad=$((bad + 1)); echo "$m: $l"; }
done
echo "$n modules converted, $bad with failures"
[ $bad = 0 ]
