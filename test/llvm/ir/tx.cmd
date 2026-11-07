;; the pack tx's IR (bin/llvmi ir-make tx; doc/llvm/LLVM-CROP-SURVEY.md, "Decided (2026-11-06, the user: the torture corpora's IR pinned)")
clang: Alpine clang version 23.1.4
corpora: tx-O0
c-O0, c-O2: clang -O0|-O2 -S -emit-llvm -std=gnu23 -w -I test/c/synth -ffile-prefix-map=ROOT/= test/c/{synth,run}/*.c
tx-O0, tx-O2: clang -O0|-O2 -S -emit-llvm -std=gnu17 -w -fno-strict-aliasing -Wno-error=implicit-function-declaration -Wno-error=implicit-int -Wno-error=int-conversion -Wno-error=incompatible-pointer-types -Wno-error=return-mismatch -Wno-error=incompatible-function-pointer-types -ffile-prefix-map=TX/= {*,ieee/*}.c (TX: modernc.org/ccorpus2@v1.6.0/assets/gcc-9.1.0/gcc/testsuite/gcc.c-torture/execute)
cpp: clang++ -O0 -S -emit-llvm -std=c++23 -w -ffile-prefix-map=ROOT/= test/cpp/{samples,runtime/samples}/*.cpp
modules: a file clang refuses, or without a main, is not one; the manifest tx.tsv: ID, SOURCE, SOURCE-SHA256, IR-SHA256
