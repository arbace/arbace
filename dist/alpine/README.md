# Arbace for Java 26 on Alpine Linux

An Alpine package (`.apk`) of Arbace for Java 26, for Alpine edge, made like Alpine's own
`clojure` package (an APKBUILD with its sources pinned by sha512, `build()`, `package()`, a `-doc`
subpackage), but shipping the self-contained runtime image of `bin/arbace-image`: a jlink'ed
JDK 26 (`java.base` and `jdk.unsupported`), Arbace's jar and its JDK AOT cache. It needs no Java
package (Alpine edge has openjdk21 and openjdk25, no 26).

- `APKBUILD`: package `arbace-java26`, version `$pkgver` = the release tag's number
  (`arbace-for-java-26-v2` is `2`), for x86_64 and aarch64.
- `arbace.sh`: `/usr/bin/arbace`, which runs the image's launcher.
- `base-image.patch`, `launcher-aot-mtime.patch`: the branch `apk26`'s changes to the tag v2
  (they go away with the next tag): the image of `java.base` and `jdk.unsupported` only, with
  `bean`, `#inst` and the optional namespaces adapted (doc/VENDOR-NOTES.md, hand changes 14-16);
  the launcher takes the AOT cache when it is not older than the jar (apk installs every file
  with the same mtime).

Build and test it here, with a throwaway key and all abuild state under `.tmp/alpine/`:

```sh
bin/alpine-package                  # x86_64 (2 min): abuild, then install into a fresh root, test
bin/alpine-package --arch aarch64   # aarch64, built and tested under qemu-user (proot; 37 min)
bin/alpine-package --checksum       # after bumping pkgver to a new tag: rewrite the sha512sums
```

The packages land in `.tmp/alpine/packages/arbace/<arch>/`. Installed:

```
/usr/bin/arbace                          the command (arbace -e '(+ 1 2)', a REPL, scripts)
/usr/lib/arbace-java26/                  the image: bin/java, bin/arbace, lib/modules, ... (98 MB)
/usr/lib/arbace-java26/lib/arbace/       arbace.jar and arbace.aot (the AOT cache, read-only)
/usr/lib/arbace-java26/legal/            the JDK's legal notices, and arbace/LICENSE.md
/usr/share/doc/arbace-java26/            README.md and doc/ (arbace-java26-doc)
/usr/share/licenses/arbace-java26/       LICENSE.md (arbace-java26-doc)
```

The design, the decisions and the measurements are in `doc/ALPINE.md`.
