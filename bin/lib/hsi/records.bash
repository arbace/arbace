# hsi's records of GHC and its helper (doc/ORACLE-RECORDS-SURVEY.md, ORS12, "As built: Haskell"): sourced by bin/hsi, bin/lib/hsi/check and
# bin/lib/hsi/aot after $root and $TMPDIR are set. bin/lib/hsi/ is out of bin/arbace-go's gate_key. Two packs (class C: GHC's STG,
# the front-end product hsi evaluates), outside the clone in the store (test/oracle-records/oracle.bash), pinned by
# test/haskell/records/NAME.sha256 with their manifests NAME.tsv beside:
#   haskell-stg-libs    PKG/MODULE.stg.ae, the boot libraries' STG as bin/hsi dumps makes it (hadrian's flags, -O2); the layout of
#                       .tmp/hsi/dumps/stg, a library directory of hsi's --lib; keyed per package: lib_keys below
#   haskell-stg-progs   ID/stg/..., ID the program's id (samples/NAME, testsuite/..., nofib/..), the helper's -O0 STG of it as bin/hsi
#                       check makes it (a program that does not compile: ID/dump-failed.txt, the helper's first error); keyed per
#                       program by the key check's prog_key computes (the sources, the flags, the helper), what .tmp/hsi/progs/ID/KEY names
# By default the libraries and programs are read from the packs; HSI_LIVE=1 (bin/hsi check --live, run --live) the helper and the
# dumps directories as before (HSI_LIBS names others in any case). An entry whose key changed is STALE, an absent one UNRECORDED, a
# member whose digest differs from the manifest's REFUSED: each fails (ORS10 (a)).
#
#   hsi_ae ARGS...             stage 0 on the checkout's namespaces (bin/hsi exe)
#   hsi_lib_keys               "PKG<TAB>KEY" for each package of the dumps plan (HSI_BIGNUM, default native)
#   hsi_libs                   the colon-joined library directories: HSI_LIBS; HSI_LIVE=1: the dumps directories; else the pack's, its
#                              keys checked
#   hsi_record_libs DUMPDIR [PKG,...]    DUMPDIR/stg's packages (those named) packed into haskell-stg-libs (the others kept)
#   hsi_progs_dir              the unpacked haskell-stg-progs (checked), or fails
#   hsi_ifaces_dir             the unpacked haskell-ifaces-ghc-9.14.1 (checked, every member keyed by the helper's identity), or fails
#   hsi_record_ifaces DIR CMD  DIR's interfaces (M.iface.edn, reexports.edn) packed into haskell-ifaces-ghc-9.14.1
# A third pack (HFS-I1 (a), doc/haskell/FRONTEND-SURVEY.md, "As built: HFS-I1 (a)"): haskell-ifaces-ghc-9.14.1, the boot libraries'
# interfaces as the helper's iface mode dumps them (M.iface.edn: the closure of the run corpora's imports) and ghc-pkg's reexports
# (reexports.edn), the native front end's input (bin/hsi check --front native); keyed by the helper's identity.
. "$root/bin/lib/repr-haskell/records.bash"
hsi_libs_pin=$root/test/haskell/records/haskell-stg-libs.sha256
hsi_progs_pin=$root/test/haskell/records/haskell-stg-progs.sha256
hsi_ifaces_pin=$root/test/haskell/records/haskell-ifaces-ghc-9.14.1.sha256
hsi_work=$root/.tmp/hsi
hsi_corpora_edn=$root/test/haskell/corpus-ghc-9.14.1.edn

hsi_ae() { ARBACE_PATH=$root "$("$root/bin/hsi" exe)" "$@"; }

hsi_lib_keys() {
  local tb dt plan bn=${HSI_BIGNUM:-native} line name
  tb=$(hsi_ae -e "(println (:sha256 (:tarball (read-string (slurp \"$hsi_corpora_edn\")))))")
  # what the dumps' flags depend on besides the plan: bin/hsi's functions that build them, the generated files the plan copies
  dt=$(sed -n '/^unit_of() {/,/^}/p;/^prefix_rel() {/,/^}/p;/^strip_srcdir() {/,/^}/p;/^dump_package() {/,/^}/p' "$root/bin/hsi" | sha256sum | cut -c1-64)
  plan=$TMPDIR/hsi-libplan.$$
  hsi_ae "$root/test/haskell/eval/dump_plan.ae" "$hsi_corpora_edn" "$bn" > "$plan"
  while IFS= read -r line; do
    name=${line%%$'\037'*}
    printf '%s\t%s\n' "$name" "$({ echo "$tb"; hsr_helper_hash; echo "$bn"; echo "$dt"; printf '%s\n' "$line"
      (cd "$root/test/haskell/generated" && find . -type f | LC_ALL=C sort | xargs sha256sum); } | sha256sum | cut -c1-64)"
  done < "$plan"
  rm -f "$plan"
}

hsi_libs() {
  if [ -n "${HSI_LIBS:-}" ]; then echo "$HSI_LIBS"; return 0; fi
  if [ -n "${HSI_LIVE:-}" ]; then echo "$hsi_work/dumps/stg:$hsi_work/dumps-extra/stg:$hsi_work/dumps-more/stg"; return 0; fi
  local man=$(oracle_manifest "$hsi_libs_pin") u keys=$TMPDIR/hsi-libkeys.$$ pk want have
  [ -f "$hsi_libs_pin" ] && [ -f "$man" ] || { echo "hsi: UNRECORDED: no $hsi_libs_pin (bin/hsi dumps --record, by hand on a host with GHC; HSI_LIVE=1 or --live the dumps directories)" >&2; return 1; }
  u=$(oracle_unpack "$hsi_libs_pin") || return 1
  hsi_lib_keys > "$keys"
  # every package of the pack keyed as the plan keys it now
  while IFS=$'\t' read -r pk have; do
    want=$(awk -F'\t' -v p="$pk" '$1 == p {print $2}' "$keys")
    [ -n "$want" ] || { echo "hsi: STALE $pk: the pack holds a package the plan no longer has" >&2; rm -f "$keys"; return 1; }
    [ "$want" = "$have" ] || { echo "hsi: STALE $pk: its sources' pin, the helper, hsi dumps' flags or the plan changed since the record (bin/hsi dumps --record)" >&2; rm -f "$keys"; return 1; }
  done < <(awk -F'\t' '!/^#/ && NF {split($1, a, "/"); if (!(a[1] in s)) {s[a[1]] = 1; print a[1] "\t" $2}}' "$man")
  rm -f "$keys"
  echo "$u/$(oracle_pin_name "$hsi_libs_pin")"
}

# hsi_record_libs DUMPDIR [PKG,...] [COMMAND]
hsi_record_libs() {
  local dd=$1 only=${2:-} how=${3:-} pin=$hsi_libs_pin stage=$hsi_work/records/libs name man d keys pk
  name=$(oracle_pin_name "$pin"); man=$(oracle_manifest "$pin")
  rm -rf "$stage"; mkdir -p "$stage/$name"; : > "$stage/ids"
  if [ -f "$man" ] && [ -f "$pin" ] && d=$(oracle_unpack "$pin" 2>"$TMPDIR/hsi-records.err"); then
    cp -a "$d/$name/." "$stage/$name/"; awk -F'\t' '!/^#/ && NF {print $1 "\t" $2}' "$man" > "$stage/ids"
  fi
  keys=$stage/keys; hsi_lib_keys > "$keys"
  for pk in $(ls "$dd/stg"); do
    [ "$pk" != samples ] || continue
    if [ -n "$only" ] && [[ ",$only," != *",$pk,"* ]]; then continue; fi
    awk -F'\t' -v p="$pk" '$1 == p {f = 1} END {exit !f}' "$keys" || { echo "hsi record: $pk is no package of the plan" >&2; return 1; }
    awk -F'\t' -v p="$pk/" 'index($1, p) != 1' "$stage/ids" > "$stage/ids.new"; mv "$stage/ids.new" "$stage/ids"
    rm -rf "${stage:?}/$name/$pk"; mkdir -p "$stage/$name/$pk"; cp "$dd/stg/$pk/"*.stg.ae "$stage/$name/$pk/"
    (cd "$stage/$name" && ls "$pk"/*.stg.ae) | while IFS= read -r m; do printf '%s\t%s\n' "$m" "$(awk -F'\t' -v p="$pk" '$1 == p {print $2}' "$keys")"; done >> "$stage/ids"
  done
  oracle_make "$pin" "$stage" "$stage/ids" "${how:-bin/hsi dumps --record${only:+ --packages $only}}" ghc || return 1
  rm -rf "$stage"
}

hsi_progs_dir() { # the unpacked pack's directory of programs, checked
  local u
  [ -f "$hsi_progs_pin" ] && [ -f "$(oracle_manifest "$hsi_progs_pin")" ] || { echo "hsi: UNRECORDED: no $hsi_progs_pin (bin/hsi check --live --dump-only --record-progs, by hand on a host with GHC)" >&2; return 1; }
  u=$(oracle_unpack "$hsi_progs_pin") || return 1
  echo "$u/$(oracle_pin_name "$hsi_progs_pin")"
}

hsi_ifaces_dir() { # the unpacked interfaces pack's directory, every member's key the helper's identity now
  local u man h
  man=$(oracle_manifest "$hsi_ifaces_pin")
  [ -f "$hsi_ifaces_pin" ] && [ -f "$man" ] || { echo "hsi: UNRECORDED: no $hsi_ifaces_pin (bin/hsi check --front native --record-ifaces, by hand on a host with GHC)" >&2; return 1; }
  h=$(hsr_helper_hash)
  awk -F'\t' -v h="$h" '!/^#/ && NF && $2 != h {bad = 1} END {exit bad}' "$man" || { echo "hsi: STALE haskell-ifaces-ghc-9.14.1: the helper changed since the record (bin/hsi check --front native --record-ifaces)" >&2; return 1; }
  u=$(oracle_unpack "$hsi_ifaces_pin") || return 1
  echo "$u/$(oracle_pin_name "$hsi_ifaces_pin")"
}

hsi_record_ifaces() { # DIR CMD: DIR/*.iface.edn and DIR/reexports.edn packed, each keyed by the helper's identity
  local dd=$1 how=$2 stage=$hsi_work/records/ifaces name h
  name=$(oracle_pin_name "$hsi_ifaces_pin"); h=$(hsr_helper_hash)
  rm -rf "$stage"; mkdir -p "$stage/$name"
  cp "$dd"/*.iface.edn "$dd/reexports.edn" "$stage/$name/" || return 1
  (cd "$stage/$name" && ls) | LC_ALL=C sort | while IFS= read -r m; do printf '%s\t%s\n' "$m" "$h"; done > "$stage/ids"
  oracle_make "$hsi_ifaces_pin" "$stage" "$stage/ids" "$how" ghc || return 1
  rm -rf "$stage"
}
