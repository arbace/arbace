#!/bin/bash
# Tests of bin/repr-java check --jdk's verdicts (test/repr/java/jdk.bash) over a fabricated checkout and a
# stubbed bin/repr-java (GT7's audit 061, findings 1 and 2). No javac tool, no jdk26u. Run: bash test/repr/java/judge-jdk-test.bash
set -uo pipefail
root=$(cd "$(dirname "$0")/../../.." && pwd)
. "$root/test/repr/java/judge-lib.bash"
t=${TMPDIR:-$root/.tmp/tmp}/judge-jdk-test.$$
rm -rf "$t"; mkdir -p "$t"
make_fake_root "$t/r"
cd "$t/r"
src=$t/r/jdk/src

# finding 1: the fallbacks are judged against a recorded ceiling
env JDK_SRC="$src" STUB_FALLBACKS=0 bash test/repr/java/jdk.bash --update > "$t/out.log" 2>&1; expect_rc "update with no fallbacks" 0 $?
env JDK_SRC="$src" STUB_FALLBACKS=0 bash test/repr/java/jdk.bash > "$t/out.log" 2>&1; expect_rc "check, no fallbacks, ceiling 0" 0 $?
env JDK_SRC="$src" STUB_FALLBACKS=5 bash test/repr/java/jdk.bash > "$t/out.log" 2>&1; rc=$?
expect_rc "check, 5 fallbacks over ceiling 0, fails" 1 $rc
grep -q 'ceiling' "$t/out.log" && grep -q 'm1' "$t/out.log" && ok "the verdict names the module and the fallbacks" || bad "the verdict names the module and the fallbacks"
env JDK_SRC="$src" STUB_FALLBACKS=5 bash test/repr/java/jdk.bash --update > "$t/out.log" 2>&1; expect_rc "update records the fallbacks" 0 $?
env JDK_SRC="$src" STUB_FALLBACKS=5 bash test/repr/java/jdk.bash > "$t/out.log" 2>&1; expect_rc "check, 5 fallbacks, ceiling 5" 0 $?
env JDK_SRC="$src" STUB_FALLBACKS=6 bash test/repr/java/jdk.bash > "$t/out.log" 2>&1; expect_rc "check, 6 fallbacks, ceiling 5, fails" 1 $?
env JDK_SRC="$src" STUB_FALLBACKS=1 REPR_JAVA_VENDOR_READ=javac bash test/repr/java/jdk.bash > "$t/out.log" 2>&1; expect_rc "javac's read has no fallbacks to judge" 0 $?

# finding 2: a JDK_SRC that is not canonical (a symlink, a trailing slash, a '..')
env JDK_SRC="$src" STUB_FALLBACKS=0 bash test/repr/java/jdk.bash --update > "$t/out.log" 2>&1
rm -f test/repr/java/expected/jdk26u-fallbacks.edn
ln -s "$t/r/jdk" "$t/link"
for s in "$t/link/src" "$src/" "$src/m1/.." "$t/r/jdk/../jdk/src"; do
  env JDK_SRC="$s" STUB_FALLBACKS=0 bash test/repr/java/jdk.bash > "$t/out.log" 2>&1; expect_rc "check with JDK_SRC=${s#"$t"/}" 0 $?
done
cp test/repr/java/expected/jdk26u.sha256 "$t/good.sha256"
env JDK_SRC="$t/link/src" STUB_FALLBACKS=0 bash test/repr/java/jdk.bash --update > "$t/out.log" 2>&1; expect_rc "update through a symlink" 0 $?
cmp -s "$t/good.sha256" test/repr/java/expected/jdk26u.sha256 && ok "the record is the same as from the canonical path" || bad "the record is the same as from the canonical path"
grep -q "$t" test/repr/java/expected/jdk26u.sha256 && bad "no host path in the record" || ok "no host path in the record"
finish
