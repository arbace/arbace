# Haskell's records of GHC and its helper (doc/ORACLE-RECORDS-SURVEY.md, ORS12, "As built: Haskell"): sourced by bin/repr-haskell, its
# test script and bin/hsi's, after $root and $TMPDIR are set (bin/lib/tmpdir.bash). It is not in bin/lib/'s keyed part (bin/arbace-go's
# gate_key leaves bin/lib/repr-haskell/ out).
#
# Two forms (test/oracle-records/oracle.bash holds the packs' side):
#   a COMMITTED record (ORS2 (a), the essential subsets): files under DIR, a manifest MAN "ID<TAB>KEY<TAB>SHA256<TAB>SIZE" with "# " header
#       lines (the command, the host identity, the date); hsr_records_unpack copies it to a work directory, refusing a file whose digest
#       differs from the manifest's (REFUSED), an entry whose key is not the current one (STALE), a manifest or file that is absent
#       (UNRECORDED); hsr_records_make writes it from a work directory; hsr_records_verify compares the live files with it byte for byte
#   a PACK (oracle_make, oracle_get): the same lines, the members in the store
#
#   hsr_helper_hash                      the helper's identity: its sources' and GHC_VERSION's SHA-256 (what the helper's dumps name as :helper-hash)
#   hsr_records_unpack MAN DIR KEY WORK  every file of the committed record into WORK/ID, all keys KEY
#   hsr_records_make MAN DIR WORK FILES KEY CMD   FILES (a file of IDs under WORK) copied into DIR, the manifest written (:ghc's identity)
#   hsr_records_verify MAN DIR WORK FILES         WORK's files against DIR's, ORS6 (a)'s verdict, and the crossed pair (which must differ)
. "$root/test/oracle-records/oracle.bash"

hsr_helper_hash() { cat "$root"/tools/hsdump/*.hs "$root/tools/hsdump/GHC_VERSION" | sha256sum | cut -c1-64; }

hsr_records_unpack() {  # MAN DIR KEY WORK
  local man=$1 dir=$2 key=$3 work=$4 id k sha size got bad=0 n=0
  [ -f "$man" ] || { echo "UNRECORDED: no $man (bin/repr-haskell test --record, by hand on a host with GHC)"; return 1; }
  while IFS=$'\t' read -r id k sha size; do
    [ -n "$id" ] || continue
    case $id in '#'*) continue ;; esac
    n=$((n + 1))
    if [ "$k" != "$key" ]; then echo "STALE $id: the helper's sources, the cases' inputs or live_origs changed since the record"; bad=1; break; fi
    [ -f "$dir/$id" ] || { echo "UNRECORDED $id: no $dir/$id"; bad=1; continue; }
    got=$(sha256sum < "$dir/$id" | cut -c1-64)
    [ "$got" = "$sha" ] || { echo "REFUSED $id: $dir/$id differs from the manifest's digest"; bad=1; continue; }
    mkdir -p "$work/$(dirname "$id")"; cp "$dir/$id" "$work/$id"
  done < "$man"
  [ "$bad" = 0 ] && [ "$n" -gt 0 ]
}

hsr_records_make() {  # MAN DIR WORK FILES KEY CMD
  local man=$1 dir=$2 work=$3 files=$4 key=$5 cmd=$6 id s n
  [ -s "$files" ] || { echo "record: no files to record" >&2; return 1; }
  rm -rf "$dir"; mkdir -p "$dir" "$(dirname "$man")"
  { echo "# committed record $(basename "$dir"): ID<TAB>KEY<TAB>SHA256<TAB>SIZE, the file test/haskell/records/$(basename "$dir")/ID (doc/ORACLE-RECORDS-SURVEY.md, As built: Haskell)"
    echo "# host: :ghc $(oracle_host_id ghc) ($(oracle_host_version ghc))"
    echo "# made by: $cmd"
    echo "# date: $(date -u +%Y-%m-%d)"
    while IFS= read -r id; do
      mkdir -p "$dir/$(dirname "$id")"; cp "$work/$id" "$dir/$id"
      s=$(sha256sum < "$dir/$id" | cut -c1-64); n=$(stat -c %s "$dir/$id")
      printf '%s\t%s\t%s\t%s\n' "$id" "$key" "$s" "$n"
    done < "$files"; } > "$man.new" && mv "$man.new" "$man"
  echo "recorded: $(grep -vc '^#' "$man") files, $(du -sk "$dir" | cut -f1) KB, $man"
}

hsr_records_verify() {  # MAN DIR WORK FILES
  local man=$1 dir=$2 work=$3 files=$4 id total=0 ndiff=0 first="" last=""
  while IFS= read -r id; do
    total=$((total + 1)); [ -n "$first" ] || first=$id; last=$id
    if [ ! -f "$dir/$id" ]; then echo "verify: $id: UNRECORDED"; ndiff=$((ndiff + 1))
    elif cmp -s "$work/$id" "$dir/$id"; then echo "verify: $id: equal"
    else echo "verify: $id: DIFFERS"; ndiff=$((ndiff + 1)); fi
  done < "$files"
  oracle_verdict ghc "$man" "$ndiff" "$total" || return 1
  # the crossed pair: the first file's live bytes against the last file's record must differ, else the verify judges nothing
  if [ "$total" -ge 2 ]; then
    if cmp -s "$work/$first" "$dir/$last"; then echo "verify: FAILED: the crossed pair ($first against $last's record) is equal: control not judged"; return 1
    else echo "verify: the crossed pair differs (control passed)"; fi
  fi
}
