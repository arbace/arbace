# tamago/

The box guest's own Go as Go forms (B1b stage S6, [doc/go/BOX-NOTES.md](../doc/go/BOX-NOTES.md),
"S6"; [doc/go/BOX-SURVEY.md](../doc/go/BOX-SURVEY.md) §6): the standard library of TamaGo's Go
distribution, the runtime included, and TamaGo's `goos` module, converted by g2a for
`tamago/amd64` and `tamago/arm64` and held as one tree (S4's BX63). These forms are the source of what the box's guest is built with: `bin/arbace-go
--build --box` prints them back to Go (`bin/tamago-vendor print`) and builds the guest with that
GOROOT and that module, for either architecture (`ARBACE_BOX_GO=orig` builds from the
toolchain's tree and Go's module cache instead). Two source roots of forms, laid out as SPEC §4.1
lays out packages:

| root | what | from |
|---|---|---|
| `goroot/go/` | every package of `go list std` with Go files for `tamago/amd64` or `tamago/arm64` (371 and 370: the runtime, `internal/...`, `vendor/golang.org/x/...` and the rest; not `cmd/`; not the two that embed third-party data and that only `cmd/trace` or tests import, `internal/trace/traceviewer` and `encoding/json/internal/jsontest`), each package's assembly and headers (`:other-files`) and embedded files in its forms directory (SPEC §9.5, §9.6); each file's forms once where both architectures convert it alike, a file one architecture only builds beside them as its build constraints select it, the package files merged (their files the union, without `:config` and `:init-order`) | TamaGo's go1.27.1 tree, `src/` |
| `goroot/arch/ARCH/go/` | the 11 files both architectures build whose forms differ by the configuration's facts only (constants' values, a selector's path through embedded fields: `internal/goarch/goarch`, `runtime/extern`, `poly1305` ...), each architecture's as converted, at the same paths; printed laid over `goroot/go/` (either architecture's give the same Go) | the same |
| `goroot/src/` | GOROOT/src's files that are no package's: `go.mod`, `go.sum`, `vendor/modules.txt`, and `runtime/cgo/abi_amd64.h`, `abi_arm64.h` (included by the runtime's `asm_ARCH.s`; `runtime/cgo` has no Go files with cgo off), as they are | the same |
| `goos/go/` | the module's packages that build for `tamago/amd64` or `tamago/arm64` (75: 69 and 62; `goos`, the `runtime/goos` a guest names as `GOOSPKG`, and `amd64`, `kvm/...`, `soc/intel/...`, `arm64`, `arm/...`, `board/...` for those machines; not the other ISAs' boards and SoCs) with their assembly, merged as `goroot/go/` (no file differs by architecture) | `github.com/usbarmory/tamago` v1.27.1 |
| `goos/go.mod` | the module's `go.mod`, as it is | the same |

Origin:
- TamaGo's Go distribution, https://github.com/usbarmory/tamago-go, `VERSION` "go1.27.1" (time
  2026-08-28T16:20:06Z), the local tree `/root/tamago-go` (Arbace's toolchain, `G2A_GOROOT`),
  pinned by `bin/tamago-vendor` as the SHA-256 of the sorted SHA-256 list of every file of its
  `src/` but `src/cmd/`: `415d2301a19dfc6890100c6dc5e04496856c29cb77db3ab822168a916a6f5202`.
- TamaGo's `goos` module, https://github.com/usbarmory/tamago v1.27.1 (go.sum
  `h1:Zl4w3hu5DaogGvcLzunS4gjwNEqdjFFJedMO3pDQa5k=`), from Go's module cache.

Derived on 2026-10-11 (amd64) and 2026-10-12 (arm64, BX63) by `bin/tamago-vendor convert`
(`bin/g2a convert --corpus ARCH` for std, `bin/g2a convert --goos tamago --goarch ARCH ./...` in a
copy of the module, for each architecture, then merged): each file's forms the converter's
output as it writes it, the package files merged; Go's comments other than doc comments are `;`
comments in the forms, which the printer does not reproduce (SPEC §10.4). Not converted: the
tests (`_test.go`, `testdata/`), `cmd/`, and the files of other configurations.

Checks (`bin/tamago-vendor`): `verify`, a fresh conversion is this tree byte for byte; `check`,
std printed equals GOROOT/src at gocmp's three levels per package (the round trip's reference),
the module's packages likewise, and the box's guest image built from the printed forms is
byte-identical to the one built from the originals (both without build IDs), each for amd64 and
arm64.

Licence: Go's BSD 3-Clause License (std, "The Go Authors") and the BSD 3-Clause License of The
TamaGo Authors (the module), [LICENSE.md](../LICENSE.md); the files keep their notices as `;;`
lines.
