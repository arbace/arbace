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

# finding 7: an errorcheck program with no full forms is unsupported only as an invalid program the converter rejects
test_no_forms() {
  load no_forms_row || return 1
  local T=$'\t' tsv=$tmp/efull.tsv
  printf 'errors\tbad.go\t1\t1\t1\t1\tbad.go:3: oops\nfail\tbad2.go\t1\t1\t1\t1\tbad2.go:4: oops\nok\tvalid.go\t1\t1\t1\t1\t\nerrors\tvalid2.go\t1\t1\t1\t1\tx\n' > "$tsv"
  [ "$(no_forms_row bad.go 1 "$tsv")" = "bad.go${T}unsupported${T}no forms: an invalid program repr/go does not convert (errors: bad.go:3: oops)" ] \
    && [ "$(no_forms_row bad2.go 1 "$tsv" | cut -f2)" = unsupported ] \
    && [ "$(no_forms_row missing.go 1 "$tsv" | cut -f2)" = error ] \
    && [ "$(no_forms_row valid.go 1 "$tsv" | cut -f2)" = error ] \
    && [ "$(no_forms_row valid2.go "" "$tsv" | cut -f2)" = error ]
}
t no-forms-only-for-invalid-programs test_no_forms

# finding 6: the corpora's key covers goi's REPL, the forms of go/parser, box.ae and the reader chosen, not the host's bytes (069 A)
test_corpus_key() {
  load corpus_key || return 1
  unset REPR_GO_READER
  local root=$tmp/ck goroot=$tmp/ck/goroot hexe=$tmp/ck/host k0 k1 d
  mkdir -p "$root/seed" "$goroot" "$root/arbace/repr/go" "$root/arbace/go/check" "$root/arbace/box" "$root/bin/lib/repr-go" \
    "$root/tools/godump" "$root/tools/gocmp" "$root/go/arbace/repr" "$root/tamago/goroot/go/go/parser" "$root/tamago/goroot/go/go/scanner" \
    "$root/tamago/goroot/go/go/ast" "$root/tamago/goroot/go/go/token"
  echo go1 > "$goroot/VERSION"; echo pin > "$root/seed/goroot-test.sha256"; echo host0 > "$hexe"
  : > "$root/arbace/repr/go.ae"; : > "$root/arbace/go/check.ae"; : > "$root/arbace/box/plan.ae"; : > "$root/bin/repr-go"
  : > "$root/bin/lib/repr-go-host.bash"; : > "$root/arbace/box.ae"
  for d in go.ae go/repl.ae go/run.ae go/build.ae go/generic.ae go/table.ae go/embed.ae; do : > "$root/arbace/$d"; done
  : > "$root/tamago/goroot/go/go/parser/parser.ae"
  k0=$(corpus_key)
  for d in arbace/go/repl.ae arbace/go/table.ae arbace/go.ae arbace/box.ae tamago/goroot/go/go/parser/parser.ae; do
    echo "changed $d" >> "$root/$d"; k1=$(corpus_key)
    [ "$k0" != "$k1" ] || { echo "the key ignores $d" >&2; return 1; }
    k0=$k1
  done
  # the host's bytes are no part of the key: the interp and the AOT steps run on different hosts over one corpora tree
  echo host1 > "$hexe"; k1=$(corpus_key)
  [ "$k0" = "$k1" ] || { echo "the key follows the host executable" >&2; return 1; }
  REPR_GO_READER=helper k1=$(corpus_key)
  [ "$k0" != "$k1" ] || { echo "the key ignores REPR_GO_READER" >&2; return 1; }
}
t corpus-key-covers-reader-sources-not-host test_corpus_key

# 069 B: the two go-command programs are skipped by gc's own run failing here, so no known entry stands for them
test_no_dead_known() {
  ! grep -qE '"(fixedbugs/issue21317|linkobj)\.go"' "${KNOWN:-$here/test/goi/check-aot-known.edn}"
}
t no-dead-aot-known-entries test_no_dead_known

# finding 5: bin/goi divergences --aot holds test/goi/essential-aot.txt's programs to the AOT record
host=${ARBACE_HOST_EXE:-$here/target/seed/amd64/arbace}
divergences() { # divergences ROOT [ARG...]: the tool on a fabricated root
  local root=$1; shift
  GOI_ROOT=$root ARBACE_PATH=$here:$here/test "$host" -m goi.divergences "$@"
}
test_essential_aot() {
  local r=$tmp/e out st
  mkdir -p "$r/test/goi"
  printf '[{:id :k :title "known" :kind :design :why "w" :decided "d" :match [#"^known "]}]\n' > "$r/test/goi/divergences-aot.edn"
  printf '[["a.go" :pass ""] ["b.go" :known "known x"] ["d.go" :pass ""]]\n' > "$r/test/goi/check-aot-linux-amd64.edn"
  printf '# the list\na.go\nb.go\nc.go\n' > "$r/test/goi/essential-aot.txt"
  out=$(divergences "$r" --aot) && st=0 || st=$?
  [ "$st" = 1 ] && printf '%s' "$out" | grep -q 'essential: b.go does not pass' \
    && printf '%s' "$out" | grep -q 'essential: c.go is not in the reference' || { echo "$out" >&2; return 1; }
  printf '# the list\na.go\nd.go\n' > "$r/test/goi/essential-aot.txt"
  divergences "$r" --aot > "$tmp/e.out" || { cat "$tmp/e.out" >&2; return 1; }
}
t essential-aot-held-to-the-record test_essential_aot

exit $failed
