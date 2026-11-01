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
# rewritten instead (commit it with the change that moved them, reviewed). The fallbacks to javac of the reads
# without javac are judged per module against test/repr/java/expected/jdk26u-fallbacks.edn ({MODULE N}, none
# listed: 0; --update rewrites it; GT7's audit 061 #1). Since JN9f (JFS120 (b)) the
# modules are read without javac (read-jdk --no-javac: the native parser and provider, a file that falls
# back read by javac, counted); REPR_JAVA_VENDOR_READ=javac reads them with javac's trees.
set -uo pipefail
root=$(cd "$(dirname "$0")/../../.." && pwd)
cd "$root"
work=.tmp/repr-java-jdk
. bin/lib/repr-java/judge.bash
# the source directory as read-jdk names it in a converted file's header (its canonical path: a symlink, a
# trailing slash or a '..' in JDK_SRC would leave the header's host path in the forms, GT7's audit 061 #2)
judge_jdk_paths || exit 2 # jdk_src, jdk: one tree (GT7's audit 061 #5)
src=$(judge_canon "$jdk_src") || exit 1
steps=${JDK_STEPS:-convert}   # JDK_JOBS: parallel conversions (default 1); JDK_THREADS: read-jdk's chunks
# in parallel within one (default 4: on javac's tool executable each chunk's javac lives with its
# converted units, about 6 GB each at the peak)
mkdir -p "$work"
expected=test/repr/java/expected/jdk26u.sha256
# the reads without javac's fallbacks to javac, a ceiling by module ({MODULE N ...}, none listed: 0; a file
# that falls back is read by javac, so its forms are javac's and the hashes cannot tell, GT7's audit 061 #1)
fallbacks=test/repr/java/expected/jdk26u-fallbacks.edn
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
  REPR_JAVA_THREADS=${JDK_THREADS:-4} bin/repr-java read-jdk --check $ro "$work/conv/$m" "$m" \
    "$src/$m/share/classes" > "$work/log/convert-$m.log" 2>&1
  [ -f "$work/conv/$m/repr-java-nojavac.edn" ] && mv "$work/conv/$m/repr-java-nojavac.edn" "$work/log/nojavac-$m.edn"
  echo "convert $m: $(tail -1 "$work/log/convert-$m.log")${ro:+; $(grep '^no javac: ' "$work/log/convert-$m.log" | sed 's/^no javac: /without javac: /')}"
}

# the read (JFS120 (b), doc/classes/JAVA-FRONTEND-SURVEY.md, "As built: JN9f"): without javac by default,
# a file that falls back read by javac (counted: each module's "no javac:" line, the totals' fallbacks);
# REPR_JAVA_VENDOR_READ=javac, javac's trees
. bin/lib/repr-java/host.bash
ro=$(repr_java_vendor_read_opts) || exit 1
export -f convert_one
export work src ro
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
if [ -n "$ro" ]; then
  # the reads without javac: the files and the fallbacks, summed over the modules
  cat $(for m in $modules; do echo "$work/log/convert-$m.log"; done) 2>"$work/log/cat.err" | grep '^no javac: ' |
    awk '{f+=$3; nat+=$5; fb+=$7} END {printf "without javac: %d files, %d native, %d fallbacks to javac (the modules'"'"' logs, $work/log/convert-M.log)\n", f, nat, fb}'
fi
[ $bad = 0 ] || exit 1

# the fallbacks by module, from the modules' logs ("no javac: N files, N native, N fallbacks to javac; ...")
: > "$work/fallbacks.tsv"
fbbad=0
if [ -n "$ro" ]; then
  for m in $modules; do
    f=$(grep '^no javac: ' "$work/log/convert-$m.log" | awk '{print $7}')
    case $f in
      ''|*[!0-9]*) echo "$m: no 'no javac:' line in $work/log/convert-$m.log: the fallbacks are unknown"; fbbad=1; continue ;;
    esac
    printf '%s\t%s\n' "$m" "$f" >> "$work/fallbacks.tsv"
  done
fi

# the forms' hashes (JFS72 (b)): each module's files, their first line without $JDK_SRC
echo "== forms"
for m in $modules; do
  find "$work/conv/$m" -name '*.ae' -exec sed -i "1s|^;; Converted from $src/|;; Converted from |" {} +
  (cd "$work/conv" && find "$m" -name '*.ae' | LC_ALL=C sort | xargs -r sha256sum)
done | LC_ALL=C sort -k2 > "$work/jdk26u.sha256"
# a module whose fallbacks are unknown: nothing is compared or recorded (review 067 E)
[ $fbbad = 0 ] || exit 1
# every header without the host's path (the strip above found its prefix)
hostpaths=$(for m in $modules; do find "$work/conv/$m" -name '*.ae' -exec awk 'FNR == 1 && /^;; Converted from \// {print FILENAME}' {} +; done | head -5)
if [ -n "$hostpaths" ]; then
  echo "forms: a converted file's first line still names a host path (JDK_SRC=$src), nothing recorded or compared:"
  echo "$hostpaths"
  exit 1
fi
printf '%s\n' $modules | LC_ALL=C sort > "$work/modules.txt"
# the fallbacks against their ceilings (not for --update, which records them)
if [ -n "$ro" ] && [ -z "$update" ]; then
  while IFS=$'\t' read -r m f; do
    c=; [ -f "$fallbacks" ] && c=$(judge_flat_get "$fallbacks" "$m")
    c=${c:-0}
    if [ "$f" -gt "$c" ]; then echo "$m: $f fallbacks to javac, over the ceiling $c ($fallbacks)"; fbbad=1; fi
  done < "$work/fallbacks.tsv"
  [ $fbbad = 0 ] && echo "fallbacks: $(awk -F'\t' '{n += $2} END {print n + 0}' "$work/fallbacks.tsv") to javac, within the ceilings ($fallbacks)"
fi
# the record's lines of the modules converted
touch "$expected"
awk 'NR==FNR {m[$1]=1; next} {split($2, p, "/"); if (p[1] in m) print}' "$work/modules.txt" "$expected" > "$work/expected.sha256"
if [ -n "$update" ]; then
  { awk 'NR==FNR {m[$1]=1; next} {split($2, p, "/"); if (!(p[1] in m)) print}' "$work/modules.txt" "$expected"
    cat "$work/jdk26u.sha256"; } | LC_ALL=C sort -k2 > "$work/record.sha256"
  cp "$work/record.sha256" "$expected"
  if [ -n "$ro" ]; then
    # the fallbacks of the modules converted rewritten (nonzero ones only), the other modules' kept
    { echo ";; bin/repr-java check --jdk (test/repr/java/jdk.bash): by module, the most files of the read without javac that may"
      echo ";; fall back to javac (JFS120 (b); GT7's audit 061 #1). None listed: 0. Rewritten by check --jdk --update."
      echo "{"
      { [ -f "$fallbacks" ] && awk 'NR==FNR {m[$1]=1; next} {sub(/;.*/, ""); gsub(/[{}]/, " "); n = split($0, t, /[ \t,]+/); for (i = 1; i < n; i += 2) if (t[i] != "" && !(t[i] in m)) print t[i], t[i + 1]}' "$work/modules.txt" "$fallbacks"
        awk -F'\t' '$2 > 0 {print $1, $2}' "$work/fallbacks.tsv"; } | LC_ALL=C sort | sed 's/^/ /'
      echo "}"; } > "$work/fallbacks.new"
    cp "$work/fallbacks.new" "$fallbacks"
    echo "update: $fallbacks written ($(awk -F'\t' '$2 > 0' "$work/fallbacks.tsv" | wc -l) modules with fallbacks)"
  fi
  echo "update: $expected written ($(wc -l < "$work/jdk26u.sha256") files of $n modules; $(wc -l < "$expected") in all): commit it"
elif diff "$work/expected.sha256" "$work/jdk26u.sha256" > "$work/jdk26u.diff"; then
  echo "forms: $(wc -l < "$work/jdk26u.sha256") files of $n modules, as expected ($expected)"
else
  echo "forms: the converted files differ from $expected ($work/jdk26u.diff):"
  grep '^[<>]' "$work/jdk26u.diff" | awk '{print $1, $3}' | sort -k2 -u | head -40
  echo "$(grep -c '^[<>]' "$work/jdk26u.diff") lines differ"
  exit 1
fi
[ $fbbad = 0 ] || exit 1
