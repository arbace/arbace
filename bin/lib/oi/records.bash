# oi's records of ocamlc and its helper ocdump (doc/ORACLE-RECORDS-SURVEY.md, ORS12, "As built: OCaml"): sourced by bin/oi after $root,
# $TMPDIR, $TESTS, $TOP, $LIB and $TESTLIB are set. It is in bin/lib/oi/, which gate_key leaves out (no build reads it).
#
# What the oi checks asked of ocamlc and ocdump at run time is Lambda, class C (ORS1): the stdlib's units' (bin/oi stdlib), each run program's
# (oi_one, o2j_dump), the REPL session's phrases' (bin/lib/oi/phrase). They are packs and a committed record, read unless OI_LIVE=1:
#   test/ocaml/records/ocaml-lambda-stdlib.{sha256,tsv}   the stdlib's units (Stdlib*.ae, Camlinternal*.ae, Testing.ae, order.txt), one key
#   test/ocaml/records/ocaml-lambda-progs.{sha256,tsv}    the run corpus's programs, ID.ae, a key each (the source, the flags, the helper)
#   test/ocaml/records/repl/ and repl.tsv                 the session's phrases: KEY.status, KEY.ae, KEY.typed (committed: small)
# A program with no record is UNRECORDED and fails (ORS10 (a)); one whose key moved is STALE; a member whose digest differs is refused.
# The manifests' header names :ocaml's identity (ORS5 (a)); the live tools run only with OI_LIVE=1 (record, verify, the oracle tier).
. "$root/test/oracle-records/oracle.bash"

OIR_DIR=$root/test/ocaml/records
OIR_STDLIB=$OIR_DIR/ocaml-lambda-stdlib.sha256
OIR_PROGS=$OIR_DIR/ocaml-lambda-progs.sha256
OIR_REPL=$OIR_DIR/repl

# the helper's identity: its source and OCaml's pin (the toolchain's identity is the manifest header's, not the key)
oir_helper_key() { cat "$root/tools/ocdump/ocdump.ml" "$root/tools/ocdump/OCAML_VERSION" | sha256sum | cut -c1-64; }
# the pinned tarball's SHA-256 (test/ocaml/corpora.edn's first :sha256: the OCaml 5.5.0 release)
oir_tarball_sha() { sed -n 's/.*:sha256 "\([0-9a-f]*\)".*/\1/p' "$root/test/ocaml/corpora.edn" | head -1; }
oir_stdlib_key() { { echo lambda-stdlib-1; oir_helper_key; oir_tarball_sha; } | sha256sum | cut -c1-64; }
# oir_prog_key ID FLAGS INCS: the program's source, its flags and library, the helper
oir_prog_key() {
  { echo lambda-prog-1; oir_helper_key; sha256sum < "$TESTS/$1.ml" | cut -c1-64; echo "flags=$2 incs=$3"
    if [ -n "$3" ]; then sha256sum < "$TOP/testsuite/lib/testing.ml" | cut -c1-64; fi; } | sha256sum | cut -c1-64
}

# oir_member PIN ID KEY: prints the member's path; status 3 UNRECORDED, 4 STALE, 1 refused, 2 the pack is absent
oir_member() {
  local pin=$1 id=$2 key=$3 man line f st=0
  man=$(oracle_manifest "$pin")
  [ -f "$pin" ] && [ -f "$man" ] || { echo "UNRECORDED: no $pin (bin/oi record-lambda, by hand on a host with ocamlc)" >&2; return 3; }
  line=$(awk -F'\t' -v id="$id" '!/^#/ && $1 == id {print; exit}' "$man")
  [ -n "$line" ] || { echo "UNRECORDED $id: not in $man" >&2; return 3; }
  [ "$(printf '%s\n' "$line" | cut -f2)" = "$key" ] || { echo "STALE $id: the source, the flags or the helper changed since the record" >&2; return 4; }
  f=$(oracle_get "$pin" "$id") || st=$?
  [ "$st" = 0 ] || return "$st"
  echo "$f"
}

# lambda_prog ID FLAGS INCS DIR U: DIR/U.ae the program's simplified Lambda, from the pack, or (OI_LIVE=1) by ocdump in DIR; a failure is an
# `(ocaml/error ...` file, as a dump's own, so the callers' error rows say why
lambda_prog() {
  local id=$1 flags=$2 incs=$3 d=$4 u=$5 base f msg
  base=$(basename "$id")
  if [ -n "${OI_LIVE:-}" ]; then
    local I=""; [ -n "$incs" ] && I="-I $TESTLIB"
    (cd "$d" && "$OCD" --mode lambda --simplify $flags $I --prefix "$base" --file "$base.ml" "$base.ml" > "$u.ae" 2> "dump.log")
    return 0
  fi
  if f=$(oir_member "$OIR_PROGS" "$id.ae" "$(oir_prog_key "$id" "$flags" "$incs")" 2> "$d/records.err"); then cp "$f" "$d/$u.ae"
  else msg=$(tr '\n"' '  ' < "$d/records.err"); printf '(ocaml/error "%s")\n' "$msg" > "$d/$u.ae"; fi
}

export -f oir_helper_key oir_tarball_sha oir_prog_key oir_member lambda_prog oracle_main oracle_store oracle_pin_sha oracle_pin_name oracle_manifest oracle_pack_path oracle_get
export OIR_PROGS OIR_STDLIB root TOP TESTS TESTLIB OCD TMPDIR

# oir_stdlib_get: the stdlib's Lambda from the pack into $LIB (and Testing.ae into $TESTLIB); status as oir_member's
oir_stdlib_get() {
  local key u man id k
  key=$(oir_stdlib_key); man=$(oracle_manifest "$OIR_STDLIB")
  [ -f "$OIR_STDLIB" ] && [ -f "$man" ] || { echo "UNRECORDED: no $OIR_STDLIB (bin/oi stdlib --record, by hand on a host with ocamlc)" >&2; return 3; }
  while IFS=$'\t' read -r id k _; do
    case $id in '#'*|'') continue ;; esac
    [ "$k" = "$key" ] || { echo "STALE $id: the helper or the tarball's pin changed since the record" >&2; return 4; }
  done < "$man"
  u=$(oracle_unpack "$OIR_STDLIB") || return $?
  rm -rf "$LIB" "$TESTLIB"; mkdir -p "$LIB" "$TESTLIB"
  cp "$u/ocaml-lambda-stdlib/"*.ae "$u/ocaml-lambda-stdlib/order.txt" "$LIB/"
  mv "$LIB/Testing.ae" "$TESTLIB/Testing.ae"
  echo "pack $(oracle_pin_sha "$OIR_STDLIB")" > "$LIB/key"
}

# oir_pack_make PIN NAME DIR IDS CMD [dirs]: the pack NAME from DIR/NAME, IDS the "MEMBER<TAB>KEY" lines; the members the old pack holds and the new
# lines do not name are kept in place (a partial recording extends the pack); with `dirs` a member is replaced with its directory's
oir_pack_make() {
  local pin=$1 name=$2 dir=$3 ids=$4 cmd=$5 mode=${6:-} u id k
  if [ -f "$pin" ] && [ -f "$(oracle_manifest "$pin")" ]; then
    u=$(oracle_unpack "$pin") || return 1
    while IFS=$'\t' read -r id k _; do
      case $id in '#'*|'') continue ;; esac
      if [ "$mode" = dirs ]; then grep -q "^$(dirname "$id")/" "$ids" && continue; else grep -q "^$id	" "$ids" && continue; fi
      mkdir -p "$dir/$name/$(dirname "$id")"; cp "$u/$name/$id" "$dir/$name/$id"; printf '%s\t%s\n' "$id" "$k" >> "$ids"
    done < "$(oracle_manifest "$pin")"
  fi
  oracle_make "$pin" "$dir" "$ids" "$cmd" ocaml
}
oir_progs_make() { oir_pack_make "$OIR_PROGS" ocaml-lambda-progs "$1" "$2" "$3"; }
