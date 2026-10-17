Arbace in the box on the Mac (Hypervisor.framework, Apple silicon)
===================================================================

Built on Linux by bin/box-mac-bundle (doc/go/BOX-NOTES.md, "S4"). The guest is
Arbace built with TamaGo for arm64, the same image KVM/arm64 runs; the launcher
is the monitor for macOS (Hypervisor.framework through purego, no cgo).

On the Mac (macOS on Apple silicon; Xcode's command-line tools for codesign;
nc and perl are macOS's own):

    tar xzf arbace-box-mac-arm64.tar.gz
    cd arbace-box-mac-arm64
    ./mac.sh all          # check, sign, hv, checklist, rebuild, with the clock's
                          # hour beside them: about an hour and a quarter

then send back mac-results.tar.gz (in this directory): every step's output,
its status, the idle CPU and the times.

The rebuild (S8, doc/go/BOX-NOTES.md): Arbace's arm64 guest image rebuilt
inside the box from the sources in rebuild/store, j2g and repr/go in Arbace's own
guest, Go's compile, asm and link in tool guests; it must equal the image the
same steps gave on Linux with the host's tools (rebuild/expected.txt). Its
guests ask up to 14 GiB each; the spawns' budget is 60% of the Mac's memory.
The clock (HV6): an idle box's wall clock against the Mac's, once a minute for
an hour.

Steps one by one: ./mac.sh check | sign | hv | checklist | rebuild | clock.
After `sign`:

    ./mac.sh run                        # the REPL (rlwrap from Homebrew, if installed)
    ./mac.sh run -e '(+ 1 2)'
    ARBACE_BOX_NET=loopback ./mac.sh run
    ./arbace-box --store DIR put FILE   # the store's tool

The launcher reads its guest from arbace-box.elf beside it (the code signature
covers the launcher alone, so the guest is not appended as on Linux).
