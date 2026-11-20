# bin/oi's ocamldoc (queue 480, DECISION 478-2 (a); doc/ocaml/OCAML-SURVEY.md "As built: queue 480"): sourced by bin/oi. ocamldoc_build
# makes OCaml 5.5.0's ocamldoc on oi, oi_ocamldoc_prog a row of header.awk's ocamldoc mode, ocamldoc_output its output as ocamltest judges it.

# ocamldoc (queue 480, DECISION 478-2 (a)): OCaml 5.5.0's ocamldoc/ (odoc_info_SOURCES then ocamldoc_SOURCES, the Makefile's order) on oi
ODOC_UNITS="odoc_config odoc_messages odoc_global odoc_types odoc_misc odoc_text_parser odoc_text_lexer odoc_text odoc_name odoc_parameter odoc_value odoc_type odoc_extension odoc_exception odoc_class odoc_module odoc_print odoc_str odoc_comments_global odoc_parser odoc_lexer odoc_see_lexer odoc_env odoc_merge odoc_sig odoc_ast odoc_search odoc_scan odoc_cross odoc_comments odoc_dep odoc_analyse odoc_info odoc_dag2html odoc_to_text odoc_ocamlhtml odoc_html odoc_man odoc_latex_style odoc_latex odoc_texi odoc_dot odoc_gen odoc_args odoc"
ODOC=$OI_DIR/ocamldoc
export ODOC ODOC_UNITS

# ocamldoc_build (queue 480): ocamldoc made in .tmp/oi/ocamldoc once a library (its key, native_key's) and source, on the native path: the tarball's
# ocamldoc/ sources, its lexers (.mll) by the native ocamllex and its parsers (.mly) by the native ocamlyacc, each unit typed and
# translated by Arbace's typer against its .mli (--intf) and compiler-libs', Unix's, Str's and Dynlink's interfaces, its imports
# written for Dynlink's checks of a plugin (main.imports: the program's interface digests, OI_DYNLINK_IMPORTS); the program's units in
# link order (units.txt: compiler-libs' its units require, as complib_need, then unix, str and dynlink, then ocamldoc's but odoc.ml, the
# row's program); then the standard library's .cmi files ocamldoc types a file against (ocamltest's `-nostdlib -I stdlib`), made on oi
# by test/ocaml/ocamldoc/oicmi.ml (`ocamlc -c` of each stdlib .mli on compiler-libs: no ocamlc), in stdlib/. A lock keeps the check's
# jobs to one build. Status 0, else 1 with the reason on the standard output
ocamldoc_build() {
  [ "${NATIVE:-0}" = 1 ] || { echo "ocamldoc: on the native path alone"; return 1; }
  local w=$ODOC key
  key=$({ cat "$LIB/key"; sha256sum "$TOP"/ocamldoc/*.ml* "$TOP"/ocamldoc/*.mly "$TOP/stdlib/expand_module_aliases.awk" "$root/test/ocaml/ocamldoc/oicmi.ml"; declare -f ocamldoc_build ocamldoc_unit ocamldoc_cmis; echo "$ODOC_UNITS"; } | sha256sum | cut -d' ' -f1)
  mkdir -p "$OI_DIR"
  (
    flock 8
    [ -f "$w/key" ] && [ "$(cat "$w/key")" = "$key" ] && exit 0
    # (a build that failed at this key: its reason, not another build for each row)
    [ -f "$w/failed.key" ] && [ "$(head -1 "$w/failed.key")" = "$key" ] && { sed -n 2p "$w/failed.key"; exit 1; }
    odfail() { printf '%s\n%s\n' "$key" "$1" > "$w/failed.key"; echo "$1"; exit 1; }
    rm -rf "$w"; mkdir -p "$w/src" "$w/cmi"
    cp "$TOP"/ocamldoc/*.ml "$TOP"/ocamldoc/*.mli "$TOP"/ocamldoc/*.mll "$TOP"/ocamldoc/*.mly "$w/src/"
    local f
    for f in "$w"/src/*.mll; do (cd "$w/src" && "$root/bin/repr-ocaml" ocamllex -q -o "$(basename "$f" .mll).ml" "$(basename "$f")") > "$w/gen.log" 2>&1 || odfail "ocamldoc: ocamllex $(basename "$f")"; done
    for f in "$w"/src/*.mly; do (cd "$w/src" && "$root/bin/repr-ocaml" ocamlyacc "$(basename "$f")") > "$w/gen.log" 2>&1 || odfail "ocamldoc: ocamlyacc $(basename "$f")"; done
    cp "$root/test/ocaml/ocamldoc/oicmi.ml" "$w/cmi/"
    : > "$w/failed.txt"
    { for f in $ODOC_UNITS; do echo "$w/src $f"; done; echo "$w/cmi oicmi"; } |
      xargs -r -L 1 -P "${OI_LIB_JOBS:-3}" bash -c 'ocamldoc_unit "$1" "$2" || true' _
    if [ -s "$w/failed.txt" ]; then odfail "ocamldoc: typer: $(head -1 "$w/failed.txt" | tr '\t' ' ' | head -c 200)"; fi
    local m M units=()
    for m in $ODOC_UNITS; do M="$(echo "${m:0:1}" | tr a-z A-Z)${m:1}"; [ "$m" = odoc ] || units+=("$w/src/$M.ae"); cat "$w/src/$m.imports"; done > "$w/main.imports"
    { complib_need "${units[@]}" "$w/src/Odoc.ae"; inc_units "unix str dynlink"; printf '%s\n' "${units[@]}"; } > "$w/units.txt"
    { complib_need "$w/cmi/Oicmi.ae"; } > "$w/cmi/units.txt"
    local miss; miss=$(lib_missing $(cat "$w/units.txt")); [ -z "$miss" ] || odfail "ocamldoc: $miss"
    local cw; cw=$(ocamldoc_cmis "$w") || odfail "$cw"
    echo "$key" > "$w/key"
  ) 8> "$OI_DIR/ocamldoc.lock"
}

# ocamldoc_unit DIR UNIT: DIR/UNIT.ml typed and translated natively (against its .mli) into DIR/Unit.ae with DIR/UNIT.imports, or listed
# in $ODOC/failed.txt
ocamldoc_unit() {
  local d=$1 m=$2 M extra=""; M="$(echo "${m:0:1}" | tr a-z A-Z)${m:1}"
  [ -f "$d/$m.mli" ] && extra=--intf
  if (cd "$d" && OI_TYPER_TIMEOUT=${OI_LIB_TIMEOUT:-240} typer_ns lambda --simplify --debug $extra --stdlib "$(native_dir)" \
        --include "$d:$COMPLIB:$UNIXLIB:$STRLIB:$DYNLIB" --imports-out "$m.imports" --file "$m.ml" "$m.ml" > "$M.ae.new" 2> "$m.typer.err"); then
    mv "$d/$M.ae.new" "$d/$M.ae"; return 0
  fi
  rm -f "$d/$M.ae.new"; printf '%s\t%s\n' "$m" "$(typer_err_text "$d/$m.typer.err")" >> "$ODOC/failed.txt"; return 1
}

# ocamldoc_cmis DIR: the standard library's .cmi files in DIR/stdlib, each of stdlib/StdlibModules' units compiled by oicmi on oi with
# the flags stdlib/Makefile and stdlib/Compflags give it (COMPFLAGS' -strict-sequence -principal; stdlib.mli -nopervasives -no-alias-deps
# and camlinternalFormatBasics.mli -nopervasives, oo.mli -no-principal; stdlib.mli and the *Labels.mli preprocessed by
# expand_module_aliases.awk), named as the Makefile names them (stdlib__List.cmi); the list's order is the implementations', so a unit
# whose interface names a later one is compiled again on the next pass, until none is left
ocamldoc_cmis() {
  local w=$1 d=$1/stdlib b fl pre pp todo left
  mkdir -p "$d"
  todo=$(sed -n '/^STDLIB_MODULE_BASENAMES = /,/^$/p' "$TOP/stdlib/StdlibModules" | tr -d '\\' | tr -s ' \n' '\n' | grep -vx 'STDLIB_MODULE_BASENAMES\|=' | grep .)
  while :; do
    left=""
    for b in $todo; do
      fl="-strict-sequence -principal"; pre=$b; pp=0
      case $b in
        stdlib) fl="$fl -nopervasives -no-alias-deps"; pp=1 ;;
        camlinternalFormatBasics) fl="$fl -nopervasives" ;;
        camlinternal*) ;;
        *) pre="stdlib__$(echo "${b:0:1}" | tr a-z A-Z)${b:1}" ;;
      esac
      case $b in *Labels) pp=1 ;; oo) fl="-strict-sequence -no-principal" ;; esac
      if [ $pp = 1 ]; then awk -f "$TOP/stdlib/expand_module_aliases.awk" "$TOP/stdlib/$b.mli" > "$d/$b.mli"; else cp "$TOP/stdlib/$b.mli" "$d/"; fi
      # shellcheck disable=SC2046
      (cd "$d" && "$EXE" run "$LIB" $(cat "$w/cmi/units.txt") "$w/cmi/Oicmi.ae" -- -I . $fl -o "$pre.cmi" "$b.mli") > "$d/$b.log" 2>&1 || left="$left $b"
    done
    left=$(echo $left)
    [ -z "$left" ] && return 0
    [ "$left" = "$(echo $todo)" ] && { echo "ocamldoc: oicmi $(echo "$left" | cut -d' ' -f1).mli: $(head -c 200 "$d/$(echo "$left" | cut -d' ' -f1).log" | tr '\n\t' '  ')"; return 1; }
    todo=$left
  done
}
export -f ocamldoc_build ocamldoc_unit ocamldoc_cmis

# oi_ocamldoc_prog ID D PLUGINS (queue 480; header.awk's ocamldoc mode): ocamldoc built (ocamldoc_build), each plugin of the test
# (PLUGINS, comma-separated: FILE.ml) typed against ocamldoc's interfaces and written as the .cmo oi's Dynlink loads (OI-CMO 1, as
# oi_dynlink's), ocamldoc's main unit the row's program D/U.ae; the run's arguments on the standard output (ocamltest's: -nostdlib
# -I STDLIB, -g each plugin's .cmo, the test's file, -o TEST.result). Status 0, else 1 with the row's reason
oi_ocamldoc_prog() {
  local id=$1 d=$2 plugins=$3 src why f b U base u gs=""
  src=$(dirname "$TESTS/$id"); base=$(basename "$id"); u=$(echo "${base:0:1}" | tr a-z A-Z)${base:1}
  why=$(ocamldoc_build) || { echo "${why:-ocamldoc: the build failed}"; return 1; }
  for f in $(echo "$plugins" | tr ',' ' '); do
    b=${f%.ml}; U=$(echo "${b:0:1}" | tr a-z A-Z)${b:1}
    cp "$src/$f" "$d/"
    if ! (cd "$d" && typer_ns lambda --simplify --stdlib "$(native_dir)" --include "$d:$ODOC/src:$COMPLIB:$UNIXLIB:$STRLIB:$DYNLIB" \
            --imports-out "$b.imports" --file "$f" "$f" > "$U.ae" 2> "typer-$b.err"); then
      echo "dump: typer $f: $(typer_err_text "$d/typer-$b.err")"; return 1
    fi
    { echo "OI-CMO 1"; sed 's/^/import /' "$d/$b.imports"; echo lambda; cat "$d/$U.ae"; } > "$d/$b.cmo"
    gs="$gs -g $b.cmo"
  done
  cp "$ODOC/src/Odoc.ae" "$d/$u.ae"
  echo "-nostdlib -I $ODOC/stdlib$gs $base.ml -o $base.result"
}
export -f oi_ocamldoc_prog

# ocamldoc_of ENV: 1 when ENV is a row of header.awk's ocamldoc mode (@ocamldoc=1, queue 480), else nothing; plugins_of ENV its plugins
ocamldoc_of() { case " $1 " in *" @ocamldoc=1 "*) echo 1 ;; esac; }
plugins_of() { local kv; for kv in $1; do case $kv in @plugins=*) echo "${kv#@plugins=}" ;; esac; done; }
# ocamldoc_output ID D OUT: the row's output as ocamltest's checks of an ocamldoc test judge it: TEST.result (check-program-output against
# TEST.reference) in OUT, ocamldoc's own output (OUT as the run wrote it) kept in OUT.tool and compared with TEST.ocamldoc.reference (a
# missing reference: no output, check-ocamldoc-output); when they differ, OUT is the result followed by that output, so the row fails
ocamldoc_output() {
  local id=$1 d=$2 out=$3 base ref
  base=$(basename "$id"); ref=$TESTS/$id.ocamldoc.reference
  mv "$out" "$out.tool"
  if [ -f "$d/$base.result" ]; then cp "$d/$base.result" "$out"; else : > "$out"; fi
  if [ -f "$ref" ]; then cmp -s "$ref" "$out.tool" || { echo "oi: ocamldoc's output differs from $base.ocamldoc.reference:"; cat "$out.tool"; } >> "$out"
  elif [ -s "$out.tool" ]; then { echo "oi: ocamldoc's output (no $base.ocamldoc.reference):"; cat "$out.tool"; } >> "$out"; fi
  return 0
}
export -f ocamldoc_of plugins_of ocamldoc_output

