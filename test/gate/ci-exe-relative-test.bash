#!/bin/bash
# queue 118, finding 1: bin/lib/ci/check with a relative CI_EXE (doc/C23-SURVEY.md C9.2 step 5 names
# CI_EXE=target/arbace-go/amd64/arbace): run1 runs the runner after `cd "$rd"`, so a relative path must have been
# made absolute first. The script runs on a fabricated checkout (arbace_tool, bin/ci and go stubbed), the stand-in
# executable at target/x/arbace, CI_EXE given relative to the checkout's top as C9.2 does; passes when run.tsv records
# status 0 for the program (127 when the path is not found after the cd). Then a CI_EXE that is not an executable
# is refused at once (status 2), and an --only list naming a program outside the run corpora too. No executable, seconds. (The VM's test of queue 118, its appendix.)
# By hand: test/gate/ci-exe-relative-test.bash [CHECKOUT]   (another checkout's bin/lib/ci/check to test)
set -uo pipefail
src=$(cd "${1:-$(dirname "$0")/../..}" && pwd)
export TMPDIR=${TMPDIR:-$src/.tmp/tmp}; mkdir -p "$TMPDIR"
t=$(mktemp -d "$TMPDIR/ci-exe-relative-test.XXXXXX"); trap 'rm -rf "$t"' EXIT
mkdir -p "$t/bin/lib/ci" "$t/test/c" "$t/target/x" "$t/go/bin"
cp "$src/bin/lib/ci/check" "$src/bin/lib/ci/budget.bash" "$t/bin/lib/ci/"; cp "$src/bin/lib/tmpdir.bash" "$t/bin/lib/"
cat > "$t/bin/lib/tools.bash" <<'S'
arbace_tool() {
  case "$*" in
    *"ppcheck lists"*) mkdir -p "$4/$6"; printf 'p1.c\tc11\t\tp1\n' > "$4/$6/gcc.list"; echo base > "$4/$6/base.txt" ;;
    *"nodes-list"*) local id; id=$(cut -f1 "$6" | head -1); printf 'node text\n' > "$5/$id.nodes"; printf '%s\tok\n' "$id" > "$5/nodes.tsv" ;;
    *" judge "*) cat "$5"/*/run.tsv ;;
  esac
}
S
printf '#!/bin/bash\nexit 0\n' > "$t/bin/ci"; chmod +x "$t/bin/ci"
printf '#!/bin/bash\necho %s/modcache\n' "$t" > "$t/go/bin/go"; chmod +x "$t/go/bin/go"
printf '#!/bin/bash\n[ "$1 $2 $3" = "-m arbace.c.repl run-nodes" ] && [ -f "$4" ] || exit 70\necho ran\n' > "$t/target/x/arbace"; chmod +x "$t/target/x/arbace"
: > "$t/test/c/ref-gcc15.edn"; : > "$t/test/c/check-linux-amd64.edn"
fails=0
out=$(cd "$t" && CI_EXE=target/x/arbace ARBACE_GOROOT=$t/go GOWHIM=$t CI_CHECK_WORK=$t/work bash bin/lib/ci/check --corpus tcc-tests2 2>&1)
st=$(awk -F'\t' '$1 == "tcc-tests2/p1.c" {print $2}' "$t/work/tcc-tests2/run.tsv" 2>&1)
if [ "$st" = 0 ]; then echo "ok:   the program ran by the relative CI_EXE (status 0)"
else echo "FAIL: status '$st' for tcc-tests2/p1.c (127: the relative path not found after the cd)"; printf '%s\n' "$out" | tail -5; fails=$((fails + 1)); fi
out=$(cd "$t" && CI_EXE=target/x/missing ARBACE_GOROOT=$t/go GOWHIM=$t CI_CHECK_WORK=$t/work2 bash bin/lib/ci/check --corpus tcc-tests2 2>&1); rc=$?
if [ "$rc" = 2 ] && [ ! -e "$t/work2/tcc-tests2/run.tsv" ]; then echo "ok:   a CI_EXE that is not an executable refused (status 2)"
else echo "FAIL: a missing CI_EXE gave status $rc"; printf '%s\n' "$out" | tail -3; fails=$((fails + 1)); fi
# queue 150: an --only entry outside the run corpora is refused (status 2) before anything runs, not skipped
printf '# a list\ntcc-tests2/p1.c\nci-run/x.c\n' > "$t/only.txt"
out=$(cd "$t" && ARBACE_GOROOT=$t/go GOWHIM=$t CI_CHECK_WORK=$t/work3 bash bin/lib/ci/check --corpus tcc-tests2 --only "$t/only.txt" 2>&1); rc=$?
if [ "$rc" = 2 ] && printf '%s' "$out" | grep -q 'ci-run/x.c' && [ ! -e "$t/work3/tcc-tests2" ]; then echo "ok:   an --only entry outside the run corpora refused (status 2)"
else echo "FAIL: an --only entry outside the run corpora gave status $rc"; printf '%s\n' "$out" | tail -3; fails=$((fails + 1)); fi
[ "$fails" = 0 ] && { echo "PASS: ci-exe-relative-test"; exit 0; }
echo "FAIL: ci-exe-relative-test ($fails)"; exit 1
