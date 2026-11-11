#!/bin/bash
# The LLVM row's negative controls are wired (GTS81 (a); doc/llvm/LLVM-CROP-SURVEY.md, "As built: LL9a"): each check family's
# verb, given a control that crosses a module with itself (which its judge must pass), fails and says so; with the committed
# controls it passes. Families: bin/repr-llvm roundtrip, roundtrip --text, verify-check, bin/repr-llvm check (the gate's verb),
# bin/llvmi check (eval), and the own verdicts' unit tests (test/repr/llvm/expect-test.bash, LLS38, LLS39). Light: four modules of test/llvm/essential*.txt and llvmi's essential subset (about 40 s).
# needs: stage 0 (ARBACE_HOST_EXE), llvm/test's tarball extracted for nothing (the essential copies are the checkout's)
set -uo pipefail
src=$(cd "$(dirname "$0")/../../.." && pwd)
export TMPDIR=${TMPDIR:-$src/.tmp/tmp}; mkdir -p "$TMPDIR"
w=$(mktemp -d "$TMPDIR/llvm-controls-test.XXXXXX")
trap 'rm -rf "$w"' EXIT
fail=0
ok() { echo "ok   $1"; }
bad() { echo "FAIL $1"; fail=1; }
# expect NAME STATUS-WANTED LOG REGEXP: the verb's status (0 or nonzero) and a line of its output
expect() { local n=$1 want=$2 st=$3 log=$4 re=$5
  if { [ "$want" = 0 ] && [ "$st" = 0 ]; } || { [ "$want" != 0 ] && [ "$st" != 0 ]; }; then
    if grep -qE -- "$re" "$log"; then ok "$n"; else bad "$n (no line /$re/)"; sed 's/^/     /' "$log" | tail -8; fi
  else bad "$n (status $st)"; sed 's/^/     /' "$log" | tail -8; fi; }
e=$src/test/llvm/essential
printf 'CodeGen-Mips-load-max-alignment\t%s/CodeGen-Mips-load-max-alignment.ll\nCodeGen-PowerPC-rotl\t%s/CodeGen-PowerPC-rotl.ll\n' "$e" "$e" > "$w/rt.list"
printf 'Analysis-BasicAA-128-bit-ptr\t%s/Analysis-BasicAA-128-bit-ptr.ll\nAssembler-2005-05-05-OpaqueUndefValues\t%s/Assembler-2005-05-05-OpaqueUndefValues.ll\nAssembler-invalid-dilocation-field-bad\t%s/Assembler-invalid-dilocation-field-bad.ll\n' "$e" "$e" "$e" > "$w/v.list"
echo "_control/self CodeGen-PowerPC-rotl CodeGen-PowerPC-rotl" > "$w/self-rt.txt"
echo "_control/self Assembler-2005-05-05-OpaqueUndefValues Assembler-2005-05-05-OpaqueUndefValues" > "$w/self-v.txt"

"$src/bin/repr-llvm" roundtrip "$w/rt.list" "$w/rt1" > "$w/rt1.log" 2>&1; expect "roundtrip: the committed controls fail, the check passes" 0 $? "$w/rt1.log" '^control _control/mips-vs-powerpc fails as it must'
REPR_LLVM_CONTROLS=$w/self-rt.txt "$src/bin/repr-llvm" roundtrip "$w/rt.list" "$w/rt2" > "$w/rt2.log" 2>&1; expect "roundtrip: a passing control fails the check" 1 $? "$w/rt2.log" '^control passed: _control/self'
"$src/bin/repr-llvm" roundtrip --text "$w/rt.list" "$w/tx1" > "$w/tx1.log" 2>&1; expect "roundtrip --text: the committed controls fail, the check passes" 0 $? "$w/tx1.log" '^control _control/mips-vs-powerpc fails as it must'
REPR_LLVM_TEXT_CONTROLS=$w/self-rt.txt "$src/bin/repr-llvm" roundtrip --text "$w/rt.list" "$w/tx2" > "$w/tx2.log" 2>&1; expect "roundtrip --text: a passing control fails the check" 1 $? "$w/tx2.log" '^control passed: _control/self'
"$src/bin/repr-llvm" verify-check "$w/v.list" "$w/v1" > "$w/v1.log" 2>&1; expect "verify-check: the committed controls fail, the check passes" 0 $? "$w/v1.log" '^control _control/fragment fails as it must'
REPR_LLVM_VERIFY_CONTROLS=$w/self-v.txt "$src/bin/repr-llvm" verify-check "$w/v.list" "$w/v2" > "$w/v2.log" 2>&1; expect "verify-check: a passing control fails the check" 1 $? "$w/v2.log" '^control passed: _control/self'
REPR_LLVM_VERIFY_CONTROLS=$w/self-v.txt "$src/bin/repr-llvm" check verify-llvmtest --essential "$w/c2" > "$w/c2.log" 2>&1; expect "repr-llvm check: a passing control fails the gate's verb" 1 $? "$w/c2.log" '^FAIL a negative control passes'
# LLS50: a split-file section judged by its parent's RUN lines (a fake tree: LLVM_SRC's parent test, the section under .../split/)
mkdir -p "$w/fk/src/llvm-project-23.1.5.src/llvm/test/D" "$w/fk/split/D/t.ll"
printf '; RUN: split-file %%s %%t\n; RUN: opt -passes=verify %%t/a.ll -disable-output\n; RUN: not opt %%t/b.ll -disable-output\n; RUN: opt -o %%t/c.ll %%t/d.ll -S\n' > "$w/fk/src/llvm-project-23.1.5.src/llvm/test/D/t.ll"
for x in a b c d; do cp "$e/Analysis-BasicAA-128-bit-ptr.ll" "$w/fk/split/D/t.ll/$x.ll"; done
printf 'D-t.ll-a\t%s\nD-t.ll-b\t%s\nD-t.ll-c\t%s\nD-t.ll-d\t%s\nAssembler-invalid-dilocation-field-bad\t%s/Assembler-invalid-dilocation-field-bad.ll\n' "$w/fk/split/D/t.ll/a.ll" "$w/fk/split/D/t.ll/b.ll" "$w/fk/split/D/t.ll/c.ll" "$w/fk/split/D/t.ll/d.ll" "$e" > "$w/sec.list"
echo "_control/section-vs-reject Assembler-invalid-dilocation-field-bad D-t.ll-a" > "$w/sec-ctl.txt"
grep -v 'D-t.ll-[bc]' "$w/sec.list" > "$w/sec2.list"
LLVM_SRC=$w/fk/src REPR_LLVM_VERIFY_CONTROLS=$w/sec-ctl.txt "$src/bin/repr-llvm" verify-check "$w/sec2.list" "$w/s1" > "$w/s1.log" 2>&1; expect "verify-check: a section's own verdict from its parent's RUN lines (a, d), the control fails as it must" 0 $? "$w/s1.log" '^control _control/section-vs-reject fails as it must'
# b (under not) and c (an output only) have no verdict of their own: opt's, and no record here, so the verb fails, with three own verdicts (a, d, the rejection)
LLVM_SRC=$w/fk/src "$src/bin/repr-llvm" verify-check "$w/sec.list" "$w/s2" > "$w/s2.log" 2>&1; expect "verify-check: a section under not, or an output only, has no own verdict" 1 $? "$w/s2.log" '\(3 by their own verdict, 2 opt'
printf '[{:subject "tx-O0.20000121-1" :reference "tx-O0.20000121-1"}]\n' > "$w/self-llvmi.edn"
LLVMI_WORK=$w/li "$src/bin/llvmi" check --essential > "$w/li1.log" 2>&1; expect "llvmi check: the committed controls fail, the check passes" 0 $? "$w/li1.log" 'negative controls: 4 judged, 0 pass'
LLVMI_WORK=$w/li LLVMI_CONTROLS=$w/self-llvmi.edn "$src/bin/llvmi" check --essential --judge > "$w/li2.log" 2>&1; expect "llvmi check: a passing control fails the check" 1 $? "$w/li2.log" '^FAIL the negative control passes: tx-O0.20000121-1'
# the own verdicts' rules (LLS38, LLS39): their unit tests, each crossed control failing
"$src/test/repr/llvm/expect-test.bash" > "$w/et.log" 2>&1; expect "expect-test: the rules' cases pass, their controls fail" 0 $? "$w/et.log" ' controls, 0 failing$'
[ "$fail" = 0 ] && echo "controls-test: all ok"
exit $fail
