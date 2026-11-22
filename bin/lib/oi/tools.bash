# bin/oi's tool mode (queue 511): sourced by bin/oi. A row of header.awk's tool mode (@tool=NAME in its environment) runs one of
# ocamltest's tools as Arbace's own program on oi, judged by the test's own .reference: oi_tool_prog NAME ID D COPY prepares the row
# (the test's files, the tool's program as the row's D/U.ae), tool_output NAME ID D OUT judges the tool's side output beside the
# row's. Outside bin/oi, so its edits leave native_key alone (queue 480's finding 2). Tools: objinfo (header.awk's objinfo mode); ocamltex
# and toplevel (queue 529, header.awk's toplevel modes: the toplevel on oi, toplevel_build).

# tool_of ENV: the row's tool (@tool=NAME), else nothing
tool_of() { local kv; for kv in $1; do case $kv in @tool=*) echo "${kv#@tool=}" ;; esac; done; }

OIOBJ=$OI_DIR/objinfo
export OIOBJ

# objinfo_build: the standard library's .cmi files the compiles of oiobjinfo read (ocamltest's `-nostdlib -I stdlib`), made on oi by
# test/ocaml/ocamldoc/oicmi.ml as ocamldoc_build makes them (ocamldoc_cmis), in .tmp/oi/objinfo/stdlib, once a library (native_key's)
# and source; a lock keeps the check's jobs to one build. Status 0, else 1 with the reason on the standard output
objinfo_build() {
  [ "${NATIVE:-0}" = 1 ] || { echo "objinfo: on the native path alone"; return 1; }
  local w=$OIOBJ key
  key=$({ cat "$LIB/key"; sha256sum "$TOP"/stdlib/*.mli "$TOP/stdlib/StdlibModules" "$TOP/stdlib/expand_module_aliases.awk" "$root/test/ocaml/ocamldoc/oicmi.ml"; declare -f objinfo_build ocamldoc_unit ocamldoc_cmis; } | sha256sum | cut -d' ' -f1)
  mkdir -p "$OI_DIR"
  (
    flock 8
    [ -f "$w/key" ] && [ "$(cat "$w/key")" = "$key" ] && exit 0
    [ -f "$w/failed.key" ] && [ "$(head -1 "$w/failed.key")" = "$key" ] && { sed -n 2p "$w/failed.key"; exit 1; }
    oifail() { printf '%s\n%s\n' "$key" "$1" > "$w/failed.key"; echo "$1"; exit 1; }
    rm -rf "$w"; mkdir -p "$w/cmi"
    cp "$root/test/ocaml/ocamldoc/oicmi.ml" "$w/cmi/"
    # (ocamldoc_unit lists a failure in $ODOC/failed.txt)
    ODOC=$w; : > "$w/failed.txt"
    ocamldoc_unit "$w/cmi" oicmi || oifail "objinfo: typer: oicmi: $(cut -f2 "$w/failed.txt" | head -c 200)"
    complib_need "$w/cmi/Oicmi.ae" > "$w/cmi/units.txt"
    local miss; miss=$(lib_missing $(cat "$w/cmi/units.txt")); [ -z "$miss" ] || oifail "objinfo: $miss"
    local cw; cw=$(ocamldoc_cmis "$w") || oifail "objinfo: ${cw#ocamldoc: }"
    echo "$key" > "$w/key"
  ) 8> "$OI_DIR/objinfo.lock"
}

# the toplevel on oi (queue 529, DECISION 528-7 (a); doc/ocaml/OCAML-SURVEY.md "As built: queue 529"): compiler-libs' ocamltoplevel,
# OCaml 5.5.0's toplevel/ (ocamltoplevel_SOURCES' bytecode units, the Makefile's order) with Arbace's own topeval.ml (a phrase's Lambda
# loaded by oi's Dynlink externals, test/ocaml/toplevel/topeval.ml) and trace.ml (no code pointer to patch), and tools/ocamltex on it
TOPLIB=$OI_DIR/toplevel
TOPLIB_UNITS="genprintval topcommon topeval trace toploop topprinters topdirs topmain"
export TOPLIB TOPLIB_UNITS

# toplevel_build: the toplevel's units typed and translated natively in .tmp/oi/toplevel/src (each against its .mli: topeval's,
# trace's and topmain's the shared toplevel/*.mli, as the Makefile copies them) with compiler-libs', Unix's and Str's interfaces, and
# tools/ocamltex.ml (with its .mli, -no-alias-deps as the Makefile compiles it) in .tmp/oi/toplevel/tex, once a library (its key,
# native_key's) and source; a lock keeps the check's jobs to one build. Status 0, else 1 with the reason on the standard output
toplevel_build() {
  [ "${NATIVE:-0}" = 1 ] || { echo "toplevel: on the native path alone"; return 1; }
  local w=$TOPLIB key
  key=$({ cat "$LIB/key"; sha256sum "$TOP"/toplevel/*.ml* "$TOP"/toplevel/byte/*.ml "$root"/test/ocaml/toplevel/*.ml "$TOP"/tools/ocamltex.ml*; declare -f toplevel_build toplevel_unit; echo "$TOPLIB_UNITS"; } | sha256sum | cut -d' ' -f1)
  mkdir -p "$OI_DIR"
  (
    flock 8
    [ -f "$w/key" ] && [ "$(cat "$w/key")" = "$key" ] && exit 0
    [ -f "$w/failed.key" ] && [ "$(head -1 "$w/failed.key")" = "$key" ] && { sed -n 2p "$w/failed.key"; exit 1; }
    tlfail() { printf '%s\n%s\n' "$key" "$1" > "$w/failed.key"; echo "$1"; exit 1; }
    rm -rf "${w:?}"; mkdir -p "$w/src" "$w/tex"
    local m
    for m in genprintval topcommon toploop topprinters topdirs; do cp "$TOP/toplevel/$m.ml" "$TOP/toplevel/$m.mli" "$w/src/"; done
    cp "$TOP"/toplevel/topeval.mli "$TOP"/toplevel/trace.mli "$TOP"/toplevel/topmain.mli "$TOP/toplevel/byte/topmain.ml" \
       "$root/test/ocaml/toplevel/topeval.ml" "$root/test/ocaml/toplevel/trace.ml" "$w/src/"
    cp "$TOP/tools/ocamltex.ml" "$TOP/tools/ocamltex.mli" "$w/tex/"
    : > "$w/failed.txt"
    { for m in $TOPLIB_UNITS; do echo "$w/src $m"; done; echo "$w/tex ocamltex --no-alias-deps"; } |
      xargs -r -L 1 -P "${OI_LIB_JOBS:-3}" bash -c 'toplevel_unit "$1" "$2" "${3:-}" || true' _
    [ -s "$w/failed.txt" ] && tlfail "toplevel: typer: $(head -1 "$w/failed.txt" | tr '\t' ' ' | head -c 200)"
    for m in $TOPLIB_UNITS; do echo "$w/src/$(echo "${m:0:1}" | tr a-z A-Z)${m:1}.ae"; done > "$w/units.txt"
    local miss; miss=$(lib_missing $(cat "$w/units.txt") "$w/tex/Ocamltex.ae"); [ -z "$miss" ] || tlfail "toplevel: $miss"
    echo "$key" > "$w/key"
  ) 8> "$OI_DIR/toplevel.lock"
}

# toplevel_unit DIR UNIT [FLAG]: DIR/UNIT.ml typed and translated natively (against its .mli) into DIR/Unit.ae, or listed in
# $TOPLIB/failed.txt
toplevel_unit() {
  local d=$1 m=$2 fl=${3:-} M; M="$(echo "${m:0:1}" | tr a-z A-Z)${m:1}"
  if (cd "$d" && OI_TYPER_TIMEOUT=${OI_LIB_TIMEOUT:-240} typer_ns lambda --simplify --debug --intf $fl --stdlib "$(native_dir)" \
        --include "$d:$TOPLIB/src:$COMPLIB:$UNIXLIB:$STRLIB" --file "$m.ml" "$m.ml" > "$M.ae.new" 2> "$m.typer.err"); then
    mv "$d/$M.ae.new" "$d/$M.ae"; return 0
  fi
  rm -f "${d:?}/${M:?}.ae.new"; printf '%s\t%s\n' "$m" "$(typer_err_text "$d/$m.typer.err")" >> "$TOPLIB/failed.txt"; return 1
}

# toplib_need DUMP...: the toplevel's units the dumps require and those they require, in TOPLIB_UNITS' order
toplib_need() {
  local -A seen; local q=() m u M
  for u in "$@"; do while read -r m; do [ -n "$m" ] && q+=("$m"); done <<< "$(dump_required "$u")"; done
  while [ ${#q[@]} -gt 0 ]; do
    m=${q[0]}; q=("${q[@]:1}"); [ -n "${seen[$m]:-}" ] && continue; seen[$m]=1
    [ -f "$TOPLIB/src/$m.ae" ] || continue
    while read -r u; do [ -n "$u" ] && q+=("$u"); done <<< "$(dump_required "$TOPLIB/src/$m.ae")"
  done
  for m in $TOPLIB_UNITS; do M="$(echo "${m:0:1}" | tr a-z A-Z)${m:1}"; [ -n "${seen[$M]:-}" ] && echo "$TOPLIB/src/$M.ae"; done
}

# env_val ENV KEY: the value of KEY=VALUE (an @KEY of header.awk's) in a row's environment, else nothing
env_val() { local kv; for kv in $1; do case $kv in "$2="*) echo "${kv#"$2"=}" ;; esac; done; }

export -f toplevel_build toplevel_unit toplib_need env_val

# oi_tool_prog NAME ID D COPY ENV: the row's program for the tool NAME in its run directory D, the test's files (COPY, comma-separated: the
# readonly files and the modules) copied from its source directory; objinfo: the standard library's .cmi files linked as D/_stdlib and
# oiobjinfo dumped as a program of the row's path (oi_dump, natively) into D/U.ae, U the test's unit. ocamltex and toplevel (queue 529):
# below; their units in D/tool.units (bin/oi's oi_one runs them before the program). Status 0, else 1 with the reason
oi_tool_prog() {
  local name=$1 id=$2 d=$3 copy=$4 env=${5:-} src base u f why
  src=$(dirname "$TESTS/$id"); base=$(basename "$id"); u=$(echo "${base:0:1}" | tr a-z A-Z)${base:1}
  case $name in
    objinfo)
      why=$(objinfo_build) || { echo "${why:-objinfo: the build failed}"; return 1; }
      for f in $(echo "$copy" | tr ',' ' '); do [ -e "$src/$f" ] && cp -r "$src/$f" "$d/"; done
      ln -sfn "$OIOBJ/stdlib" "$d/_stdlib"
      mkdir -p "$d/_tool"; cp "$root/test/ocaml/objinfo/oiobjinfo.ml" "$d/_tool/"
      why=$(oi_dump "$d/_tool" oiobjinfo "" complib) || { echo "objinfo: $why"; return 1; }
      mv "$d/_tool/Oiobjinfo.ae" "$d/$u.ae"
      # (the test's own source is an input, not the program: oi_one copied it beside)
      return 0 ;;
    ocamltex)
      # tools/ocamltex as ocamltest's script runs it (`ocamltex -repo-root ${ocamlsrcdir} TEST.input -o TEST.output`): the repo root
      # D/_root, its stdlib/ the standard library's .cmi files made on oi (objinfo_build's); the program linked as the Makefile links
      # it, -linkall: every unit of the standard library, Unix, Str, compiler-libs' ocamlcommon (bin/oi's COMPLIB_UNITS) and the toplevel
      why=$(objinfo_build) || { echo "${why:-objinfo: the build failed}"; return 1; }
      why=$(toplevel_build) || { echo "${why:-toplevel: the build failed}"; return 1; }
      for f in $(echo "$copy" | tr ',' ' '); do cp -r "$src/$f" "$d/"; done
      mkdir -p "$d/_root"; ln -sfn "$OIOBJ/stdlib" "$d/_root/stdlib"
      cp "$TOPLIB/tex/Ocamltex.ae" "$d/$u.ae"
      local lib=${LIB_NATIVE:-$LIB} m
      { sed "s|^\([^ ]*\).*|$lib/\1.ae|" "$lib/order.txt"; inc_units "unix str"
        for m in $COMPLIB_UNITS; do echo "$COMPLIB/$(echo "${m:0:1}" | tr a-z A-Z)${m:1}.ae"; done; cat "$TOPLIB/units.txt"; } > "$d/tool.units"
      return 0 ;;
    toplevel)
      # self-contained-toplevel's header: the modules (@topmods=FILE.ml,...) compiled, each its .cmi by `ocamlc -c` on oi (oiobjinfo:
      # compiler-libs' Typemod writes it, the .cmi the test's programs read with Cmi_format) and its Lambda by the typer; the `ocaml`
      # script action (@topscript=FILE.ml:ARG,..., `ocaml with ocamlcommon`) an oi run of the script over compiler-libs (DECISION 528-7
      # (a)); the link of @topall=FILE,... (a .cmo the module's unit, a .ml typed in order, the last the test's own, D/U.ae) with
      # ocamlcommon, ocamlbytecomp and ocamltoplevel: the units its dumps require. The run's OCAMLLIB is D/_stdlib (header.awk's), the
      # standard library's .cmi files made on oi
      why=$(objinfo_build) || { echo "${why:-objinfo: the build failed}"; return 1; }
      why=$(toplevel_build) || { echo "${why:-toplevel: the build failed}"; return 1; }
      for f in $(echo "$copy" | tr ',' ' '); do cp -r "$src/$f" "$d/"; done
      ln -sfn "$OIOBJ/stdlib" "$d/_stdlib"
      local lib=${LIB_NATIVE:-$LIB} b B sf sa own=""
      mkdir -p "$d/_tool"; cp "$root/test/ocaml/objinfo/oiobjinfo.ml" "$d/_tool/"
      why=$(oi_dump "$d/_tool" oiobjinfo "" complib) || { echo "toplevel: oiobjinfo: $why"; return 1; }
      for f in $(env_val "$env" @topmods | tr ',' ' '); do
        b=${f%.ml}; B=$(echo "${b:0:1}" | tr a-z A-Z)${b:1}
        # shellcheck disable=SC2046
        (cd "$d" && limited_cpu "$TIMEOUT" "$(oi_wall)" "$EXE" run "$lib" $(inc_units complib "$d/_tool/Oiobjinfo.ae") _tool/Oiobjinfo.ae -- -c -nostdlib -I _stdlib "$f" -objinfo) > "$d/ocamlc-$b.log" 2>&1 \
          || { echo "toplevel: ocamlc -c $f on oi: $(cat "$d/ocamlc-$b.log" "$d/ocamlc.out" 2>/dev/null | head -c 200 | tr '\n\t' '  ')"; return 1; }
        (cd "$d" && typer_ns lambda --simplify --stdlib "$(native_dir)" --include "$d" --cmi-out "$b.cmi.ae" --file "$f" "$f" > "$B.ae" 2> "typer-$b.err") \
          || { echo "dump: typer $f: $(typer_err_text "$d/typer-$b.err")"; return 1; }
      done
      sf=$(env_val "$env" @topscript); sa=${sf#*:}; sf=${sf%%:*}; b=${sf%.ml}
      if [ -n "$sf" ]; then
        why=$(oi_dump "$d" "$b" "" complib) || { echo "toplevel: the ocaml script $sf: $why"; return 1; }
        B=$(echo "${b:0:1}" | tr a-z A-Z)${b:1}
        # shellcheck disable=SC2046
        (cd "$d" && limited_cpu "$TIMEOUT" "$(oi_wall)" "$EXE" run "$lib" $(inc_units complib "$d/$B.ae") "$B.ae" -- $(echo "$sa" | tr ',' ' ')) > "$d/script-$b.log" 2>&1 \
          || { echo "toplevel: the ocaml script $sf on oi: $(head -c 200 "$d/script-$b.log" | tr '\n\t' '  ')"; return 1; }
        mv "$d/$B.ae" "$d/$B.script.ae"
      fi
      local all last; all=$(env_val "$env" @topall | tr ',' ' '); last=${all##* }
      [ "$last" = "$base.ml" ] || { echo "toplevel: the link's last unit $last is not the test's"; return 1; }
      for f in $all; do
        b=${f%.*}; B=$(echo "${b:0:1}" | tr a-z A-Z)${b:1}
        case $f in
          *.cmo) [ -f "$d/$B.ae" ] || { echo "toplevel: $f was not compiled"; return 1; }; own="$own $d/$B.ae" ;;
          "$last") (cd "$d" && typer_ns lambda --simplify --stdlib "$(native_dir)" --include "$d:$COMPLIB:$TOPLIB/src" --file "$f" "$f" > "$u.ae" 2> "typer-$b.err") \
                     || { echo "dump: typer $f: $(typer_err_text "$d/typer-$b.err")"; return 1; } ;;
          *.ml) (cd "$d" && typer_ns lambda --simplify --stdlib "$(native_dir)" --include "$d" --cmi-out "$b.cmi.ae" --file "$f" "$f" > "$B.ae" 2> "typer-$b.err") \
                  || { echo "dump: typer $f: $(typer_err_text "$d/typer-$b.err")"; return 1; }; own="$own $d/$B.ae" ;;
          *) echo "toplevel: the link's unit $f"; return 1 ;;
        esac
      done
      local tl; tl=$(toplib_need "$d/$u.ae")
      # shellcheck disable=SC2086
      { complib_need "$d/$u.ae" $tl; echo "$tl"; printf '%s\n' $own; } | grep . > "$d/tool.units"
      return 0 ;;
    *) echo "tool: $name is not a tool of bin/oi"; return 1 ;;
  esac
}

# tool_output NAME ID D OUT: the tool's side output judged as ocamltest judges it, appended to the row's output OUT when it differs, so
# the row fails; objinfo: the compiler's output (D/ocamlc.out) against TEST.compilers.reference (check-ocamlc.byte-output; none: no output)
tool_output() {
  local name=$1 id=$2 d=$3 out=$4 ref
  case $name in
    objinfo)
      ref=$TESTS/$id.compilers.reference
      if [ -f "$ref" ]; then cmp -s "$ref" "$d/ocamlc.out" || { echo "oi: the compiler's output differs from $(basename "$ref"):"; cat "$d/ocamlc.out" 2>/dev/null; } >> "$out"
      elif [ -s "$d/ocamlc.out" ]; then { echo "oi: the compiler's output (no $(basename "$id").compilers.reference):"; cat "$d/ocamlc.out"; } >> "$out"; fi ;;
    ocamltex)
      # (queue 529) ocamltest's script action writes the script's output and errors into the test's output file after ocamltex's own
      # (-o TEST.output, check-program-output's against TEST.reference): the row's output is TEST.output, then the run's own (none on success)
      local base; base=$(basename "$id")
      mv "$out" "$out.tool"
      if [ -f "$d/$base.output" ]; then cp "$d/$base.output" "$out"; else : > "$out"; fi
      cat "$out.tool" >> "$out" ;;
  esac
  return 0
}
export -f tool_of objinfo_build oi_tool_prog tool_output
