# RFP23 (b) (doc/rust/FRONTEND-SURVEY.md, "As built: RFP23 (b), RFP24 (b)"): what bin/rust-front's essential judges read of
# rustc, recorded once per program as oracle packs in the oracle-records format (doc/ORACLE-RECORDS-SURVEY.md §3.2;
# test/oracle-records/oracle.bash: the pack in the store, its pin and manifest in test/rust/records/), made by hand on the host
# with rustc and the helper (ORS5 (a)), verified there by a rerun (the oracle tier), read at the check with no helper and no rustc.
# Sourced by bin/rust-front after it sets $root, $helper and $work (and bin/lib/tmpdir.bash). In no key (bin/lib/repr-rust/ is
# arbace_crop_paths').
#
# The kinds, one pack each (test/rust/records/rust-front-KIND.{sha256,tsv}):
#   std       the helper's std-names.edn and std-iface.edn (--std-names, --std-iface: RFS4, RFP26 (a)), their digests also
#             test/rust/front/std-{names,iface}.sha256's
#   built     rustc's MIR after mir_built and after each pass to runtime MIR (-Zdump-mir, the .after.mir files only: the judge
#             reads no .before.mir) of test/rust/front/a*.rs, members NAME/FILE
#   captures  each closure's kind, captures and fake reads (--captures, RFP20 (a)) of test/rust/synthetic/closures.rs and the
#             test/rust/front/ programs with a closure, members NAME.cap.edn
#   mono      run-built's record (RFP23 (b)): the helper's --mono-mir dump (the symbols, instances and layouts, std's
#             resolved callees, the bodies the program's own replace) and rustc's executable's exit status and output, of
#             test/rust/front/a*.rs, members NAME/NAME.record.edn, NAME/status, NAME/stdout; the checkout's absolute path in
#             the dump's locations written @ROOT@ and put back at the read
# An entry's key: the kind, the program's path in the checkout and the SHA-256 of its bytes, the helper's source digest
# (bin/repr-rust's helper_hash), the flags and (built) the passes dumped; the toolchain's identity is the manifest's header's.
# A program the step needs whose key the manifest lacks is UNRECORDED, one whose key moved STALE: either fails the check.

. "$root/test/oracle-records/oracle.bash"

rfr_passes='built|SimplifyCfg-initial|PromoteTemps|SimplifyCfg-promote-consts|ImpossibleClauses|CleanupPostBorrowck|RemoveNoopLandingPads|SimplifyCfg-post-analysis|Derefer|AddCallGuards|PostAnalysisNormalize|Subtyper|ElaborateDrops|AbortUnwindingCalls|AddMovesForPackedDrops|EraseDerefTemps|ElaborateBoxDerefs|StateTransform|LowerIntrinsics|RemovePlaceMention|SimplifyCfg-pre-optimizations'

rfr_pin() { echo "$root/test/rust/records/rust-front-$1.sha256"; }
rfr_helper_hash() {
  cat "$root"/tools/rsdump/*.rs "$root/tools/rsdump/RUSTC_VERSION" "$root/tools/rsdump/apks.sha256" | sha256sum | cut -c1-64
}
# the programs of a kind, paths relative to the checkout
rfr_programs() {
  local f
  case $1 in
    std) echo std ;;
    built|mono) (cd "$root" && ls test/rust/front/a*.rs) ;;
    captures) (cd "$root" && { echo test/rust/synthetic/closures.rs; for f in test/rust/front/*.rs; do grep -q '|' "$f" && echo "$f"; done; }) ;;
  esac
}
rfr_name() { basename "$1" .rs; }
# the helper's flags for a program of a kind (bin/rust-front's build_oracles, run-built and capture_oracles, as they were)
rfr_flags() {  # KIND REL
  local k=$1 f=$root/$2 pf="" oc=-Coverflow-checks=off ct=bin ed=2024
  case $k in
    std) echo "--edition 2024 --crate-type bin -A warnings" ;;
    built|mono)
      grep -q '^#!\[no_std\]' "$f" && ! grep -q eh_personality "$f" && pf="-C panic=abort"
      grep -q '^// rf: overflow-checks' "$f" && oc=-Coverflow-checks=on
      grep -q "fn main" "$f" || ct=lib
      if [ "$k" = built ]; then echo "--edition 2024 --crate-type $ct -A warnings -C opt-level=0 -C debuginfo=0 $pf $oc"
      else echo "--edition 2024 --crate-type bin -A warnings -C opt-level=0 -C debuginfo=0 $pf $oc -Zshare-generics=no"; fi ;;
    captures)
      grep -q "fn main" "$f" || ct=lib
      [ "$(rfr_name "$2")" = edition2015 ] && ed=2015
      grep -q '^#!\[no_std\]' "$f" && pf="-C panic=abort"
      echo "--edition $ed --crate-type $ct -A warnings $pf" ;;
  esac
}
rfr_key() {  # KIND REL
  local k=$1 rel=$2
  { echo "rust-front $k $rel"
    if [ "$k" = std ]; then echo "fn main() {}"; else sha256sum < "$root/$rel" | cut -c1-64; fi
    rfr_helper_hash
    rfr_flags "$k" "$rel"
    [ "$k" = built ] && echo "$rfr_passes"; true; } | sha256sum | cut -c1-64
}

# rfr_live KIND REL OUT: the helper (as rustc) makes the program's members under OUT (OUT/ID for each member ID); 1 on a failure
rfr_live() {
  local k=$1 rel=$2 out=$3 n f s
  n=$(rfr_name "$rel"); f=$root/$rel; s=$out.scratch
  [ -x "$helper" ] || { echo "rust-front records: no helper ($helper): bin/repr-rust helper, on the host with rustc" >&2; return 1; }
  rm -rf "$s"; mkdir -p "$s" "$out"
  case $k in
    std)
      printf 'fn main() {}\n' > "$s/empty.rs"
      (ulimit -v 4000000; cd "$s" && "$helper" rustc --std-names std-names.edn --std-iface std-iface.edn -- $(rfr_flags std std) -o empty.bin empty.rs > std.log 2>&1) || { cat "$s/std.log" >&2; return 1; }
      cp "$s/std-names.edn" "$s/std-iface.edn" "$out/" ;;
    built)
      (ulimit -v 4000000; cd "$s" && "$helper" rustc -- $(rfr_flags built "$rel") -Zdump-mir="$rfr_passes" -Zdump-mir-dir="$n" -o "$n.bin" "$f" > "$n.log" 2>&1) || { cat "$s/$n.log" >&2; return 1; }
      mkdir -p "$out/$n"; cp "$s/$n/"*.after.mir "$out/$n/" ;;
    captures)
      (ulimit -v 2000000; cd "$s" && "$helper" rustc --captures "$n.cap.edn" -- $(rfr_flags captures "$rel") -o "$n.cap.bin" "$f" > "$n.cap.log" 2>&1) || { cat "$s/$n.cap.log" >&2; return 1; }
      cp "$s/$n.cap.edn" "$out/" ;;
    mono)
      (ulimit -v 4000000; cd "$s" && "$helper" rustc --mono-mir "$n.record.edn" -- $(rfr_flags mono "$rel") -o "$n.rustc.bin" "$f" > "$n.log" 2>&1) || { cat "$s/$n.log" >&2; return 1; }
      mkdir -p "$out/$n"
      "$s/$n.rustc.bin" > "$out/$n/stdout" 2>&1; echo $? > "$out/$n/status"
      sed "s|$root/|@ROOT@/|g" "$s/$n.record.edn" > "$out/$n/$n.record.edn" ;;
  esac
  rm -rf "$s"
}
# the member ids of a program under DIR (the kind's directory), each with its key: "ID<TAB>KEY" lines
rfr_ids() {  # KIND REL DIR
  local k=$1 rel=$2 d=$3 n key
  n=$(rfr_name "$rel"); key=$(rfr_key "$k" "$rel")
  case $k in
    std) printf 'std-names.edn\t%s\nstd-iface.edn\t%s\n' "$key" "$key" ;;
    captures) printf '%s.cap.edn\t%s\n' "$n" "$key" ;;
    built|mono) (cd "$d" && find "$n" -type f | LC_ALL=C sort) | while read -r id; do printf '%s\t%s\n' "$id" "$key"; done ;;
  esac
}
# the program's prefix of member ids in a manifest (a program's key is the one of its members)
rfr_prefix() {  # KIND REL
  case $1 in std) echo std-names.edn ;; captures) echo "$(rfr_name "$2").cap.edn" ;; *) echo "$(rfr_name "$2")/" ;; esac
}

# bin/rust-front records make [KIND...]: the packs made afresh (by hand, on the host with rustc: ORS5 (a))
rfr_make() {
  local kinds=("$@") k rel d ids
  [ ${#kinds[@]} -gt 0 ] || kinds=(std built captures mono)
  [ "$(oracle_host_id rustc)" != none ] || { echo "rust-front records make: no rustc here (the oracle tier's host)" >&2; return 1; }
  for k in "${kinds[@]}"; do
    d=$work/records/make; rm -rf "$d/rust-front-$k"; mkdir -p "$d/rust-front-$k"; ids=$d/rust-front-$k.ids; : > "$ids"
    for rel in $(rfr_programs "$k"); do
      rfr_live "$k" "$rel" "$d/rust-front-$k" || { echo "rust-front records make: $k: $rel failed" >&2; return 1; }
      rfr_ids "$k" "$rel" "$d/rust-front-$k" >> "$ids"
    done
    if [ "$k" = std ]; then
      (cd "$d/rust-front-std" && sha256sum -c --quiet "$root/test/rust/front/std-names.sha256" "$root/test/rust/front/std-iface.sha256") ||
        { echo "rust-front records make: std's names or interface differ from test/rust/front/std-*.sha256 (regenerate them first: RFP26 (a))" >&2; return 1; }
    fi
    oracle_make "$(rfr_pin "$k")" "$d" "$ids" "bin/rust-front records make $k" rustc || return 1
  done
}

# rfr_stage KIND [REL...]: the pack unpacked and checked (oracle_unpack), each program's key held to the manifest; prints the
# directory holding the kind's members (for mono: a copy with @ROOT@ put back); exit 1 on a STALE, UNRECORDED or refused entry
rfr_stage() {
  local k=$1 pin man u rel pre key got bad=0 rels
  shift; pin=$(rfr_pin "$k"); man=$(oracle_manifest "$pin")
  [ -f "$pin" ] || { echo "rust-front records: no pin $pin (bin/rust-front records make $k, on the host with rustc)" >&2; return 1; }
  if [ $# -gt 0 ]; then rels=("$@"); else rels=($(rfr_programs "$k")); fi
  for rel in "${rels[@]}"; do
    pre=$(rfr_prefix "$k" "$rel"); key=$(rfr_key "$k" "$rel")
    got=$(awk -F'\t' -v p="$pre" '!/^#/ && substr($1, 1, length(p)) == p {print $2; exit}' "$man")
    if [ -z "$got" ]; then echo "rust-front records: UNRECORDED: $k $rel (not in $(basename "$man"))" >&2; bad=1
    elif [ "$got" != "$key" ]; then echo "rust-front records: STALE: $k $rel (its key moved: the program, the helper or the flags changed since the record)" >&2; bad=1; fi
  done
  [ $bad = 0 ] || return 1
  u=$(oracle_unpack "$pin") || return 1
  if [ "$k" = mono ]; then
    local s=$work/records/stage/mono-$(oracle_pin_sha "$pin")
    if [ ! -f "$s.done" ]; then
      rm -rf "$s"; mkdir -p "$s"; cp -r "$u/rust-front-mono/." "$s/"
      find "$s" -name '*.record.edn' -exec sed -i "s|@ROOT@/|$root/|g" {} +
      : > "$s.done"
    fi
    echo "$s"
  else
    echo "$u/rust-front-$k"
  fi
}

# bin/rust-front records verify [KIND...] [--sample N]: the live helper over the entries, compared member by member with the
# packs (ORS6 (a)'s verdict); then the crossed pair (ORS7 (a)): one program's live output against another's member, which must
# differ. Absent (exit 0) where rustc or the helper is not.
rfr_verify() {
  local kinds=() sample=0 k rel d u n ndiff total st=0 fa
  while [ $# -gt 0 ]; do case $1 in --sample) sample=$2; shift 2 ;; *) kinds+=("$1"); shift ;; esac; done
  [ ${#kinds[@]} -gt 0 ] || kinds=(std built captures mono)
  if [ "$(oracle_host_id rustc)" = none ] || [ ! -x "$helper" ]; then echo "verify: rust-front: absent (no rustc or no helper here)"; return 0; fi
  for k in "${kinds[@]}"; do
    u=$(oracle_unpack "$(rfr_pin "$k")") || { st=1; continue; }
    u=$u/rust-front-$k
    d=$work/records/verify/$k; rm -rf "$d"; mkdir -p "$d"
    ndiff=0 total=0
    local progs=($(rfr_programs "$k"))
    [ "$sample" -gt 0 ] && [ ${#progs[@]} -gt "$sample" ] && progs=("${progs[@]:0:$sample}")
    for rel in "${progs[@]}"; do
      total=$((total+1))
      if ! rfr_live "$k" "$rel" "$d"; then ndiff=$((ndiff+1)); echo "verify: $k $rel: the helper failed"; continue; fi
      n=$(rfr_name "$rel")
      case $k in std) fa="std-names.edn std-iface.edn" ;; captures) fa=$n.cap.edn ;; *) fa=$n ;; esac
      for f in $fa; do
        diff -r "$d/$f" "$u/$f" > "$d/$n.diff" 2>&1 || { ndiff=$((ndiff+1)); echo "verify: $k $rel differs ($d/$n.diff)"; break; }
      done
    done
    oracle_verdict rustc "$(oracle_manifest "$(rfr_pin "$k")")" "$ndiff" "$total" || st=1
    # the crossed pair (ORS7 (a)): the first program's live output against the second's member (std: the names afresh against
    # the interface's member), which must differ
    local pa="" pb="" a b
    if [ "$k" = std ]; then pa=$d/std-names.edn pb=$u/std-iface.edn
    elif [ ${#progs[@]} -ge 2 ]; then
      a=$(rfr_name "${progs[0]}") b=$(rfr_name "${progs[1]}")
      case $k in captures) pa=$d/$a.cap.edn pb=$u/$b.cap.edn ;; built) pa=$d/$a pb=$u/$b ;; mono) pa=$d/$a/$a.record.edn pb=$u/$b/$b.record.edn ;; esac
    fi
    if [ -n "$pa" ]; then
      if diff -r "$pa" "$pb" > "$d/control.diff" 2>&1; then echo "verify: $k: control ($pa against $pb) PASSED (a bug)"; st=1
      else echo "verify: $k: control (one program afresh against another's member): differs, as it must"; fi
    fi
  done
  return $st
}
