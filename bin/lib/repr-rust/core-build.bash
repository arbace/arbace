# RF12 (doc/rust/FRONTEND-SURVEY.md, "As built: RF12 (first slice)"): the front end over core's own sources, module by module.
# Sourced by bin/rust-front (root, work, libsrc set there).
#
#   bin/rust-front core-oracle      the oracle tier, by hand: the helper as rustc 1.99.1 over the copy of rust-src's core, -Zdump-mir=built
#                                   (--emit=metadata, -C opt-level=0 -C debuginfo=0 -C overflow-checks=off, the paths remapped to
#                                   library/), into .tmp/rust-front/core/oracle/{mir/,index.tsv}; the copy patches the two statics of
#                                   task/wake.rs whose closures name themselves (rustc's dump cycles on them: E0391), the rest as pinned
#   bin/rust-front core-check [FILE...]
#                                   arbace.rust.front.core-check over core's module files (default: core-modules.txt's first-slice
#                                   list), one process, GOMEMLIMIT 1800MiB, std's data from the pack rust-front-std, the oracle above

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
  (cd "$o" && RUSTC_BOOTSTRAP=1 "$helper" rustc -- --edition 2024 --crate-type lib --crate-name core -A warnings -C opt-level=0 -C debuginfo=0 \
     -C overflow-checks=off --emit=metadata -o core.rmeta -Zdump-mir=built "--remap-path-prefix=$lib=library" -Zdump-mir-dir=mir \
     "$lib/core/src/lib.rs" > rustc.log 2>&1) || { echo "core-oracle: rustc failed (see $o/rustc.log)" >&2; return 1; }
  (cd "$o/mir" && for f in *; do printf '%s\t' "$f"; head -1 "$f" | sed 's/^\/\/ MIR for `\(.*\)` after built$/\1/'; done) > "$o/index.tsv"
  echo "core-oracle: $(wc -l < "$o/index.tsv") bodies"
}

core_check() {
  local lib m fd
  lib=$(core_lib) || return 1
  m=$(core_modules) || return 1
  fd=$(records_front) || return 1
  [ -f "$work/core/oracle/index.tsv" ] || { echo "core-check: no oracle (bin/rust-front core-oracle, by hand with rustc)" >&2; return 1; }
  if [ $# -eq 0 ]; then set -- $(sed "s|^|$lib/core/src/|" "$root/test/rust/front/core-modules.txt"); fi
  cd "$root" && GOMEMLIMIT=${GOMEMLIMIT:-1800MiB} arbace_tool -m arbace.rust.front.core-check "$lib" --check "$fd" "$work/core/oracle" "$m" "$@" 2>&1 |
    grep -v -E '^Reflection warning|recur arg|^Auto-boxing'
}
