;; the pack rt's IR (bin/llvmi ir-make rt; doc/llvm/LLVM-CROP-SURVEY.md, "As built: LLS31 (a)"): test/llvm/rt-builds.tsv's modules and Arbace's mutants
clang: Alpine clang version 23.1.5
rustc: rustc 1.99.1 (58cf8f9c3 2026-10-13) (Alpine Linux Rust 1.99.1-r0)
llvm-stress: LLVM version 23.1.5
torture BUILD: clang -O0 -g (g0) | -O2 -g (g2) | -O2 (o2) | -O2 -flto (lto) | -O2 --target=x86_64-pc-windows-msvc (win) -S -emit-llvm -std=gnu17 -w -fno-strict-aliasing -Wno-error=implicit-function-declaration -Wno-error=implicit-int -Wno-error=int-conversion -Wno-error=incompatible-pointer-types -Wno-error=return-mismatch -Wno-error=incompatible-function-pointer-types -ffile-prefix-map=TX/= -fdebug-compilation-dir=. NAME.c (TX: gcc-15.2.0.tar.xz's gcc/testsuite/gcc.c-torture/execute)
C++ BUILD: clang++ -O0 -g (g0) | -O2 -g (g2) | -O2 -flto (lto) | -O2 --target=x86_64-pc-windows-msvc (win) | -O0 --target=x86_64-apple-macosx -nostdlibinc -nostdinc++ -isystem TREE/... (mac; TREE: cpp_full_sysroot, the headers made without g++) -S -emit-llvm -std=c++23 -w -ffile-prefix-map=ROOT/= -fdebug-compilation-dir=. test/cpp/samples/NAME.cpp
Rust BUILD: rustc -C opt-level=0 -g (g0) | -C opt-level=2 -g (g2) | -C opt-level=2 -C lto=fat (lt) -C codegen-units=1 --emit=llvm-ir --crate-type=bin --edition 2021 --remap-path-prefix=ROOT/= test/rust/sample/shapes.rs
stress: llvm-stress -size=60 -seed=N
mutants (bin/repr-llvm mutants LIST OUT N FIRST FAMILY, arbace.repr.llvm.mutate; SET N FIRST FAMILY INPUT):
  a 340 0 base modules
  b 340 1000 base modules
  h1 600 500000 - modules
  h2 600 600000 - modules
  ctl 1 900016 base hw
  ctlh 1 900050 base hw
  ctlu 1 900055 base hw
modules: a build the compiler refuses is not one; the manifest rt.tsv: ID, SOURCE (mutant-SET:BASE), SOURCE-SHA256 (a mutant's base's IR), IR-SHA256
