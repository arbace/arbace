# tamago/cmd/

Go's toolchain as Go forms (standalone S9, [doc/STANDALONE-NOTES.md](../../doc/STANDALONE-NOTES.md),
"S9: Go's toolchain as forms"; [doc/STANDALONE-SURVEY.md](../../doc/STANDALONE-SURVEY.md) §7, step
b1'): the packages of TamaGo's go1.27.1 `src/cmd/` that `cmd/compile`, `cmd/link` and `cmd/asm`
build from, converted by repr/go for the three configurations Arbace builds the tools for, linux/amd64
(the host's tools) and tamago/amd64, tamago/arm64 (the box's tool guests, `bin/box-tools`), and
held as one tree as `tamago/goroot/` is (S4's BX63). `bin/toolchain-vendor` converts, verifies,
prints and checks them. Laid out as SPEC §4.1 lays out packages, at GOROOT/src's import paths:

| root | what |
|---|---|
| `go/` | the 105 packages (`cmd/compile/...` 51, `cmd/internal/...` 25, `cmd/link/...` 20, `cmd/asm/...` 5, `cmd/vendor/golang.org/x/telemetry/...` 4) of `go list -deps cmd/compile cmd/link cmd/asm` under `cmd/` for any of the three configurations (the same 105 for each), each file's forms once (no file converts differently by configuration), a file some configurations build only beside them as its build constraints select it (`mapfile_mmap.go` for linux, `mapfile_read.go` for tamago, and so on: 4 packages), the package files merged (their files the union, without `:config` and `:init-order`), the assembly they build (`ssa`'s `flags_amd64_test.s`, `flags_arm64_test.s`, `obj/arm64`'s `asm_arm64_test.s`: in the package, though named `_test`) in its forms directory |
| `conf/CONF/go/` | the files whose forms differ by configuration, each configuration's at the same path (none today) |
| `src/cmd/` | GOROOT/src/cmd's files of no package that the build reads: `go.mod`, `go.sum`, `vendor/modules.txt`, and `compile/default.pgo` (the profile `cmd/compile` is built with, PGO), as they are |

Origin: TamaGo's Go distribution, https://github.com/usbarmory/tamago-go, `VERSION` "go1.27.1",
the local tree `/root/tamago-go` (Arbace's toolchain, `ARBACE_GOROOT`), its whole tree pinned by
`seed/goroot.sha256` and its `src/cmd/` by `bin/toolchain-vendor` (the SHA-256 of the sorted
SHA-256 list of every file under it). `cmd/vendor/golang.org/x/telemetry` is vendored there by Go
from https://go.googlesource.com/telemetry at the version `src/cmd/go.mod` names.

Derived on 2026-10-15 by `bin/toolchain-vendor convert` (`bin/repr-go read --goos GOOS --goarch
ARCH`, run in GOROOT/src/cmd, for each configuration; then merged): each file's forms the
converter's output as it writes it, the package files merged; Go's comments other than doc
comments are `;` comments in the forms, which the printer does not reproduce (SPEC §10.4). Not
converted: the tests, the toolchain's other commands (`cmd/go`, `cmd/vet` ...) and the packages
only they import.

Checks (`bin/toolchain-vendor`): `verify`, a fresh conversion is this tree byte for byte; `check`,
`compile`, `link` and `asm` built from the printed forms are byte-identical to the ones built
from the originals (both with empty build IDs), for each configuration: as host executables for
linux/amd64, as `bin/box-tools` builds the tool guests for tamago/amd64 and tamago/arm64.

Licence: Go's BSD 3-Clause License ("The Go Authors"), [LICENSE.md](../../LICENSE.md); the files
keep their notices as `;;` lines.
