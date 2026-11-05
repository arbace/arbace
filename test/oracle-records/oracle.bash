# The oracles' records and packs (doc/ORACLE-RECORDS-SURVEY.md, ORS1-ORS14 decided (a), "As built: the tooling and C"):
# sourced by bin/oracle-records and by the crops' tools after they set $root (and bin/lib/tmpdir.bash).
# It lives here, not in bin/lib/ as the survey's §6 names it: bin/arbace-go's gate_key hashes bin/lib/ (but the crops' and the
# repr checks' directories), so a new file there rekeys every build and every step on them. The commit that moves it to
# bin/lib/oracle.bash with its exclusion from gate_key lands with the next change that rekeys every build (GTS240's).
#
# A pack is a deterministic tar.xz of an oracle's dumps, products or installed tree (classes B, C and E), outside the
# clone in the store, pinned in the checkout by two committed files beside each other:
#   test/CROP/records/NAME.sha256  "SHA256  NAME.tar.xz": the pack's bytes (a step's :pins)
#   test/CROP/records/NAME.tsv     its manifest: "# " header lines (the pack, the host identity that made it, the
#                                  command, the date), then one member a line "ID<TAB>KEY<TAB>SHA256<TAB>SIZE", sorted
#                                  by ID: the member is NAME/ID in the tar (a leading / of ID dropped), KEY the entry's
#                                  key (the input's digest, the helper's, the toolchain's identity and the flags hashed,
#                                  ORS4 (a); "tree" for a member of an installed tree, which has no input)
# The tar: sorted by name, POSIX format, mtime 0, owner and group 0, modes 0644 and 0755, no atime or ctime (the seed's
# recipe, bin/lib/seed-go.bash's seed_go_tar), compressed by xz -9e with one thread.
# The store (ORS3 (a)): ARBACE_ORACLE_PACKS, by default the main checkout's .tmp/oracle-packs/ (shared by the worktrees
# and the clean run's clone, as .tmp/gate-exe is): NAME/SHA256.tar.xz (every pinned version kept), unpacked/SHA256/NAME/
# (a pack's members, each checked against the manifest whenever it is unpacked or read).
#
#   oracle_main                the main checkout (a worktree's or the clean run's clone's stripped)
#   oracle_store               the store's directory
#   oracle_pin_sha PIN         the pack's SHA-256 a pin names
#   oracle_manifest PIN        the manifest beside the pin
#   oracle_pack_path PIN       the pack's file in the store (it may not exist)
#   oracle_make PIN DIR IDS CMD TOOL  packs DIR/NAME/ID for each "ID<TAB>KEY" line of the file IDS into the store, and
#                              writes the manifest and the pin (TOOL: the toolchain that made it, gcc, its identity in the
#                              header; anything else written as it is)
#   oracle_unpack PIN          the pack unpacked (checked: its bytes against the pin, each member against the manifest,
#                              no member the manifest lacks); prints the directory that holds NAME/; exit 1 refused, 2 absent
#   oracle_get PIN ID          the member's path, its digest checked against the manifest; exit 3: unrecorded (ORS10 (a):
#                              the ID is not in the manifest), 1: refused (a digest differs), 2: the pack is absent
#   oracle_host_id TOOL        the toolchain's identity (gcc: as bin/lib/gate-run.bash's gate_gcc_id), none without it
#   oracle_record_header PREFIX TOOL CMD  the header lines of a record (ORS5 (a)): the command, the identity, the date

oracle_main() {
  local m=${root%%/.tmp/gate-clean/*}
  echo "${m%%/.tmp/worktrees/*}"
}
oracle_store() { echo "${ARBACE_ORACLE_PACKS:-$(oracle_main)/.tmp/oracle-packs}"; }
oracle_pin_sha() { awk 'NF && $1 !~ /^#/ {print $1; exit}' "$1"; }
oracle_pin_name() { local b; b=$(basename "$1"); echo "${b%.sha256}"; }
oracle_manifest() { echo "${1%.sha256}.tsv"; }
oracle_pack_path() { echo "$(oracle_store)/$(oracle_pin_name "$1")/$(oracle_pin_sha "$1").tar.xz"; }

oracle_tar() {  # DIR NAME: the deterministic tar of DIR/NAME on standard output
  local d=$1 n=$2
  find "$d/$n" -type d -exec chmod 0755 {} + -exec chmod u-s,g-s,o-t {} + && find "$d/$n" -type f -exec chmod 0644 {} + || return 1
  LC_ALL=C tar -C "$d" --sort=name --format=posix \
    --pax-option=exthdr.name=%d/PaxHeaders/%f,delete=atime,delete=ctime \
    --mtime=@0 --owner=0 --group=0 --numeric-owner -cf - "$n"
}

oracle_make() {  # PIN DIR IDS CMD HOST
  local pin=$1 dir=$2 ids=$3 cmd=$4 host=$5 name man store sha id key f s n
  name=$(oracle_pin_name "$pin"); man=$(oracle_manifest "$pin"); store=$(oracle_store)
  [ -d "$dir/$name" ] || { echo "oracle: no $dir/$name to pack" >&2; return 1; }
  [ -s "$ids" ] || { echo "oracle: no members listed for $name ($ids)" >&2; return 1; }
  # every file under DIR/NAME is a listed member, and every listed member is there
  (cd "$dir/$name" && find . -type f -printf '%P\n' | LC_ALL=C sort) > "$TMPDIR/oracle-make.files"
  awk -F'\t' '{id=$1; sub(/^\//, "", id); print id}' "$ids" | LC_ALL=C sort > "$TMPDIR/oracle-make.ids"
  if ! cmp -s "$TMPDIR/oracle-make.files" "$TMPDIR/oracle-make.ids"; then
    echo "oracle: $dir/$name's files are not the listed members:" >&2
    diff "$TMPDIR/oracle-make.ids" "$TMPDIR/oracle-make.files" | head -5 >&2; return 1
  fi
  mkdir -p "$store/$name" "$(dirname "$pin")"
  oracle_tar "$dir" "$name" | xz -9e -T1 -c > "$store/$name/new.$$" || { rm -f "$store/$name/new.$$"; return 1; }
  sha=$(sha256sum < "$store/$name/new.$$" | cut -c1-64)
  mv "$store/$name/new.$$" "$store/$name/$sha.tar.xz"
  { echo "# pack $name: ID<TAB>KEY<TAB>SHA256<TAB>SIZE, the member $name/ID (doc/ORACLE-RECORDS-SURVEY.md §3.2; test/oracle-records/oracle.bash)"
    if [ "$(oracle_host_id "$host" 2>"$TMPDIR/oracle-make.err")" = none ]; then echo "# host: $host"
    else echo "# host: :$host $(oracle_host_id "$host") ($(oracle_host_version "$host"))"; fi
    echo "# made by: $cmd"
    echo "# date: $(date -u +%Y-%m-%d)"
    LC_ALL=C sort -t "$(printf '\t')" -k1,1 "$ids" | while IFS=$'\t' read -r id key; do
      f=$dir/$name/${id#/}; s=$(sha256sum < "$f" | cut -c1-64); n=$(stat -c %s "$f")
      printf '%s\t%s\t%s\t%s\n' "$id" "$key" "$s" "$n"
    done; } > "$man.new" && mv "$man.new" "$man"
  printf '%s  %s.tar.xz\n' "$sha" "$name" > "$pin"
  echo "oracle: packed $name: $(grep -vc '^#' "$man") members, $(stat -c %s "$store/$name/$sha.tar.xz") bytes xz, $sha"
}

oracle_check_members() {  # DIR MANIFEST: every member of the manifest in DIR with its digest and size, no other file
  local d=$1 man=$2 st=0
  awk -F'\t' '!/^#/ && NF {id=$1; sub(/^\//, "", id); print $3 "  " id}' "$man" > "$TMPDIR/oracle-check.sums"
  (cd "$d" && sha256sum -c --quiet --strict "$TMPDIR/oracle-check.sums") > "$TMPDIR/oracle-check.out" 2>&1 || st=1
  (cd "$d" && find . -type f -printf '%P\n' | LC_ALL=C sort) > "$TMPDIR/oracle-check.files"
  awk '{print $2}' "$TMPDIR/oracle-check.sums" | LC_ALL=C sort > "$TMPDIR/oracle-check.ids"
  if ! cmp -s "$TMPDIR/oracle-check.files" "$TMPDIR/oracle-check.ids"; then
    echo "members not in the manifest: $(comm -13 "$TMPDIR/oracle-check.ids" "$TMPDIR/oracle-check.files" | head -3 | tr '\n' ' ')" >> "$TMPDIR/oracle-check.out"; st=1
  fi
  [ $st = 0 ] || head -5 "$TMPDIR/oracle-check.out" >&2
  return $st
}

oracle_unpack() {  # PIN: prints the directory holding NAME/
  local pin=$1 name sha pack u store got
  name=$(oracle_pin_name "$pin"); sha=$(oracle_pin_sha "$pin"); store=$(oracle_store)
  [ -n "$sha" ] || { echo "oracle: $pin pins nothing" >&2; return 1; }
  u=$store/unpacked/$sha
  if [ ! -d "$u/$name" ]; then
    pack=$(oracle_pack_path "$pin")
    [ -f "$pack" ] || { echo "oracle: no pack $name ($sha) in $store: bin/oracle-records fetch $name (the mirror), or record it on a host with its toolchain" >&2; return 2; }
    got=$(sha256sum < "$pack" | cut -c1-64)
    [ "$got" = "$sha" ] || { echo "oracle: $pack is $got, $pin pins $sha: refused" >&2; return 1; }
    rm -rf "$u.$$"; mkdir -p "$u.$$"
    xz -dc "$pack" | tar -C "$u.$$" -xf - || { rm -rf "$u.$$"; echo "oracle: cannot unpack $pack" >&2; return 1; }
    mkdir -p "$u"; [ -d "$u/$name" ] || mv "$u.$$/$name" "$u/$name"; rm -rf "$u.$$"
  fi
  # the members checked at every unpack (a tampered member, one byte flipped, is refused: ORS7 (a), the control
  # test/oracle-records/controls/)
  oracle_check_members "$u/$name" "$(oracle_manifest "$pin")" || { echo "oracle: $u/$name differs from $(oracle_manifest "$pin"): refused" >&2; return 1; }
  echo "$u"
}

oracle_get() {  # PIN ID: the member's path
  local pin=$1 id=$2 name sha store man line want f got
  name=$(oracle_pin_name "$pin"); sha=$(oracle_pin_sha "$pin"); store=$(oracle_store); man=$(oracle_manifest "$pin")
  line=$(awk -F'\t' -v id="$id" '!/^#/ && $1 == id {print; exit}' "$man")
  [ -n "$line" ] || { echo "oracle: unrecorded: $id is not in $man" >&2; return 3; }
  if [ ! -d "$store/unpacked/$sha/$name" ]; then oracle_unpack "$pin" > "$TMPDIR/oracle-get.dir" || return $?; fi
  f=$store/unpacked/$sha/$name/${id#/}
  want=$(printf '%s\n' "$line" | cut -f3)
  got=$(sha256sum < "$f" 2>"$TMPDIR/oracle-get.err" | cut -c1-64)
  [ "$got" = "$want" ] || { echo "oracle: $f is ${got:-missing}, $man records $want: refused" >&2; return 1; }
  echo "$f"
}

oracle_host_id() {  # TOOL
  case $1 in
    gcc)
      local g c
      g=$(command -v gcc) || { echo none; return; }
      g=$(readlink -f "$g"); c=$(readlink -f "$(gcc -print-prog-name=cc1 2>"$TMPDIR/oracle-gcc.err")" 2>"$TMPDIR/oracle-gcc.err")
      { gcc --version 2>"$TMPDIR/oracle-gcc.err" | head -1
        sha256sum < "$g" | cut -c1-64
        if [ -f "$c" ]; then sha256sum < "$c" | cut -c1-64; else echo "no cc1"; fi
        awk '{print $2}' "$root/test/c/host/headers.sha256" 2>"$TMPDIR/oracle-gcc.err" | while read -r h; do
          if [ -f "$h" ]; then sha256sum "$h"; else echo "missing $h"; fi; done; } | sha256sum | cut -c1-64 ;;
    chez) oracle_scheme_id "${CHEZ:-chez}" --version /usr/lib/csv10.3.0/ta6le/petite.boot /usr/lib/csv10.3.0/ta6le/scheme.boot /usr/lib/csv10.3.0/ta6le/chez.boot ;;
    chibi) oracle_scheme_id "${CHIBI:-chibi-scheme}" -V ;;
    ghc) oracle_ghc_id ;;
    rustc) oracle_rustc_id ;;
    cxx) oracle_cxx_id ;;
    llvm) oracle_llvm_id ;;
    *) echo "oracle: no identity recipe for $1" >&2; echo none ;;
  esac
}
oracle_scheme_id() {  # CMD VERSION-FLAG FILE...: bin/lib/gate-run.bash's gate_scheme_id (:chez, :chibi): the version line, the executable's
  # SHA-256 through its links, the FILEs' (Chez's boot files); none when the command is not installed
  local c=$1 v=$2 x f; shift 2
  x=$(command -v "$c") || { echo none; return; }
  { "$c" "$v" 2>&1 | head -1
    sha256sum < "$(readlink -f "$x")" | cut -c1-64
    for f in "$@"; do if [ -f "$f" ]; then sha256sum < "$f" | cut -c1-64; else echo "missing $f"; fi; done; } | sha256sum | cut -c1-64
}
oracle_ghc_id() {  # GT9's :host-ids :ghc recipe (test/gate/crops/haskell.edn, steps.edn): ghc's --version line, then the SHA-256 of
  # {libdir}/../bin/ghc-[0-9]* and of {libdir}/package.conf.d/ghc-[0-9]*.conf (a glob with no file: "missing GLOB"); none without ghc
  command -v ghc > "$TMPDIR/oracle-ghc.err" || { echo none; return; }
  local d g p m
  d=$(ghc --print-libdir 2>"$TMPDIR/oracle-ghc.err")
  { ghc --version 2>"$TMPDIR/oracle-ghc.err" | head -1
    for g in "$d/../bin/ghc-[0-9]*" "$d/package.conf.d/ghc-[0-9]*.conf"; do
      m=0
      for p in $g; do [ -f "$p" ] && { sha256sum < "$p" | cut -c1-64; m=1; }; done
      [ $m = 1 ] || echo "missing $g"
    done; } | sha256sum | cut -c1-64
}
oracle_rustc_id() {  # GT9's :host-ids :rustc recipe (test/gate/crops/llvm-rust.edn, steps.edn): `rustc --version -v` (all of it), the executable's
  # SHA-256 through its links, then the SHA-256 of {sysroot}/lib/librustc_driver-*.so (the helper links to it); none without rustc
  command -v rustc > "$TMPDIR/oracle-rustc.err" || { echo none; return; }
  local d p m x
  d=$(rustc --print sysroot 2>"$TMPDIR/oracle-rustc.err")
  x=$(readlink -f "$(command -v rustc)")
  { rustc --version -v 2>"$TMPDIR/oracle-rustc.err"
    sha256sum < "$x" | cut -c1-64
    m=0
    for p in "$d"/lib/librustc_driver-*.so; do [ -f "$p" ] && { sha256sum < "$p" | cut -c1-64; m=1; }; done
    [ $m = 1 ] || echo "missing $d/lib/librustc_driver-*.so"; } | sha256sum | cut -c1-64
}
oracle_cxx_id() {  # bin/lib/gate-run.bash's gate_cxx_id (the :cxx host, CX10; GT9's :host-ids :cxx): g++'s and clang++'s --version lines, the digests of
  # their drivers, cc1plus and clang's binary through their links, and of the headers C++'s checks read (test/cpp/host/headers.sha256's); none without either
  local g c p
  g=$(command -v g++) && c=$(command -v clang++) || { echo none; return; }
  g=$(readlink -f "$g"); c=$(readlink -f "$c"); p=$(readlink -f "$(g++ -print-prog-name=cc1plus 2>"$TMPDIR/oracle-cxx.err")" 2>"$TMPDIR/oracle-cxx.err")
  { g++ --version 2>"$TMPDIR/oracle-cxx.err" | head -1
    clang++ --version 2>"$TMPDIR/oracle-cxx.err" | head -1
    sha256sum < "$g" | cut -c1-64
    if [ -f "$p" ]; then sha256sum < "$p" | cut -c1-64; else echo "no cc1plus"; fi
    sha256sum < "$c" | cut -c1-64
    awk '{print $2}' "$root/test/cpp/host/headers.sha256" 2>"$TMPDIR/oracle-cxx.err" | while read -r h; do
      if [ -f "$h" ]; then sha256sum "$h"; else echo "missing $h"; fi; done; } | sha256sum | cut -c1-64
}
oracle_llvm_id() {  # GT9's :host-ids :llvm recipe (gt9-verbs: test/gate/steps.edn): llvm-as's --version lines, the digests of llvm-as, opt, lli and FileCheck of
  # ${REPR_LLVM_BIN:-/usr/lib/llvm23/bin} and of ../lib/libLLVM.so.*; none without llvm-as
  local b=${REPR_LLVM_BIN:-/usr/lib/llvm23/bin} f p m
  [ -x "$b/llvm-as" ] || { echo none; return; }
  { "$b/llvm-as" --version 2>"$TMPDIR/oracle-llvm.err"
    for f in llvm-as opt lli FileCheck; do
      if [ -f "$b/$f" ]; then sha256sum < "$(readlink -f "$b/$f")" | cut -c1-64; else echo "missing $b/$f"; fi; done
    m=0
    for p in "$b"/../lib/libLLVM.so.*; do [ -f "$p" ] && { sha256sum < "$(readlink -f "$p")" | cut -c1-64; m=1; }; done
    [ $m = 1 ] || echo "missing $b/../lib/libLLVM.so.*"; } | sha256sum | cut -c1-64
}
oracle_host_version() {  # TOOL: its version line
  case $1 in
    cxx) { g++ --version 2>"$TMPDIR/oracle-cxx.err" | head -1; clang++ --version 2>"$TMPDIR/oracle-cxx.err" | head -1; } | tr '\n' ';' | sed 's/;$//' ;;
    llvm) "${REPR_LLVM_BIN:-/usr/lib/llvm23/bin}/llvm-as" --version 2>"$TMPDIR/oracle-llvm.err" | sed -n 2p | sed 's/^ *//' ;;
    gcc) gcc --version 2>"$TMPDIR/oracle-gcc.err" | head -1 ;;
    chez) "${CHEZ:-chez}" --version 2>&1 | head -1 ;;
    chibi) "${CHIBI:-chibi-scheme}" -V 2>&1 | head -1 ;;
    ghc) ghc --version 2>"$TMPDIR/oracle-ghc.err" | head -1 ;;
    rustc) rustc --version 2>"$TMPDIR/oracle-rustc.err" | head -1 ;;
    *) echo unknown ;;
  esac
}
oracle_record_header() {  # PREFIX TOOL CMD
  local p=$1
  echo "$p made by: $3"
  echo "$p host: :$2 $(oracle_host_id "$2") ($(oracle_host_version "$2"))"
  echo "$p date: $(date -u +%Y-%m-%d)"
}
oracle_record_host() {  # FILE: the identity a record's header names ("none" when it names none)
  sed -n 's/^[;#]* *host: :[a-z]* \([0-9a-f]\{64\}\).*/\1/p' "$1" | head -1 | grep . || echo none
}

# ORS6 (a): a verify's outcome. oracle_verdict TOOL RECORD DIFFERING TOTAL: prints the verdict; exit 1 when entries differ
# under the identity the record names (stale, or a non-deterministic helper), 0 when they agree or the toolchain moved
# (reported with the differing entries, not failed: a re-record is the user's decision)
oracle_verdict() {
  local tool=$1 rec=$2 ndiff=$3 total=$4 have want
  have=$(oracle_host_id "$tool"); want=$(oracle_record_host "$rec")
  if [ "$ndiff" = 0 ]; then echo "verify: $rec: $total entries, all equal (:$tool $(echo "$have" | cut -c1-16))"; return 0; fi
  if [ "$have" != "$want" ]; then
    echo "verify: $rec: toolchain moved: records of :$tool $(echo "$want" | cut -c1-16), host $(echo "$have" | cut -c1-16) ($(oracle_host_version "$tool")); $ndiff of $total entries differ (not failed, ORS6 (a): a re-record day)"
    return 0
  fi
  echo "verify: FAILED: $rec: $ndiff of $total entries differ under the identity the record names (:$tool $(echo "$have" | cut -c1-16)): stale, or the helper is not deterministic"
  return 1
}
