# RF12 (doc/rust/FRONTEND-SURVEY.md, "As built: RF12 (first slice)"): the front end over core's own sources, module by module.
# Sourced by bin/rust-front (root, work, libsrc set there).
#
#   bin/rust-front core-oracle      the oracle tier, by hand: the helper as rustc 1.99.1 over the copy of rust-src's core, -Zdump-mir=built
#                                   (--emit=metadata, -C opt-level=0 -C debuginfo=0 -C overflow-checks=off, the paths remapped to
#                                   library/), into .tmp/rust-front/core/oracle/{mir/,index.tsv}; the copy patches the two statics of
#                                   task/wake.rs whose closures name themselves (rustc's dump cycles on them: E0391), the rest as pinned
#   bin/rust-front core-records make
#                                   the pack rust-core-built (test/rust/records/rust-core-built.{sha256,tsv}): core-oracle's bodies of
#                                   the first slice's modules (test/rust/front/core-modules.txt, every body whose key starts with one of
#                                   their module paths) and their index; an entry's key the copy's tree digest, the helper's digest and
#                                   the flags (no rustc at check time)
#   bin/rust-front core-check [--records] [FILE...]
#                                   arbace.rust.front.core-check over core's module files (default: core-modules.txt's first-slice
#                                   list), one process, GOMEMLIMIT 1800MiB, std's data from the pack rust-front-std, the oracle above
#                                   (--records: rustc's bodies from the pack rust-core-built, no helper); caches in .tmp/rust-front/core/
#                                   (index.edn: core's macros and names; native-iface.edn: core's interface from its sources so far)

core_lib() {
  local d=$work/core/lib src
  if [ ! -f "$d/.done" ]; then
    rf_libsrc_take || { echo "core: no rust-src library (bin/repr-rust src-take)" >&2; return 1; }
    src=${RF_MONO_LIB:-$libsrc}
    rm -rf "$d"; mkdir -p "$d"
    cp -r "$src/core" "$d/core"
    ln -s "$src/stdarch" "$d/stdarch"; ln -s "$src/portable-simd" "$d/portable-simd"
    sed -i 's/|this| RawWaker::new(this, &VTABLE),/|this| RawWaker::new(this, loop {}),/' "$d/core/src/task/wake.rs"
    : > "$d/.done"
  fi
  echo "$d"
}

core_modules() {
  local lib m=$work/core/modules.txt
  lib=$(core_lib) || return 1
  if [ ! -s "$m" ]; then
    (cd "$root" && GOMEMLIMIT=1800MiB arbace_tool -m arbace.rust.front.core-check "$lib" --list > "$m.tmp" 2> "$m.err") || { cat "$m.err" >&2; return 1; }
    mv "$m.tmp" "$m"
  fi
  echo "$m"
}

core_oracle() {
  local lib o=$work/core/oracle f
  lib=$(core_lib) || return 1
  rm -rf "$o"; mkdir -p "$o/mir"
  [ -x "$helper" ] || { echo "core-oracle: no helper (bin/repr-rust helper)" >&2; return 1; }
  # the helper as rustc, as build-oracles runs it (its driver leaves trimmed_def_paths off, so the dumps print full paths as the
  # programs' oracles do; rustc's own driver turns it on)
  # shellcheck disable=SC2086
  (cd "$o" && RUSTC_BOOTSTRAP=1 "$helper" rustc -- $core_flags -o core.rmeta "--remap-path-prefix=$lib=library" -Zdump-mir-dir=mir \
     "$lib/core/src/lib.rs" > rustc.log 2>&1) || { echo "core-oracle: rustc failed (see $o/rustc.log)" >&2; return 1; }
  (cd "$o/mir" && for f in *; do printf '%s\t' "$f"; head -1 "$f" | sed 's/^\/\/ MIR for `\(.*\)` after built$/\1/'; done) > "$o/index.tsv"
  echo "core-oracle: $(wc -l < "$o/index.tsv") bodies"
}

core_flags="--edition 2024 --crate-type lib --crate-name core -A warnings -C opt-level=0 -C debuginfo=0 -C overflow-checks=off --emit=metadata -Zdump-mir=built"
core_pin=$root/test/rust/records/rust-core-built.sha256

# the entry's key: the copy of core's sources (the patched wake.rs among them), the helper, the flags
core_key() {
  local lib; lib=$(core_lib) || return 1
  { (cd "$lib/core" && find . -type f -name '*.rs' -print0 | LC_ALL=C sort -z | xargs -0 sha256sum); rfr_helper_hash; echo "$core_flags"; } | sha256sum | cut -c1-64
}

# the first slice's bodies: the oracle's files whose key starts with a listed module's path (`ops-arith-`; lib.rs's: none)
core_records_make() {
  local o=$work/core/oracle d=$work/core/records/make key m p
  [ -f "$o/index.tsv" ] || { echo "core-records: no oracle (bin/rust-front core-oracle first)" >&2; return 1; }
  key=$(core_key) || return 1
  rm -rf "$d"; mkdir -p "$d/rust-core-built"
  : > "$d/prefixes"
  while read -r m; do
    p=${m%.rs}; p=${p%/mod}; p=${p//\//-}
    echo "core.$p-" >> "$d/prefixes"; echo "core.$p." >> "$d/prefixes"
  done < "$root/test/rust/front/core-modules.txt"
  awk -F'\t' 'NR==FNR {pre[NR]=$0; n=NR; next} {for (i=1; i<=n; i++) if (index($1, pre[i]) == 1) {print; break}}' "$d/prefixes" "$o/index.tsv" > "$d/rust-core-built/index.tsv"
  # one member a module (its bodies' dumps one after another, each after a line `//// FILE`): a short manifest
  rm -rf "$d/rust-core-built/mir"; mkdir -p "$d/rust-core-built/bodies"
  while read -r m; do
    p=${m%.rs}; p=${p%/mod}; p=${p//\//-}
    awk -F'\t' -v a="core.$p-" -v b="core.$p." 'index($1, a) == 1 || index($1, b) == 1 {print $1}' "$d/rust-core-built/index.tsv" |
      while read -r f; do printf '//// %s\n' "$f"; cat "$o/mir/$f"; done > "$d/rust-core-built/bodies/$p.mir"
  done < "$root/test/rust/front/core-modules.txt"
  (cd "$d/rust-core-built" && find . -type f -printf '%P\n' | LC_ALL=C sort | awk -v k="$key" '{print $0 "\t" k}') > "$d/ids"
  oracle_make "$core_pin" "$d" "$d/ids" "bin/rust-front core-records make" rustc
}

# the pack staged: prints the directory holding mir/ and index.tsv; STALE when the key moved
core_records_stage() {
  local man key got u
  man=$(oracle_manifest "$core_pin")
  [ -f "$core_pin" ] || { echo "core-check: no pin $core_pin (bin/rust-front core-records make, by hand with rustc)" >&2; return 1; }
  key=$(core_key) || return 1
  got=$(awk -F'\t' '!/^#/ && $1 == "index.tsv" {print $2; exit}' "$man")
  [ "$got" = "$key" ] || { echo "core-check: STALE: rust-core-built (core's copy, the helper or the flags changed since the record)" >&2; return 1; }
  u=$(oracle_unpack "$core_pin") || return 1
  echo "$u/rust-core-built"
}

core_check() {
  local lib m fd od=$work/core/oracle
  lib=$(core_lib) || return 1
  m=$(core_modules) || return 1
  fd=$(records_front) || return 1
  if [ "${1:-}" = --records ]; then shift; od=$(core_records_stage) || return 1; fi
  [ -f "$od/index.tsv" ] || { echo "core-check: no oracle (bin/rust-front core-oracle, by hand with rustc; or --records)" >&2; return 1; }
  if [ $# -eq 0 ]; then set -- $(sed "s|^|$lib/core/src/|" "$root/test/rust/front/core-modules.txt"); fi
  cd "$root" && GOMEMLIMIT=${GOMEMLIMIT:-1800MiB} arbace_tool -m arbace.rust.front.core-check "$lib" --check "$fd" "$od" "${CORE_CACHE_DIR:-$work/core}" "$m" "$@" 2>&1 |
    grep --line-buffered -v -E '^Reflection warning|recur arg|^Auto-boxing'
}

# RF12-P1 (a): the essential step rust-core-essential: core-check --records over test/rust/front/core-essential.txt (no helper, no
# rustc: stubs first on PATH, their log empty), its per-module lines and total held to test/rust/front/core-essential-known.txt
# (--record rewrites it, by hand), the crossed control (an equal body's text against another body's rustc text) differing
core_essential() {
  local rec=0 ess=$work/core-essential stubs st=0 lib known=$root/test/rust/front/core-essential-known.txt t calls
  [ "${1:-}" = --record ] && rec=1
  mkdir -p "$ess"; stubs=$ess/stubs; mkdir -p "$stubs"
  for t in rustc rustdoc cargo gcc cc clang; do printf '#!/bin/sh\necho "stub $0 called" >> "%s/stubs.log"\nexit 99\n' "$stubs" > "$stubs/$t"; chmod +x "$stubs/$t"; done
  : > "$stubs/stubs.log"
  lib=$(core_lib) || return 1
  # its own caches, made afresh (core's index from the sources, no native interface of another run)
  rm -rf "$ess/cache"; mkdir -p "$ess/cache"
  # shellcheck disable=SC2046
  CORE_CACHE_DIR=$ess/cache PATH=$stubs:$PATH core_check --records $(sed "s|^|$lib/core/src/|" "$root/test/rust/front/core-essential.txt") > "$ess/check.log" 2>&1 ||
    { echo "rust-core essential: FAIL: core-check exited ($ess/check.log)"; st=1; }
  grep -E '^core-check: (core::|total)' "$ess/check.log" > "$ess/summary.txt"
  cat "$ess/summary.txt"
  if grep -q '^core-check: control .*differs, as it must' "$ess/check.log"; then echo "rust-core essential: control: differs, as it must"
  else echo "rust-core essential: FAIL: the crossed control did not differ ($ess/check.log)"; st=1; fi
  calls=$(wc -l < "$stubs/stubs.log")
  [ "$calls" = 0 ] || { echo "rust-core essential: FAIL: $calls calls of a stubbed toolchain ($stubs/stubs.log)"; st=1; }
  if [ $rec = 1 ]; then cp "$ess/summary.txt" "$known"; echo "rust-core essential: recorded $known"
  elif ! diff "$known" "$ess/summary.txt" > "$ess/known.diff"; then
    echo "rust-core essential: FAIL: the summary differs from $known:"; head -20 "$ess/known.diff"; st=1
  else echo "rust-core essential: the summary is test/rust/front/core-essential-known.txt's"; fi
  return $st
}

# RF12-P6: bin/rust-front core-export: the core bodies of test/rust/front/core-native.txt (a regex of names, then module files)
# built by core-check through the 21 passes and written with their equality to rustc's built MIR (the pack rust-core-built)
# into .tmp/rust-front/core/native-core.edn, which `run-built --native-core` takes (RF_CORE_NATIVE) in place of the record's
# core bodies where equal; about 40 minutes (num/mod.rs), 2.3 GB at GOMEMLIMIT=2000MiB
core_export() {
  local lib spec=$root/test/rust/front/core-native.txt re files out=$work/core/native-core.edn
  lib=$(core_lib) || return 1
  re=$(grep -v '^#' "$spec" | head -n 1)
  files=$(grep -v '^#' "$spec" | tail -n +2 | sed "s|^|$lib/core/src/|")
  rm -f "$out"
  # shellcheck disable=SC2086
  CORE_ONLY="$re" CORE_EXPORT_NAMES="$re" CORE_EXPORT=$out core_check --records $files
  [ -s "$out" ] && echo "core-export: $(grep -c '^(core/body' "$out") bodies in $out"
}

# RF19: bin/rust-front core-attrs: the codegen attributes of core's functions after expansion (the macro-made ones core's sources
# do not show as items: int_impl!'s and uint_impl!'s methods, the operator impls' macros, cmp's), from the interface pass (no body
# built) over test/rust/front/core-attrs.txt's module files, into .tmp/rust-front/core/core-attrs.edn ({def-path [names]}), which
# rsi-build gives mono (RF_CORE_ATTRS); kept by a key (the list, the copy of core's sources, the front end's expansion, resolution,
# lowering and collection; not mono.ae, whose local-attrs names them) in core-attrs.edn.key; light: one process, about 23
# minutes (num/mod.rs 18), 2.3 GB
core_attrs_key() {
  local lib=$1
  { cat "$root/test/rust/front/core-attrs.txt"
    (cd "$lib/core/src" && grep -v '^#' "$root/test/rust/front/core-attrs.txt" | xargs sha256sum)
    (cd "$root" && sha256sum arbace/rust/front/{expand,expand_derive,expand_format,resolve,lower,collect,core_check}.ae)
    cat "$(records_front)/std-iface.edn" | sha256sum; } | sha256sum | cut -c1-64
}

core_attrs() {
  local lib m fd out=$work/core/core-attrs.edn key
  lib=$(core_lib) || return 1
  m=$(core_modules) || return 1
  fd=$(records_front) || return 1
  key=$(core_attrs_key "$lib")
  if [ -s "$out" ] && [ "$(cat "$out.key" 2>/dev/null)" = "$key" ]; then echo "core-attrs: current ($out)"; return 0; fi
  rm -f "$out" "$out.key"
  (cd "$root" && GOMEMLIMIT=${GOMEMLIMIT:-1800MiB} arbace_tool -m arbace.rust.front.core-check "$lib" --attrs "$fd" "$work/core" "$m" "$out" \
     $(grep -v '^#' "$root/test/rust/front/core-attrs.txt" | sed "s|^|$lib/core/src/|") 2>&1 | grep --line-buffered -v -E '^Reflection warning|recur arg|^Auto-boxing')
  [ -s "$out" ] || { echo "core-attrs: no $out"; return 1; }
  echo "$key" > "$out.key"
}
