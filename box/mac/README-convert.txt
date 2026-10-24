tamago/goroot converted in the box on the Mac (Hypervisor.framework, Apple silicon)
===================================================================================

Built on Linux by bin/box-mac-bundle --convert (doc/go/GO-PARSER-SURVEY.md,
GP8, GPS56 (c)). The guest is Arbace built with TamaGo for arm64; the launcher
is the monitor for macOS (Hypervisor.framework through purego, no cgo).

The conversion: tamago/goroot, the box guest's standard library as Go forms,
converted inside the box from the Go text of TamaGo's GOROOT/src (in
convert/store): a driver guest spawns std's two reads at once, for
tamago/amd64 and tamago/arm64 (repr/go's native reader, go/parser compiled,
goi's checker, the converter), each in a guest of its own, then assembles and
merges them. Every digest must equal what the same steps gave on Linux
(convert/expected.edn): each architecture's forms, and the tree, which is the
commit's tamago/goroot byte for byte.

On the Mac (macOS on Apple silicon; Xcode's command-line tools for codesign;
perl is macOS's own):

    tar xzf arbace-box-mac-arm64-convert.tar.gz
    cd arbace-box-mac-arm64-convert
    ./mac.sh convert      # check, sign, the conversion: minutes

It ends with PASS or FAIL, the digests and the times; then send back
mac-results.tar.gz (in this directory): every step's output and status, the
conversion's steps (convert-steps.edn), its log, its times and the launchers'
resident memory (sampled every 2 s).

The budgets (on an M2 Max, 12 CPUs: each read 6 threads in a 12-vCPU guest of
13 GiB, both at once, about 20 to 28 GB resident at most):

    CONVERT_JOBS=N        each read's threads (default half the Mac's CPUs)
    CONVERT_CPUS=N        each read's vCPUs (default twice the threads)
    CONVERT_RAM=13GiB     each read's guest
    CONVERT_SERIAL=1      one read after the other (half the memory; the
                          default on a Mac with under 40 GiB)
    CONVERT_DRY=1         check the bundle and print the command, nothing run

If a read's guest is refused its vCPUs or memory (the log names the request),
run it again with CONVERT_CPUS=8, or CONVERT_SERIAL=1.

After `./mac.sh sign`, ./mac.sh run starts the REPL in the box, as in the full
bundle.
