#!/bin/bash
# The re-seed's refusals (queue 037's findings 1, 2, 5 and 7; doc/C23-SURVEY.md C9.4, CS255-CS257): on a fabricated git
# checkout (bin/seed and its libraries copied, bin/jrt's overlay and Go's toolchain stubbed), bin/seed --make
#   refuses a G3 the fixpoint did not vouch for (no fixpoint.edn, or one of another version or executable) when it
#   writes the committed seed, and packs it into another archive with a warning;
#   refuses a checkout that is not clean (an untracked file, a modified one) or not a git checkout;
#   packs the same tar from a setgid .tmp (directories' special bits cleared);
# bin/arbace-go's fixpoint (extracted, its build stubbed) writes g3's printed-by and fixpoint.edn only when G2 = G3; and
# bin/lib/version.bash's arbace_version abbreviates the describe to 12 and honours ARBACE_VERSION only for its commit,
# clean, of its runtime. No executable, seconds.
# By hand: test/gate/seed-make-test.bash [CHECKOUT]   (another checkout's scripts to test, e.g. an older one's copy)
set -uo pipefail
src=$(cd "${1:-$(dirname "$0")/../..}" && pwd)
export TMPDIR=${TMPDIR:-$src/.tmp/tmp}; mkdir -p "$TMPDIR"
T=$(mktemp -d "$TMPDIR/seed-make-test.XXXXXX"); trap 'rm -rf "$T"' EXIT
fails=0
ok() { echo "ok:   $1"; }
bad() { echo "FAIL: $1"; fails=$((fails + 1)); }
unset ARBACE_VERSION ARBACE_SEED_GO ARBACE_SEED_GO_BUILD ARBACE_HOST_EXE
P=$T/root
mkdir -p "$P/bin/lib" "$P/seed" "$P/arbace/lang" "$P/go" "$T/goroot/src/runtime"
cp "$src/bin/seed" "$P/bin/"; for f in tmpdir renamed-env seed-go version; do cp "$src/bin/lib/$f.bash" "$P/bin/lib/"; done
echo go1.27.1 > "$T/goroot/VERSION"; echo 'package runtime' > "$T/goroot/src/runtime/x.go"
export ARBACE_GOROOT=$T/goroot
echo "$(root=$P bash -c ". '$P/bin/lib/seed-go.bash'; seed_go_goroot_hash '$T/goroot'")  go1.27.1" > "$P/seed/goroot.sha256"
echo '(ns arbace.lang.x)' > "$P/arbace/lang/x.ae"
# bin/jrt overlay: the overlay file's path, runtime/ and reflect/ beside it
mkdir -p "$T/ovl/runtime" "$T/ovl/reflect"; echo 'package runtime' > "$T/ovl/runtime/p.go"; echo 'package reflect' > "$T/ovl/reflect/r.go"
printf '#!/bin/bash\necho %s/ovl/overlay.json\n' "$T" > "$P/bin/jrt"; chmod +x "$P/bin/jrt"
printf '.tmp/\ntarget/\n' > "$P/.gitignore"
git -C "$P" init -q && git -C "$P" add -A && git -C "$P" -c user.name=t -c user.email=t@t commit -qm one || { echo "FAIL: git"; exit 1; }
h=$(root=$P bash -c ". '$P/bin/lib/seed-go.bash'; seed_go_host_arch")
g3=$P/target/arbace-go/fixpoint/g3
mkdir -p "$g3/work-$h/mod/cmd/arbace" "$g3/j2g/prog" "$g3/$h"
echo 'module arbace' > "$g3/work-$h/mod/go.mod"; echo 'package main' > "$g3/work-$h/mod/cmd/arbace/main.go"
echo arbace.core > "$g3/j2g/prog/namespaces.txt"; printf '#!/bin/sh\n' > "$g3/$h/arbace"; chmod +x "$g3/$h/arbace"
version=$(cd "$P" && bash -c ". bin/lib/version.bash; arbace_version '$P'")
make() { (cd "$P" && ARBACE_SEED_GO_BUILD=$g3 bin/seed --make "$@") > "$T/make.log" 2>&1; }
vouch() { printf '{:holds true :g3-sha256 "%s" :version %s}\n' "$(sha256sum < "$g3/$h/arbace" | cut -c1-64)" "${1:-$version}" > "$g3/fixpoint.edn"; }

# finding 1: the describe at 12, ARBACE_VERSION honoured only for this commit, clean, this runtime
c=$(git -C "$P" rev-parse HEAD)
case $version in *":describe \"${c:0:12}\""*) ok "the describe abbreviated to 12 ($c)" ;; *) bad "describe not at 12: $version" ;; esac
given=${version/:describe \"${c:0:12}\"/:describe \"arbace-reaps-4-crops-v1-3-g${c:0:12}\"}
got=$(cd "$P" && ARBACE_VERSION=$given bash -c ". bin/lib/version.bash; arbace_version '$P'" 2>&1)
[ "$got" = "$given" ] && ok "ARBACE_VERSION of this commit honoured (another describe)" || bad "ARBACE_VERSION not honoured: $got"
other=${version/$c/0123456789012345678901234567890123456789}
(cd "$P" && ARBACE_VERSION=$other bash -c ". bin/lib/version.bash; arbace_version '$P'") > "$T/v.log" 2>&1 \
  && bad "ARBACE_VERSION of another commit honoured" || ok "ARBACE_VERSION of another commit refused"
rt=${version/:runtime \"/:runtime \"0}
(cd "$P" && ARBACE_VERSION=${rt/%\"\}/\"\}} bash -c ". bin/lib/version.bash; arbace_version '$P'") > "$T/v.log" 2>&1 \
  && bad "ARBACE_VERSION of another runtime honoured" || ok "ARBACE_VERSION of another runtime refused"

# finding 2: a G3 the fixpoint did not vouch for
rm -f "$g3/fixpoint.edn"
make && bad "--make packed an unvouched G3 into the committed seed" || { [ -f "$P/seed/arbace-seed-go.tar.xz" ] && bad "an archive written" || ok "an unvouched G3 refused for the committed seed"; }
make "$P/.tmp/out/a.tar.xz" && grep -q 'warning:.*fixpoint.edn' "$T/make.log" && ok "an unvouched G3 packed into another archive, with a warning" || bad "another archive: $(tail -2 "$T/make.log")"
vouch "${version/:dirty false/:dirty false }"
make && bad "a fixpoint.edn of another version accepted" || ok "a fixpoint.edn of another version refused"
vouch; printf '#!/bin/sh\n# rebuilt\n' > "$g3/$h/arbace"
make && bad "a fixpoint.edn of another executable accepted" || ok "a fixpoint.edn naming another executable refused"
printf '#!/bin/sh\n' > "$g3/$h/arbace"; vouch

# finding 7: a tree not clean
echo x > "$P/go/extra.ae"
make && bad "--make on an untracked file" || ok "an untracked file refused"
rm "$P/go/extra.ae"; echo '(ns arbace.lang.x) ;' > "$P/arbace/lang/x.ae"
make && bad "--make on a modified file" || ok "a modified file refused"
git -C "$P" checkout -q arbace/lang/x.ae

# a vouched G3 of a clean checkout: packed into the committed seed
make && [ -s "$P/seed/arbace-seed-go.tar.xz" ] && ok "a vouched G3 packed into the committed seed" || bad "a vouched G3: $(tail -3 "$T/make.log")"
xz -dc "$P/seed/arbace-seed-go.tar.xz" | tar -xOf - arbace-seed-go/SEED.edn | grep -qF ":version $version" && ok "SEED.edn records the version" || bad "SEED.edn's version"
git -C "$P" checkout -q seed; git -C "$P" clean -qfd seed

# finding 5: a setgid .tmp gives the same tar
make "$P/.tmp/out/b.tar.xz" || bad "--make b: $(tail -2 "$T/make.log")"
chmod 2755 "$P/.tmp"
make "$P/.tmp/out/c.tar.xz" || bad "--make c: $(tail -2 "$T/make.log")"
xz -dc "$P/.tmp/out/b.tar.xz" > "$T/b.tar"; xz -dc "$P/.tmp/out/c.tar.xz" > "$T/c.tar"
cmp -s "$T/b.tar" "$T/c.tar" && ok "the same tar from a setgid .tmp" || bad "the tars differ under a setgid .tmp: $(tar -tvf "$T/c.tar" | head -1)"

# outside git: refused
rm -rf "$P/.git"
make "$P/.tmp/out/d.tar.xz" && bad "--make outside git" || ok "--make outside git refused"

# finding 2: the fixpoint writes g3's printed-by and fixpoint.edn only when G2 = G3
F=$T/fp; mkdir -p "$F/bin/lib"; cp "$src/bin/lib/version.bash" "$F/bin/lib/"
printf 'arbace_host_exe() { echo %s/host-exe; }\n' "$T" > "$F/bin/lib/host.bash"; printf 'host\n' > "$T/host-exe"
printf 'target/\n' > "$F/.gitignore"
git -C "$F" init -q && git -C "$F" add -A && git -C "$F" -c user.name=t -c user.email=t@t commit -qm fp || bad "git of the fixpoint's checkout"
sed -n '/^fixpoint() {/,/^}/p' "$src/bin/arbace-go" > "$T/fixpoint.bash"
grep -q '^fixpoint() {' "$T/fixpoint.bash" || bad "no fixpoint() in $src/bin/arbace-go"
fp_run() { # SAME: G3 the same as G2 (1) or not (0); prints g3's files after the run
  (root=$F discard=$T/discard same=$1
   host_arch() { echo amd64; }; gate_key() { echo k; }; since() { echo 0s; }
   arbace_version() { echo '{:describe "x" :commit "c" :dirty false :runtime "r"}'; }
   build() { mkdir -p "$dir/amd64" "$dir/j2g"; echo r > "$dir/j2g/report.edn"
             case $dir in */g3) [ "$same" = 1 ] && echo g > "$dir/amd64/arbace" || echo g3 > "$dir/amd64/arbace" ;; *) echo g > "$dir/amd64/arbace" ;; esac
             echo "bin/arbace-go --build: j2g and repr/go on the host executable" > "$dir/printed-by"; }
   . "$T/fixpoint.bash"; fixpoint > "$T/fp.log" 2>&1
   ls "$F/target/arbace-go/fixpoint/g3"; grep -c 'Go-hosted fixpoint' "$F/target/arbace-go/fixpoint/g3/printed-by")
}
out=$(fp_run 0 2>&1)
case $out in *fixpoint.edn*) bad "a fixpoint that did not hold wrote fixpoint.edn" ;; *) ok "a fixpoint that did not hold: no fixpoint.edn" ;; esac
[ "$(printf '%s\n' "$out" | tail -1)" = 0 ] && ok "a fixpoint that did not hold: g3's printed-by not the fixpoint's" || bad "printed-by: $out"
out=$(fp_run 1 2>&1)
case $out in *fixpoint.edn*) ok "a fixpoint that held wrote fixpoint.edn" ;; *) bad "a fixpoint that held: $out $(tail -3 "$T/fp.log")" ;; esac
[ "$(printf '%s\n' "$out" | tail -1)" = 1 ] && ok "a fixpoint that held: g3's printed-by the fixpoint's" || bad "printed-by: $out"
grep -q ':holds true :g3-sha256 "[0-9a-f]\{64\}" :version {:describe "x"' "$F/target/arbace-go/fixpoint/g3/fixpoint.edn" && ok "fixpoint.edn names G3's hash and the version" || bad "fixpoint.edn: $(cat "$F/target/arbace-go/fixpoint/g3/fixpoint.edn")"
# queue 132: a fixpoint that held on a checkout not clean (an untracked file, which describe --dirty does not see) vouches for nothing
echo x > "$F/bin/lib/extra.bash"
out=$(fp_run 1 2>&1)
case $out in *fixpoint.edn*) bad "a fixpoint that held on an untracked file wrote fixpoint.edn" ;; *) ok "a fixpoint that held on an untracked file: no fixpoint.edn" ;; esac
grep -q 'not clean' "$T/fp.log" && ok "the fixpoint says why G3 is not vouched" || bad "no word of the unclean checkout: $(tail -2 "$T/fp.log")"
rm "$F/bin/lib/extra.bash"

[ "$fails" = 0 ] && { echo "PASS: seed-make-test"; exit 0; }
echo "FAIL: seed-make-test ($fails)"; exit 1
