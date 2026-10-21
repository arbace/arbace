#!/bin/bash
# bin/repr-java check --jdk [MODULE...]: jdk26u's modules converted by repr/java, forms-level (no class files
# compiled or compared with javac's since standalone S4, doc/STANDALONE-NOTES.md, "S4: the JVM
# removed", O2). Run through bin/repr-java check (from the repository root). Since JN6 on javac's
# tool executable (bin/repr-java's only host; doc/classes/JAVA-FRONTEND-SURVEY.md, "As built: JN6"),
# read-jdk attributing each module against the platform, which holds every module of the JDK
# image (JFS37; until JN6 on the frozen JVM, whose JDK held them).
#
# Per module M of $JDK_SRC/*/share/classes (largest first): bin/repr-java ... jdk --check, the
# module's Java files into Java forms, $work/conv/M (no failures, no javac errors, every file
# read back by arbace.core/read); then the totals.
set -uo pipefail
root=$(cd "$(dirname "$0")/../../.." && pwd)
cd "$root"
work=.tmp/repr-java-jdk
src=${JDK_SRC:-/root/jdk26u/src}
steps=${JDK_STEPS:-convert}   # JDK_JOBS: parallel conversions (default 1); JDK_THREADS: read-jdk's chunks
# in parallel within one (default 4: on javac's tool executable each chunk's javac lives with its
# converted units, about 6 GB each at the peak)
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
  REPR_JAVA_THREADS=${JDK_THREADS:-4} bin/repr-java read-jdk --check "$work/conv/$m" "$m" \
    "$src/$m/share/classes" > "$work/log/convert-$m.log" 2>&1
  echo "convert $m: $(tail -1 "$work/log/convert-$m.log")"
}

export -f convert_one
export work src
mkdir -p "$work/log"

if has convert; then
  echo "== convert"
  printf '%s\n' $modules | xargs -P "${JDK_JOBS:-1}" -I{} bash -c 'convert_one {}'
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
