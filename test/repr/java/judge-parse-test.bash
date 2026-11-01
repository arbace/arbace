#!/bin/bash
# Tests of bin/repr-java parse-check's verdicts (bin/lib/repr-java/parse-check) over a fabricated checkout and a
# stubbed bin/repr-java (GT7's audit 061, findings 3 and 4). No javac tool, no jdk26u. Run: bash test/repr/java/judge-parse-test.bash
set -uo pipefail
root=$(cd "$(dirname "$0")/../../.." && pwd)
. "$root/test/repr/java/judge-lib.bash"
t=${TMPDIR:-$root/.tmp/tmp}/judge-parse-test.$$
rm -rf "$t"; mkdir -p "$t"
make_fake_parse_root "$t/r"
cd "$t/r"
pc() { env JDK_SRC="$t/r/jdk/src" REPR_JAVA_PARSE_WORK="$t/work" "$@" bash bin/lib/repr-java/parse-check --corpus "${CORPUS:-samples}" ${PCARGS:-} > "$t/out.log" 2>&1; }

# finding 3: javac's syntax errors are judged
STUB_DIAG=0 pc; expect_rc "samples, 0 diagnostics, recorded 0" 0 $?
STUB_DIAG=1 pc; rc=$?; expect_rc "samples, 1 diagnostic, recorded 0, fails" 1 $rc
grep -q 'recorded 0' "$t/out.log" && ok "the verdict names the recorded count" || bad "the verdict names the recorded count"
CORPUS=jdk26u STUB_DIAG=3 pc; expect_rc "jdk26u, 3 diagnostics, recorded 0, fails" 1 $?
CORPUS=mutants STUB_DIAG=500 pc; expect_rc "mutants, count not recorded: reported, not judged" 0 $?
grep -q 'no recorded count' "$t/out.log" && ok "the unrecorded count is said" || bad "the unrecorded count is said"
CORPUS=mutants STUB_DIAG=500 PCARGS=--record pc; expect_rc "mutants, --record" 0 $?
CORPUS=mutants STUB_DIAG=500 pc; expect_rc "mutants, 500 recorded 500" 0 $?
CORPUS=mutants STUB_DIAG=499 pc; expect_rc "mutants, 499 recorded 500, fails" 1 $?
grep -q '^ :samples 0' test/repr/java/parse-check-diagnostics.edn && grep -q '^;;' test/repr/java/parse-check-diagnostics.edn && ok "--record keeps the others and the comments" || bad "--record keeps the others and the comments"
finish
