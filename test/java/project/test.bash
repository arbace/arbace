#!/bin/bash
# bin/java-project test (doc/go/BUILD.md, "Java sources by module"): the fixtures of the Java project model
# (A1), artifacts sources-first (A2), compile what resolves (A3) and jars on the class path (A11), with
# their negative controls. Light: about two minutes, each process under half a GiB. Work under
# .tmp/java-project-test (JAVA_PROJECT_TEST_WORK). Needs javac's tool executable (REPR_JAVA_EXE, as
# bin/repr-java) and the tools' host.
#   - the toy repository is test/java/project/make-jars' output byte for byte (--check);
#   - test/programs/java-project: compiled module by module by javac on Arbace (classes), read with the
#     forms of the native trees equal to javac's trees' (--check front) and read without javac equal to
#     javac's read (--check nread);
#   - its toylib closure's report: JsonFormat left out for com.fasterxml.jackson.core, JsonUser after it
#     (the cascade), Unused not reached, PlainFormat kept; the multi-release list names the binary jar's
#     release 11 Helper; Platform read at release 25's layer ("release 11"); the split package demo.common
#     merged with both loads; the resources (the services file, toylib.properties, util's words.txt; not
#     Maven's pom.properties), and the same a directory a module (resources.jars, A5's class path);
#   - :patches: toylib read with a variant applied (controls/patched.edn), listed in WORK/src/toylib.patches.edn;
#   - the controls, each of which must fail (test/java/project/controls/): a patch that does not apply; a model with an unknown key, a
#     cycle, :closure on a module of sources; a root naming an absent type; a module naming the
#     annotations jar's type without the jar (and with it, compiled); release 10 reads Platform's base
#     layer; a forms file changed in a copy of the native trees is found by the comparison; a resource outside
#     META-INF in two modules, differing (META-INF's: the first module's kept, the later listed in WORK/resources.shadowed);
#     an :outside value neither :platform nor :class-path; a class in two modules (A16: controls/shadow.edn, the first
#     module's in forms.all, the later listed in WORK/classes.shadowed): the later copy in forms.all, and with :shadow false
#     (controls/shadow-off.edn) the read itself;
#   - :outside (A8, A8b; test/programs/outside): the read's WORK/outside.args, j2g's --outside-platform (the platform's
#     directory) and --outside-classes (the binary-only jar unpacked, named by its coordinate);
#   - :jdk-extra (A15, GH13 (a); controls/jdk-extra.edn): java.xml as the program's world extension, the plan's module
#     of kind jdk first (jdk26u's share, unix and linux layers), its cut from org.xml.sax.SAXParseException three files
#     (--patch-module, -Xprefer:source), read by read-jdk with both fronts and nread equal, its forms in forms.all byte for
#     byte bin/javac-vendor's read of the same files (jdk/javac/forms) and their headers jdk26u's paths, its resources
#     (the JDK catalog, Xerces' messages; not HTML) the first directory of resources.jars, its variant copied by digest
#     and named by --variants in outside.args; :classes (the set itself, no cut: Locator and Location alone); controls:
#     java.base, no :closure, both :closure and :classes, an unknown key, a jdk26u tree not at the pin, a root or a
#     :classes name the module lacks.
set -uo pipefail
root=$(cd "$(dirname "$0")/../../.." && pwd)
. "$root/bin/lib/tmpdir.bash"
w=${JAVA_PROJECT_TEST_WORK:-$root/.tmp/java-project-test}
fx=$root/test/programs/java-project
ctl=$root/test/java/project/controls
jp=$root/bin/java-project
rm -rf "$w"; mkdir -p "$w"
pass=0 fail=0
ok() { echo "  pass: $1"; pass=$((pass + 1)); }
ko() { echo "  FAIL: $1"; fail=$((fail + 1)); }
expect() { local what=$1; shift; if "$@"; then ok "$what"; else ko "$what"; fi; }
must_fail() { # must_fail WHAT LOG CMD...: the command fails (a control)
  local what=$1 log=$2; shift 2
  if "$@" > "$log" 2>&1; then ko "control passed: $what ($log)"; else ok "control fails: $what"; fi
}
has() { grep -qF -- "$2" "$1"; }
logged() { local log=$1; shift; "$@" > "$log" 2>&1; } # logged LOG CMD...: CMD's output into LOG

echo "java-project test:"
expect "the toy repository is make-jars' output" logged "$w/make-jars.log" "$root/test/java/project/make-jars" --check
expect "classes: every module compiled by javac on Arbace" logged "$w/classes.log" "$jp" classes "$fx" "$w/fx"
expect "read --check front: the native trees' forms equal javac's" logged "$w/front.log" "$jp" read "$fx" "$w/fx" --check front
expect "read --check nread: the forms read without javac equal javac's" logged "$w/nread.log" "$jp" read "$fx" "$w/fx" --check nread
expect "read --chunk 1: the forms of one process a package equal one process's" \
  bash -c '"$1" read "$2" "$3/fx1" --chunk 1 > "$3/chunk.log" 2>&1 && "$1" read "$2" "$3/fx2" > "$3/whole.log" 2>&1 && diff -r "$3/fx1/forms.all" "$3/fx2/forms.all" > "$3/chunk.diff"' _ "$jp" "$fx" "$w"
r=$w/fx/closure/toylib/report.edn
expect "closure: JsonFormat left out for com.fasterxml.jackson.core" \
  has "$r" '{:file "org/example/toylib/ext/JsonFormat.java", :round 1, :cause :absent, :names "com.fasterxml.jackson.core"}'
expect "closure: JsonUser left out after JsonFormat (the cascade)" \
  has "$r" ':file "org/example/toylib/ext/JsonUser.java", :round 2, :cause :absent, :names "JsonFormat", :left-out "org.example.toylib.ext.JsonFormat"}'
expect "closure: PlainFormat (an optional root) kept" has "$r" '"org/example/toylib/ext/PlainFormat.java"'
expect "closure: Unused (reached by no root) not kept" bash -c '! grep -q Unused "$1"' _ "$r"
expect "multi-release: the binary jar's release 11 Helper without its source listed" \
  has "$w/fx/src/toylib.mr.edn" '"META-INF/versions/11/org/example/toylib/Helper.class"'
expect "multi-release: Platform read at release 25's layer" has "$w/fx/forms.all/org/example/toylib/Platform.ae" '"release 11"'
expect "split package: demo.common's namespace loads both modules' classes" \
  bash -c 'grep -q "(load \"common/Other\")" "$1" && grep -q "(load \"common/Shared\")" "$1"' _ "$w/fx/forms.all/demo/common.ae"
expect "resources: the services file, toylib.properties and words.txt, not Maven's metadata" \
  bash -c 'cd "$1" && [ -f META-INF/services/org.example.toylib.ext.Format ] && [ -f toylib.properties ] && [ -f demo/util/words.txt ] && [ ! -e META-INF/maven ] && [ ! -e META-INF/MANIFEST.MF ]' _ "$w/fx/resources.all"
expect "resources by module: util's words.txt in 001-util, toylib's services file in 002-toylib, app none" \
  bash -c 'cd "$1" && [ "$(ls | tr "\n" " ")" = "001-util 002-toylib " ] && [ -f 001-util/demo/util/words.txt ] && [ -f 002-toylib/META-INF/services/org.example.toylib.ext.Format ] && [ ! -e 001-util/META-INF ]' _ "$w/fx/resources.jars"
expect "the forms name their files by module (no work path)" bash -c '! grep -rq "$1" "$2"' _ "$w" "$w/fx/forms.all"

expect ":patches: the variant applied before the closure, read, and listed" \
  bash -c '"$1" read "$2" "$3/p" --base "$4" --root "controls=$5" --no-classes > "$3/patched.log" 2>&1 \
    && grep -q "\"patched \"" "$3/p/forms.all/org/example/toylib/Greeter.ae" \
    && grep -q ":patch \"greeter-hello.patch\" :sha256 \"[0-9a-f]*\" :files \[\"org/example/toylib/Greeter.java\"\]" "$3/p/src/toylib.patches.edn"' \
  _ "$jp" "$ctl/patched.edn" "$w" "$fx" "$ctl"

expect "META-INF in two modules, differing: the first module's in resources.all, the later listed, each its own by module" \
  bash -c '"$1" read "$2" "$3/md" --base "$4" --root "controls=$5" --no-classes > "$3/meta-dup.log" 2>&1 \
    && [ "$(cat "$3/md/resources.all/META-INF/NOTICE")" = "module a'"'"'s notice" ] \
    && [ "$(cat "$3/md/resources.shadowed")" = "META-INF/NOTICE b" ] \
    && [ "$(cat "$3/md/resources.jars/002-b/META-INF/NOTICE")" = "module b'"'"'s notice" ]' \
  _ "$jp" "$ctl/meta-dup.edn" "$w" "$fx" "$ctl"

expect "A16: a class in two modules the first module's in forms.all (the plan's order), the later listed, an equal one silent" \
  bash -c '"$1" read "$2" "$3/sh" --base "$4" --root "controls=$5" --no-classes > "$3/shadow.log" 2>&1 \
    && grep -q "\"new\"" "$3/sh/forms.all/shadow/dup/Version.ae" \
    && [ "$(cat "$3/sh/classes.shadowed")" = "shadow/dup/Version.ae old" ] \
    && [ -f "$3/sh/forms.all/shadow/dup/Same.ae" ] && [ -f "$3/sh/forms.all/shadow/dup/OldOnly.ae" ] \
    && grep -q "(load \"dup/OldOnly\")" "$3/sh/forms.all/shadow/dup.ae"' \
  _ "$jp" "$ctl/shadow.edn" "$w" "$fx" "$ctl"

ofx=$root/test/programs/outside
expect ":outside: outside.args names the platform's modules and the class path's binary jar by its coordinate (A8, A8b)" \
  bash -c '"$1" read "$2" "$3/out" --no-classes > "$3/outside.log" 2>&1 \
    && [ "$(sed -n 1p "$3/out/outside.args")" = --outside-platform ] && [ -d "$(sed -n 2p "$3/out/outside.args")/java.desktop" ] \
    && [ "$(sed -n 3p "$3/out/outside.args")" = --outside-classes ] \
    && d=$(sed -n 4p "$3/out/outside.args") && [ "${d##*=}" = org.example:toyext:1.0 ] && [ -f "${d%=*}/org/example/toyext/ToyText.class" ] \
    && [ "$(wc -l < "$3/out/outside.args")" = 4 ]' _ "$jp" "$ofx" "$w"
expect "  (without :outside, no outside.args lines: the fixture of java-project)" bash -c '[ ! -s "$1/fx/outside.args" ]' _ "$w"

jx=$ctl/jdk-extra.edn
jxo=(--base "$fx" --root "controls=$ctl")
expect "A15 :jdk-extra: the plan's first module java.xml, of kind jdk, jdk26u's layers" \
  bash -c '"$2" plan "$3" "${@:4}" > "$1/jx.plan" 2>&1 && [ "$(sed -n 2p "$1/jx.plan")" = "$(printf "module\tjava.xml")" ] \
    && grep -qx "$(printf "kind\tjdk")" "$1/jx.plan" && grep -q "^src.*/src/java.xml/share/classes.*/src/java.xml/unix/classes" "$1/jx.plan" \
    && grep -qx "$(printf "module\tapp")" "$1/jx.plan"' _ "$w" "$jp" "$jx" "${jxo[@]}"
expect "A15: read --check front: java.xml's cut read by read-jdk, both trees' forms equal" \
  logged "$w/jx-front.log" "$jp" read "$jx" "$w/jx" "${jxo[@]}" --check front
expect "A15: read --check nread: read-jdk without javac equal" logged "$w/jx-nread.log" "$jp" read "$jx" "$w/jx" "${jxo[@]}" --check nread
expect "A15: the cut keeps SAXParseException, SAXException and Locator alone" \
  bash -c 'sed -n "/^ :kept\$/,\$p" "$1" | grep -o "\"[^\"]*\"" | tr "\n" " " | grep -qx "\"org/xml/sax/Locator.java\" \"org/xml/sax/SAXException.java\" \"org/xml/sax/SAXParseException.java\" "' \
  _ "$w/jx/closure/java.xml/report.edn"
expect "A15: the forms in forms.all, byte for byte bin/javac-vendor's read of the same files (jdk/javac/forms)" \
  bash -c 'for f in Locator SAXException SAXParseException; do cmp -s "$1/org/xml/sax/$f.ae" "$2/org/xml/sax/$f.ae" || exit 1; done
    [ "$(find "$1/org/xml" -name "*.ae" | wc -l)" = 4 ] && [ -f "$1/demo/xmlerr/Where.ae" ]' _ "$w/jx/forms.all" "$root/jdk/javac/forms"
expect "A15: the forms' headers name jdk26u's paths" \
  has "$w/jx/forms.all/org/xml/sax/Locator.ae" ";; Converted from jdk26u src/java.xml/share/classes/org/xml/sax/Locator.java by arbace.repr.java"
expect "A15: java.xml's resources the first directory of resources.jars (the catalog, Xerces' messages; no HTML)" \
  bash -c 'cd "$1" && [ "$(ls | tr "\n" " ")" = "001-java.xml " ] && [ -f 001-java.xml/jdk/xml/internal/jdkcatalog/JDKCatalog.xml ] \
    && [ -f 001-java.xml/com/sun/org/apache/xerces/internal/impl/msg/XMLMessages.properties ] && [ -z "$(find . -name "*.html")" ]' _ "$w/jx/resources.jars"
expect "A15: outside.args names the variants' directory, by digest, holding SecuritySupport.ae" \
  bash -c '[ "$(sed -n 1p "$1/outside.args")" = --variants ] && d=$(sed -n 2p "$1/outside.args") && [ "$(wc -l < "$1/outside.args")" = 2 ] \
    && [[ $d == "$1"/variants/????????????????????  ]] && cmp -s "$d/SecuritySupport.ae" "$2/overlay/jdk/javac/variants/SecuritySupport.ae"' _ "$w/jx" "$root"
sed 's/:closure {:roots \["org.xml.sax.SAXParseException"\]}/:classes ["org.xml.sax.Locator" "javax.xml.stream.Location"]/' "$jx" > "$w/jx-classes.edn"
expect "A15 :classes: the set itself, no cut: Locator and Location read, nothing else of java.xml" \
  bash -c '"$2" read "$3" "$1/jc" "${@:4}" --no-classes > "$1/jx-classes.log" 2>&1 \
    && [ "$(sed -n "/^ :kept\$/,\$p" "$1/jc/closure/java.xml/report.edn" | grep -o "\"[^\"]*\"" | tr "\n" " ")" = "\"javax/xml/stream/Location.java\" \"org/xml/sax/Locator.java\" " ] \
    && [ -f "$1/jc/forms.all/javax/xml/stream/Location.ae" ] && [ -f "$1/jc/forms.all/org/xml/sax/Locator.ae" ] \
    && [ "$(find "$1/jc/forms.all" -name "*.ae" | grep -vc demo/)" = 4 ]' _ "$w" "$jp" "$w/jx-classes.edn" "${jxo[@]}"

# round 53 (1): Clojure's suite's java.xml (test/clojure/jdk-extra/java-xml.edn: a plan of :jdk-extra alone, :classes the
# measured SAX and StAX set test/clojure/jdk-extra/java-xml-classes.txt, javac's tool world's two variants)
cx=$root/test/clojure/jdk-extra/java-xml.edn
expect "round 53: the suite's plan: java.xml alone, of kind jdk, 246 :classes, two variants, no other module" \
  bash -c '"$2" plan "$3" > "$1/cx.plan" 2>&1 && [ "$(grep -c "^module" "$1/cx.plan")" = 1 ] && grep -qx "$(printf "module\tjava.xml")" "$1/cx.plan" \
    && grep -qx "$(printf "kind\tjdk")" "$1/cx.plan" && [ "$(grep "^classes" "$1/cx.plan" | tr "\t" "\n" | tail -n +2 | wc -l)" = 246 ] \
    && [ "$(grep "^variants" "$1/cx.plan" | tr "\t" "\n" | tail -n +2 | sed "s|.*/||" | tr "\n" " ")" = "SecuritySupport.ae JdkXmlConfig.ae " ]' _ "$w" "$jp" "$cx"
expect "round 53: the class set: 246 files, javac's tool world's 221 java.xml files (overlay/jdk/javac/closure.txt) and StAX's 25" \
  bash -c '"$2" sources "$3" "$1/cx" > "$1/cx-sources.log" 2>&1 && [ "$(wc -l < "$1/cx/closure/java.xml/kept.txt")" = 246 ] \
    && grep "^java.xml " "$4/overlay/jdk/javac/closure.txt" | cut -d" " -f2 | LC_ALL=C sort > "$1/cx-tool.txt" \
    && [ "$(LC_ALL=C comm -23 "$1/cx-tool.txt" "$1/cx/closure/java.xml/kept.txt" | wc -l)" = 0 ] \
    && [ "$(LC_ALL=C comm -13 "$1/cx-tool.txt" "$1/cx/closure/java.xml/kept.txt" | wc -l)" = 25 ] \
    && grep -qx com/sun/xml/internal/stream/XMLInputFactoryImpl.java "$1/cx/closure/java.xml/kept.txt"' _ "$w" "$jp" "$cx" "$root"
expect "round 53: read --chunk 20: every read process under 1 GiB, 246 forms, the tool world's 221 byte for byte jdk/javac/forms'" \
  bash -c '"$2" read "$3" "$1/cx" --no-classes --chunk 20 > "$1/cx-read.log" 2>&1 || exit 1
    pk=$(cat "$1"/cx/chunks/java.xml/*.read.time | sed -n "s/.*Maximum resident set size (kbytes): //p" | sort -n | tail -1)
    echo "  (the peak: $pk kB, $(ls "$1"/cx/chunks/java.xml/*.list | wc -l) chunks)"; [ "$pk" -lt 1048576 ] || exit 1
    [ "$(cd "$1/cx/forms.all" && find . -name "*.ae" | while read -r f; do b=${f%.ae}; [ -f "$1/cx/closure/java.xml/src/$b.java" ] && echo "$f"; done | wc -l)" = 246 ] || exit 1
    sed "s/\.java\$/.ae/" "$1/cx-tool.txt" | while read -r f; do cmp -s "$1/cx/forms.all/$f" "$4/jdk/javac/forms/$f" || { echo "  differs: $f"; exit 1; }; done' \
  _ "$w" "$jp" "$cx" "$root"
expect "round 53: outside.args: the two variants' directory alone (no :outside)" \
  bash -c '[ "$(wc -l < "$1/outside.args")" = 2 ] && [ "$(sed -n 1p "$1/outside.args")" = --variants ] && [ "$(ls "$(sed -n 2p "$1/outside.args")" | tr "\n" " ")" = "JdkXmlConfig.ae SecuritySupport.ae " ]' _ "$w/cx"

echo "controls:"
sed 's/:closure {:roots/:classes ["org.xml.sax.Locator"] :closure {:roots/' "$jx" > "$w/jx-both.edn"
must_fail "A15: :jdk-extra with both :closure and :classes" "$w/c19.log" "$jp" plan "$w/jx-both.edn" "${jxo[@]}"
sed 's/:closure {:roots \["org.xml.sax.SAXParseException"\]}/:classes ["org.xml.sax.NoSuchType"]/' "$jx" > "$w/jx-classes-absent.edn"
must_fail "A15: a :classes name the module lacks" "$w/c20.log" "$jp" sources "$w/jx-classes-absent.edn" "$w/c20" "${jxo[@]}"
expect "  (its message names the class)" has "$w/c20.log" ":classes with no file: org.xml.sax.NoSuchType"
sed 's/:module "java.xml"/:module "java.base"/' "$jx" > "$w/jx-base.edn"
must_fail "A15: :jdk-extra java.base" "$w/c14.log" "$jp" plan "$w/jx-base.edn" "${jxo[@]}"
expect "  (its message names the shipped world)" has "$w/c14.log" "java.base is the shipped world's"
sed 's/:closure {:roots \["org.xml.sax.SAXParseException"\]}//' "$jx" > "$w/jx-noclosure.edn"
must_fail "A15: :jdk-extra without :closure" "$w/c15.log" "$jp" plan "$w/jx-noclosure.edn" "${jxo[@]}"
sed 's/:module "java.xml"/:module "java.xml" :patches []/' "$jx" > "$w/jx-key.edn"
must_fail "A15: :jdk-extra with an unknown key" "$w/c16.log" "$jp" plan "$w/jx-key.edn" "${jxo[@]}"
mkdir -p "$w/fakejdk/src/java.xml/share/classes/org/xml"
cp -r "${JDK:-/root/jdk26u}/src/java.xml/share/classes/org/xml/sax" "$w/fakejdk/src/java.xml/share/classes/org/xml/"
must_fail "A15: a jdk26u tree not at the platform's pin" "$w/c17.log" "$jp" sources "$jx" "$w/c17" "${jxo[@]}" --root "jdk26u=$w/fakejdk"
expect "  (its message names the pin)" has "$w/c17.log" "is not jdk26u at the platform's pin"
sed 's/org.xml.sax.SAXParseException/org.xml.sax.NoSuchType/' "$jx" > "$w/jx-absent.edn"
must_fail "A15: a :jdk-extra root the module lacks" "$w/c18.log" "$jp" sources "$w/jx-absent.edn" "$w/c18" "${jxo[@]}"
expect "  (its message names the root)" has "$w/c18.log" "roots with no file (or left out): org.xml.sax.NoSuchType"
printf '{:java {:outside [:platform]}}\n' > "$w/cx-none.edn"
must_fail "round 53: a plan with neither :modules nor :jdk-extra" "$w/c21.log" "$jp" plan "$w/cx-none.edn"
expect "  (its message names :jdk-extra)" has "$w/c21.log" "absent only beside :jdk-extra"
must_fail ":outside with a value neither :platform nor :class-path" "$w/c10.log" "$jp" plan "$ctl/outside-bad.edn" --base "$fx"
must_fail "a resource outside META-INF in two modules, differing" "$w/c9.log" \
  "$jp" read "$ctl/res-dup.edn" "$w/c9" --base "$fx" --root "controls=$ctl" --no-classes
expect "  (its message names the resource)" has "$w/c9.log" "the resource shared.txt in two modules (b's differs)"
must_fail "A16: the second module's copy of a shadowed class in forms.all" "$w/c12.log" \
  grep -q '"old"' "$w/sh/forms.all/shadow/dup/Version.ae"
must_fail "A16: :shadow false, a class in two modules" "$w/c13.log" \
  "$jp" read "$ctl/shadow-off.edn" "$w/c13" --base "$fx" --root "controls=$ctl" --no-classes
expect "  (its message names the class)" grep -Eq "merge-forms: shadow/dup/(Version|Same).ae in two modules" "$w/c13.log"
must_fail "an unknown module key" "$w/c1.log" "$jp" plan "$ctl/unknown-key.edn" --base "$fx"
must_fail "a cycle of :deps" "$w/c2.log" "$jp" plan "$ctl/cycle.edn" --base "$fx"
must_fail ":closure on a module of sources" "$w/c3.log" "$jp" plan "$ctl/closure-on-src.edn" --base "$fx"
must_fail "a root naming an absent type" "$w/c4.log" "$jp" sources "$ctl/root-absent.edn" "$w/c4" --base "$fx"
expect "  (its message names the root)" has "$w/c4.log" "the root org/example/toylib/ext/JsonUser.java does not resolve"
must_fail "a module naming the annotation without its jar" "$w/c5.log" \
  "$jp" classes "$ctl/no-annotations-jar.edn" "$w/c5" --base "$fx" --root "controls=$ctl"
sed 's|{:name "n" :src \["$controls/nullable-src"\]}|{:name "n" :src ["$controls/nullable-src"] :artifacts ["org.example:toyapi:1.0"]}|' \
  "$ctl/no-annotations-jar.edn" > "$w/with-jar.edn"
expect "  (with the jar it compiles)" logged "$w/c5b.log" "$jp" classes "$w/with-jar.edn" "$w/c5b" --base "$fx" --root "controls=$ctl"
"$jp" read "$ctl/release-10.edn" "$w/c6" --base "$fx" --no-classes > "$w/c6.log" 2>&1
must_fail "release 10 reads the layer of release 11" "$w/c6.grep" grep -q '"release 11"' "$w/c6/forms/toylib/org/example/toylib/Platform.ae"
expect "  (it reads the base)" has "$w/c6/forms/toylib/org/example/toylib/Platform.ae" '"base"'
must_fail "a patch whose context the sources lack" "$w/c8.log" "$jp" sources "$ctl/patch-absent.edn" "$w/c8" --base "$fx" --root "controls=$ctl"
expect "  (its message names the patch)" has "$w/c8.log" "greeter-absent.patch does not apply"
cp -a "$w/fx/check-front/app" "$w/c7"
sed -i 's/"world"/"wereld"/' "$w/c7/demo/app/Main.ae"
must_fail "a forms file changed in the native trees' copy" "$w/c7.log" bash -c '
  diff -rq -x "repr-java-*.edn" "$1" "$2" | sed "s|^Files $1/\(.*\) and .* differ\$|\1|" > "$3"; [ ! -s "$3" ]' _ "$w/fx/forms/app" "$w/c7" "$w/c7.differ"

# a sources jar's module-info.java: dropped from the unpacked tree, so javac reads on the class path (controls/modinfo)
mkdir -p "$w/mi/repo/org/example/modlib/1.0"
(cd "$ctl/modinfo/lib" && TZ=UTC zip -q -X -r "$w/mi/repo/org/example/modlib/1.0/modlib-1.0-sources.jar" module-info.java demo)
expect "module-info.java in a sources jar: the dependent reads java.awt.Point (class path, no single-module javac)" \
  bash -c '"$1" read "$2" "$3/mi/w" --repository "$3/mi/repo" --root "controls=$4" > "$3/mi.log" 2>&1 \
    && [ ! -e "$3/mi/w/src/modlib/module-info.java" ] && [ -f "$3/mi/w/forms.all/demo/modapp/App.ae" ] && ! grep -q "does not read" "$3/mi.log"' \
  _ "$jp" "$ctl/modinfo/model.edn" "$w" "$ctl"
must_fail "the library as a module of sources with its module-info.java: javac's single-module mode, does not read" "$w/mi-src.log" \
  "$jp" read "$ctl/modinfo/model-src.edn" "$w/mi/src" --root "controls=$ctl"
expect "  (its message is javac's)" bash -c 'grep -rq "does not read" "$1" "$2"/mi/src/log' _ "$w/mi-src.log" "$w"

echo "java-project test: $pass passed, $fail failed ($w)"
[ $fail = 0 ]
