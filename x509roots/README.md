# x509roots/

Mozilla's trusted roots for jrt's TLS (doc/go/BOX-NOTES.md, "TLS (option 2)", BX73), vendored as
Go forms: a source root of Go forms of its own (`x509roots/go/<import path>.ae`), like `box/`
and `tamago/`, which the Go build adds to jrt's program (`bin/build-go --root x509roots`: the
native executables, the box's guest, `bin/jrt`, the j2g checks).

| package (import path) | forms | from |
|---|---|---|
| `arbace/x509roots/fallback/bundle` | `go/arbace/x509roots/fallback/bundle.ae`, `bundle/` (with `bundle.der`, embedded) | `golang.org/x/crypto/x509roots/fallback`'s `bundle/` |

Origin: the module `golang.org/x/crypto/x509roots/fallback` (https://go.googlesource.com/crypto,
directory `x509roots/fallback`) at `3b414e3e8284a71c817c4d1009acdd5d3a464ff9` (2026-10-11), version
`v0.0.0-20261011195143-3b414e3e8284`, go.sum `h1:vbII1qQTqQ3ya1k/bfJWuHiUNzCkFD7LRbGiO3gSwVE=`,
downloaded into Go's module cache by `go mod download` from proxy.golang.org on 2026-10-12. Of the
module, only its package `bundle` (`roots.go`, the generated `bundle.go`, `bundle.der`: 120 roots
of Mozilla's NSS trust store as DER, with their distrust-after constraints, 128,016 bytes) and its
tests; not its package `fallback`, whose `init` calls `x509.SetFallbackRoots` (jrt gives its
roots to each connection instead: `jrt.TLSRoots`, go/arbace/jrt/tls.ae), nor `bundle126_test.go`
(`//go:build !go1.27`, which the conversion for go1.27.1 leaves out).

Derived on 2026-10-12 by `bin/x509roots-vendor convert`: the package copied into a module
`arbace`, its import path rewritten to `arbace/x509roots/fallback/bundle` (the only change to its
text: none of its files imports it, its tests name it), converted by `bin/repr-go read --tests` for
linux/amd64 (its files build alike for every target), the package file written as
`bin/box-vendor` writes box/'s (no `:config`, no `:init-order`), `bundle.der` copied into the forms
directory (SPEC §9.6; its hash is in `:embed-files`). Go's comments are `;` comments in the forms.

`bin/x509roots-vendor verify` converts afresh and compares (byte for byte); `bin/x509roots-vendor
check` prints the forms and compares them with the renamed original (gocmp's tree, export and code
levels, with tests) for linux/amd64, linux/arm64, tamago/amd64 and tamago/arm64, and runs the
package's tests on the printed copy.

To take a newer bundle (Mozilla's store changes a few times a year; Go publishes a new version of
the module each time): set `mod_ver` and `mod_zip` in `bin/x509roots-vendor` to it, `convert`,
`check`, and record it here, in LICENSE.md and in the journal.

Licence: Go's BSD 3-Clause License (LICENSE.md, "x509roots/").
