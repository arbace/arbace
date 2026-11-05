# bin/oi's native front end (doc/ocaml/TYPER.md): sourced by bin/oi.
#
#   bin/oi typer-check [--record] [NAME...]   test/ocaml/typer/'s samples typed and translated by Arbace (arbace.repr.ocaml.typer,
#                                  on stage 0): each signature against the oracle's (ocdump --mode types), each Lambda against the
#                                  oracle's raw translation (ocdump --mode lambda, equal up to the identifiers' stamps and the static
#                                  exits' numbers), then each native Lambda run on oi against the sample's NAME.out (the ocaml
#                                  toplevel's run of the file; --record rewrites them)
#   bin/oi run-native FILE.ml [ARG...]       FILE typed and translated by Arbace (no ocamlc for the program) and run on oi with the
#                                  stdlib's Lambda (bin/oi stdlib's, still the oracle's: TYPER.md, T8)

TYPER_DIR=$root/test/ocaml/typer

typer_ns() {
  local exe; exe=$(arbace_exe)
  ARBACE_PATH="$root" GOMEMLIMIT=${OI_TYPER_GOMEMLIMIT:-1500MiB} GOMAXPROCS=${OI_TYPER_THREADS:-2} "$exe" -m arbace.repr.ocaml.typer "$@"
}

unit_of() { local u; u=$(basename "$1" .ml); echo "$(echo "${u:0:1}" | tr a-z A-Z)${u:1}"; }

cmd_typer_check() {
  local record=0 names=() n
  while [ $# -gt 0 ]; do case $1 in --record) record=1 ;; *) names+=("$1") ;; esac; shift; done
  [ ${#names[@]} -gt 0 ] || mapfile -t names < <(cd "$TYPER_DIR" && ls *.ml | sed 's/\.ml$//')
  mkdir -p "$OI_DIR"; need_tree
  local w=$OI_DIR/typer; rm -rf "$w"; mkdir -p "$w/oracle" "$w/native" "$w/run"
  if [ $record = 1 ]; then
    for n in "${names[@]}"; do (cd "$TYPER_DIR" && ocaml "$n.ml" > "$n.out" 2>&1) || true; done
  fi
  # the oracle: the host's compiler-libs through tools/ocdump (the judge's only use of OCaml's own compiler)
  for n in "${names[@]}"; do
    (cd "$TYPER_DIR" && "$OCD" --mode lambda --width 120 --prefix "$n" --file "$n.ml" "$n.ml") > "$w/oracle/$n.lambda"
    (cd "$TYPER_DIR" && "$OCD" --mode types --prefix "$n" --file "$n.ml" "$n.ml") > "$w/oracle/$n.types"
  done
  local st=0
  typer_ns check --dir "$TYPER_DIR" --oracle "$w/oracle" --out "$w/native" --stdlib "$TOP/stdlib" --report "$w/report.edn" \
    "${names[@]}" || st=1
  cmd_stdlib > "$OI_DIR/stdlib.log"; cmd_build
  local ok=0 bad=0 u
  for n in "${names[@]}"; do
    u=$(unit_of "$n")
    if [ ! -f "$w/native/$u.ae" ]; then echo "oi: typer-check: $n: no native Lambda"; bad=$((bad + 1)); continue; fi
    GOMAXPROCS=1 "$EXE" run "$LIB" "$w/native/$u.ae" > "$w/run/$n.out" 2>&1 || true
    if cmp -s "$w/run/$n.out" "$TYPER_DIR/$n.out"; then ok=$((ok + 1)); else bad=$((bad + 1)); echo "oi: typer-check: $n: oi's output differs"; fi
  done
  echo "oi: typer-check: ${#names[@]} samples run on oi from the native Lambda: $ok equal to the toplevel's output, $bad differ"
  [ $bad = 0 ] && [ $st = 0 ]
}

cmd_run_native() {
  [ $# -ge 1 ] || die "usage: bin/oi run-native FILE.ml [ARG...]"
  local file=$1; shift
  cmd_stdlib > "$OI_DIR/stdlib.log"; cmd_build
  local d; d=$(mktemp -d "$OI_DIR/run.XXXXXX")
  local u; u=$(unit_of "$file")
  typer_ns lambda --stdlib "$TOP/stdlib" --file "$(basename "$file")" "$file" > "$d/$u.ae" || { rm -rf "$d"; exit 2; }
  local st=0
  "$EXE" run "$LIB" "$d/$u.ae" -- "$@" || st=$?
  rm -rf "$d"
  exit $st
}
