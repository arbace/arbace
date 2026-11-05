# bin/oi's native front end (doc/ocaml/TYPER.md): sourced by bin/oi.
#
#   bin/oi typer-check [--record] [NAME...]   test/ocaml/typer/'s samples typed and translated by Arbace (arbace.repr.ocaml.typer,
#                                  on stage 0): each signature against the oracle's (ocdump --mode types), each Lambda against the
#                                  oracle's raw translation (-drawlambda: ocdump --mode lambda) and Simplif's output against -dlambda's
#                                  (--simplify), equal up to the identifiers' stamps and the static exits' numbers; the oracle's dumps
#                                  are committed in test/ocaml/typer/oracle/ (TY2) and, with ocdump present, verified against a fresh
#                                  dump; then each native simplified Lambda run on oi against the sample's NAME.out (the ocaml
#                                  toplevel's run of the file; the library is the native one, so without ocdump only the dump comparison is skipped). --record
#                                  rewrites the dumps and the outputs
#   bin/oi stdlib-check [UNIT...]            the native library (bin/oi stdlib, TY18) against the oracle's (OI_STDLIB=ocamlc: ocamlc and ocdump):
#                                  each unit typed afresh and its Lambda equal to the oracle's (canonical form), the library's files byte for
#                                  byte those of the fresh run, the same units, lib/testing's Lambda equal, the typer's samples run on it
#   bin/oi run-native FILE.ml [ARG...]       FILE typed and translated by Arbace (no ocamlc, no ocdump) and run on oi with the native library

TYPER_DIR=$root/test/ocaml/typer
TYPER_ORACLE=$TYPER_DIR/oracle

typer_ns() {
  local exe; exe=$(arbace_exe)
  ARBACE_PATH="$root" GOMEMLIMIT=${OI_TYPER_GOMEMLIMIT:-1500MiB} GOMAXPROCS=${OI_TYPER_THREADS:-2} "$exe" -m arbace.repr.ocaml.typer "$@"
}

# the directory the typer reads the library's interfaces from: the sources as the native build prepared them, with lib/testing's
native_dir() { if [ "$OI_STDLIB" = ocamlc ]; then echo "$OI_DIR/stdlib-build"; else echo "$OI_DIR/stdlib-native-build"; fi; }

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
  cmd_stdlib > "$OI_DIR/stdlib.log"; cmd_build
  # the oracle: the committed dumps (TYPER_ORACLE), made by the host's compiler-libs through tools/ocdump; with ocdump present they are
  # dumped afresh and must equal the committed ones (--record rewrites them and the toplevel's outputs)
  local have_ocd=0 st=0
  # OI_TYPER_NO_OCDUMP=1: judged on the committed dumps alone, as on a host without compiler-libs
  if [ -z "${OI_TYPER_NO_OCDUMP:-}" ] && "$root/bin/repr-ocaml" ocdump-build > "$OI_DIR/ocdump-build.log" 2>&1; then have_ocd=1; fi
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
  typer_ns check --dir "$TYPER_DIR" --oracle "$TYPER_ORACLE" --out "$w/native" --stdlib "$(native_dir)" --report "$w/report.edn" \
    "${names[@]}" || st=1
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
  typer_ns lambda --simplify --stdlib "$(native_dir)" --file "$(basename "$file")" "$file" > "$d/$u.ae" || { rm -rf "$d"; exit 2; }
  local st=0
  local tl=""; grep -q 'Testing\.' "$file" && tl="$TESTLIB/Testing.ae"
  "$EXE" run "$LIB" $tl "$d/$u.ae" -- "$@" || st=$?
  rm -rf "$d"
  exit $st
}

cmd_stdlib_check() {
  local units=("$@") st=0
  [ "$OI_STDLIB" = native ] || die "stdlib-check compares the native library with the oracle's: OI_STDLIB is native"
  cmd_stdlib > "$OI_DIR/stdlib.log"; cmd_build
  # the oracle's library, by ocamlc and ocdump (lib-ocamlc), in the order of StdlibModules
  OI_STDLIB=ocamlc "$0" stdlib > "$OI_DIR/stdlib-ocamlc.log" || { tail -5 "$OI_DIR/stdlib-ocamlc.log" >&2; die "stdlib-check: the oracle's library"; }
  [ ${#units[@]} -gt 0 ] || mapfile -t units < "$ORACLE_LIB/order.txt"
  local nat=$OI_DIR/stdlib-native; rm -rf "$nat"; mkdir -p "$nat"
  # each unit typed afresh, its Lambda equal (canonical form) to the oracle's
  typer_ns stdlib-check --dir "$(native_dir)" --lib "$ORACLE_LIB" --out "$nat" "${units[@]}" | tee "$OI_DIR/stdlib-check.txt" || st=1
  # the library bin/oi stdlib made: the same units byte for byte, the same set and a link order the oracle's dependencies accept
  local u n=0
  for u in "${units[@]}"; do
    if cmp -s "$nat/$u.ae" "$LIB/$u.ae"; then n=$((n + 1)); else echo "oi: stdlib-check: $u: the library's Lambda differs from a fresh native run"; st=1; fi
  done
  echo "oi: stdlib-check: $n of ${#units[@]} units of the native library equal to a fresh typing"
  if [ "$(LC_ALL=C sort "$LIB/order.txt" | paste -sd' ')" = "$(LC_ALL=C sort "$ORACLE_LIB/order.txt" | paste -sd' ')" ]; then
    echo "oi: stdlib-check: the native link order has the oracle's units ($(wc -l < "$LIB/order.txt")), computed from the dependencies"
  else echo "oi: stdlib-check: the native library's units differ from the oracle's"; st=1; fi
  if cmp -s "$LIB/order.txt" "$ORACLE_LIB/order.txt"; then echo "oi: stdlib-check: its order is the oracle's"
  else echo "oi: stdlib-check: its order differs from StdlibModules' (both satisfy the dependencies)"; fi
  # the testsuite's lib/testing: the oracle's Lambda
  if cmp -s "$TESTLIB/Testing.ae" "$OI_DIR/testlib-ocamlc/Testing.ae"; then echo "oi: stdlib-check: lib/testing's Lambda byte for byte the oracle's"
  else
    if typer_ns canon-equal "$TESTLIB/Testing.ae" "$OI_DIR/testlib-ocamlc/Testing.ae"; then echo "oi: stdlib-check: lib/testing's Lambda equal to the oracle's (up to stamps)"
    else echo "oi: stdlib-check: lib/testing differs from the oracle's"; st=1; fi
  fi
  # the typer's samples, natively translated, run on the library
  local w=$OI_DIR/typer-native; rm -rf "$w"; mkdir -p "$w/native" "$w/run"
  typer_ns check --dir "$TYPER_DIR" --oracle "$TYPER_ORACLE" --out "$w/native" --stdlib "$(native_dir)" \
    $(cd "$TYPER_DIR" && ls *.ml | sed 's/\.ml$//') > "$w/check.txt" || true
  local ok=0 bad=0 name f
  for f in "$TYPER_DIR"/*.ml; do
    name=$(basename "$f" .ml); u=$(unit_of "$name")
    [ -f "$w/native/$u.ae" ] || { bad=$((bad + 1)); continue; }
    GOMAXPROCS=1 "$EXE" run "$LIB" "$w/native/$u.ae" > "$w/run/$name.out" 2>&1 || true
    if cmp -s "$w/run/$name.out" "$TYPER_DIR/$name.out"; then ok=$((ok + 1)); else bad=$((bad + 1)); echo "oi: stdlib-check: $name: oi's output differs on the native library"; fi
  done
  echo "oi: stdlib-check: the typer's samples on the native library: $ok equal to the toplevel's output, $bad differ"
  [ $bad = 0 ] && return $st
}
