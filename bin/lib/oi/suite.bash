# bin/oi's suite verbs (O7, OC100-OC109; doc/ocaml/OCAML-SURVEY.md, "As built: O7 (write-ahead)"): the programs the
# run corpus of O5 does not hold, as a second corpus of programs that are lists of units (ocamltest's
# `modules`, a package's library and a test), run at the levels oi and the light AOT level has, judged by
# test/ocaml/oi_check.ae and o2j_check.ae with other files. Sourced by bin/oi (its variables and functions).
#
#   corpus rows ($S/corpus.tsv): ID <TAB> run <TAB> FLAGS <TAB> INCS <TAB> ARGS <TAB> ENV <TAB> BASE <TAB> UNITS
#                                 ID <TAB> class <TAB> REASON
#   BASE the directory UNITS (space separated, in link order, the main last) are relative to; a unit
#   "=FILE" is test/ocaml/opam/FILE of the checkout.

S=$root/.tmp/o7
SUITE_REFS=$root/test/ocaml/ref-suite.edn
SUITE_OI=$root/test/ocaml/check-suite-oi.edn
SUITE_OI_CLASSES=$root/test/ocaml/divergences-suite.edn
SUITE_AOT=$root/test/ocaml/check-suite-aot.edn
SUITE_AOT_CLASSES=$root/test/ocaml/divergences-suite-aot.edn
OPAM=$root/.tmp/repr-ocaml/opam
. "$root/bin/lib/oi/testsuite.bash"

# the test .ml files of the testsuite, with the actions of their header (bin/lib/oi/actions.awk)
suite_actions() { # -> "ID<TAB>run|class<TAB>..." for every .ml of tests/ (ID without the extension)
  local f
  (cd "$TESTS" && find . -name '*.ml' | sed 's|^\./||' | LC_ALL=C sort) | while read -r f; do
    printf '%s\t%s\n' "${f%.ml}" "$(awk -f "$root/bin/lib/oi/actions.awk" "$TESTS/$f")"
  done
}

cmd_suite_corpus() {
  mkdir -p "$S"; need_source; cmd_corpus > "$OI_DIR/corpus.log"
  local out=$S/corpus.tsv
  # the testsuite's run programs the run corpus of O5 does not hold
  suite_actions > "$S/actions.tsv"
  awk -F'\t' 'NR == FNR { if ($2 == "run") o5[$1] = 1; next }
    $2 == "run" && !($1 in o5) {
      dir = $1; sub(/[^\/]*$/, "", dir); name = $1; sub(/.*\//, "", name)
      units = ""; n = split($7, m, " "); for (i = 1; i <= n; i++) units = units (units == "" ? "" : " ") dir m[i]
      units = units (units == "" ? "" : " ") $1 ".ml"
      printf "%s\trun\t%s\t%s\t%s\t%s\t%s\t%s\n", $1, $3, $4, $5, $6, "'"$TESTS"'", units
    }' "$OI_DIR/corpus.tsv" "$S/actions.tsv" > "$out.ts"
  # the packages' programs (test/ocaml/opam-programs.tsv: PKG ID FLAGS ARGS UNITS), a package whose sources are not fetched out
  : > "$out.opam"
  # every package of the set extracted, else the 33 opam rows would leave the corpus unseen (review 008's finding 4)
  local pkg
  for pkg in $(grep -v '^#' "$root/test/ocaml/opam-programs.tsv" | cut -f1 | LC_ALL=C sort -u); do
    [ -f "$OPAM/$pkg/.extracted" ] || die "suite corpus: the opam package $pkg is not fetched out (bin/repr-ocaml corpus fetch)"
  done
  if [ -d "$OPAM" ]; then
    grep -v '^#' "$root/test/ocaml/opam-programs.tsv" | awk -F'\t' 'NF >= 5 && $2 != "class" { printf "opam/%s/%s\trun\t%s\t\t%s\t\t%s\t%s\n", $1, $2, $3, $4, "'"$OPAM"'/" $1, $5 }
      NF >= 3 && $2 == "class" { printf "opam/%s/%s\tclass\t%s\n", $1, $3, $4 }' >> "$out.opam"
  fi
  cat "$out.ts" "$out.opam" | LC_ALL=C sort > "$out"; rm -f "$out.ts" "$out.opam"
  echo "oi: suite corpus: $(awk -F'\t' '$2 == "run"' "$out" | wc -l) programs ($(grep -c '^opam/' "$out") of the opam set), $(awk -F'\t' '$2 == "class"' "$out" | wc -l) classed ($out)"
}

suite_selected() {
  select_from "$S/corpus.tsv"
}

# suite_stage ID BASE UNITS DIR: the units' files (and a testsuite program's directory) copied into DIR, flat
suite_stage() {
  local id=$1 base=$2 units=$3 d=$4 u
  mkdir -p "$d"
  # a testsuite program's directory beside it: the data files (ocamltest copies the test, its modules and its readonly files only)
  case $id in opam/*) ;; *) find "$base/$(dirname "$id")" -maxdepth 1 -type f ! -name '*.ml' ! -name '*.mli' ! -name '*.reference' ! -name '*.c' -exec cp {} "$d/" \;
    # and the .ml files its header names as readonly_files, data (parsetree/test's source.ml: OT21 (b))
    local ro f; ro=$(sed -n 's/.*readonly_files *= *"\([^"]*\)".*/\1/p' "$base/$id.ml" | head -1)
    for f in $ro; do case $f in *.ml) [ -f "$base/$(dirname "$id")/$f" ] && cp "$base/$(dirname "$id")/$f" "$d/" ;; esac; done ;; esac
  for u in $units; do
    case $u in =*) cp "$root/test/ocaml/opam/${u#=}" "$d/" ;; *) cp "$base/$u" "$d/" ;; esac
  done
}

suite_names() { # UNITS -> file basenames
  local u; for u in $1; do basename "${u#=}"; done
}

# suite_ref_one ID FLAGS INCS ARGS ENV BASE UNITS: ocamlc's and ocamlopt's runs, the line of ref.tsv
suite_ref_one() {
  local id=$1 flags=$2 incs=$3 args=$4 env=$5 base=$6 units=$7
  local d=$S/ref/$id
  rm -rf "$d"; mkdir -p "$d"
  suite_stage "$id" "$base" "$units" "$d"
  local names; names=$(suite_names "$units")
  local line="" c
  mkdir -p "$d/run-byte" "$d/run-opt"; find "$d" -maxdepth 1 -type f -exec cp {} "$d/run-byte/" \; -exec cp {} "$d/run-opt/" \;
  for c in ocamlc ocamlopt; do
    local ext=byte cst=0 st=0 extra
    [ $c = ocamlopt ] && ext=opt
    # each compiler's program built and run in a copy of the staged directory (a run's files are not the other's, OT9's fixes)
    local r=$d/run-$ext
    extra=$(inc_ref $c "$incs" "$r")
    (cd "$r" && $c -w -a $flags $extra $names -o "prog.$ext") > "$d/$c.log" 2>&1 || cst=$?
    if [ $cst != 0 ]; then line="$line	compile-error	-	0"; continue; fi
    (cd "$r" && run_redirected "$env" "$d/$ext.out" timeout "$TIMEOUT" "./prog.$ext" $args) || st=$?
    line="$line	$st	$(sha256sum < "$d/$ext.out" | cut -d' ' -f1)	$(wc -c < "$d/$ext.out")"
  done
  local same=false last; last=$(echo "$units" | awk '{print $NF}')
  [ -f "$base/${last%.ml}.reference" ] && cmp -s "$d/byte.out" "$base/${last%.ml}.reference" && same=true
  printf '%s%s\t%s\n' "$id" "$line" "$same"
  rm -rf "$d/run-byte" "$d/run-opt"
}

cmd_suite_record_ref() {
  opts "$@"; cmd_stdlib > "$OI_DIR/stdlib.log"; cmd_suite_corpus > "$S/corpus.log"
  export -f suite_ref_one suite_stage suite_names; export S OI_DIR TESTS TESTLIB TIMEOUT root
  local out=$S/ref.tsv
  suite_selected | awk -F'\t' '$2 == "run" { printf "%s%c%s%c%s%c%s%c%s%c%s%c%s%c", $1, 0, $3, 0, $4, 0, $5, 0, $6, 0, $7, 0, $8, 0 }' | xargs -0 -r -n 7 -P "$JOBS" bash -c 'suite_ref_one "$@"' _ > "$out.new"
  LC_ALL=C sort "$out.new" > "$out"; rm -f "$out.new"
  # --only, --only-list: the rows run replace theirs, the others are kept (record-ref-only, as bin/oi record-ref's)
  "$(arbace_exe)" "$root/test/ocaml/oi_check.ae" "record-ref${ONLY:+-only}" "$out" "$S/corpus.tsv" "$SUITE_REFS"
  sed -i '1,/^\[$/{/^\[$/!d}' "$SUITE_REFS"
  sed -i '1i ;; ocamlc'"'"'s and ocamlopt'"'"'s runs of the suite corpus (bin/oi suite record-ref; doc/ocaml/OCAML-SURVEY.md, "As built: O7\n;; (write-ahead)"): the testsuite'"'"'s run programs without a .reference or with ocamltest'"'"'s `modules` (the units compiled in\n;; the order listed), and the opam set'"'"'s programs (test/ocaml/opam-programs.tsv: a package'"'"'s library units and a test), each\n;; compiled by the host'"'"'s OCaml 5.5.0 and run; same fields as test/ocaml/ref-ocaml55.edn. Read with the Clojure reader.' "$SUITE_REFS"
}

# suite_dump ID FLAGS INCS BASE UNITS: the units compiled (their .cmi for the next) and their simplified Lambda
# dumped into $S/run/ID/<Unit>.ae; the list of the .ae files in link order in units.list; "error" when one fails
suite_dump() {
  local id=$1 flags=$2 incs=$3 base=$4 units=$5
  local d=$S/run/$id u n b U
  rm -rf "$d"; mkdir -p "$d"
  suite_stage "$id" "$base" "$units" "$d"
  local I; I=$(inc_I "$incs")
  : > "$d/units.list"
  if [ "${NATIVE:-0}" = 1 ]; then suite_dump_native "$d" "$flags" "$incs" "$units"; return; fi
  for u in $units; do
    n=$(basename "${u#=}"); b=${n%.*}; U=$(echo "${b:0:1}" | tr a-z A-Z)${b:1}
    if ! (cd "$d" && ocamlc -w -a $flags $I -c "$n") > "$d/$n.log" 2>&1; then
      echo "ocamlc -c $n: $(head -c 300 "$d/$n.log" | tr '\n\t' '  ')" > "$d/error"; return
    fi
    case $n in *.mli) continue ;; esac
    (cd "$d" && "$OCD" --mode lambda --simplify $flags $I --prefix "$b" --file "$n" "$n" > "$U.ae" 2> "$n.dump.log")
    if grep -q '^(ocaml/error' "$d/$U.ae"; then echo "dump $n: $(head -c 300 "$d/$U.ae" | tr '\n\t' '  ')" > "$d/error"; return; fi
    echo "$d/$U.ae" >> "$d/units.list"
  done
}

# suite_dump_native DIR FLAGS INCS UNITS (TY24 (a), the native path): each unit in link order typed and translated by Arbace's typer
# (no ocamlc, no ocdump), the earlier units' interfaces found in DIR (--include): an .mli of the program's own, else the signature the
# typer wrote for the unit (--sig-out, as the toplevel's top_N.mli); a unit with its .mli is coerced to it (--intf)
suite_dump_native() {
  local d=$1 flags=$2 incs=$3 units=$4 u n b U nf="" f inc
  for f in $flags; do
    case $f in -nolabels) nf="$nf --nolabels" ;; -unsafe) nf="$nf --unsafe" ;; -no-strict-formats) ;;
      *) echo "typer: flag $f not modelled" > "$d/error"; return ;; esac
  done
  # the program's libraries (unix, threads, str, compiler-libs, testing) after its own directory, by their sources' interfaces
  inc=$d; [ -n "$incs" ] && inc="$d:$(inc_dirs "$incs")"
  for u in $units; do
    n=$(basename "${u#=}"); b=${n%.*}; U=$(echo "${b:0:1}" | tr a-z A-Z)${b:1}
    case $n in *.mli) continue ;; esac
    local extra=""
    if [ -f "$d/$b.mli" ]; then extra=--intf; else extra="--sig-out $d/$b.mli"; fi
    if ! (cd "$d" && typer_ns lambda --simplify --stdlib "$(native_dir)" --include "$inc" $nf $extra --file "$n" "$n" > "$U.ae" 2> "$n.typer.err"); then
      echo "typer $n: $(typer_err_text "$d/$n.typer.err")" > "$d/error"; return
    fi
    echo "$d/$U.ae" >> "$d/units.list"
  done
}

# suite_oi_one ID FLAGS INCS ARGS ENV BASE UNITS: "ID<TAB>STATUS<TAB>SHA<TAB>BYTES<TAB>SECONDS<TAB>NOTE"
suite_oi_one() {
  local id=$1 flags=$2 incs=$3 args=$4 env=$5 base=$6 units=$7
  suite_dump "$id" "$flags" "$incs" "$base" "$units"
  local d=$S/run/$id
  if [ -f "$d/error" ]; then printf '%s\t-\t-\t0\t0\tdump: %s\n' "$id" "$(cat "$d/error")"; return; fi
  local tl; tl=$(inc_units "$incs" $(cat "$d/units.list") | tr '\n' ' ')
  local miss; miss=$(lib_missing $tl)
  if [ -n "$miss" ]; then printf '%s\t-\t-\t0\t0\tdump: typer: %s\n' "$id" "$miss"; return; fi
  local st=0 t0 t1 note=""
  t0=$(date +%s.%N); times > "$d/cpu0"
  (cd "$d" && run_redirected "$env" "$d/oi.out" limited_cpu "$TIMEOUT" "$(oi_wall)" "$EXE" run "$LIB" $tl $(cat "$d/units.list") -- $args) || st=$?
  t1=$(date +%s.%N); times > "$d/cpu1"
  note=$(grep -a -m1 -o 'oi: .*' "$d/oi.out" | head -c 300 | tr '\t' ' ' || true)
  st=$(oi_status "$st" "$(oi_cpu_secs "$d/cpu0" "$d/cpu1")")
  [ "$st" = 124 ] && note="timeout ${TIMEOUT}s"
  printf '%s\t%s\t%s\t%s\t%s\t%s\n' "$id" "$st" "$(sha256sum < "$d/oi.out" | cut -d' ' -f1)" "$(wc -c < "$d/oi.out")" "$(echo "$t1 - $t0" | bc)" "$note"
}

suite_header() { # LEVEL: replace the first comment block of a results file
  local f=$1 text=$2
  sed -i '1,/^\[$/{/^\[$/!d}' "$f"; printf '%s\n' "$text" | cat - "$f" > "$f.new"; mv "$f.new" "$f"
}

# suite_results ORACLE-RESULTS BASE: the results file a suite check judges against: the oracle path's (--ocdump), or on the native path
# test/ocaml/BASE-native.edn once a --record wrote it (as TY19 (b)'s check-oi-native.edn: the two records side by side), until then the
# oracle path's (a native row that differs a CHANGED line); --record on the native path starts the native record from the oracle's rows
suite_results() {
  local o=$1 n=$root/test/ocaml/$2-native.edn
  if [ "${NATIVE:-0}" != 1 ]; then echo "$o"; return; fi
  if [ ! -f "$n" ] && [ "$RECORD" = 1 ]; then cp "$o" "$n"; fi
  if [ -f "$n" ]; then
    grep -q "^judge $2-native\.edn " "$root/test/ocaml/controls.txt" || sed -n "s/^judge $2\.edn /judge $2-native.edn /p" "$root/test/ocaml/controls.txt" >> "$root/test/ocaml/controls.txt"
    echo "$n"
  else echo "$o"; fi
}

# the native path's environment for the suite's dumps (TY24 (a)): the typer's process limits, its functions exported; the oracle's
# path (--ocdump) needs the tree (ocdump, lib/testing by ocamlc)
suite_native_env() {
  if [ "${NATIVE:-0}" != 1 ]; then need_tree; return; fi
  export OI_TYPER_THREADS=${OI_TYPER_THREADS:-1} OI_TYPER_GOMEMLIMIT=${OI_TYPER_GOMEMLIMIT:-1200MiB}
  export -f typer_ns arbace_exe die native_dir; export NATIVE OI_STDLIB TOP
}

cmd_suite_check() {
  opts "$@"
  if [ "$AOT" = 1 ] && [ "$LEVEL" = compiled ]; then cmd_suite_check_compiled; return; fi
  if [ "$AOT" = 1 ]; then cmd_suite_check_aot; return; fi
  cmd_stdlib > "$OI_DIR/stdlib.log"; cmd_build; cmd_suite_corpus > "$S/corpus.log"; suite_native_env
  export -f limited_cpu oi_wall oi_status oi_cpu_secs suite_oi_one suite_dump suite_dump_native suite_stage suite_names; export S OI_DIR TESTS TESTLIB TIMEOUT EXE LIB OCD GOMEMLIMIT root
  local out=$S/check.tsv
  suite_selected > "$S/selected.tsv"; export OI_SELECTED=$S/selected.tsv
  awk -F'\t' '$2 == "run" { printf "%s%c%s%c%s%c%s%c%s%c%s%c%s%c", $1, 0, $3, 0, $4, 0, $5, 0, $6, 0, $7, 0, $8, 0 }' "$OI_SELECTED" | xargs -0 -r -n 7 -P "$JOBS" bash -c 'suite_oi_one "$@"' _ > "$out.new"
  LC_ALL=C sort "$out.new" > "$out"; rm -f "$out.new"; oi_key "$S/corpus.tsv" > "$out.key"
  local mode=judge; [ "$RECORD" = 1 ] && mode=record
  { [ -n "$ONLY" ] || [ -n "$ONLY_LIST" ]; } && mode=$mode-only
  local res; res=$(suite_results "$SUITE_OI" check-suite-oi)
  OI_OWN=$(own_verdicts oi "$S/corpus.tsv" suite) "$(arbace_exe)" "$root/test/ocaml/oi_check.ae" "$mode" "$out" "$S/corpus.tsv" "$SUITE_REFS" "$res" "$SUITE_OI_CLASSES" || return $?
  if [ "$RECORD" = 1 ]; then
    suite_header "$res" ';; oi'"'"'s results over the suite corpus (bin/oi suite check --record; doc/ocaml/OCAML-SURVEY.md, "As built: O7 (write-ahead)"):
;; [PROGRAM RESULT REASON] as test/ocaml/check-oi.edn; the programs: the testsuite'"'"'s run tests O5'"'"'s corpus does not hold and the
;; opam set'"'"'s. Judged against ocamlc'"'"'s runs in test/ocaml/ref-suite.edn, classes test/ocaml/divergences-suite.edn.'
  fi
}

# the compiled level of the suite corpus (OT9's first fix; bin/oi's c2_programs): the units suite_dump dumps translated in the compiled
# style, built in shards and run alone; test/ocaml/check-suite-aot-compiled.edn, classes divergences-suite-aot-compiled.edn
cmd_suite_check_compiled() {
  cmd_stdlib > "$OI_DIR/stdlib.log"; cmd_build; cmd_suite_corpus > "$S/corpus.log"; cmd_aot_clib
  export -f limited_cpu oi_wall oi_status oi_cpu_secs suite_oi_one suite_dump suite_stage suite_names; export S OI_DIR TESTS TESTLIB UNIXLIB TIMEOUT EXE LIB OCD GOMEMLIMIT root
  suite_selected > "$S/selected.tsv"; export OI_SELECTED=$S/selected.tsv
  oi_rows_for "$S/check.tsv" "$S/oi.tsv" "$S/selected.tsv" suite_oi_one 6 "$S/corpus.tsv"
  C2=$S/compiled; rm -rf "$C2"; mkdir -p "$C2"
  C2_OVERLAY=$(cd "$root" && ARBACE_HOST_EXE=$(arbace_exe) bin/jrt overlay | tail -1) || die "bin/jrt overlay"
  suite_selected | awk -F'\t' '$2 == "run" { printf "%s%c%s%c%s%c%s%c%s%c", $1, 0, $3, 0, $4, 0, $7, 0, $8, 0 }' | xargs -0 -r -n 5 -P 4 bash -c 'suite_dump "$@"' _
  : > "$C2/cprogs.list"; : > "$C2/progs.tsv"
  local id kind flags incs args env base units d n=0 out=$C2/check.tsv
  : > "$out.new"
  while IFS=$'\037' read -r id kind flags incs args env base units; do
    d=$S/run/$id
    if [ -f "$d/error" ]; then printf '%s\t-\t-\t0\t0\tdump: %s\n' "$id" "$(cat "$d/error")" >> "$out.new"; continue; fi
    n=$((n + 1))
    # the library units from ocaml.u (OT26 (a), bin/oi's cmd_check_compiled), the seventh field the unix mark (OT25 (a))
    printf 'ocaml.p%s\t%s\t%s%s\n' "$n" "$d/out" "$(inc_units "$incs" $(cat "$d/units.list") | sed -e 's|.*/|@|' -e 's/\.ae$/\t/' | tr -d '\n')" "$(paste -sd'\t' "$d/units.list")" >> "$C2/cprogs.list"
    printf '%s\037p%s\037%s\037%s\037%s\037%s\037%s\n' "$id" "$n" "$d" "$args" "$env" "$(tail -1 "$d/units.list")" "$(case " $incs " in *" unix "*) echo 1 ;; *) echo 0 ;; esac)" >> "$C2/progs.tsv"
  done < <(suite_selected | awk -F'\t' '$2 == "run"' | tr '\t' '\037')
  c2_programs "$out.new"
  [ "${O2J_PLAN_ONLY:-}" = 1 ] && return 0
  LC_ALL=C sort "$out.new" > "$out"; rm -f "$out.new"
  [ "${O2J_KEEP:-}" = 1 ] || rm -f "$C2"/s*/prog
  local mode=judge; [ "$RECORD" = 1 ] && mode=record
  [ -n "$ONLY" ] && mode=$mode-only
  O2J_LEVEL=compiled O2J_KNOWN=$root/test/ocaml/aot-compiled-known.edn O2J_SLOW=$root/test/ocaml/aot-compiled-slow.edn \
    "$(arbace_exe)" "$root/test/ocaml/o2j_check.ae" "$mode" "$out" "$S/corpus.tsv" "$SUITE_REFS" "$S/oi.tsv" \
    "$root/test/ocaml/check-suite-aot-compiled.edn" "$root/test/ocaml/divergences-suite-aot-compiled.edn"
}

cmd_suite_check_aot() {
  cmd_stdlib > "$OI_DIR/stdlib.log"; cmd_build; cmd_suite_corpus > "$S/corpus.log"; cmd_aot_lib > "$O2J/aot-lib.log"; suite_native_env
  local jobs=${O2J_JOBS:-2}
  [ -n "${OPT_JOBS:-}" ] && jobs=$OPT_JOBS
  mkdir -p "$O2J/tmp"
  # oi's rows for the same programs, from this oi build (review 008's finding 1)
  export -f limited_cpu oi_wall oi_status oi_cpu_secs suite_oi_one suite_dump suite_dump_native suite_stage suite_names; export S OI_DIR TESTS TESTLIB TIMEOUT EXE LIB OCD GOMEMLIMIT root
  suite_selected > "$S/selected.tsv"; export OI_SELECTED=$S/selected.tsv
  oi_rows_for "$S/check.tsv" "$S/oi.tsv" "$S/selected.tsv" suite_oi_one 6 "$S/corpus.tsv"
  local A=$S/aot; rm -rf "$A"; mkdir -p "$A"
  # dump (many at once), translate (one process), run (shards)
  suite_selected | awk -F'\t' '$2 == "run" { printf "%s%c%s%c%s%c%s%c%s%c", $1, 0, $3, 0, $4, 0, $7, 0, $8, 0 }' | xargs -0 -r -n 5 -P 4 bash -c 'suite_dump "$@"' _
  local tl=$A/translate.list bl=$A/batch.list
  : > "$tl"; : > "$bl"
  suite_selected | awk -F'\t' '$2 == "run"' | tr '\t' '\037' | while IFS=$'\037' read -r id kind flags incs args env base units; do
    local d=$S/run/$id last b
    [ -f "$d/error" ] && continue
    last=$(echo "$units" | awk '{print $NF}'); b=$(basename "${last%.*}")
    printf '%s\t%s%s\n' "$A/out/$id" "$(inc_units "$incs" $(cat "$d/units.list") | sed 's/$/\t/' | tr -d '\n')" "$(paste -sd'\t' "$d/units.list")" >> "$tl"
    printf '%s\t%s\t%s\t%s\t%s\t%s\n' "$id" "$d/o2j.out" "$A/out/$id/files.txt" "$d/$(echo "${b:0:1}" | tr a-z A-Z)${b:1}.ae" "$(echo "$args" | tr ' ' '\t')" "$env" >> "$bl"
  done
  o2j progs "$O2J_LIB" "$tl" > "$A/translate.log" 2>&1 || { tail -5 "$A/translate.log"; die "translate"; }
  local i
  awk -F'\t' '$NF == ""' "$bl" | sed 's/\t$//' > "$bl.batch"
  split -n "l/$jobs" -d "$bl.batch" "$A/shard."
  : > "$O2J/crash.log"
  for i in "$A"/shard.[0-9]*; do o2j_shard "$i" "$i.res" & done
  wait
  cat "$A"/shard.*.res > "$A/results.tsv" 2>&1 || : > "$A/results.tsv"
  o2j_env_runs "$bl" "$A/results.tsv"
  local out=$A/check.tsv
  : > "$out.new"
  suite_selected | awk -F'\t' '$2 == "run" {print $1}' | while read -r id; do
    local d=$S/run/$id st secs note=""
    if [ -f "$d/error" ]; then printf '%s\t-\t-\t0\t0\tdump: %s\n' "$id" "$(cat "$d/error")" >> "$out.new"; continue; fi
    local r; r=$(grep -m1 -F "refused	$A/out/$id	" "$A/translate.log" | cut -f3 || true)
    if [ -n "$r" ]; then printf '%s\t-\t-\t0\t0\trefused: %s\n' "$id" "$r" >> "$out.new"; continue; fi
    st=$(awk -F'\t' -v id="$id" '$1 == id {print $2}' "$A/results.tsv" | head -1)
    secs=$(awk -F'\t' -v id="$id" '$1 == id {print $3}' "$A/results.tsv" | head -1)
    [ -f "$d/o2j.out" ] || : > "$d/o2j.out"
    note=$(grep -a -m1 -o 'o2j: .*' "$d/o2j.out" | head -c 300 | tr '\t' ' ' || true)
    case $st in timeout) note="timeout ${TIMEOUT}s of CPU"; st=124 ;; crash*) note="$st"; st=125 ;; "") note="no result"; st=125 ;; esac
    printf '%s\t%s\t%s\t%s\t%s\t%s\n' "$id" "$st" "$(sha256sum < "$d/o2j.out" | cut -d' ' -f1)" "$(wc -c < "$d/o2j.out")" "$secs" "$note" >> "$out.new"
  done
  LC_ALL=C sort "$out.new" > "$out"; rm -f "$out.new"; aot_key "$S/corpus.tsv" > "$out.key"
  local mode=judge; [ "$RECORD" = 1 ] && mode=record
  { [ -n "$ONLY" ] || [ -n "$ONLY_LIST" ]; } && mode=$mode-only
  local res; res=$(suite_results "$SUITE_AOT" check-suite-aot)
  OI_OWN=$(own_verdicts aot "$S/corpus.tsv" suite) "$(arbace_exe)" "$root/test/ocaml/o2j_check.ae" "$mode" "$out" "$S/corpus.tsv" "$SUITE_REFS" "$S/oi.tsv" "$res" "$SUITE_AOT_CLASSES" || return $?
  if [ "$RECORD" = 1 ]; then
    suite_header "$res" ';; The AOT level'"'"'s results over the suite corpus (bin/oi suite check --aot --record; doc/ocaml/OCAML-SURVEY.md, "As built: O7
;; (write-ahead)"): [PROGRAM RESULT REASON OI] as test/ocaml/check-aot.edn, at the light level (OC87); judged against ocamlopt'"'"'s runs in
;; test/ocaml/ref-suite.edn and oi'"'"'s in test/ocaml/check-suite-oi.edn, classes test/ocaml/divergences-suite-aot.edn.'
  fi
}

cmd_suite_divergences() {
  cmd_pins || return 1
  if [ "${1:-}" = --aot ]; then
    "$(arbace_exe)" "$root/test/ocaml/o2j_check.ae" divergences "$SUITE_AOT" "$SUITE_AOT_CLASSES"; return
  fi
  "$(arbace_exe)" "$root/test/ocaml/oi_check.ae" divergences "$SUITE_OI" "$SUITE_OI_CLASSES"
}

cmd_suite() {
  local verb=${1:-}; [ -n "$verb" ] || die "usage: bin/oi suite corpus|record-ref|check|divergences|actions|expect|rollup"
  shift
  case $verb in
    corpus) cmd_suite_corpus ;;
    key) [ "${OI_CHECK_DEFAULT:-native}" = native ] && NATIVE=1; cmd_suite_corpus > "$S/corpus.log"; oi_key "$S/corpus.tsv" ;;
    record-ref) cmd_suite_record_ref "$@" ;;
    check) cmd_suite_check "$@" ;;
    divergences) cmd_suite_divergences "$@" ;;
    actions) suite_actions ;;
    roles) cmd_suite_roles ;;
    expect) opts "$@"; cmd_suite_expect ;;
    rollup) cmd_suite_rollup "$@" ;;
    *) die "unknown suite verb $verb" ;;
  esac
}
