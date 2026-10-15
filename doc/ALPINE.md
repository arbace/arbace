# Alpine environment requirements

The host Arbace is developed on, and what of it Arbace needs. As of 2026-10-12.

## The host

- Alpine Linux edge, x86_64 (`/etc/alpine-release` 3.25.0_alpha20260805), kernel
  `linux-virt` (6.18), with /dev/kvm (nested KVM under the host's hypervisor).
- Repositories (`/etc/apk/repositories`): edge's `main`, `community` and `testing`
  (https://dl-cdn.alpinelinux.org/alpine/edge/).

## The host's packages as one line

`apk list --installed` shows 560 packages; `/etc/apk/world`, what was asked for by name, holds
84. 17 of these are already installed by the dependencies of others (bash, ca-certificates,
cloud-utils-growpart, curl, e2fsprogs, file, ghc, git, linux-headers, make,
openssh-sftp-server, python3, rlwrap, rust, tar, tzdata, unzip), which leaves 67:

```sh
apk add alex alpine-base alpine-sdk alsa-lib-dev atop autoconf automake bat bubblewrap cabal \
  cargo chez-scheme chrony clang clojure cloud-init coreutils cpio cups-dev dhcpcd diffutils \
  doas dosfstools e2fsprogs-extra fastfetch findutils fontconfig-dev gcc-cross-embedded gdb \
  github-cli go grep grub-efi happy hdparm htop hyperfine joker linux-virt maven mosh mount \
  newlib nvme-cli ocaml5 openjdk25-src openssh openssh-server-pam perf pigz procps proot py3-pip \
  qemu-aarch64 qemu-guest-agent qemu-system-aarch64 reptyr rsync sed strace tmux tree ugrep vim \
  wget zellij zip
```

None of the 67 is implied by the others, so no shorter list of world's names gives the same
installation. The line's dependency closure is 485 packages, and the other 75 come by
`install_if`: the `-openrc` service scripts, the `-pyc` caches, `git-perl`, `gcc-gdb` and the
like. apk adds those on its own when what they name is present.

Packages installed on the host after 2026-10-12 for the user's own use (ffmpeg, for screen
recordings; world holds 85 since) are outside Arbace's set and not in the line.

`bin/alpine-env` derives and checks it:

- `world` prints the line, computed from `/etc/apk/world` and `/lib/apk/db/installed`.
- `check` confirms, offline, that the line's closure with `install_if` is exactly the installed
  set.
- `simulate` runs `apk add --simulate` of the line into an empty root under `.tmp/alpine-env/`,
  against the repositories, and compares the result with the host.

On 2026-10-12 `simulate` installed the same 560 packages by name. 18 came in newer versions than
the host has, because edge moved after the last upgrade (2026-10-10): abuild, abuild-sudo,
fakeroot, linux-virt, libcap2, libcap-getcap, libfdt and util-linux's packages (blkid,
libblkid, libfdisk, liblastlog2, libmount, libsmartcols, libuuid, mount, partx, sfdisk,
util-linux-dev).

The line therefore mirrors the host by name, not by version. Edge keeps no snapshots, so
versions cannot be pinned there. Arbace's own inputs that must not move are pinned outside apk
(below).

## What Arbace uses

Most of the 67 serve other work on this host: GHC and cabal, Rust, OCaml, Chez Scheme, joker,
cloud-init, the editors and the shells' tools. The table below lists what Arbace's scripts
(`bin/`) call, and what builds the inputs that apk does not provide. It is derived from the
scripts and has not yet been tried on a fresh host.

| Need | Packages |
|---|---|
| The scripts | `bash coreutils findutils grep sed diffutils tar xz cpio unzip` (flock, setsid and awk from busybox) |
| Helpers in some scripts (`box-suite`, `box-smoke`, `tamago-vendor`, `arbace-go`'s REPL check, `box-vendor`, `box-arm64-vm`, `net-check`, `alpine-env`) | `python3` |
| Sources, Clojure's suite's jars from Maven Central, the arm64 VM's netboot | `git curl ca-certificates` |
| The REPL's line editing (the executable runs its interactive REPL under it, doc/go/EXEC-NOTES.md; checked by `bin/arbace-go --smoke` and `bin/box-smoke`; optional) | `rlwrap` |
| arm64 executables run on amd64 (`bin/arbace-go`, `bin/jrt`, `bin/a2g-evalproof`) | `qemu-aarch64` |
| The arm64 box's checks (`bin/box-arm64-vm`) | `qemu-system-aarch64 openssh-client` |
| go-whim's KVM tests and jrt's tests use cgo; `readelf` | `gcc musl-dev linux-headers binutils` |
| jrt-convert's generator JDK, and the boot JDK for jdk26u | `openjdk25-jdk` (on this host by `openjdk25-src`) |
| Building jdk26u (headless) | `alpine-sdk autoconf zip alsa-lib-dev cups-dev fontconfig-dev` |
| Bootstrapping TamaGo's Go from source | `go` (Alpine's Go, used for nothing else) |

As one line:

```sh
apk add bash coreutils findutils grep sed diffutils tar xz cpio unzip python3 git curl \
  ca-certificates rlwrap qemu-aarch64 qemu-system-aarch64 openssh-client gcc musl-dev \
  linux-headers binutils openjdk25-jdk alpine-sdk autoconf zip alsa-lib-dev cups-dev fontconfig-dev go
```

## What apk does not provide

These are built or fetched once, and pinned where Arbace depends on their bytes:

- **JDK 26**: jdk26u (https://github.com/openjdk/jdk26u) at
  `baf63fbe42b8758448fee570a9d9bb914272d259`, in `/root/jdk26u`.
  - Configured with `--enable-headless-only --disable-warnings-as-errors`, boot JDK
    `/usr/lib/jvm/java-25-openjdk`.
  - Its image, `build/linux-x86_64-server-release/images/jdk`, is the `java` on PATH (26.0.2).
  - It ran the bootstrap, the tools and the oracle's reference until S4; since then (2026-10-12)
    it runs only the frozen jar of `arbace-for-java-26-v4`: the oracle's reference, j2a, a2g's edge
    (`bin/a2g-edge`), the fixtures' forms and the data generators.
  - Its sources are j2a's input (`bin/jrt-convert update`). Since S1, the build itself reads
    `jdk/`, not the checkout.
- **TamaGo's Go**, go1.27.1 (https://github.com/usbarmory/tamago-go, VERSION 2026-08-28), in
  `/root/tamago-go` (`G2A_GOROOT`).
  - It builds everything Go: the executables, the box, g2a's helpers and go-whim's monitor and
    tests.
  - Its `src/` is pinned by `seed/goroot.sha256` (checked by `bin/seed --build` and `--verify`;
    `--check` checks the seed's own hash), and its tree by `bin/tamago-vendor`.
  - Since 2026-10-12 nothing runs Alpine's `go`, so an upgrade of the `go` package changes
    nothing.
- **go-whim**: https://github.com/arbace/go-whim at `80c49bf`, in `/root/go-whim`
  (`bin/box-vendor`).
- **Go's module cache**: `/root/go/pkg/mod`. The builds run with `GOPROXY=off`, so it must hold
  these modules, each checked against the go.sum hash its script names:
  - TamaGo's goos module `github.com/usbarmory/tamago v1.27.1`;
  - `github.com/ebitengine/purego v0.11.1`;
  - `golang.org/x/crypto/x509roots/fallback v0.0.0-20261011195143-3b414e3e8284`;
  - go-whim's requirements (`remyoudompheng/bigfft` and five `modernc.org` modules).

  `bin/x509roots-vendor` and `bin/box-vendor` download theirs when missing.
- **Fetched by the scripts into `.tmp/`**:
  - Clojure's suite's jars from Maven Central (`bin/clojure-tests`, checked by SHA-1);
  - Alpine 3.24.2's aarch64 netboot for the arm64 VM (`bin/box-arm64-vm`).
- **/dev/kvm**, for the box on amd64 (`bin/arbace-go --smoke --box`, go-whim's tests).
