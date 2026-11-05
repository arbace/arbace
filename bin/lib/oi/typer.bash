# bin/oi's native front end (doc/ocaml/TYPER.md): sourced by bin/oi.
#
#   bin/oi typer-check [--record] [NAME...]   test/ocaml/typer/'s samples typed and translated by Arbace (arbace.repr.ocaml.typer,
#                                  on stage 0): each signature against the oracle's (ocdump --mode types), each Lambda against the
#                                  oracle's raw translation (-drawlambda: ocdump --mode lambda) and Simplif's output against -dlambda's
#                                  (--simplify), equal up to the identifiers' stamps and the static exits' numbers; the oracle's dumps
#                                  are committed in test/ocaml/typer/oracle/ (TY2) and, with ocdump present, verified against a fresh
#                                  dump; then each native simplified Lambda run on oi against the sample's NAME.out (the ocaml
#                                  toplevel's run of the file; skipped without ocdump: oi's stdlib still needs ocamlc). --record
#                                  rewrites the dumps and the outputs
#   bin/oi run-native FILE.ml [ARG...]       FILE typed and translated by Arbace (no ocamlc for the program) and run on oi with the
#                                  stdlib's Lambda (bin/oi stdlib's, still the oracle's: TYPER.md, T8)

TYPER_DIR=$root/test/ocaml/typer
TYPER_ORACLE=$TYPER_DIR/oracle

typer_ns() {
  local exe; exe=$(arbace_exe)
  ARBACE_PATH="$root" GOMEMLIMIT=${OI_TYPER_GOMEMLIMIT:-1500MiB} GOMAXPROCS=${OI_TYPER_THREADS:-2} "$exe" -m arbace.repr.ocaml.typer "$@"
}

unit_of() { local u; u=$(basename "$1" .ml); echo "$(echo "${u:0:1}" | tr a-z A-Z)${u:1}"; }

# the oracle's dumps of a sample (TY2): the types, the raw Lambda (-drawlambda) and the simplified one (-dlambda)
typer_dump() {
  local n=$1 out=$2
  (cd "$TYPER_DIR" && "$OCD" --mode lambda --width 120 --prefix "$n" --file "$n.ml" "$n.ml") > "$out/$n.lambda"
  (cd "$TYPER_DIR" && "$OCD" --mode lambda --simplify --width 120 --prefix "$n" --file "$n.ml" "$n.ml") > "$out/$n.dlambda"
  (cd "$TYPER_DIR" && "$OCD" --mode types --prefix "$n" --file "$n.ml" "$n.ml") > "$out/$n.types"
}

cmd_typer_check() {
  local record=0 names=() n
  while [ $# -gt 0 ]; do case $1 in --record) record=1 ;; *) names+=("$1") ;; esac; shift; done
  [ ${#names[@]} -gt 0 ] || mapfile -t names < <(cd "$TYPER_DIR" && ls *.ml | sed 's/\.ml$//')
  mkdir -p "$OI_DIR"
  "$root/bin/repr-ocaml" corpus fetch > "$OI_DIR/fetch.log" 2>&1 || { cat "$OI_DIR/fetch.log" >&2; die "corpus fetch"; }
  local w=$OI_DIR/typer; rm -rf "$w"; mkdir -p "$w/fresh" "$w/native" "$w/run"
  # the oracle: the committed dumps (TYPER_ORACLE), made by the host's compiler-libs through tools/ocdump; with ocdump present they are
  # dumped afresh and must equal the committed ones (--record rewrites them and the toplevel's outputs)
  local have_ocd=0 st=0
  if "$root/bin/repr-ocaml" ocdump-build > "$OI_DIR/ocdump-build.log" 2>&1; then have_ocd=1; fi
  if [ $record = 1 ]; then
    [ $have_ocd = 1 ] || { cat "$OI_DIR/ocdump-build.log" >&2; die "typer-check --record needs ocdump (compiler-libs)"; }
    mkdir -p "$TYPER_ORACLE"
    for n in "${names[@]}"; do (cd "$TYPER_DIR" && ocaml "$n.ml" > "$n.out" 2>&1) || true; typer_dump "$n" "$TYPER_ORACLE"; done
  elif [ $have_ocd = 1 ]; then
    local stale=0 x
    for n in "${names[@]}"; do
      typer_dump "$n" "$w/fresh"
      for x in types lambda dlambda; do
        if ! cmp -s "$w/fresh/$n.$x" "$TYPER_ORACLE/$n.$x"; then echo "oi: typer-check: $n.$x: the committed dump differs from ocdump's"; stale=$((stale + 1)); fi
      done
    done
    echo "oi: typer-check: the committed oracle dumps verified against a fresh ocdump: $stale differ"
    [ $stale = 0 ] || st=1
  else
    echo "oi: typer-check: no ocdump (compiler-libs): the committed oracle dumps judge alone, the runs on oi skipped"
  fi
  typer_ns check --dir "$TYPER_DIR" --oracle "$TYPER_ORACLE" --out "$w/native" --stdlib "$TOP/stdlib" --report "$w/report.edn" \
    "${names[@]}" || st=1
  [ $have_ocd = 1 ] || return $st
  cmd_stdlib > "$OI_DIR/stdlib.log"; cmd_build
  local ok=0 bad=0 u
  for n in "${names[@]}"; do
    u=$(unit_of "$n")
    if [ ! -f "$w/native/$u.ae" ]; then echo "oi: typer-check: $n: no native Lambda"; bad=$((bad + 1)); continue; fi
    GOMAXPROCS=1 "$EXE" run "$LIB" "$w/native/$u.ae" > "$w/run/$n.out" 2>&1 || true
    if cmp -s "$w/run/$n.out" "$TYPER_DIR/$n.out"; then ok=$((ok + 1)); else bad=$((bad + 1)); echo "oi: typer-check: $n: oi's output differs"; fi
  done
  echo "oi: typer-check: ${#names[@]} samples run on oi from the native simplified Lambda: $ok equal to the toplevel's output, $bad differ"
  [ $bad = 0 ] && [ $st = 0 ]
}

cmd_run_native() {
  [ $# -ge 1 ] || die "usage: bin/oi run-native FILE.ml [ARG...]"
  local file=$1; shift
  cmd_stdlib > "$OI_DIR/stdlib.log"; cmd_build
  local d; d=$(mktemp -d "$OI_DIR/run.XXXXXX")
  local u; u=$(unit_of "$file")
  typer_ns lambda --simplify --stdlib "$TOP/stdlib" --file "$(basename "$file")" "$file" > "$d/$u.ae" || { rm -rf "$d"; exit 2; }
  local st=0
  "$EXE" run "$LIB" "$d/$u.ae" -- "$@" || st=$?
  rm -rf "$d"
  exit $st
}
