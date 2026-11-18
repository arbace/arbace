#!/bin/bash
# java.base's CaseFolding.java made from data alone (JOP10's first half, round 54, branch n-java-indep2;
# test/javac-platform/casefolding.ae, bin/jrt-convert casefolding): the packed values of GenerateCaseFolding's foldingToLong
# on fabricated foldings (one, two and three mappings, a supplementary code point), the regex filter on a fabricated
# UnicodeData (an entry kept, one dropped because its folding maps back), and with the pinned UCD 16.0.0 present the
# recorded copy byte for byte, with the negative control: jdk26u's own UnicodeData.txt (Unicode 17, the case mappings of
# javac's tool) must give another file, 28 entries fewer. Seconds, under 1 GiB; no JDK.
# needs: exe
set -uo pipefail
root=$(cd "$(dirname "$0")/../.." && pwd)
. "$root/bin/lib/tmpdir.bash"
t=$TMPDIR/casefolding-test
rm -rf "$t"; mkdir -p "$t"
fails=0
ok() { echo "ok   $1"; }
no() { echo "FAIL $1"; fails=$((fails + 1)); }
exe=${ARBACE_HOST_EXE:-$root/target/seed/amd64/arbace}

# 1. the generator's parts on fabricated data
cat > "$t/unit.ae" <<'EOF'
(load-file (str (System/getenv "CF_ROOT") "/test/javac-platform/casefolding.ae"))
(in-ns 'javac.casefolding)
(println "long1" (format "0x%013x" (folding->long ["0041" "0061"])))
(println "long3" (format "0x%013x" (folding->long ["00DF" "0073" "0073"])))
(println "long4" (format "0x%013x" (folding->long ["1F82" "1F00" "03B9" "0301"])))
(println "longsupp" (format "0x%013x" (folding->long ["10400" "10428" "0001"])))
(let [maps (simple-case-maps ["0041;LATIN CAPITAL LETTER A;Lu;0;L;;;;;N;;;;0061;"
                              "0061;LATIN SMALL LETTER A;Ll;0;L;;;;;N;;;0041;;0041"
                              "1E9E;LATIN CAPITAL LETTER SHARP S;Lu;0;L;;;;;N;;;;00DF;"])
      out (generate ["a" "%%%Expanded_Case_Map_Entries" "%%%Entries" "z"]
                    ["# c" "0041; C; 0061; # A" "1E9E; S; 00DF; # SHARP S" "00C5; S; 0061; # fabricated"] maps)]
  (println "kept-sharp" (boolean (re-find #"entry\(0x1E9E, 0x00DF\)" out)))
  (println "dropped-a" (not (re-find #"entry\(0x0041, 0x0061\)" out)))
  (println "kept-fab" (boolean (re-find #"entry\(0x00C5, 0x0061\)" out)))
  (println "cps" (boolean (re-find #"CASE_FOLDING_CPS = \{\n        0X0041\n    \};" out))))
EOF
o=$(CF_ROOT=$root ARBACE_PATH=$root "$exe" "$t/unit.ae" 2>&1)
# foldingToLong as Java computes it: the second mapping at bit 16, the third at 32, the mappings' count at bit 48
echo "$o" | grep -qx 'long1 0x0000000000061' && ok "foldingToLong: one mapping is the mapping" || no "long1: $o"
echo "$o" | grep -qx 'long3 0x2000000730073' && ok "foldingToLong: two mappings, the count in bits 48" || no "long3: $o"
echo "$o" | grep -qx 'long4 0x3030103b91f00' && ok "foldingToLong: three mappings at shifts 16 and 32, the count 3" || no "long4: $o"
echo "$o" | grep -qx 'longsupp 0x0000000010428' && ok "foldingToLong: a supplementary code point keeps its first mapping alone" || no "longsupp: $o"
echo "$o" | grep -qx 'kept-sharp true' && ok "regex part: 1E9E -> 00DF kept (00DF's simple uppercase is not 1E9E)" || no "kept-sharp: $o"
echo "$o" | grep -qx 'dropped-a true' && ok "regex part: 0041 -> 0061 dropped (0061 maps back to 0041)" || no "dropped-a: $o"
echo "$o" | grep -qx 'kept-fab true' && ok "regex part: 00C5 -> 0061 kept" || no "kept-fab: $o"
echo "$o" | grep -qx 'cps true' && ok "the [CF] array's layout" || no "cps: $o"

# 2. the recorded copy from the pinned UCD 16.0.0, and the control (Unicode 17's mappings)
main=$(git -C "$root" worktree list --porcelain | sed -n '1s/^worktree //p')
ucd=$main/.tmp/scheme/ucd-16.0.0
jdk=${JDK:-/root/jdk26u}
if [ -f "$ucd/UnicodeData.txt" ] && [ -f "$jdk/src/java.base/share/data/unicodedata/CaseFolding.txt" ]; then
  if ARBACE_HOST_EXE=$exe "$root/bin/jrt-convert" casefolding --ucd "$ucd" > "$t/cf.log" 2>&1; then ok "UCD 16.0.0: the recorded CaseFolding.java byte for byte"
  else no "UCD 16.0.0: $(tail -1 "$t/cf.log")"; fi
  mkdir -p "$t/u17"; cp "$jdk/src/java.base/share/data/unicodedata/UnicodeData.txt" "$t/u17/"
  ARBACE_HOST_EXE=$exe "$root/bin/jrt-convert" casefolding --ucd "$t/u17" > "$t/c17.log" 2>&1
  grep -q 'is not test/scheme/ucd.edn' "$t/c17.log" && ok "control: a UnicodeData.txt other than the pin is refused" || no "control, the pin: $(tail -1 "$t/c17.log")"
  s=$jdk/src/java.base/share
  ARBACE_PATH=$root "$exe" "$root/test/javac-platform/casefolding.ae" "$s/classes/jdk/internal/lang/CaseFolding.java.template" \
    "$s/data/unicodedata/CaseFolding.txt" "$t/u17/UnicodeData.txt" "$t/cf17.java" > "$t/g17.log" 2>&1
  rec=$root/jdk/javac/platform-records/java.base/jdk/internal/lang/CaseFolding.java
  if [ -f "$t/cf17.java" ] && ! cmp -s "$t/cf17.java" "$rec" &&
     [ $(( $(grep -c 'entry(0x' "$rec") - $(grep -c 'entry(0x' "$t/cf17.java") )) = 28 ]; then
    ok "control: Unicode 17's case mappings (javac's tool's) give another file, 28 entries fewer"
  else no "control, Unicode 17: $(tail -1 "$t/g17.log")"; fi
else
  echo "note: no $ucd/UnicodeData.txt (bin/repr-scheme ucd fetch) or no jdk26u: the recorded copy's check not run"
fi

[ $fails = 0 ] && { echo "casefolding-test: all passed"; exit 0; }
echo "casefolding-test: $fails failed"; exit 1
