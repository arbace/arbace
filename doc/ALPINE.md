# Arbace for Java 26 as an Alpine package

`dist/alpine/` packages Arbace for Java 26 for Alpine Linux edge as `arbace-java26`, and
`bin/alpine-package` builds and tests it here. This document records the design, the decisions
with their alternatives, and what was built and measured (2026-10-10).

## The model: Alpine's `clojure`

Alpine's `community/clojure` (aports master, `pkgver=1.12.6`) is an APKBUILD with two sources
pinned by sha512 (GitHub's archive of the tag `clojure-$pkgver` and the tools tarball), a
`build()` (`mvn -Plocal package`), a `package()` installing `clojure.jar` under
`/usr/share/clojure` and `clj`/`clojure` in `/usr/bin`, a `-doc` subpackage, `arch="noarch"`,
`license="EPL-1.0"`, and `depends="bash java-jdk rlwrap"`: it needs a Java package.

`arbace-java26` keeps the form and changes what it ships: the self-contained runtime image of
`bin/arbace-image` (a jlink'ed JDK 26, Arbace's jar, its JDK AOT cache, the launcher). Alpine edge
has `openjdk21` and `openjdk25` but no 26, and Arbace's classes are class file version 70 (Java
26) with an AOT cache tied to one JVM build, so a `java-jdk` dependency could not serve it anyway.

## The image: `java.base` and `jdk.unsupported`

The user decided (2026-10-10) that Arbace targets the server side only: every jlink'ed image
holds `java.base` and `jdk.unsupported` (the REPL's interrupt handler, `sun.misc.Signal`), and
everything else is optional, used when the runtime has it (a full JDK, or an image made with
`ARBACE_IMAGE_MODULES`). Before, `bin/arbace-image` took the modules `jdeps --print-module-deps`
found in the jar and added `jdk.unsupported.desktop`: ten modules with `java.desktop`,
`java.sql`, `java.xml` and what they require. What needed them, and what changed on the branch
(`doc/VENDOR-NOTES.md`, hand changes 14 to 16; released in `arbace-for-java-26-v3`):

- `java.desktop`: `bean` used `java.beans.Introspector`; it now finds the properties by
  reflection with the Introspector's rules (checked against it on 77 classes, 583 properties:
  equal except where an explicit `BeanInfo` exists, which only `java.desktop`'s
  `java.awt.Component` has). `arbace.inspector` and `arbace.java.browse-ui` (Swing) stay
  optional and say so: requiring them without `java.desktop` throws an
  `UnsupportedOperationException` naming the module and `ARBACE_IMAGE_MODULES`.
  `arbace.java.browse/browse-url` tries `xdg-open` first and already did without
  `java.awt.Desktop`.
- `java.sql`: `#inst` was tied to it (`instant.clj` loaded only when `java.sql.Timestamp`
  exists); now `#inst` (as `java.util.Date`) is always there, and `java.sql.Timestamp`'s
  printing and `read-instant-timestamp` load only with `java.sql`. `resultset-seq` loads
  without it (its hint resolves when it runs).
- `java.xml`: `arbace.xml` (and its SAX handler class) stays optional with the same clear error.
  Nothing in `arbace.core`, the REPL or `arbace.main` loads it.

The JDK 26.0.2 AOT class-linking problem that `jdk.unsupported.desktop` worked around (the
cache stopped the JVM at startup for some module sets, VENDOR-NOTES "The runtime image") does
not occur with `java.base,jdk.unsupported`: the cache keeps AOT class linking with no extra
module. It does for `java.base,java.sql,jdk.unsupported` (also tried: adding `jdk.attach` fixes
that set); `bin/arbace-image` keeps its fallback (training with `-XX:-AOTClassLinking`) for such
sets made with `ARBACE_IMAGE_MODULES`.

## `arb`: the REPL with line editing (3-r1)

As Alpine's `clojure` ships `clj` (`rlwrap` around `clojure`), the package ships `/usr/bin/arb`
(`dist/alpine/arb.sh`): `rlwrap -m -r -q '\"' -b "(){}[],^%#@\";:'"` around `/usr/bin/arbace`,
the flags `clj` uses (history, completion of words seen, Clojure's delimiters as word breaks);
without `rlwrap` it says so and runs `arbace`. `arbace` stays plain for scripts and pipes.
`depends="bash rlwrap"` (rlwrap, readline, libptytty, ncurses' terminfo: under 1 MiB, readline
already there through bash). The user's decision, 2026-10-10. A packaging change only: the same
tag, `pkgrel=1`. `arbace-java26-3-r1.apk` (x86_64): 31 MiB, installed 98 MiB, 21 packages in a
fresh root; every check of `bin/alpine-package` passes (start 190 ms with the cache, 622 ms
without); `arb` under a pseudo-terminal evaluates and recalls history (the up arrow).

## Decisions

**Name: `arbace-java26`.** Arbace's main line leaves the JVM; a later standalone package can
then be `arbace` without inheriting this one's versions. The command is still `arbace`
(`/usr/bin/arbace`); the two would conflict on it, which is right (one `arbace` command at a
time). Alternative: `arbace` with `pkgver=26.2` (Java feature, release); rejected for the
version clash with a future `arbace`.

**Version: the tag's number.** Releases are tags `arbace-for-java-26-vN`; `pkgver=N`
(`arbace-for-java-26-v3` is `3-r0`). A fix release `-v3.1` would be `3.1`, still a valid apk
version and ordered after `2`. A change of the bundled JDK (a newer Temurin 26.0.x) with the same
tag is a rebuild: `pkgrel` + 1, as Alpine bumps `pkgrel` when a dependency changes.

**Source: the tag's GitHub archive.**
`https://github.com/arbace/arbace/archive/refs/tags/arbace-for-java-26-v$pkgver.tar.gz`, saved as
`arbace-java26-$pkgver.tar.gz` (abuild refuses `v2.tar.gz`-style names), top directory
`arbace-arbace-for-java-26-v$pkgver`. GitHub makes it with `git archive` and GNU `gzip -n`
(level 6): `git archive --format=tar --prefix=arbace-<tag>/ <tag> | gzip -n` reproduces it byte
for byte (checked for v2, sha512 `2db89aad…`, and v3, `ecd20b98…`, each the same as the
download). So the checksum of a
release is known before (and without) downloading it, and `bin/alpine-package` builds offline
from the local tag. For a new tag: bump `pkgver`, `pkgrel=0`, run `bin/alpine-package
--checksum` (it downloads or makes the tarball and rewrites the sums). The APKBUILD cannot pin
the tarball of the tag that contains it, so it is bumped in a commit after the tag, as aports
does in its own repository.

**The JDK at build time: Eclipse Temurin 26 for Alpine, pinned.** Options considered:

1. jdk26u built from source inside `build()`: an hour-scale build (about 10 GB of disk) on
   every package build, for a JDK the package then throws away except for the image. Rejected.
2. An `openjdk26` APKBUILD of its own first, modelled on aports' `community/openjdk25` (the
   jdk26u tag's archive, `--with-jdk...` against the previous JDK as boot JDK, `-jdk`, `-jmods`,
   `-jre-headless` subpackages), then `makedepends="openjdk26-jdk openjdk26-jmods"`. This is the
   path for aports itself, where prebuilt binaries are not accepted, and the obvious one once
   Alpine ships `openjdk26`. Not made here: it is a second package to maintain for one
   consumer, its own heavy build per architecture (an aarch64 one only on an aarch64 builder or
   cross-compiled), and nothing in Arbace needs more than a stock JDK 26.
3. **A prebuilt JDK 26 for musl from a reputable vendor, pinned by sha512: chosen.** Eclipse
   Adoptium publishes Temurin `jdk-26.0.2.1+1` for `alpine-linux` on x64 and aarch64
   (`https://github.com/adoptium/temurin26-binaries/releases`, `api.adoptium.net` lists their
   SHA-256, which match). It is the same OpenJDK 26.0.2.1 as the development JDK (a local
   jdk26u build), and the JDK the branch's CI already uses (`.github/workflows/gate.yml`,
   `actions/setup-java` with `temurin`). Bellsoft Liberica and Azul Zulu also ship musl builds;
   Temurin is the vendor-neutral one.

Temurin 26 ships no `jmods/`: `jlink` links from the JDK's own run-time image (JEP 493). That
works, but jlink then cannot cross-link: the image is made by the JDK of the target
architecture, which is how a native builder works anyway.

**License:** `EPL-1.0 AND BSD-3-Clause AND Apache-2.0 AND GPL-2.0-only WITH
Classpath-exception-2.0 AND Zlib AND Unicode-3.0 AND MPL-2.0 AND (CC0-1.0 OR MIT) AND
GPL-3.0-or-later WITH GCC-exception-3.1`. From `LICENSE.md`: Arbace and its vendored Clojure,
spec.alpha and core.specs.alpha under EPL-1.0, ASM (`arbace.asm`) BSD-3-Clause, Guava's Murmur3
Apache-2.0. The image bundles the JDK, GPLv2 with the Classpath Exception (Alpine's own openjdk
packages say `GPL-2.0-with-classpath-exception`, which is not an SPDX identifier), and the
third-party code `java.base` carries (its `legal/java.base/*.md`): zlib, the Unicode data, CLDR
and ICU (Unicode-3.0), the Public Suffix List (MPL-2.0), SipHash (CC0-1.0 or MIT), c-libutl
(BSD-3-Clause) and GCC's runtime (GPL-3.0-or-later with the runtime library exception). With
the ten-module image the list also had FreeType, libjpeg, LittleCMS, HarfBuzz (`java.desktop`),
Xerces, Xalan and the W3C DOM (`java.xml`). The notices are the image's `legal/<module>/`
(jlink copies them), installed with the binaries in the main package at
`/usr/lib/arbace-java26/legal/`, with Arbace's `LICENSE.md` beside them in `legal/arbace/`;
`LICENSE.md` is also in `/usr/share/licenses/arbace-java26/` (moved to `-doc` by abuild, as for
every package).

**Layout.**

```
/usr/bin/arbace                                exec /usr/lib/arbace-java26/bin/arbace "$@"
/usr/lib/arbace-java26/                        the image, as bin/arbace-image made it
  bin/arbace, bin/java, bin/keytool            the launcher (bin/arbace of the tag), the JVM
  lib/modules, lib/server/libjvm.so, ...       the jlink'ed JDK: java.base, jdk.unsupported
  lib/arbace/arbace.jar, lib/arbace/arbace.aot Arbace and its AOT cache
  legal/                                       the JDK's notices, legal/arbace/LICENSE.md
/usr/share/doc/arbace-java26/                  README.md, doc/ (arbace-java26-doc)
/usr/share/licenses/arbace-java26/LICENSE.md   (arbace-java26-doc)
```

A directory of its own under `/usr/lib` like Alpine's JDKs (`/usr/lib/jvm/java-25-openjdk`);
the image is relocatable, so the path is free. `/usr/bin/arbace` is a two-line `sh` wrapper, not
a symlink: the launcher finds the image from its own path (`$(dirname "$0")/..`), which a
symlink in `/usr/bin` would break.

**The AOT cache, read-only.** The JVM only maps the cache (`-XX:AOTCache`), so a root-owned
file in `/usr/lib` works for every user. Two things stood in the way, both from apk:

- abuild archives every file with mtime `$SOURCE_DATE_EPOCH` (`apk_tar --mtime`, for
  reproducible packages), and the JVM takes a cache only when each class path jar has the
  mtime and size it had at training (`aotClassLocation.cpp`, "This file is not the one used
  while building"). So `build()` dates the image's jar `@$SOURCE_DATE_EPOCH` and trains the
  cache again (`bin/arbace --aot-train`, with the options `bin/arbace-image` found it needs);
  the installed jar then has exactly that mtime. The JDK's `lib/modules` is not checked
  by time, and the cache applies after the move to `/usr/lib/arbace-java26` (the JVM matches
  the class path by its common prefix with the run-time image, as `bin/arbace-image` relies on).
- The launcher used the cache only when it was newer than the jar (`-nt`); with equal mtimes
  it never was. Since v3 it is "not older than the jar" (v2 needed a patch); the JVM's own
  check still guards against a wrong cache.

`check()` runs the image with `-XX:AOTMode=on`, which fails rather than run without the cache,
and the test of the installed package does the same.

**`arch="x86_64 aarch64"`**, not `noarch`: the image is native code. `options="!strip"` keeps
the image as jlink made it and as its cache was trained (Temurin's libraries are stripped
already). Like Alpine's openjdk packages, `sonameprefix="arbace-java26:"` and
`ldpath=/usr/lib/arbace-java26/lib:...` keep the image's own libraries (`libjvm.so`,
`libjava.so`, ...) to themselves instead of offering them as `so:libjvm.so` to other packages.

**Dependencies.** `scanelf` over the image: every library needs only musl
(`libc.musl-*.so.1`) and the image's own libraries (zlib is bundled in `libzip.so`). abuild
traces them: `depends` are `bash` (the launcher is a bash script) and
`so:libc.musl-<arch>.so.1`. (The ten-module image also needed `so:libasound.so.2`, alsa-lib, for
`java.desktop`'s `libjsound.so`; under emulation that missing library in the build root even
stopped the aarch64 package at abuild's dependency tracing.) `makedepends="bash coreutils cpio
diffutils findutils unzip"`: what `bin/build-arbace` uses besides the JDK, which comes as a
source.

**`-doc`** holds `README.md` and `doc/` (the journal, the spec, the notes), and the license file.
No `-dev`: there are no headers or link libraries; the jar carries the `.clj` sources already.

## Building it here: `bin/alpine-package`

All abuild state lives in the checkout's `.tmp/alpine/`: `ABUILD_USERDIR` with a throwaway key
made by `abuild-keygen -a -n`, `SRCDEST` (the sources), `REPODEST` (the repository
`arbace/<arch>/`), and a copy of `dist/alpine/` as the aport `aports/arbace/arbace-java26/`
(abuild writes `src/` and `pkg/` next to the APKBUILD). `abuild -F` (it runs as root here), under
`flock` on the main checkout's `.tmp/heavy.lock`, with `APK="apk --keys-dir .tmp/alpine/keys"` so
that abuild's indexing trusts the throwaway key alone, and `SOURCE_DATE_EPOCH` set to the tag's
commit time (abuild's default, the APKBUILD's commit or mtime, changes with every copy). Nothing
goes to `~/.abuild` or `/etc/apk/keys`. The tag's tarball is made from the local tag, the JDK
downloaded once.

The test installs the package into a fresh root (`.tmp/alpine/root-<arch>`: `apk --root
--initdb` with Alpine edge's main and community and the local repository; Alpine's keys and the
throwaway public key in that root's `/etc/apk/keys` only) and runs it there with `proot`
(unprivileged chroot; with `-q qemu-aarch64` for aarch64).

## Results (x86_64)

The release `arbace-for-java-26-v3` (tag `7aaedeb` on commit `71ffc9f`, pushed 2026-10-10)
holds the base image and the launcher change, so the APKBUILD is `pkgver=3`, `pkgrel=0`, with
no patches. Its source, `https://github.com/arbace/arbace/archive/refs/tags/arbace-for-java-26-v3.tar.gz`,
has sha512 `ecd20b985bc9eb59…` three ways: the download, `git archive | gzip -n` of the tag,
and the tarball `bin/alpine-package` made. Built from it on the development machine (64 cores,
shared, Alpine edge, abuild 3.18.0_rc7):

- `arbace-java26-3-r0.apk`: 33,432,584 bytes (31.9 MiB), installed 98 MiB; `-doc` 204,468
  bytes. `abuild -F` 1 min 52 s. Depends `bash`, `so:libc.musl-x86_64.so.1`. In a fresh root
  with no Java package: `-e`, the REPL, the cache required, all the base-image checks below;
  start 198 ms with the cache, 600 ms without.

Before the release, v2 with the two patches that v3 now contains (`base-image.patch`,
`launcher-aot-mtime.patch`, since removed), measured in detail:

- `abuild -F`: 1 min 52 s (once the heavy-run lock was free), most of it the bootstrap (stages
  1-3 identical at 5,780 classes, the verifier clean, native tests 49/2,847, the jar equal to
  stage 3's), then the image (jlink from Temurin's run-time image: `java.base,jdk.unsupported`;
  the cache trained with AOT class linking, no fallback), the cache trained again against the
  dated jar, and `check()`.
- `arbace-java26-2-r0.apk`: 31.9 MiB (33,433,941 bytes); installed 98 MiB (the image:
  `lib/modules` 25 MB, `libjvm.so` 29 MB, the cache 36 MB, the jar 7.5 MB).
  `arbace-java26-doc-2-r0.apk`: 188 KiB, installed 514 KB.
- Traced dependencies: `bash` and `so:libc.musl-x86_64.so.1`; `scanelf` finds no other library
  outside the image. In the test root, 18 packages in all (busybox, apk-tools, bash, ...) and no
  Java package.
- In that root: `arbace -e '(+ 1 2)'` prints `3`; the REPL reads stdin (`(def x 20)`,
  `(+ x 22)` gives `42`); with `-XX:AOTMode=on` it runs (`java.home` `/usr/lib/arbace-java26`,
  VM `26.0.2.1+1`), so the installed cache applies.
- On the installed image (`.tmp/basetest/run.sh`, not committed: the checks of the base image):
  `#inst` reads and prints as `java.util.Date` and round-trips, also through `arbace.edn`;
  `read-instant-timestamp` is absent and `java.sql.Timestamp` not found; `bean` of a `File`, a
  `Thread` and a `Long`; `arbace.repl` (`doc`), `arbace.pprint`, a script requiring
  `arbace.string`, `set`, `walk`, `data`, `zip`, `test`, `java.io`, `java.shell`, `spec.alpha`,
  `core.reducers`, `datafy`, `reflect`, `math`; `resultset-seq` is defined and fails only when
  called (`ClassNotFoundException: java.sql.ResultSet`); `arbace.xml`, `arbace.inspector` and
  `arbace.java.browse-ui` fail to load with the `UnsupportedOperationException` naming their
  module; `arbace.java.browse` loads.
- Startup, `arbace -e nil`, mean of 10, the installed image run directly (not under proot): 194 ms
  with the AOT cache, 620 ms without (`ARBACE_AOT=off`).

The ten-module image against the base image, both made by `bin/arbace-image` from the same jar
with the development JDK (`ARBACE_IMAGE_MODULES=java.desktop,java.sql,jdk.unsupported.desktop`
for the old set), `-e nil`, two rounds of 20 launches each, machine shared:

| image | size | `lib/modules` | `.tar.gz` | `-e nil` with the cache | without |
|---|---:|---:|---:|---:|---:|
| ten modules (before) | 133 MB | 54 MB | 43 MB | 186-188 ms | 613-626 ms |
| `java.base,jdk.unsupported` | 100 MB | 25 MB | 32 MB | 180-186 ms | 607-622 ms |

The `.apk` went from 42 MiB (45,066,415 bytes; installed 132 MiB, plus alsa-lib) to 31.9 MiB
(installed 98 MiB). The start time is the same: the cache already held only what a launch
loads.

## aarch64

Made here, by emulation, from v2 with the two patches (the same files as v3; not rebuilt for v3,
37 minutes of emulation for an identical jar and image). Neither jlink nor the AOT cache can be made for aarch64 from x86_64
(Temurin's JDK has no jmods to cross-link with, and the cache must be trained by the target's
JVM), so the whole package is built as an aarch64 builder would build it: `bin/alpine-package
--arch aarch64` unpacks Alpine edge's aarch64 minirootfs (`alpine-minirootfs-20260805-aarch64`,
its published sha512 checked) into `.tmp/alpine/rootfs-aarch64`, adds `abuild`, `build-base`
(abuild requires it for an arch-specific package) and the makedepends with the root's own
`apk`, and runs the same `abuild -F` inside it with `proot -q qemu-aarch64` (qemu-user; nothing
changes in the host's binfmt settings). Temurin's aarch64 JDK runs the bootstrap, jlink and the
training, all emulated.

- `abuild -F`: 37 min 13 s (against 1 min 52 s natively on x86_64): stages identical at 5,780
  classes, the verifier clean (50 to 61 s per stage against about 1 s), native tests 49/2,847,
  the image `java.base,jdk.unsupported` with AOT class linking.
- The jar is byte for byte the one built on x86_64 with Temurin and the one built with the
  development JDK (sha256 `7269e45c…`): the bootstrap's output depends neither on the
  architecture nor on the JDK build.
- `arbace-java26-2-r0.apk` (aarch64): 31.1 MiB (32,598,167 bytes), installed 96 MiB; `-doc` 188
  KiB. Depends `bash`, `so:libc.musl-aarch64.so.1`.
- Installed into a fresh aarch64 root and run there under qemu-user: `arbace -e '(+ 1 2)'`, the
  REPL on stdin, the cache required (`-XX:AOTMode=on`; VM `26.0.2.1+1`, `os.arch` `aarch64`),
  `#inst` and `bean`, `arbace.xml`'s clear failure. Start times under emulation mean little:
  4.2 s with the cache, 8.1 s without (mean of 3).

An earlier attempt with the ten-module image stopped at abuild's dependency tracing: the
build root lacked `libasound.so.2`, which `java.desktop`'s `libjsound.so` needs; the base
image has no such library.

For releases, a native aarch64 builder is better than 37 minutes of emulation: for instance an
`alpine:edge` container on GitHub's `ubuntu-24.04-arm` runners running the same `abuild`.

## For the user

Decided and open:

- **Released**: `arbace-for-java-26-v3`; the name `arbace-java26` with `pkgver` = the tag's
  number and Temurin 26.0.2.1 as the build JDK were accepted (2026-10-10).
- **Where to publish** (decided later): the packages are signed with a throwaway key in `.tmp/alpine/abuild/`.
  A real repository needs a kept key (its public half published for `/etc/apk/keys`) and a
  place to serve `arbace/<arch>/APKINDEX.tar.gz` (e.g. GitHub release assets or Pages). For
  aports itself, the prebuilt JDK would have to become an `openjdk26` package first.
- **aarch64 release builds**: emulation here works but takes 37 minutes (see above); a native
  aarch64 builder is the better home, e.g. an `alpine:edge` container on GitHub's
  `ubuntu-24.04-arm` runners, running the same `abuild`.
- **The base image** is in v3; main gets the same changes separately (there as hand changes
  15-17, main having its own 14).
