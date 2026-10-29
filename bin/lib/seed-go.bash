# The Go-source seed of the bootstrap (doc/STANDALONE-NOTES.md, "S4: the Go-source seed"; the
# standalone survey's D3): Arbace's Go program as j2g and repr/go print it, the module that
# bin/arbace-go --build compiles, packed into one deterministic archive pinned by its SHA-256.
# Sourced by bin/seed, with $root set to the repository root. Defines:
#
#   seed_go_make ARCHIVE    pack the last bin/arbace-go --build of this checkout (its host
#                           architecture's work module, the patched runtime of bin/jrt overlay,
#                           j2g's list of namespaces, the version map) into ARCHIVE
#   seed_go_build ARCHIVE OUT ARCH...
#                           stage 0: unpack ARCHIVE into OUT/src and build OUT/<arch>/arbace with
#                           Go's toolchain ($ARBACE_GOROOT, the version the seed names), as
#                           bin/arbace-go --build links its executables
#   seed_go_tar DIR         the deterministic tar of DIR's arbace-seed-go/ on standard output
#
# The archive (SA2): a POSIX tar, its members sorted by name, mtime 0, owner and group 0,
# modes 0644 and 0755, no atime or ctime, compressed by xz -9e with one thread (one block).
# Inside, arbace-seed-go/:
#   SEED.edn         what the seed is: its format, the version map linked into the executable,
#                    Go's version, the build's settings, what made it (read with read)
#   mod/             the module arbace, as bin/build-go --line-file printed it: go.mod,
#                    arbace/cmd/arbace (main, the image's types, the embedded namespaces' sources
#                    res/), arbace/lang (j2g's translation of Clojure's runtime), arbace/jrt (jrt's
#                    hand-written Go and the JDK's classes j2g translated, with their data files),
#                    arbace/x509roots; cmd/arbace/image.bin empty (SA3: the image is prepared by
#                    the built executable, not carried)
#   overlay/runtime/ the Go runtime's files patched by overlay/go (bin/jrt overlay), and
#   overlay/reflect/ reflect's file added by it (goi's run-time named types, GR9a; seeds before it
#                    have none), given to go build through an overlay file written for the
#                    toolchain's GOROOT: overlay/PKG/F.go is GOROOT/src/PKG/F.go
#   namespaces.txt   the embedded namespaces the image prepares (j2g's prog/namespaces.txt)

seed_go_top=arbace-seed-go
seed_go_format=1

# the Go toolchain: TamaGo's Go distribution, the one bin/arbace-go builds with (ARBACE_GOROOT)
seed_go_goroot() { echo "${ARBACE_GOROOT:-/root/tamago-go}"; }

# the toolchain's pin (SA4): seed/goroot.sha256 names Go's version and the SHA-256 of GOROOT's
# sources, defined as sha256sum's lines (SHA-256, two spaces, the path relative to GOROOT) of
# VERSION and every file under src/, in the C locale's byte order of their paths, hashed again
# (pkg/, bin/, caches and the rest of GOROOT do not count; src/ must hold only regular files and
# directories). The pin file's line: "<sha256>  <version>"
seed_go_pin=$root/seed/goroot.sha256

# seed_go_goroot_hash GOROOT: that hash (about 1-4 s for TamaGo's 12,175 files)
seed_go_goroot_hash() {
  local g=$1 odd
  [ -f "$g/VERSION" ] && [ -d "$g/src" ] || { echo "bin/seed: $g is not a GOROOT" >&2; return 1; }
  odd=$(cd "$g" && find src ! -type f ! -type d -print -quit)
  [ -z "$odd" ] || { echo "bin/seed: $g/$odd is neither a file nor a directory" >&2; return 1; }
  (cd "$g" && find VERSION src -type f -print0 | LC_ALL=C sort -z | xargs -0 sha256sum | sha256sum | cut -c1-64)
}

# seed_go_test_hash GOROOT: the hash of GOROOT/test seed/goroot-test.sha256 pins (GTS50 (a)): sha256sum's lines
# of every file under test/, in the C locale's byte order of their paths, hashed again (about 1 s for 3,516 files)
seed_go_test_hash() {
  local g=$1
  [ -d "$g/test" ] || { echo "bin/seed: $g has no test/" >&2; return 1; }
  (cd "$g" && find test -type f -print0 | LC_ALL=C sort -z | xargs -0 sha256sum | sha256sum | cut -c1-64)
}

# seed_go_toolchain_check [SEED.EDN]: GOROOT is the pinned toolchain (seed/goroot.sha256), and
# the one SEED.edn names when given; prints the hash
seed_go_toolchain_check() {
  local goroot have hv pv ph sv sh
  goroot=$(seed_go_goroot)
  [ -f "$seed_go_pin" ] || { echo "bin/seed: no toolchain pin $seed_go_pin" >&2; return 1; }
  ph=$(grep -v '^#' "$seed_go_pin" | awk 'NF {print $1; exit}'); pv=$(grep -v '^#' "$seed_go_pin" | awk 'NF {print $2; exit}')
  hv=$(head -1 "$goroot/VERSION"); have=$(seed_go_goroot_hash "$goroot") || return 1
  if [ "$hv" != "$pv" ] || [ "$have" != "$ph" ]; then
    echo "bin/seed: $goroot is $hv with sources $have; pinned $pv, $ph (seed/goroot.sha256)" >&2; return 1
  fi
  if [ -n "${1:-}" ]; then
    sv=$(seed_go_field "$1" go | tr -d '"'); sh=$(seed_go_field "$1" goroot-sha256 | tr -d '"')
    if [ "$sv" != "$hv" ] || [ "$sh" != "$have" ]; then
      echo "bin/seed: the seed wants Go $sv with sources ${sh:-(none recorded)}; $goroot is $hv, $have" >&2; return 1
    fi
  fi
  echo "$have"
}

# go_env ARCH: the build's environment for linux/ARCH (bin/arbace-go's go_env, GOOS linux)
seed_go_env() {
  local a=$1
  echo GOROOT="$(seed_go_goroot)" GOTOOLCHAIN=local GOFLAGS= GOWORK=off GO111MODULE=on GOPROXY=off \
    GOEXPERIMENT= CGO_ENABLED=0 GOOS=linux GOARCH="$a" \
    "$( [ "$a" = amd64 ] && echo GOAMD64=v1 || echo GOARM64=v8.0 )"
}

seed_go_host_arch() { case $(uname -m) in aarch64) echo arm64 ;; *) echo amd64 ;; esac; }

seed_go_tar() {
  local d=$1
  find "$d/$seed_go_top" -type d -exec chmod 0755 {} + && find "$d/$seed_go_top" -type f -exec chmod 0644 {} + || return 1
  LC_ALL=C tar -C "$d" --sort=name --format=posix \
    --pax-option=exthdr.name=%d/PaxHeaders/%f,delete=atime,delete=ctime \
    --mtime=@0 --owner=0 --group=0 --numeric-owner -cf - "$seed_go_top"
}

seed_go_make() {
  local archive=$1 host dir mod ovl rt nss version goversion goroothash t files bytes printedby
  host=$(seed_go_host_arch)
  # the build packed: the last bin/arbace-go --build, or ARBACE_SEED_GO_BUILD (the Go-hosted
  # fixpoint's G3, target/arbace-go/fixpoint/g3: bin/arbace-go --fixpoint, S3), whose file
  # printed-by, when present, says what printed it
  dir=${ARBACE_SEED_GO_BUILD:-$root/target/arbace-go}
  mod=$dir/work-$host/mod
  nss=$dir/j2g/prog/namespaces.txt
  [ -f "$mod/go.mod" ] && [ -f "$nss" ] && [ -x "$dir/$host/arbace" ] \
    || { echo "bin/seed: no build in $dir: run bin/arbace-go --build first" >&2; return 1; }
  # the overlay printed by repr/go, on the packed build's own executable when no host is named
  ovl=$(cd "$root" && ARBACE_HOST_EXE=${ARBACE_HOST_EXE:-$dir/$host/arbace} bin/jrt overlay | tail -1) || return 1
  rt=$(dirname "$ovl")/runtime
  [ -d "$rt" ] || { echo "bin/seed: no patched runtime in $rt" >&2; return 1; }
  [ -d "$(dirname "$ovl")/reflect" ] || { echo "bin/seed: no reflect overlay in $(dirname "$ovl")/reflect" >&2; return 1; }
  . "$root/bin/lib/version.bash"
  version=$(arbace_version "$root") || return 1
  case $version in *':dirty true'*) echo "bin/seed: warning: the checkout is dirty; the seed records it" >&2 ;; esac
  goversion=$(head -1 "$(seed_go_goroot)/VERSION")
  goroothash=$(seed_go_toolchain_check) || return 1
  printedby="bin/arbace-go --build: j2g and repr/go on the host executable"
  [ -s "$dir/printed-by" ] && printedby=$(cat "$dir/printed-by")
  t=$(mktemp -d "$root/.tmp/seed-go-make.XXXXXX") || return 1
  mkdir -p "$t/$seed_go_top/overlay"
  cp -r "$mod" "$t/$seed_go_top/mod"
  # the image is not carried (SA3): the executable built from the seed prepares it
  : > "$t/$seed_go_top/mod/cmd/arbace/image.bin"
  cp -r "$rt" "$t/$seed_go_top/overlay/runtime"
  cp -r "$(dirname "$ovl")/reflect" "$t/$seed_go_top/overlay/reflect"
  cp "$nss" "$t/$seed_go_top/namespaces.txt"
  files=$(find "$t/$seed_go_top" -type f | wc -l)
  bytes=$(find "$t/$seed_go_top" -type f -printf '%s\n' | awk '{s+=$1} END {print s}')
  cat > "$t/$seed_go_top/SEED.edn" <<EOF
;; The Go-source seed of Arbace's bootstrap (doc/STANDALONE-NOTES.md, "S4: the Go-source seed"),
;; made by bin/seed --make, built into stage 0 by bin/seed --build.
{:format $seed_go_format
 ;; the version map of the commit the seed was printed from, linked as main.arbaceVersion
 :version $version
 ;; the toolchain: TamaGo's Go distribution (github.com/usbarmory/tamago-go), GOROOT's VERSION
 :go "$goversion"
 ;; the SHA-256 of GOROOT's sources: VERSION and src/ (seed/goroot.sha256, bin/lib/seed-go.bash)
 :goroot-sha256 "$goroothash"
 :module "arbace"
 :main "arbace/cmd/arbace"
 :goos "linux"
 :goarch ["amd64" "arm64"]
 :env {"CGO_ENABLED" "0" "GOAMD64" "v1" "GOARM64" "v8.0" "GOEXPERIMENT" ""}
 :build ["-trimpath" "-buildvcs=false"]
 :ldflags "-s -w -X 'main.arbaceVersion=<:version>'"
 :overlay "overlay/PKG/*.go replace or add GOROOT/src/PKG/*.go (runtime, reflect)"
 :image "prepared by the executable built without it: ARBACE_PREPARE, requiring namespaces.txt"
 :printed-by "$printedby"
 :files $files
 :bytes $bytes}
EOF
  mkdir -p "$(dirname "$archive")"
  seed_go_tar "$t" | xz -9e -T1 -c > "$archive.new" || { rm -rf "$t"; return 1; }
  mv -f "$archive.new" "$archive"
  rm -rf "$t"
  echo "bin/seed: $archive ($(stat -c %s "$archive") bytes, $((files + 1)) files, $bytes bytes unpacked; SHA-256 $(sha256sum "$archive" | cut -d' ' -f1))"
}

# seed_go_field SEED.EDN KEY: a top-level field's text (the line ' :KEY value')
seed_go_field() { sed -n "s/^[{ ]:$2 //p" "$1" | head -1; }

seed_go_build() {
  local archive=$1 out=$2; shift 2
  local src=$out/src chk s goroot want have version ovl f a exe0 image nss t0
  goroot=$(seed_go_goroot)
  rm -rf "$out"; mkdir -p "$out"
  xz -dc "$archive" | tar -C "$out" -xf - || { echo "bin/seed: cannot unpack $archive" >&2; return 1; }
  mv "$out/$seed_go_top" "$src"
  s=$src/SEED.edn
  [ "$(seed_go_field "$s" format)" = "$seed_go_format" ] || { echo "bin/seed: $archive: format $(seed_go_field "$s" format), not $seed_go_format" >&2; return 1; }
  chk=$(seed_go_toolchain_check "$s") || return 1
  version=$(seed_go_field "$s" version)
  # the overlay file for this GOROOT (bin/jrt overlay's, with the seed's files: overlay/PKG/F.go
  # for GOROOT/src/PKG/F.go; a seed before GR9a has overlay/runtime/ only)
  ovl=$src/overlay/overlay.json
  {
    printf '{"Replace": {'
    local sep= pd
    for pd in "$src"/overlay/*/; do
      for f in "$pd"*.go; do
        [ -f "$f" ] || continue
        printf '%s\n "%s": "%s"' "$sep" "$goroot/src/$(basename "$pd")/$(basename "$f")" "$f"
        sep=,
      done
    done
    printf '}}\n'
  } > "$ovl"
  # without the image first (SA3): the host's executable prepares it, then each architecture's
  # executable is linked with it, as bin/arbace-go --build does
  image=$src/mod/cmd/arbace/image.bin
  exe0=$out/prepare/arbace
  t0=$(date +%s)
  if [ ! -s "$image" ]; then
    echo "== stage 0: go build $(seed_go_host_arch), without the image" >&2
    mkdir -p "$out/prepare"
    (cd "$src/mod" && env $(seed_go_env "$(seed_go_host_arch)") "$goroot/bin/go" build -trimpath -buildvcs=false \
       -overlay "$ovl" -o "$exe0" arbace/cmd/arbace) > "$out/build-prepare.log" 2>&1 \
      || { tail -20 "$out/build-prepare.log" >&2; echo "bin/seed: the build failed" >&2; return 1; }
    echo "== stage 0: prepare the namespaces (the image)" >&2
    nss=$(tr '\n' ' ' < "$src/namespaces.txt")
    printf '(doseq [n (quote [%s])]\n  (try (require n) (catch Throwable e (println "image: not prepared:" n (.getMessage e)))))\n' "$nss" \
      > "$out/prepare.ae"
    GOGC=${GOGC:-400} ARBACE_PREPARE=$image "$exe0" "$out/prepare.ae" > "$out/prepare.log" 2>&1 \
      || { tail -20 "$out/prepare.log" >&2; echo "bin/seed: prepare failed ($out/prepare.log)" >&2; return 1; }
    [ -s "$image" ] || { echo "bin/seed: prepare wrote no image ($out/prepare.log)" >&2; return 1; }
    ! grep '^image:' "$out/prepare.log" >&2
    rm -rf "$out/prepare"
  fi
  for a in "$@"; do
    echo "== stage 0: go build $a" >&2
    mkdir -p "$out/$a"
    (cd "$src/mod" && env $(seed_go_env "$a") "$goroot/bin/go" build -trimpath -buildvcs=false \
       "-ldflags=-s -w -X 'main.arbaceVersion=$version'" -overlay "$ovl" -o "$out/$a/arbace" arbace/cmd/arbace) \
       > "$out/build-$a.log" 2>&1 \
      || { tail -20 "$out/build-$a.log" >&2; echo "bin/seed: $a: the build failed" >&2; return 1; }
    echo "bin/seed: $out/$a/arbace ($(stat -c %s "$out/$a/arbace") bytes; SHA-256 $(sha256sum "$out/$a/arbace" | cut -d' ' -f1))" >&2
  done
  echo "   stage 0 in $(( $(date +%s) - t0 )) s" >&2
}
