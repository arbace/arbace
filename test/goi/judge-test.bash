#!/bin/bash
# Tests of bin/goi check's judging on fabricated inputs (report 060): the functions are cut out of bin/goi
# (GOI=PATH names another copy) and run on made-up files and rows; no build, no run of Arbace or gc.
#   test/goi/judge-test.bash        prints "ok NAME" per test, exits 1 when one fails
set -u
here=$(cd "$(dirname "$0")/../.." && pwd)
goi=${GOI:-$here/bin/goi}
tmp=${TMPDIR:-$here/.tmp/tmp}/goi-judge-test.$$
mkdir -p "$tmp"; trap 'rm -rf "$tmp"' EXIT
failed=0

# fn NAME: the function NAME of bin/goi (to its closing brace at the line start, or on its line)
fn() {
  awk -v n="$1" '
    $0 ~ "^" n "\\(\\) \\{" { p = 1; print; if ($0 ~ /\}[ ]*$/ && $0 !~ /\{[ ]*$/) exit; next }
    p { print; if ($0 == "}") exit }' "$goi"
}
load() { local f; for f in "$@"; do eval "$(fn "$f")" || return 1; done; }
t() { # t NAME COMMAND...: the command's status is the test's
  local name=$1; shift
  if "$@"; then echo "ok $name"; else echo "FAIL $name"; failed=1; fi
}

# finding 1: a program whose name has a hyphen has its forms under the munged name
test_hyphen_listed() {
  load forms_rel action_of run_program_list || return 1
  declare -F run_program_list > /dev/null || return 1
  local g=$tmp/h/goroot/test c=$tmp/h/c d=$tmp/h/d
  mkdir -p "$g/typeparam" "$c/go/test/typeparam" "$d/go/test"
  printf '// run -gcflags="-l"\n\npackage main\n' > "$g/typeparam/dictionaryCapture-noinline.go"
  printf '// run\n\npackage main\n' > "$g/plain.go"
  printf '// run\n\npackage main\n' > "$g/nofms.go"
  printf '// rundir\n\npackage ignored\n' > "$g/typeparam/dir-test.go"
  : > "$c/go/test/typeparam/dictionaryCapture_noinline.ae"
  : > "$c/go/test/plain.ae"
  mkdir -p "$d/go/test/typeparam/dir_test_dir"
  [ "$(run_program_list "$g" "$c" "$d")" = "$(printf 'plain.go\ntypeparam/dictionaryCapture-noinline.go\ntypeparam/dir-test.go')" ]
}
t hyphen-program-listed test_hyphen_listed

test_forms_rel() {
  load forms_rel || return 1
  [ "$(forms_rel a/b-c.d.go)" = a/b_c_d ] && [ "$(forms_rel x/y.go)" = x/y ]
}
t forms-rel test_forms_rel

# finding 2: a known program is known only for the kind of result its reason stands for
test_known_kinds() {
  load apply_known || return 1
  local T=$'\t' k=$tmp/k.tsv r=$tmp/r.tsv
  { printf 'a.go\tpositions: a call split over lines\n'
    printf 'b.go\truns the go command: the log differs\n'
    printf 'c.go\tgo build'"'"'s -complete: no body\n'
    printf 'd.go\terrorcheck: the forms say another program\n'
    printf 'e.go\tspeed: slow\n'
    printf 'f.go\tgoi'"'"'s own thing\n'
    printf 'g.go\tpositions: a call split over lines\n'; } > "$k"
  { printf 'a.go\tfail\tstatus 2 (gc 2), stderr differs: x\n'
    printf 'b.go\terror\ttimeout\n'
    printf 'c.go\terror\tthe AOT build failed: no body\n'
    printf 'd.go\terror\tprinting the forms failed: NullPointerException\n'
    printf 'e.go\terror\ttimeout 10 s\n'
    printf 'f.go\tskip\tx\n'
    printf 'g.go\terror\tthe AOT build failed: printer crashed\n'; } > "$r"
  local want got
  want=$(printf 'a.go\tknown\tpositions: a call split over lines\nb.go\terror\ttimeout\nc.go\tknown\tgo build'"'"'s -complete: no body\nd.go\terror\tprinting the forms failed: NullPointerException\ne.go\tknown\tspeed: slow\nf.go\tknown\tgoi'"'"'s own thing\ng.go\terror\tthe AOT build failed: printer crashed')
  got=$(apply_known "$k" "$r")
  [ "$got" = "$want" ] || { printf '%s\n' "$got" > "$tmp/got"; printf '%s\n' "$want" > "$tmp/want"; diff "$tmp/got" "$tmp/want" >&2; return 1; }
}
t known-only-its-kind test_known_kinds

exit $failed
