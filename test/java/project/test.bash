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
#     an :outside value neither :platform nor :class-path;
#   - :outside (A8, A8b; test/programs/outside): the read's WORK/outside.args, j2g's --outside-platform (the platform's
#     directory) and --outside-classes (the binary-only jar unpacked, named by its coordinate).
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

ofx=$root/test/programs/outside
expect ":outside: outside.args names the platform's modules and the class path's binary jar by its coordinate (A8, A8b)" \
  bash -c '"$1" read "$2" "$3/out" --no-classes > "$3/outside.log" 2>&1 \
    && [ "$(sed -n 1p "$3/out/outside.args")" = --outside-platform ] && [ -d "$(sed -n 2p "$3/out/outside.args")/java.desktop" ] \
    && [ "$(sed -n 3p "$3/out/outside.args")" = --outside-classes ] \
    && d=$(sed -n 4p "$3/out/outside.args") && [ "${d##*=}" = org.example:toyext:1.0 ] && [ -f "${d%=*}/org/example/toyext/ToyText.class" ] \
    && [ "$(wc -l < "$3/out/outside.args")" = 4 ]' _ "$jp" "$ofx" "$w"
expect "  (without :outside, no outside.args lines: the fixture of java-project)" bash -c '[ ! -s "$1/fx/outside.args" ]' _ "$w"

echo "controls:"
must_fail ":outside with a value neither :platform nor :class-path" "$w/c10.log" "$jp" plan "$ctl/outside-bad.edn" --base "$fx"
must_fail "a resource outside META-INF in two modules, differing" "$w/c9.log" \
  "$jp" read "$ctl/res-dup.edn" "$w/c9" --base "$fx" --root "controls=$ctl" --no-classes
expect "  (its message names the resource)" has "$w/c9.log" "the resource shared.txt in two modules (b's differs)"
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

echo "java-project test: $pass passed, $fail failed ($w)"
[ $fail = 0 ]
