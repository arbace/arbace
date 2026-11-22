# bin/oi's tool mode (queue 511): sourced by bin/oi. A row of header.awk's tool mode (@tool=NAME in its environment) runs one of
# ocamltest's tools as Arbace's own program on oi, judged by the test's own .reference: oi_tool_prog NAME ID D COPY prepares the row
# (the test's files, the tool's program as the row's D/U.ae), tool_output NAME ID D OUT judges the tool's side output beside the
# row's. Outside bin/oi, so its edits leave native_key alone (queue 480's finding 2). Tools: objinfo (header.awk's objinfo mode).

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

# oi_tool_prog NAME ID D COPY: the row's program for the tool NAME in its run directory D, the test's files (COPY, comma-separated: the
# readonly files and the modules) copied from its source directory; objinfo: the standard library's .cmi files linked as D/_stdlib and
# oiobjinfo dumped as a program of the row's path (oi_dump, natively) into D/U.ae, U the test's unit. Status 0, else 1 with the reason
oi_tool_prog() {
  local name=$1 id=$2 d=$3 copy=$4 src base u f why
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
  esac
  return 0
}
export -f tool_of objinfo_build oi_tool_prog tool_output
