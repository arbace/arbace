#!/bin/bash
# The own platform's tooling (doc/classes/JAVA-FRONTEND-SURVEY.md, "Own platform", JOP1-JOP8 (a)), on fabricated trees
# under .tmp/ (seconds, no javac, no build tree read): bin/javac-vendor's module check under pipefail (java.base, the data's
# first module, was refused), classes modules' control 2 passing over a module without a class, the pin's kind, the own
# pin's lines and own_check with its negative control, the generated sources' pack (deterministic, unpacked by
# bin/lib/repr-java/platform.bash's repr_java_gensrc_root for the own kind), the known file's entries, and the audit's rule
# (a read under the build tree fails with the own pin, outside the oracle tier).
# needs: exe
set -uo pipefail
root=$(cd "$(dirname "$0")/../.." && pwd)
here=$root
. "$root/bin/lib/tmpdir.bash"
t=$TMPDIR/javac-platform-test
rm -rf "$t"; mkdir -p "$t/out" "$t/work"
fails=0
ok() { echo "ok   $1"; }
no() { echo "FAIL $1"; fails=$((fails + 1)); }

# bin/javac-vendor's functions, OUT the fabricated tree
cp "$root/jdk/javac/platform-own-known.edn" "$t/out/"
# shellcheck disable=SC1090
JAVAC_VENDOR_LIB=1 . "$root/bin/javac-vendor" --work "$t/work" --out "$t/out"
set +e

# 1. the module check under pipefail: java.base first among many modules (sed's output beyond a pipe's buffer)
{ echo '{:jdk "x"'; echo ' :modules {'; echo '  "java.base"'; for i in $(seq 1 20000); do echo "  \"m$i\""; done; echo ' }}'; } > "$t/out/platform-build.edn"
pbuild=$t/out/platform-build.edn
if pb_has_module java.base; then ok "pb_has_module: java.base, the first of 20,001 modules"; else no "pb_has_module refuses java.base"; fi
if pb_has_module m20000; then ok "pb_has_module: the last module"; else no "pb_has_module refuses the last module"; fi
if ! pb_has_module jdk.none; then ok "pb_has_module: a module not there is refused"; else no "pb_has_module takes a module not there"; fi
# the control: the old pipe into grep -q fails here (why the check was refused)
if ! (pb_modules | grep -qx java.base); then ok "control: the old 'pb_modules | grep -qx' fails under pipefail"
else echo "note the old pipe passed this time (SIGPIPE's timing); the fix does not depend on it"; fi

# 2. control 2's subject: java.se (equal, no class) passed over, java.logging's first equal class taken
d=$t/work/classes/modules; mkdir -p "$d/java.se" "$d/java.logging" "$d/jdk.net"
printf 'java.se\t1\t0\t0\tequal\t1\t1\t0\t0\t0\njdk.net\t9\t2\t2\tknown\t1\t1\t1\t0\t0\njava.logging\t35\t2\t2\tequal\t1\t1\t0\t0\t0\n' > "$d/summary.tsv"
: > "$d/java.se/build.sums"; : > "$d/java.se/diffs.tsv"
printf 'jdk/net/A$1.class\taa\nzz.class\tbb\n' > "$d/jdk.net/build.sums"; printf 'jdk/net/A$1.class\taa\tcc\n' > "$d/jdk.net/diffs.tsv"
printf 'java/util/logging/Level.class\taa\njava/util/logging/Logger.class\tbb\n' > "$d/java.logging/build.sums"; : > "$d/java.logging/diffs.tsv"
read -r m c < <(control2_pick "$d")
[ "$m $c" = "jdk.net zz.class" ] && ok "control2_pick: java.se (no class) passed over, jdk.net's undiffering class" || no "control2_pick gave '$m $c'"
printf 'java.se\t1\t0\t0\tequal\t1\t1\t0\t0\t0\n' > "$d/summary.tsv"
read -r m c < <(control2_pick "$d")
[ -z "$m" ] && ok "control2_pick: none when no module has a class" || no "control2_pick took '$m $c' from a run without a class"

# 3. the known file: 48 entries, every one :param-names with two digests, read by known_tsv
n=$(known_tsv "$t/out/platform-own-known.edn" | awk -F'\t' 'length($3) == 64 && length($4) == 64 && $5 == "param-names"' | wc -l)
[ "$n" = 48 ] && ok "platform-own-known.edn: 48 :param-names entries" || no "platform-own-known.edn: $n well-formed :param-names entries, not 48"

# 4. the pin's kind
printf '# a build pin\n%064d  java.base\n' 0 > "$t/out/platform.sha256"
[ "$(platform_kind)" = build ] && ok "platform_kind: a pin without the kind line is the build tree's" || no "platform_kind of a build pin"
# 5. the own pin's lines, own_check and its negative control
o=$t/own; mkdir -p "$o/modules/java.base/java/lang" "$o/modules/java.logging" "$o/gensrc/java.base/a" "$o/gensrc/java.logging"
echo A > "$o/modules/java.base/java/lang/Object.class"; echo M > "$o/modules/java.base/module-info.class"
echo L > "$o/modules/java.logging/module-info.class"
echo 'class X {}' > "$o/gensrc/java.base/a/X.java"; echo 'class Y {}' > "$o/gensrc/java.logging/Y.java"
printf '{:jdk "x"\n :modules {\n  "java.base"\n  "java.logging"\n }}\n' > "$pbuild"
own_pin_lines "$o" > "$o/pin.sha256"
cp "$o/pin.sha256" "$t/out/platform.sha256"
[ "$(platform_kind)" = own ] && ok "platform_kind: the own pin" || no "platform_kind of the own pin"
[ "$(pin_modules | tr '\n' ' ')" = "java.base java.logging " ] && ok "pin_modules: the modules, not the gensrc lines" || no "pin_modules: $(pin_modules | tr '\n' ' ')"
[ -n "$(pin_line "$t/out/platform.sha256" gensrc/java.logging)" ] && ok "the own pin has the generated sources' lines" || no "no gensrc/java.logging line in the own pin"
own_check "$o" "$t/out/platform.sha256" 2>"$t/check.err" && ok "own_check: the platform is its pin's" || no "own_check fails its own pin ($(cat "$t/check.err"))"
echo B > "$o/modules/java.base/java/lang/Object.class"; own_pin_lines "$o" > "$o/pin.sha256"
if ! own_check "$o" "$t/out/platform.sha256" 2>"$t/check.err" && grep -q 'java.base' "$t/check.err"; then ok "own_check: a changed class file fails, naming its module (negative control)"
else no "own_check passed a changed class file"; fi
echo 'class Z {}' > "$o/gensrc/java.logging/Z.java"; echo A > "$o/modules/java.base/java/lang/Object.class"; own_pin_lines "$o" > "$o/pin.sha256"
if ! own_check "$o" "$t/out/platform.sha256" 2>"$t/check.err" && grep -q 'gensrc/java.logging' "$t/check.err"; then ok "own_check: a generated source more fails (negative control)"
else no "own_check passed a changed generated module"; fi
rm "$o/gensrc/java.logging/Z.java"

# 6. the pack: deterministic, and repr_java_gensrc_root unpacks it for the own kind
own_gensrc_pack "$o/gensrc" "$t/p1.tar.xz" && sleep 1 && touch "$o/gensrc/java.base/a/X.java" && own_gensrc_pack "$o/gensrc" "$t/p2.tar.xz"
cmp -s "$t/p1.tar.xz" "$t/p2.tar.xz" && ok "own_gensrc_pack: the same bytes twice (an mtime apart)" || no "own_gensrc_pack is not deterministic"
r=$t/root; mkdir -p "$r/jdk/javac"; cp "$t/out/platform.sha256" "$r/jdk/javac/"; cp "$t/p1.tar.xz" "$r/jdk/javac/platform-gensrc.tar.xz"
g=$( (root=$r; . "$here/bin/lib/repr-java/platform.bash"; repr_java_gensrc_root) )
[ -n "$g" ] && cmp -s "$g/java.logging/Y.java" "$o/gensrc/java.logging/Y.java" && ok "repr_java_gensrc_root: the own kind's pack unpacked ($g)" || no "repr_java_gensrc_root: no unpacked pack ('$g')"
k=$( (root=$r; . "$here/bin/lib/repr-java/platform.bash"; repr_java_platform_kind) )
[ "$k" = own ] && ok "repr_java_platform_kind: own" || no "repr_java_platform_kind: $k"
printf '# a build pin\n' > "$r/jdk/javac/platform.sha256"
k=$( (root=$r; . "$here/bin/lib/repr-java/platform.bash"; repr_java_platform_kind) )
[ "$k" = build ] && ok "repr_java_platform_kind: build" || no "repr_java_platform_kind of a build pin: $k"

# 7. the audit's rule (JOP6 (a)): a read under the build tree fails with the own pin outside the oracle tier
mkdir -p "$t/jdk/build/x/jdk/modules" "$t/jdk/build/x/images/jdk/lib" "$t/jdk/src"
touch "$t/jdk/build/x/jdk/modules/a.class" "$t/jdk/build/x/images/jdk/lib/modules" "$t/jdk/src/a.java"
cat > "$t/audit.ae" <<EOF
(require '[gate.audit :as a])
(let [ops {"$t/jdk/build/x/jdk/modules/a.class" #{"R"} "$t/jdk/build/x/images/jdk/lib/modules" #{"R"} "$t/jdk/src/a.java" #{"R"}}
      d (fn [f] (= f "jdk/javac/platform.sha256"))]
  (println "banned" (count (a/outside-problems ops "/none" "$t/jdk" "$t/jdk/build/x/images/jdk" d [:jdk] true)))
  (println "allowed" (count (a/outside-problems ops "/none" "$t/jdk" "$t/jdk/build/x/images/jdk" d [:jdk] false)))
  (println "own" (a/own-pin? "$r") (a/own-pin? "$root")))
EOF
cp "$t/out/platform.sha256" "$r/jdk/javac/platform.sha256"
root=$here
out=$(ARBACE_PATH=$root/test:$root "${ARBACE_HOST_EXE:?}" "$t/audit.ae" 2>&1)
echo "$out" | grep -qx 'banned 1' && ok "audit: the build tree's class file read fails with the own pin (the frozen JVM's image and src/ do not)" || no "audit, banned: $out"
echo "$out" | grep -qx 'allowed 0' && ok "audit: nothing fails with the build pin or in the oracle tier" || no "audit, allowed: $out"
echo "$out" | grep -qx 'own true false' && ok "audit: own-pin? reads the pin's kind" || no "audit, own-pin?: $out"

[ $fails = 0 ] && { echo "javac-platform-test: all passed"; exit 0; }
echo "javac-platform-test: $fails failed"; exit 1
