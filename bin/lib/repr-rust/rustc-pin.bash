# The pinned rustc (RIND11 (a), the user 2026-11-18; doc/rust/RUST-NOTES.md, "The pinned rustc"): Alpine's rust 1.99.1-r0 with
# its cargo and the LLVM it was built with (llvm23-libs 23.1.3-r0, scudo-malloc 23.1.3-r0), the apks pinned by SHA-256 in
# tools/rsdump/rustc-apks.sha256, extracted into a tree under the main checkout's .tmp, never installed: the host's apk moves
# on (rust 1.100.0 since 2026-11-15) while tools/rsdump/RUSTC_VERSION stays. rustc, rustdoc and cargo find their own
# libraries through their RUNPATH ($ORIGIN/../lib: librustc_driver, libLLVM, libscudo of the tree); the rest (musl, libstdc++,
# libgcc, zlib, zstd, libffi, libxml2, curl, OpenSSL, sqlite, gcc as the linker) is the host's. Sourced after $root is set.
#
#   rustc_pin_dir     the apks' directory: ARBACE_RUSTC_APKS, else the main checkout's .tmp/rust-VERSION (VERSION the pin's)
#   rustc_pin_home    the extracted tree: ARBACE_RUSTC_HOME, else rustc_pin_dir's root/ (its usr/bin holds rustc, rustdoc, cargo)
#   rustc_pin_path    PATH gains the tree's usr/bin first when its rustc is at the pin; ARBACE_RUSTC (exported) names the rustc
#                     at the pin: the tree's, else the PATH's when it is at the pin, else rustc_pin_absent, a name no command has:
#                     bin/gate's host :rustc (test/gate/steps.edn's :host-ids) is then absent (no key, no run, no failure)
#   rustc_pin_take    the pinned apks of rustc_pin_dir checked by their SHA-256 and extracted into rustc_pin_home (when its
#                     marker root.extracted is not the pin file's digest); a missing apk: the message names bin/repr-rust rustc fetch
#   rustc_pin_fetch   the missing pinned apks downloaded into rustc_pin_dir (the main checkout only: a worktree never downloads)
#                     from ARBACE_RUSTC_MIRRORS (Alpine's mirrors; dl-cdn has dropped 1.99.1, the lagging mirrors keep it), then taken

. "$root/bin/lib/crops/take.bash" # crop_main
rustc_pin_absent=rustc-not-at-the-pin
rustc_pin_file=$root/tools/rsdump/rustc-apks.sha256
rustc_pin_mirrors_default="https://dl-cdn.alpinelinux.org/alpine/edge/main/x86_64 https://mirror.latigo.net/alpine/edge/main/x86_64 http://mirror.mobinhost.com/alpine/edge/main/x86_64"

rustc_pin_of() { sed -n "s/^$1 //p" "$root/tools/rsdump/RUSTC_VERSION"; }
rustc_pin_dir() { echo "${ARBACE_RUSTC_APKS:-$(crop_main)/.tmp/rust-$(rustc_pin_of version)}"; }
rustc_pin_home() { echo "${ARBACE_RUSTC_HOME:-$(rustc_pin_dir)/root}"; }
# rustc_pin_at EXE: status 0 when EXE runs and is the pinned build (its version and commit, and the pinned driver library
# librustc_driver-DRIVER.so in its sysroot's lib/)
rustc_pin_at() {
  local v s
  v=$("$1" --version 2>"$TMPDIR/rustc-pin.err") || return 1
  case $v in "rustc $(rustc_pin_of version) ($(rustc_pin_of commit) "*) ;; *) return 1 ;; esac
  s=$("$1" --print sysroot 2>"$TMPDIR/rustc-pin.err") || return 1
  [ -f "$s/lib/librustc_driver-$(rustc_pin_of driver).so" ]
}

rustc_pin_path() {
  local h p
  h=$(rustc_pin_home)
  if [ -x "$h/usr/bin/rustc" ] && rustc_pin_at "$h/usr/bin/rustc"; then
    case ":$PATH:" in ":$h/usr/bin:"*) ;; *) PATH=$h/usr/bin:$(printf %s ":$PATH:" | sed "s|:$h/usr/bin:|:|g; s|^:||; s|:\$||") ;; esac
    export PATH ARBACE_RUSTC=$h/usr/bin/rustc
    return 0
  fi
  p=$(command -v rustc 2>"$TMPDIR/rustc-pin.err") || p=
  if [ -n "$p" ] && rustc_pin_at "$p"; then export ARBACE_RUSTC=$p; else export ARBACE_RUSTC=$rustc_pin_absent; fi
}

rustc_pin_take() {
  local d h sha f want
  d=$(rustc_pin_dir); h=$(rustc_pin_home)
  want=$(sha256sum < "$rustc_pin_file" | cut -c1-64)
  mkdir -p "$d"
  (
    flock 9
    if [ "$(cat "$h.extracted" 2>"$TMPDIR/rustc-pin.err")" = "$want" ] && [ -x "$h/usr/bin/rustc" ]; then exit 0; fi
    while read -r sha f; do
      case $sha in ''|'#'*) continue ;; esac
      [ -f "$d/$f" ] || { echo "rustc pin: no $d/$f (tools/rsdump/rustc-apks.sha256): bin/repr-rust rustc fetch, in the main checkout" >&2; exit 1; }
      [ "$(sha256sum < "$d/$f" | cut -c1-64)" = "$sha" ] || { echo "rustc pin: $d/$f: SHA-256 is not $sha (tools/rsdump/rustc-apks.sha256)" >&2; exit 1; }
    done < "$rustc_pin_file"
    rm -rf "${h:?}.new.$$"; mkdir -p "$h.new.$$"
    while read -r sha f; do
      case $sha in ''|'#'*) continue ;; esac
      tar --warning=no-unknown-keyword -xzf "$d/$f" -C "$h.new.$$" || exit 1
    done < "$rustc_pin_file"
    rm -f "$h.new.$$"/.PKGINFO "$h.new.$$"/.SIGN.* "$h.new.$$"/.pre-* "$h.new.$$"/.post-*
    rm -rf "${h:?}"; mv "$h.new.$$" "$h"
    echo "$want" > "$h.extracted"
    echo "== rustc pin: $(rustc_pin_of version) extracted into $h" >&2
  ) 9> "$d/take.lock"
}

rustc_pin_fetch() {
  local d sha f m ok
  d=$(rustc_pin_dir)
  [ "$(crop_main)" = "$root" ] || [ -n "${ARBACE_RUSTC_APKS:-}" ] ||
    { echo "rustc pin: a worktree never downloads: fetch once in the main checkout (cd $(crop_main) && bin/repr-rust rustc fetch)" >&2; return 1; }
  mkdir -p "$d"
  while read -r sha f; do
    case $sha in ''|'#'*) continue ;; esac
    [ -f "$d/$f" ] && [ "$(sha256sum < "$d/$f" | cut -c1-64)" = "$sha" ] && continue
    ok=
    for m in ${ARBACE_RUSTC_MIRRORS:-$rustc_pin_mirrors_default}; do
      echo "== rustc pin: $f from $m" >&2
      if curl -sSfL -m 1800 -o "$d/$f.part.$$" "$m/$f" 2>"$TMPDIR/rustc-pin.err" &&
         [ "$(sha256sum < "$d/$f.part.$$" | cut -c1-64)" = "$sha" ]; then
        mv "$d/$f.part.$$" "$d/$f"; ok=1; break
      fi
      rm -f "$d/$f.part.$$"
    done
    [ -n "$ok" ] || { echo "rustc pin: $f at $sha on none of the mirrors (ARBACE_RUSTC_MIRRORS): another version is never taken" >&2; return 1; }
  done < "$rustc_pin_file"
  rustc_pin_take
}

# rustc_pin_relink DIR: an extracted apk's absolute links into /usr (rustc-dev's librustc_driver-*.so -> /usr/lib/..., the rust
# package's file that an install puts there) pointed at the pinned tree's file instead, when it is there: nothing is installed
rustc_pin_relink() {
  local h l t
  h=$(rustc_pin_home)
  [ -d "$h/usr" ] || return 0
  find "$1" -type l -lname '/usr/*' > "$TMPDIR/rustc-pin.links" 2>"$TMPDIR/rustc-pin.err" || return 0
  while IFS= read -r l; do
    t=$(readlink "$l")
    [ -e "$h$t" ] && ln -sfn "$h$t" "$l"
  done < "$TMPDIR/rustc-pin.links"
  return 0
}
