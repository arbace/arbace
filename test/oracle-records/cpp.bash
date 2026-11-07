# C++'s records (doc/ORACLE-RECORDS-SURVEY.md, ORS12, "As built: C++ and LLVM IR"): what bin/cppi asks of clang++ and g++ at run time,
# recorded as packs (classes B and C), sourced by bin/cppi, bin/lib/cppi/{check,check-aot} and bin/repr-cpp after test/oracle-records/oracle.bash.
# It lives here, not in bin/lib/, for the reason oracle.bash gives (bin/lib is in gate_key).
#
# The IR packs (class C: the front end's product that cppi's lowering and run levels consume, plus the outcomes of the native steps on it):
#   cpp-ir-samples      samples/NAME and runtime/NAME, the programs of test/cpp/samples and test/cpp/runtime/samples (bin/cppi record-ir)
#   cpp-ir-libstdcxx, cpp-ir-gxx-dg-run   the run corpora's programs (the night's: they need the pinned gcc tarball's testsuite)
#   cpp-runtime-ir      the lowered runtime's units: UNIT/x.ll (bin/cppi runtime-build)
# A member ID/FILE: ir.status, ir.log, x.ll (clang++ -O0 -S -emit-llvm), and for the lowering's levels the outcomes of the original's link and run
# (orig-link.status, orig-link.log, obj.log, orig.status, orig.out, orig.err, no-main), g++'s build and run (gxx-link.status, gxx-link.log, gxx.status,
# gxx.out, gxx.err) and x.o.sha256 (the SHA-256 of clang -c x.ll: the print's object is compared with it in the oracle tier only).
# The key of an ID's members (ORS4 (a)): the source's bytes, the standard, the arguments (the checkout's path as @ROOT@), the recorded clang line
# (test/cpp/host/host.edn's :clang), the headers' pin (test/cpp/host/headers.sha256), the pack's own recipe version. A run reads the records unless
# CPPI_LIVE=1 (the oracle tier and the recording itself): an ID not in the pack is UNRECORDED, one whose key moved STALE (ORS10 (a)): ir.status 99, the
# program fails the check.
#
#   cpr_live                 true when CPPI_LIVE is set
#   cpr_clang_line           the recorded clang line
#   cpr_pin ID               the pin of the pack that holds ID (samples/, runtime/ -> cpp-ir-samples; libstdcxx/, NN_ -> cpp-ir-libstdcxx; gxx-dg-run/)
#   cpr_key ID STD ARGS PATH the key
#   cpr_ir_get ID STD ARGS PATH DIR   the members of ID into DIR; exit 3 UNRECORDED, 4 STALE, 2 absent pack, 1 refused
#   cpr_ir_or_fail ID STD ARGS PATH DIR  as cpr_ir_get, a failure written as DIR/ir.status 99 and DIR/ir.log
#   cpr_ir_ids DIR           the IDs-with-keys lines of a record's staging tree DIR (ID/FILE<TAB>KEY), for oracle_make

cpr_recipe=cpr-ir-1
cpr_live() { [ -n "${CPPI_LIVE:-}" ]; }
cpr_clang_line() { sed -n 's/^ *{\{0,1\}:clang  *"\(.*\)"$/\1/p' "$root/test/cpp/host/host.edn" | head -1; }
cpr_headers_digest() { sha256sum < "$root/test/cpp/host/headers.sha256" | cut -c1-64; }
cpr_norm() { local m; m=$(oracle_main); local t=${1//$root\//@ROOT@/}; echo "${t//$m\//@MAIN@/}"; }
cpr_pin() {
  local id=$1 n
  case $id in
    samples/*|runtime/*) n=cpp-ir-samples ;;
    libstdcxx/*|[0-9][0-9]_*) n=cpp-ir-libstdcxx ;;
    gxx-dg-run/*) n=cpp-ir-gxx-dg-run ;;
    *) n=cpp-ir-samples ;;
  esac
  echo "$root/test/cpp/records/$n.sha256"
}
cpr_id() { case $1 in [0-9][0-9]_*) echo "libstdcxx/$1" ;; *) echo "$1" ;; esac; }  # the ID of a list's NAME
cpr_key() { # ID STD ARGS PATH
  { printf '%s\n%s\n%s\n%s\n%s\n%s\n' "$cpr_recipe" "$2" "$(cpr_norm "$3")" "$(cpr_clang_line)" "$(cpr_headers_digest)" "$(sha256sum < "$4" | cut -c1-64)"; } | sha256sum | cut -c1-64
}
cpr_ir_get() { # ID STD ARGS PATH DIR
  local id=$(cpr_id "$1") std=$2 args=$3 p=$4 d=$5 pin man want key f rest names n
  pin=$(cpr_pin "$id"); man=$(oracle_manifest "$pin")
  [ -f "$pin" ] && [ -f "$man" ] || { echo "UNRECORDED: no pack pinned for $id ($(basename "$pin"))" >&2; return 3; }
  want=$(awk -F'\t' -v i="$id/ir.status" '!/^#/ && $1 == i {print $2; exit}' "$man")
  [ -n "$want" ] || { echo "UNRECORDED: $id is not in $man (bin/cppi record-ir, by hand on a host with clang++)" >&2; return 3; }
  key=$(cpr_key "$id" "$std" "$args" "$p")
  [ "$key" = "$want" ] || { echo "STALE: $id's key moved (the source, its options, the recorded clang line or the headers' pin changed): record again by hand" >&2; return 4; }
  mkdir -p "$d"
  names=$(awk -F'\t' -v i="$id/" '!/^#/ && index($1, i) == 1 { r = substr($1, length(i) + 1); if (r !~ /\//) print r }' "$man")
  for n in $names; do
    f=$(oracle_get "$pin" "$id/$n") || return $?
    cp "$f" "$d/$n"
  done
}
cpr_ir_or_fail() { # ID STD ARGS PATH DIR
  local msg rc
  mkdir -p "$5"
  msg=$(cpr_ir_get "$@" 2>&1); rc=$?
  if [ $rc != 0 ]; then echo 99 > "$5/ir.status"; printf '%s\n' "$msg" > "$5/ir.log"; echo "$msg" >&2; fi
  return 0
}
cpr_stage_ids() { # DIR: every file under DIR as "ID/FILE<TAB>KEY" for the IDs of the keys file DIR/.keys (ID<TAB>KEY a line)
  local dir=$1 id key f
  while IFS=$'\t' read -r id key; do
    for f in "$dir/$id"/*; do [ -f "$f" ] && printf '%s/%s\t%s\n' "$id" "$(basename "$f")" "$key"; done
  done < "$dir/.keys"
}
cpr_prepare() { # the packs unpacked and checked once, before the parallel runs that read their members (oracle_check_members' scratch files are shared)
  local pin
  cpr_live && return 0
  for pin in "$root"/test/cpp/records/*.sha256; do [ -f "$pin" ] && { oracle_unpack "$pin" > "$TMPDIR/cpr-prepare.out" || return $?; }; done
  return 0
}
cpr_dump_dirs() { # the unpacked cxxdump packs of test/repr/cpp/records/, colon-joined (cxxdump -records): each pack is a cache's layout, KK/KEY.*
  local pin u out=
  for pin in "$root"/test/repr/cpp/records/cxxdump-*.sha256; do
    [ -f "$pin" ] || continue
    u=$(oracle_unpack "$pin") || return $?
    out=$out${out:+:}$u/$(oracle_pin_name "$pin")
  done
  [ -n "$out" ] || { echo "UNRECORDED: no cxxdump pack in test/repr/cpp/records/ (bin/repr-cpp record-dumps, by hand)" >&2; echo /nonexistent; return 0; }
  echo "$out"
}
# the xargs-run functions of bin/cppi (one, rt_ir) call these through `bash -c`
export cpr_recipe root
export -f cpr_live cpr_clang_line cpr_headers_digest cpr_norm cpr_pin cpr_id cpr_key cpr_ir_get cpr_ir_or_fail
export -f oracle_main oracle_store oracle_pin_sha oracle_pin_name oracle_manifest oracle_pack_path oracle_check_members oracle_unpack oracle_get
