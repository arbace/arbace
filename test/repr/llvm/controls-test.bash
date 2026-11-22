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
# c (an output only) has no verdict of its own: opt's, and no record here, so the verb fails; b (under a plain `not opt`) is rejected by its
# own verdict since D1120-2 (a), its content a valid module: four own verdicts (a, b, d, the rejection)
LLVM_SRC=$w/fk/src "$src/bin/repr-llvm" verify-check "$w/sec.list" "$w/s2" > "$w/s2.log" 2>&1; expect "verify-check: a section under a plain not is rejected by its own verdict, an output only has none" 1 $? "$w/s2.log" '\(4 by their own verdict, 1 opt'
expect "verify-check: the plain not's own invalid against a valid module's verify differs" 1 1 "$w/s2/verify.tsv" '^D-t.ll-b	invalid	valid	differs	'
# D1120-2 (a): `invalid` sections with their fragment, from the parent (i: --check-prefix) or from the section itself (j: FileCheck %t/j.ll,
# after a read with --disable-verify, which claims nothing); `not --crash` (k) and a `not` of a pass (l) give none
printf '; RUN: split-file %%s %%t\n; RUN: not llvm-as < %%t/i.ll 2>&1 | FileCheck %%s --check-prefix=BAD-I\n; RUN: llvm-as %%t/j.ll --disable-verify -o /dev/null\n; RUN: not llvm-as %%t/j.ll -o /dev/null 2>&1 | FileCheck %%t/j.ll\n; RUN: not --crash opt %%t/k.ll -S\n; RUN: not opt -passes=instcombine %%t/l.ll -S\n; BAD-I: <stdin>:4:18: error: invalid field \x27bad\x27\n' > "$w/fk/src/llvm-project-23.1.5.src/llvm/test/D/v.ll"
mkdir -p "$w/fk/split/D/v.ll"
for x in i j k l; do cp "$e/Assembler-invalid-dilocation-field-bad.ll" "$w/fk/split/D/v.ll/$x.ll"; done
{ for x in i j k l; do printf 'D-v.ll-%s\t%s\n' "$x" "$w/fk/split/D/v.ll/$x.ll"; done; } > "$w/sec4.list"
LLVM_SRC=$w/fk/src "$src/bin/repr-llvm" verify-check "$w/sec4.list" "$w/s4" > "$w/s4.log" 2>&1; expect "verify-check: sections a plain not llvm-as rejects are invalid by their own verdict, not --crash and a pass are not" 1 $? "$w/s4.log" '\(2 by their own verdict, 2 opt'
expect "verify-check: the parent's prefix gives a section its fragment" 1 1 "$w/s4/verify.tsv" "^D-v.ll-i	invalid	invalid	same	same	invalid field 'bad'"
expect "verify-check: FileCheck's check file a section gives its fragment" 1 1 "$w/s4/verify.tsv" "^D-v.ll-j	invalid	invalid	same	same	invalid field 'bad'"
# LLS30 revisited (2026-11-20): a probe states its reviewed verdict (`; verdict:`): p-good's (addf: `add` on a float, invalid with
# LLParser's message) is met; the same probe flipped to valid (p-flip) differs, as a flipped verdict must
mkdir -p "$w/pr"
{ printf '; verdict: invalid invalid operand type for instruction\n; rule: llvm/lib/AsmParser/LLParser.cpp:1 (the control)\n'; cat "$src/test/llvm/probes/verify/addf.ll"; } > "$w/pr/p-good.ll"
{ printf '; verdict: valid\n; rule: - (the flipped control)\n'; cat "$src/test/llvm/probes/verify/addf.ll"; } > "$w/pr/p-flip.ll"
printf 'p-good\t%s\np-flip\t%s\n' "$w/pr/p-good.ll" "$w/pr/p-flip.ll" > "$w/pr.list"
"$src/bin/repr-llvm" verify-check "$w/pr.list" "$w/s5" > "$w/s5.log" 2>&1; expect "verify-check: a probe's stated verdict is its own (2 own, 0 opt), the flipped one fails the verb" 1 $? "$w/s5.log" '\(2 by their own verdict, 0 opt'
expect "verify-check: a probe's stated verdict met" 1 1 "$w/s5/verify.tsv" '^p-good	invalid	invalid	same	same	'
expect "verify-check: a probe's flipped verdict differs" 1 1 "$w/s5/verify.tsv" '^p-flip	valid	invalid	differs	'
# review 1111, finding 5: sections the tools may never verify have no own verdict: `||` (the failure allowed), --disable-verify=VALUE,
# llc started after the IR verifier, llvm-link dropping the unused (--only-needed), and any section of an XFAIL parent (D/x.ll)
printf '; RUN: split-file %%s %%t\n; RUN: opt %%t/e.ll -disable-output || true\n; RUN: opt --disable-verify=true %%t/f.ll -S\n; RUN: llc -start-after=codegenprepare %%t/g.ll -o -\n; RUN: llvm-link --only-needed %%t/h.ll -S\n' > "$w/fk/src/llvm-project-23.1.5.src/llvm/test/D/u.ll"
printf '; XFAIL: *\n; RUN: split-file %%s %%t\n; RUN: opt -passes=verify %%t/a.ll -disable-output\n' > "$w/fk/src/llvm-project-23.1.5.src/llvm/test/D/x.ll"
mkdir -p "$w/fk/split/D/u.ll" "$w/fk/split/D/x.ll"
for x in e f g h; do cp "$e/Analysis-BasicAA-128-bit-ptr.ll" "$w/fk/split/D/u.ll/$x.ll"; done
cp "$e/Analysis-BasicAA-128-bit-ptr.ll" "$w/fk/split/D/x.ll/a.ll"
{ for x in e f g h; do printf 'D-u.ll-%s\t%s\n' "$x" "$w/fk/split/D/u.ll/$x.ll"; done; printf 'D-x.ll-a\t%s\n' "$w/fk/split/D/x.ll/a.ll"
  printf 'Assembler-invalid-dilocation-field-bad\t%s/Assembler-invalid-dilocation-field-bad.ll\n' "$e"; } > "$w/sec3.list"
LLVM_SRC=$w/fk/src "$src/bin/repr-llvm" verify-check "$w/sec3.list" "$w/s3" > "$w/s3.log" 2>&1; expect "verify-check: ||, --disable-verify=, llc -start-after, llvm-link --only-needed and XFAIL give a section no own verdict" 1 $? "$w/s3.log" '\(1 by their own verdict, 5 opt'
printf '[{:subject "tx-O0.20000121-1" :reference "tx-O0.20000121-1"}]\n' > "$w/self-llvmi.edn"
LLVMI_WORK=$w/li "$src/bin/llvmi" check --essential > "$w/li1.log" 2>&1; expect "llvmi check: the committed controls fail, the check passes" 0 $? "$w/li1.log" 'negative controls: 4 judged, 0 pass'
LLVMI_WORK=$w/li LLVMI_CONTROLS=$w/self-llvmi.edn "$src/bin/llvmi" check --essential --judge > "$w/li2.log" 2>&1; expect "llvmi check: a passing control fails the check" 1 $? "$w/li2.log" '^FAIL the negative control passes: tx-O0.20000121-1'
# the own verdicts' rules (LLS38, LLS39): their unit tests, each crossed control failing
"$src/test/repr/llvm/expect-test.bash" > "$w/et.log" 2>&1; expect "expect-test: the rules' cases pass, their controls fail" 0 $? "$w/et.log" ' controls, 0 failing$'
[ "$fail" = 0 ] && echo "controls-test: all ok"
exit $fail
