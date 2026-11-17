;; the pack tx's IR (bin/llvmi ir-make tx; doc/llvm/LLVM-CROP-SURVEY.md, "Decided (2026-11-06, the user: the torture corpora's IR pinned)")
clang: Alpine clang version 23.1.5
corpora: tx-O0,tx-O2
c-O0, c-O2: clang -O0|-O2 -S -emit-llvm -std=gnu23 -w -I test/c/synth -ffile-prefix-map=ROOT/= test/c/{synth,run}/*.c
tx-O0, tx-O2: clang -O0|-O2 -S -emit-llvm -std=gnu17 -w -fno-strict-aliasing -Wno-error=implicit-function-declaration -Wno-error=implicit-int -Wno-error=int-conversion -Wno-error=incompatible-pointer-types -Wno-error=return-mismatch -Wno-error=incompatible-function-pointer-types DG -ffile-prefix-map=TX/= {*,ieee/*}.c (TX: modernc.org/ccorpus2@v1.6.0/assets/gcc-9.1.0/gcc/testsuite/gcc.c-torture/execute; DG: the test's dg-options and dg-additional-options as bin/llvmi's tx_dg_options selects them, LLS54 (a))
dg-options (ID, the options added):
  tx-O0.20001121-1	-fgnu89-inline
  tx-O0.20020107-1	-fgnu89-inline
  tx-O0.20021127-1	-std=c99
  tx-O0.20040409-1w	-fwrapv
  tx-O0.20040409-2w	-fwrapv
  tx-O0.20040409-3w	-fwrapv
  tx-O0.20040709-3	-fno-common
  tx-O0.20050316-2	-mno-mmx
  tx-O0.20050604-1	-mno-mmx
  tx-O0.20101011-1	-fnon-call-exceptions
  tx-O0.920612-1	-fwrapv
  tx-O0.920711-1	-fwrapv
  tx-O0.930526-1	-fgnu89-inline
  tx-O0.930529-1	-fwrapv
  tx-O0.961223-1	-fgnu89-inline
  tx-O0.980608-1	-fgnu89-inline
  tx-O0.bcp-1	-fgnu89-inline
  tx-O0.eeprof-1	-finstrument-functions
  tx-O0.loop-2c	-fgnu89-inline
  tx-O0.lto-tbaa-1	-fno-ipa-cp
  tx-O0.p18298	-fgnu89-inline
  tx-O0.pr22493-1	-fwrapv
  tx-O0.pr23047	-fwrapv
  tx-O0.pr39501	-ffast-math
  tx-O0.pr53645	-std=gnu89
  tx-O0.pr53645-2	-std=gnu89
  tx-O0.pr57124	-fno-strict-overflow
  tx-O0.pr68381	-O -fexpensive-optimizations
  tx-O0.pr68390	-O2
  tx-O0.pr68506	-fno-builtin-abort
  tx-O0.pr68532	-O2 -ftree-vectorize -fno-vect-cost-model
  tx-O0.pr71626-2	-fpic
  tx-O0.pr7284-1	-std=c89
  tx-O0.restrict-1	-fgnu89-inline
  tx-O0.stkalign	-fno-inline
  tx-O0.unroll-1	-fgnu89-inline
  tx-O0.va-arg-7	-fgnu89-inline
  tx-O0.va-arg-8	-fgnu89-inline
  tx-O0.wchar_t-1	-finput-charset=utf-8
  tx-O2.20001121-1	-fgnu89-inline
  tx-O2.20020107-1	-fgnu89-inline
  tx-O2.20021127-1	-std=c99
  tx-O2.20040409-1w	-fwrapv
  tx-O2.20040409-2w	-fwrapv
  tx-O2.20040409-3w	-fwrapv
  tx-O2.20040709-3	-fno-common
  tx-O2.20050316-2	-mno-mmx
  tx-O2.20050604-1	-mno-mmx
  tx-O2.20101011-1	-fnon-call-exceptions
  tx-O2.920612-1	-fwrapv
  tx-O2.920711-1	-fwrapv
  tx-O2.930526-1	-fgnu89-inline
  tx-O2.930529-1	-fwrapv
  tx-O2.961223-1	-fgnu89-inline
  tx-O2.980608-1	-fgnu89-inline
  tx-O2.bcp-1	-fgnu89-inline
  tx-O2.eeprof-1	-finstrument-functions
  tx-O2.loop-2c	-fgnu89-inline
  tx-O2.lto-tbaa-1	-fno-ipa-cp
  tx-O2.p18298	-fgnu89-inline
  tx-O2.pr22493-1	-fwrapv
  tx-O2.pr23047	-fwrapv
  tx-O2.pr39501	-ffast-math
  tx-O2.pr53645	-std=gnu89
  tx-O2.pr53645-2	-std=gnu89
  tx-O2.pr57124	-fno-strict-overflow
  tx-O2.pr68381	-O -fexpensive-optimizations
  tx-O2.pr68390	-O2
  tx-O2.pr68506	-fno-builtin-abort
  tx-O2.pr68532	-O2 -ftree-vectorize -fno-vect-cost-model
  tx-O2.pr71626-2	-fpic
  tx-O2.pr7284-1	-std=c89
  tx-O2.restrict-1	-fgnu89-inline
  tx-O2.stkalign	-fno-inline
  tx-O2.unroll-1	-fgnu89-inline
  tx-O2.va-arg-7	-fgnu89-inline
  tx-O2.va-arg-8	-fgnu89-inline
  tx-O2.wchar_t-1	-finput-charset=utf-8
cpp: clang++ -O0 -S -emit-llvm -std=c++23 -w -ffile-prefix-map=ROOT/= test/cpp/{samples,runtime/samples}/*.cpp
modules: a file clang refuses, or without a main, is not one; the manifest tx.tsv: ID, SOURCE, SOURCE-SHA256, IR-SHA256
