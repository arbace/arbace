#!/bin/bash
# The wiring of repr/go's negative controls (doc/GATE-SURVEY.md, "As built: GTS88 (b)"), on a few files (light):
#  1. native-check: the real controls of two programs hold (exit 0); a fabricated control crossing a program with itself
#     passes at every level, so the check exits non-zero and prints CONTROL FAILED.
#     The real control also holds at the level :known (native-check's known differences: the subject's crossed
#     difference not excused by the reference's own entries).
#  2. roundtrip's check (bin/lib/repr-go/check --controls): a fabricated control (a package against itself) fails the
#     run; the real one holds. Needs a candidate: the identity copy of two packages (check --make identity is the whole
#     tree; here a copy of the two packages).
#  3. the same for the directory tests (check --dirtests, linux/amd64): closure5.dir and ddd2.dir copied as the
#     candidate, the real dirtest controls hold, a directory test against itself fails the run.
# Usage: test/repr/go/controls-test.bash        needs: ARBACE_HOST_EXE (stage 0), TamaGo's go
set -u
here=$(cd "$(dirname "$0")/../../.." && pwd)
: "${ARBACE_HOST_EXE:?set ARBACE_HOST_EXE to stage 0 (a tool must not build one here)}"
cd "$here" || exit 2
. bin/lib/tmpdir.bash || true
t=$here/.tmp/repr-go-controls-test; rm -rf "$t"; mkdir -p "$t"
fail=0
check() { # NAME EXPECTED-STATUS ACTUAL
  if [ "$2" = "$3" ]; then echo "ok   $1 (exit $3)"; else echo "FAIL $1: exit $3, wanted $2"; fail=1; fi
}

# 1. native-check
printf 'test/helloworld.go\ntest/printbig.go\n' > "$t/ids"
cat > "$t/real.edn" <<'EOE'
[{:id "t/real" :subject "test/helloworld.go" :reference "test/printbig.go" :levels [:tree :ann :forms :known]}]
EOE
cat > "$t/fake.edn" <<'EOE'
[{:id "t/fake" :subject "test/helloworld.go" :reference "test/helloworld.go"}]
EOE
REPR_GO_NATIVE_CONTROLS=$t/real.edn bin/repr-go native-check --only "$t/ids" test > "$t/native-real.log" 2>&1; s=$?
check "native-check: a real control holds" 0 $s
grep -q '^CONTROL ok t/real held \[:tree :ann :forms :known\]' "$t/native-real.log" || { echo "FAIL native-check: no CONTROL ok row held at the four levels"; fail=1; }
REPR_GO_NATIVE_CONTROLS=$t/fake.edn bin/repr-go native-check --only "$t/ids" test > "$t/native-fake.log" 2>&1; s=$?
check "native-check: a control that passes fails the check" 1 $s
grep -q '^CONTROL FAILED t/fake' "$t/native-fake.log" || { echo "FAIL native-check: no CONTROL FAILED row"; fail=1; }

# 2. the round trip's check
goroot=${ARBACE_GOROOT:-/root/tamago-go}
mkdir -p "$t/cand/src/container/list" "$t/cand/src/container/ring"
cp "$goroot"/src/container/list/*.go "$t/cand/src/container/list/"; cp "$goroot"/src/container/ring/*.go "$t/cand/src/container/ring/"
rm -f "$t"/cand/src/*/*_test.go
printf 'package\tcontainer/list\tcontainer/ring\n' > "$t/rt-real.txt"
printf 'package\tcontainer/list\tcontainer/list\n' > "$t/rt-fake.txt"
bin/repr-go check --no-tests --only '^container/(list|ring)$' --controls "$t/rt-real.txt" "$t/cand" > "$t/rt-real.log" 2>&1; s=$?
check "check: a real control holds" 0 $s
grep -q '^CONTROL ok package container/list vs container/ring' "$t/rt-real.log" || { echo "FAIL check: no CONTROL ok row"; fail=1; }
bin/repr-go check --no-tests --only '^container/(list|ring)$' --controls "$t/rt-fake.txt" "$t/cand" > "$t/rt-fake.log" 2>&1; s=$?
check "check: a control that passes fails the check" 1 $s
grep -q '^CONTROL FAILED package container/list vs container/list' "$t/rt-fake.log" || { echo "FAIL check: no CONTROL FAILED row"; fail=1; }

# 3. the directory tests' round trip check
mkdir -p "$t/dcand/test"; cp -r "$goroot/test/closure5.dir" "$goroot/test/ddd2.dir" "$t/dcand/test/"
printf 'dirtest closure5.go ddd2.go\ndirtest ddd2.go closure5.go\n' > "$t/dt-real.txt"
printf 'dirtest closure5.go closure5.go\n' > "$t/dt-fake.txt"
bin/repr-go check --goos linux --dirtests -j 2 --only '^(closure5|ddd2)\.go$' --controls "$t/dt-real.txt" "$t/dcand" > "$t/dt-real.log" 2>&1; s=$?
check "check --dirtests: the real controls hold" 0 $s
[ "$(grep -c '^CONTROL ok dirtest ' "$t/dt-real.log")" = 2 ] || { echo "FAIL check --dirtests: not two CONTROL ok rows"; fail=1; }
bin/repr-go check --goos linux --dirtests -j 2 --only '^(closure5|ddd2)\.go$' --controls "$t/dt-fake.txt" "$t/dcand" > "$t/dt-fake.log" 2>&1; s=$?
check "check --dirtests: a control that passes fails the check" 1 $s
grep -q '^CONTROL FAILED dirtest closure5.go vs closure5.go' "$t/dt-fake.log" || { echo "FAIL check --dirtests: no CONTROL FAILED row"; fail=1; }
exit $fail
