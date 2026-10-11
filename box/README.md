# box/

go-whim's box, vendored as Go forms (B1b stage S0, [doc/go/BOX-NOTES.md](../doc/go/BOX-NOTES.md);
decisions D3 (a) and D8, [doc/go/BOX-SURVEY.md](../doc/go/BOX-SURVEY.md) §10): the hypervisor API,
the monitor, the doorbell ABI and the TamaGo board on which a Go program runs as a guest of
/dev/kvm or Hypervisor.framework. `box/` is a source root of Go forms of its own, laid out as
SPEC §4.1 lays out packages (`box/go/<import path>.ae`), like `overlay/`: outside `go/`, so that
jrt's module (`bin/jrt`, which prints `go/`) and the Go build are unchanged until stage S1 brings
the box into a program.

| package (import path) | forms | from go-whim |
|---|---|---|
| `arbace/box/hv` | `go/arbace/box/hv.ae`, `hv/` | `hv/` |
| `arbace/box/vmm` | `go/arbace/box/vmm.ae`, `vmm/` | `vmm/` (not `vmm/cmd/whim-guest`) |
| `arbace/box/abi` | `go/arbace/box/abi.ae`, `abi/` | `guest/abi/` |
| `arbace/box/board` | `go/arbace/box/board.ae`, `board/` (with `boot_amd64.s`, `boot_arm64.s`) | `guest/tamago/board/` |
| `arbace/box/libsys` | `go/arbace/box/libsys.ae`, `libsys/` | `internal/libsys/` |

Origin: https://github.com/arbace/go-whim (local clone `/root/go-whim`) at
`80c49bf53cd6340487ac623f3d6d135b77ac1db6` (2026-10-06; the box's files last changed at
`815e11a`), the paths above, with their tests (`_test.go`). Derived on 2026-10-11 by
`bin/box-vendor convert`: the packages moved to the module `arbace` under `arbace/box/`, every
import of them rewritten (the only change to their text), and converted by `bin/g2a convert
--tests` for linux/amd64, linux/arm64, darwin/arm64, tamago/amd64 and tamago/arm64, the five
trees merged into one (the file forms are identical across configurations; the package files
carry no `:config`, and each file's build constraints select it, as gc does). The assembly
(`board/*.s`) is copied as it is (SPEC §9.5). Go's other comments are `;` comments in the forms,
which the printer does not reproduce (SPEC §10.4).

`bin/box-vendor check` proves the tree: printed back (`bin/g2a build --print-only --tests
--layout lines`), each package equals go-whim's at gc's three levels (gocmp, the round trip's
oracle), go-whim's monitor and TamaGo guests built from the printed files are byte-identical to
go-whim's own (without build IDs), and go-whim's tests pass on them, on /dev/kvm.

Not standalone yet: `vmm` imports go-whim's editor (`github.com/arbace/go-whim/editor`: its
`Config.Host` is an `editor.Host`, and `editor.Exit` ends a run), which stays in go-whim; stage
S1 makes the dispatch generic. On darwin, `hv` and `libsys` import purego
(`github.com/ebitengine/purego` v0.11.1), not vendored.

Licence: the Eclipse Public License 1.0, as Arbace ([LICENSE.md](../LICENSE.md)): the user's own
code, licensed so by the user on 2026-10-11 (D8). Maintained by hand from here on; go-whim keeps
its own copy, and nothing flows between them automatically.
