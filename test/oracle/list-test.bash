#!/bin/bash
# Test of bin/oracle's --only FILE and ids (GT8.4's rotation): about 40 s, on stage 0 (ARBACE_HOST_EXE, default
# target/seed/amd64/arbace) as the implementation and the frozen jar (bin/lib/frozen.bash) as the runner's JVM.
# Not in bin/gate-tests (it runs the corpus files it lists). Prints "ok NAME" per case; exit status 1 when one fails.
set -u
here=$(cd "$(dirname "$0")/../.." && pwd)
root=$here; . "$here/bin/lib/tmpdir.bash"
host=${ARBACE_HOST_EXE:-$here/target/seed/amd64/arbace}
[ -x "$host" ] || { echo "oracle list-test: no executable $host" >&2; exit 2; }
tmp=$(mktemp -d "$here/.tmp/oracle-list-test.XXXXXX") || exit 1
trap 'rm -rf "$tmp"' EXIT
failed=0
t() { local name=$1; shift; if "$@"; then echo "ok $name"; else echo "FAIL $name"; failed=1; fi; }
oracle() { "$here/bin/oracle" check "$host -" "$@"; }

# ids: every recorded case once (the expected files' :count summed), FILE:LINE or FILE:INDEX
test_ids() {
  "$here/bin/oracle" ids > "$tmp/ids" || return 1
  want=$(grep -rhoE '^\{:part :[a-z]+ :source "[^"]*" :count [0-9]+' "$here/test/oracle/expected" | awk '{s+=$NF} END{print s}')
  [ "$(wc -l < "$tmp/ids")" = "$want" ] && [ "$(sort -u "$tmp/ids" | wc -l)" = "$want" ] \
    && grep -qx 'test/oracle/forms/collections.ae:6' "$tmp/ids" && grep -qx 'test/oracle/regex/anchors.ae:1' "$tmp/ids" \
    && [ "$("$here/bin/oracle" ids regex | grep -vc '^test/oracle/regex/')" = 0 ]
}
t ids test_ids

# a list selects its cases alone (blank and # lines ignored); an id no case has is ignored; the unlisted are not failures
printf '# a list\n\ntest/oracle/forms/reader.ae:9\ntest/oracle/regex/anchors.ae:2\nno/such.ae:1\n' > "$tmp/l1"
test_select() {
  oracle --only "$tmp/l1" > "$tmp/o1" 2>&1 || return 1
  grep -q '^== --only: 2 listed cases in 2 corpus files; 1 listed ids are no recorded case' "$tmp/o1" \
    && grep -q 'reader.ae  *1/ *1 ok' "$tmp/o1" && grep -q 'anchors.ae  *1/ *1 ok' "$tmp/o1" \
    && grep -q '^== 2 of 2 cases match' "$tmp/o1" && ! grep -v '^   control' "$tmp/o1" | grep -q 'collections'
}
t list-selects-and-ignores-the-unlisted test_select

# the control (subject collections.ae:6, reference :7): left when not both listed, judged when listed
test_control_left() { grep -q '^   control control-1: left' "$tmp/o1"; }
t control-left-when-not-listed test_control_left
printf 'test/oracle/forms/collections.ae:6\ntest/oracle/forms/collections.ae:7\n' > "$tmp/l2"
test_control_judged() {
  oracle --only "$tmp/l2" > "$tmp/o2" 2>&1 || return 1
  grep -q 'differs, as a control must' "$tmp/o2" && grep -q '^== 2 of 2 cases match' "$tmp/o2"
}
t control-judged-when-listed test_control_judged

# a known mismatch listed is compared with the reference alone: as recorded; the unlisted known ones are not "now passing"
printf 'test/oracle/forms/security.ae:172\n' > "$tmp/l3"
test_known() {
  oracle --only "$tmp/l3" --expected "$here/test/oracle/known-go-amd64-frozen.edn" > "$tmp/o3" 2>&1 || return 1
  grep -q ': 1 recorded, 0 new, 0 now passing, 0 changed, as recorded' "$tmp/o3"
}
t known-mismatches-of-the-list-only test_known

# a reference without it: the failure line the gate reads, and the exit status
echo '{}' > "$tmp/empty.edn"
test_fail_line() {
  oracle --only "$tmp/l3" --expected "$tmp/empty.edn" > "$tmp/o4" 2>&1 && return 1
  [ "$(sed -nE 's/^   new mismatch: ([^ ]+)$/\1/p' "$tmp/o4")" = test/oracle/forms/security.ae:172 ]
}
t failure-line test_fail_line

# refusals: a list of nothing, ids that are no case, --write-expected
test_refusals() {
  : > "$tmp/l5"; oracle --only "$tmp/l5" > "$tmp/o5" 2>&1 && return 1
  echo 'no/such.ae:1' > "$tmp/l6"; oracle --only "$tmp/l6" > "$tmp/o6" 2>&1 && return 1
  grep -q 'none of the 1 listed ids is a recorded case' "$tmp/o6" || return 1
  oracle --only "$tmp/l3" --write-expected "$tmp/w.edn" > "$tmp/o7" 2>&1 && return 1
  [ ! -e "$tmp/w.edn" ]
}
t refusals test_refusals
exit $failed
