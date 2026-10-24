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
# read back by arbace.core/read); then the totals. Since JFS72 (b) (doc/classes/JAVA-FRONTEND-SURVEY.md,
# "As built: JN9a") the forms themselves are checked: each converted file's SHA-256, by its path
# M/PATH (its first line without $JDK_SRC, so that the output does not depend on where jdk26u is),
# into $work/jdk26u.sha256, compared with test/repr/java/expected/jdk26u.sha256 (the lines of the
# modules converted; a module of the record not converted is not compared); with --update (bin/repr-java
# check --jdk --update, or check --update --jdk) the record's lines of the converted modules are
# rewritten instead (commit it with the change that moved them, reviewed).
set -uo pipefail
root=$(cd "$(dirname "$0")/../../.." && pwd)
cd "$root"
work=.tmp/repr-java-jdk
src=${JDK_SRC:-/root/jdk26u/src}
steps=${JDK_STEPS:-convert}   # JDK_JOBS: parallel conversions (default 1); JDK_THREADS: read-jdk's chunks
# in parallel within one (default 4: on javac's tool executable each chunk's javac lives with its
# converted units, about 6 GB each at the peak)
mkdir -p "$work"
expected=test/repr/java/expected/jdk26u.sha256
update=
if [ "${1:-}" = --update ]; then update=1; shift; fi

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
[ $bad = 0 ] || exit 1

# the forms' hashes (JFS72 (b)): each module's files, their first line without $JDK_SRC
echo "== forms"
for m in $modules; do
  find "$work/conv/$m" -name '*.ae' -exec sed -i "1s|^;; Converted from $src/|;; Converted from |" {} +
  (cd "$work/conv" && find "$m" -name '*.ae' | LC_ALL=C sort | xargs -r sha256sum)
done | LC_ALL=C sort -k2 > "$work/jdk26u.sha256"
printf '%s\n' $modules | LC_ALL=C sort > "$work/modules.txt"
# the record's lines of the modules converted
touch "$expected"
awk 'NR==FNR {m[$1]=1; next} {split($2, p, "/"); if (p[1] in m) print}' "$work/modules.txt" "$expected" > "$work/expected.sha256"
if [ -n "$update" ]; then
  { awk 'NR==FNR {m[$1]=1; next} {split($2, p, "/"); if (!(p[1] in m)) print}' "$work/modules.txt" "$expected"
    cat "$work/jdk26u.sha256"; } | LC_ALL=C sort -k2 > "$work/record.sha256"
  cp "$work/record.sha256" "$expected"
  echo "update: $expected written ($(wc -l < "$work/jdk26u.sha256") files of $n modules; $(wc -l < "$expected") in all): commit it"
elif diff "$work/expected.sha256" "$work/jdk26u.sha256" > "$work/jdk26u.diff"; then
  echo "forms: $(wc -l < "$work/jdk26u.sha256") files of $n modules, as expected ($expected)"
else
  echo "forms: the converted files differ from $expected ($work/jdk26u.diff):"
  grep '^[<>]' "$work/jdk26u.diff" | awk '{print $1, $3}' | sort -k2 -u | head -40
  echo "$(grep -c '^[<>]' "$work/jdk26u.diff") lines differ"
  exit 1
fi
