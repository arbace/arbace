#!/bin/bash
# Test of bin/diffcheck's --only FILE (GT8.4's rotation), at the level of test/diffcheck/main.ae on an existing diff
# build and its corpora: DIFFCHECK_WORK (default: the main checkout's .tmp/diffcheck, left by a bin/diffcheck run;
# exit 0 "skipped" without arbace-diff and gen/corpora.edn), run by stage 0 (ARBACE_HOST_EXE, default
# target/seed/amd64/arbace). A few seconds. Not in bin/gate-tests. Prints "ok NAME"; exit status 1 when one fails.
set -u
here=$(cd "$(dirname "$0")/../.." && pwd)
root=$here; . "$here/bin/lib/tmpdir.bash"
. "$here/bin/lib/gate-cache.bash"
host=${ARBACE_HOST_EXE:-$here/target/seed/amd64/arbace}
work=${DIFFCHECK_WORK:-$(arbace_main_checkout "$here")/.tmp/diffcheck}
[ -x "$work/arbace-diff" ] && [ -f "$work/gen/corpora.edn" ] && [ -x "$host" ] \
  || { echo "diffcheck list-test: skipped (no $work/arbace-diff, gen/corpora.edn or $host)"; exit 0; }
tmp=$(mktemp -d "$here/.tmp/diffcheck-list-test.XXXXXX") || exit 1
trap 'rm -rf "$tmp"' EXIT
failed=0
t() { local name=$1; shift; if "$@"; then echo "ok $name"; else echo "FAIL $name"; failed=1; fi; }
run() { ARBACE_PATH=$here/test "$host" -m diffcheck.main run "$work/arbace-diff" "$work/gen" -j 4 "$@"; }
known=$here/test/diffcheck/known.edn

test_ids() {
  ARBACE_PATH=$here/test "$host" -m diffcheck.main ids "$work/gen" > "$tmp/ids" || return 1
  [ -s "$tmp/ids" ] && grep -qx 'test/oracle/forms/collections.ae:6' "$tmp/ids" && grep -qx 'test/oracle/forms/state_errors.ae:480' "$tmp/ids"
}
t ids test_ids

# a list selects its cases alone (blank and # lines ignored); an unknown id is ignored; the unlisted are no failures
printf '# a list\n\ntest/oracle/forms/reader.ae:9\nno/such.ae:1\n' > "$tmp/l1"
test_select() {
  run --expected "$known" --only "$tmp/l1" > "$tmp/o1" 2>&1 || return 1
  grep -q 'reader.ae  *clojure  *1/ *1 agree' "$tmp/o1" && grep -q '^== 1 of 1 cases agree' "$tmp/o1" \
    && grep -q '1 listed ids are no case (ignored)' "$tmp/o1" && grep -q ': 0 recorded, 0 new, 0 now passing, 0 changed, as recorded' "$tmp/o1"
}
t list-selects-and-the-unlisted-known-ones-are-not-passing test_select
test_control_left() { grep -q '^   control control-1: left' "$tmp/o1"; }
t control-left-when-not-listed test_control_left

printf 'test/oracle/forms/collections.ae:6\ntest/oracle/forms/collections.ae:7\n' > "$tmp/l2"
test_control_judged() {
  run --only "$tmp/l2" > "$tmp/o2" 2>&1 || return 1
  grep -q 'differs, as a control must' "$tmp/o2" && grep -q '^== 2 of 2 cases agree' "$tmp/o2"
}
t control-judged-when-listed test_control_judged

# a known disagreement listed: as recorded; against an empty reference: the failure line the gate reads
printf 'test/oracle/forms/state_errors.ae:480\n' > "$tmp/l3"
test_known() {
  run --expected "$known" --only "$tmp/l3" > "$tmp/o3" 2>&1 || return 1
  grep -q ': 1 recorded, 0 new, 0 now passing, 0 changed, as recorded' "$tmp/o3" || return 1
  echo '{}' > "$tmp/empty.edn"
  run --expected "$tmp/empty.edn" --only "$tmp/l3" > "$tmp/o4" 2>&1 && return 1
  [ "$(sed -nE 's/^   new mismatch: ([^ ]+)$/\1/p' "$tmp/o4")" = test/oracle/forms/state_errors.ae:480 ]
}
t known-and-failure-line test_known

test_refusals() {
  : > "$tmp/l5"; run --only "$tmp/l5" > "$tmp/o5" 2>&1 && return 1
  echo 'no/such.ae:1' > "$tmp/l6"; run --only "$tmp/l6" > "$tmp/o6" 2>&1 && return 1
  grep -q 'none of the 1 listed ids is a case' "$tmp/o6" || return 1
  run --only "$tmp/l3" --write-expected "$tmp/w.edn" > "$tmp/o7" 2>&1 && return 1
  [ ! -e "$tmp/w.edn" ]
}
t refusals test_refusals
exit $failed
