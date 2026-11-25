# bin/oi's ppx verbs (queue 546, the user's 539b-1, T1: ppxlib's bootstrap on oi; doc/ocaml/OCAML-SURVEY.md, "As built: ppxlib's
# bootstrap"). ppxlib 0.39.0's build run on oi from the pinned opam set (test/ocaml/corpora.edn's :opam-ppxlib and its closure), no
# ppx of the host's: the programs of test/ocaml/ppx-programs.tsv (opam-programs.tsv's format; not rows of the suite corpus, no record),
# each staged, typed by Arbace's typer and run on oi as the suite's programs are (suite_oi_one), their units' build texts by
# arbace.repr.ocaml.prep (astlib's and ast's pp, ocaml-compiler-libs' ocaml_common and ocaml_shadow, src/'s generated units).
# Sourced by bin/oi after suite.bash.
#
#   bin/oi ppx gen     src/gen's gen_ast_pattern and gen_ast_builder run on oi over ast/ast.ml: the units ppxlib's src/dune makes,
#                      ast_pattern_generated.ml and ast_builder_generated.ml, kept in .tmp/oi/ppx/gen (OI_PPXLIB_GEN to the staging)
#   bin/oi ppx check   the metaquot driver (test/metaquot/driver.ml: Ppxlib.Driver.standalone with ppxlib.metaquot linked) run on
#                      oi over the cram cases of ppxlib's test/metaquot/run.t (each `cat > F << EOF` input, then `./driver.exe F`),
#                      its output against the expected output the cram file holds, line for line; its negative control: a case's
#                      output judged against another case's expected output must differ

PPX=$OI_DIR/ppx
PPX_ROWS=$root/test/ocaml/ppx-programs.tsv

# ppx_row ID: the corpus row (suite_oi_one's fields, tab-separated) of the program ID of ppx-programs.tsv, @PPXIN@ in its ARGS the
# directory of the extracted cram inputs
ppx_row() {
  grep -v '^#' "$PPX_ROWS" | awk -F'\t' -v id="$1" -v opam="$OPAM" '$2 == id { printf "opam/%s/%s\t%s\t%s\t%s\t%s\t%s\t%s\n", $1, $2, $3, $7, $4, $6, opam "/" $1, $5 }' |
    sed "s|@OCAMLSRCDIR@|$TOP|g; s|@PPXIN@|$PPX/in|g"
}

# ppx_run ID: the program run on oi as the suite runs one (its run directory $S/run/opam/PKG/ID); the result line on stdout
ppx_run() {
  local row; row=$(ppx_row "$1"); [ -n "$row" ] || die "ppx: no program $1 in $PPX_ROWS"
  local id flags incs args env base units
  IFS=$'\037' read -r id flags incs args env base units <<< "$(tr '\t' '\037' <<< "$row")"
  suite_oi_one "$id" "$flags" "$incs" "$args" "$env" "$base" "$units"
}

ppx_env() {
  opts "$@"
  cmd_stdlib > "$OI_DIR/stdlib.log"; cmd_build; NATIVE=1; suite_native_env
  mkdir -p "$PPX" "$S/run"
  export OI_PPXLIB_GEN=$PPX/gen OI_OCAML_TOP=$TOP
  export -f limited_cpu oi_wall oi_status oi_cpu_secs oi_limit_kill suite_oi_one suite_dump suite_dump_native suite_stage suite_gen suite_names suite_cap suite_unit_file suite_wrap_one suite_wrap_path suite_needs_build suite_alias
  export S OI_DIR TESTS TESTLIB TIMEOUT EXE LIB OCD GOMEMLIMIT root
}

# the generated units: each generator's run, the file it writes in its run directory kept in $PPX/gen
cmd_ppx_gen() {
  ppx_env "$@"
  rm -rf "$PPX/gen.new"; mkdir -p "$PPX/gen.new"
  local g f st=0
  for g in gen_ast_pattern:ast_pattern_generated.ml gen_ast_builder:ast_builder_generated.ml; do
    f=${g#*:}; g=${g%%:*}
    ppx_run "$g" | tee "$PPX/$g.result"
    if [ "$(cut -f2 "$PPX/$g.result")" = 0 ] && [ -s "$S/run/opam/ppxlib/$g/$f" ]; then cp "$S/run/opam/ppxlib/$g/$f" "$PPX/gen.new/"
    else echo "oi: ppx gen: $g made no $f ($S/run/opam/ppxlib/$g)" >&2; st=1; fi
  done
  [ $st = 0 ] || die "ppx gen failed"
  rm -rf "$PPX/gen"; mv "$PPX/gen.new" "$PPX/gen"
  echo "oi: ppx gen: $(ls "$PPX/gen" | tr '\n' ' ')in $PPX/gen"
}

# ppx_cram RUN.T DIR: the cram file's cases into DIR: each `cat > F << EOF` input as DIR/in/F, each `./driver.exe F`'s expected output
# (the indented lines that follow it, their two spaces taken off; cram's `[N]`, the exit status, as DIR/exp/F.status) as DIR/exp/F.out;
# the cases' names, one a line, in DIR/cases
ppx_cram() {
  local t=$1 d=$2
  rm -rf "$d/in" "$d/exp"; mkdir -p "$d/in" "$d/exp"; : > "$d/cases"
  awk -v d="$d" '
    function flush() { if (cur != "") { close(outf) }; cur = "" }
    /^  \$ cat > [^ ]+ << EOF$/ { flush(); mode = "in"; f = $4; inf = d "/in/" f; printf "" > inf; next }
    mode == "in" && /^  > EOF$/ { close(inf); mode = ""; next }
    mode == "in" && /^  > / { print substr($0, 5) >> inf; next }
    mode == "in" && /^  >$/ { print "" >> inf; next }
    /^  \$ \.\/driver\.exe [^ ]+$/ { flush(); mode = "out"; cur = $3; outf = d "/exp/" cur ".out"; printf "" > outf; print cur >> (d "/cases"); next }
    mode == "out" && /^  \[[0-9]+\]$/ { s = $1; gsub(/[^0-9]/, "", s); print s > (d "/exp/" cur ".status"); close(d "/exp/" cur ".status"); next }
    mode == "out" && /^  / { print substr($0, 3) >> outf; next }
    mode == "out" { flush(); mode = ""; next }
  ' "$t"
}

# the metaquot driver on oi over test/metaquot/run.t's cases: typed once (suite_dump), run once a case in the inputs' directory
cmd_ppx_check() {
  ppx_env "$@"
  [ -s "$PPX/gen/ast_pattern_generated.ml" ] && [ -s "$PPX/gen/ast_builder_generated.ml" ] || die "ppx check: no generated units (bin/oi ppx gen)"
  local cram=$OPAM/ppxlib/test/metaquot/run.t c
  ppx_cram "$cram" "$PPX"
  [ -s "$PPX/cases" ] || die "ppx check: no case in $cram"
  local row id flags incs args env base units
  row=$(ppx_row metaquot_driver); [ -n "$row" ] || die "ppx: no program metaquot_driver in $PPX_ROWS"
  IFS=$'\037' read -r id flags incs args env base units <<< "$(tr '\t' '\037' <<< "$row")"
  suite_dump "$id" "$flags" "$incs" "$base" "$units"
  local d=$S/run/$id
  [ -f "$d/error" ] && die "ppx check: the driver: $(cat "$d/error")"
  local tl; tl=$(inc_units "$incs" $(cat "$d/units.list") | tr '\n' ' ')
  local miss; miss=$(lib_missing $tl); [ -z "$miss" ] || die "ppx check: the driver: typer: $miss"
  rm -rf "$PPX/out"; mkdir -p "$PPX/out"
  local pass=0 fail=0 st want
  while read -r c; do
    st=0
    (cd "$PPX/in" && run_redirected "$env" "$PPX/out/$c.out" limited_cpu "$TIMEOUT" "$(oi_wall)" "$EXE" run "$LIB" $tl $(cat "$d/units.list") -- "$c") || st=$?
    want=0; [ -f "$PPX/exp/$c.status" ] && want=$(cat "$PPX/exp/$c.status")
    if [ "$st" = "$want" ] && cmp -s "$PPX/out/$c.out" "$PPX/exp/$c.out"; then echo "pass $c"; pass=$((pass + 1))
    else echo "FAIL $c: status $st (cram's $want); diff:"; diff "$PPX/exp/$c.out" "$PPX/out/$c.out" | head -20; fail=$((fail + 1)); fi
  done < "$PPX/cases"
  # the negative control (GTS81 (a)): the first case's output against the second's expected output must differ
  local c1 c2; c1=$(sed -n 1p "$PPX/cases"); c2=$(sed -n 2p "$PPX/cases")
  if [ -n "$c2" ]; then
    if cmp -s "$PPX/out/$c1.out" "$PPX/exp/$c2.out"; then echo "control $c1-vs-$c2: the same: the comparison passes what differs"; fail=$((fail + 1))
    else echo "control $c1-vs-$c2: differs, as a control must"; fi
  fi
  echo "oi: ppx check: $pass of $(wc -l < "$PPX/cases") cases of $cram pass, $fail failures ($PPX/out)"
  [ $fail = 0 ]
}

cmd_ppx() {
  local verb=${1:-}; [ -n "$verb" ] || die "usage: bin/oi ppx gen|check"
  shift
  case $verb in
    gen) cmd_ppx_gen "$@" ;;
    check) cmd_ppx_check "$@" ;;
    *) die "unknown ppx verb $verb" ;;
  esac
}
