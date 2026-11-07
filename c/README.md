# Arbace's C target: x86_64-linux-musl

What repr/c's reader, ci and cf2g read a C program with, and nothing of a toolchain (doc/C23-SURVEY.md, "As built: C's own
target"; doc/CROPS-SCORE.md, the independence of the C crop). Before it, the reader read gcc 15.2's recorded answers
(`test/c/host/`: its predefined macros, `__has_` answers and search list) and gcc's installed headers in place, and musl's
`configure` was run by gcc. Those stay as the oracle tier's:
`ARBACE_C_TARGET=gcc` reads them, and `bin/repr-c target oracle`, `bin/ci musl verify-derived` compare the two.

- `include/`: Arbace's own headers, installed at `/usr/lib/arbace/include` (first on the search list). Written from ISO/IEC
  9899:2024 (C23) §5.2.5, §7.7, §7.9, §7.10, §7.15, §7.16, §7.17, §7.18, §7.19, §7.20, §7.21, §7.22, §7.24 and Annex H, the
  System V AMD64 ABI (version 1.0) and the predefined macros of `target/`; not from gcc's headers (GPL-3.0 with the GCC
  Runtime Library Exception), which were not read: `float.h`, `limits.h`, `stdarg.h`, `stddef.h`, `stdbool.h`,
  `stdalign.h`, `stdnoreturn.h`, `iso646.h`, `stdint.h` (musl's, then C23's widths), `stdatomic.h`, `stdbit.h`,
  `stdckdint.h`, `varargs.h` (an error); `omp.h` (the OpenMP 5.2 API's runtime routines, declarations only: Arbace's C has
  no OpenMP); the x86 intrinsics headers `mmintrin.h`, `xmmintrin.h`, `emmintrin.h`, `pmmintrin.h`, `tmmintrin.h`,
  `smmintrin.h`, `immintrin.h`, `x86intrin.h` and `mm_malloc.h` (Intel's names and signatures as the Intel Intrinsics
  Guide gives them, declared only: Arbace's C has no SIMD, doc/c/SPEC.md §14, so a call does not link; the subset the
  corpora use, the rest left out). Arbace's own, under the EPL-1.0 (LICENSE.md).
- musl 1.2.6's headers, installed at `/usr/include`: its release tarball (test/c/corpora.edn's `:musl-1.2.6`, pinned by
  SHA-256, MIT) as `make install-headers` lays them out (`include/`, `arch/generic/bits` with `arch/x86_64/bits` over it,
  the generated `bits/alltypes.h` and `bits/syscall.h`), not Alpine's musl-dev (which patches `libintl.h` and
  `paths.h`). The generated headers are made as musl's Makefile makes them (sed over the `.in` files, no compiler;
  `bin/lib/ci/musl`'s `derive_tree`), byte for byte what `CC=gcc ./configure` gives (`bin/ci musl verify-derived`, with gcc, by hand);
  `config.mak` is written from configure's rules with no compiler probed (ci reads musl's sources itself).
- The libraries' headers (`target/libraries.txt`): gmp-dev's `gmp.h` and pcre2-dev's `pcre2.h`, which four benchmarksgame
  programs include. They are libraries' interfaces, not a toolchain's: taken from the host's packages' files, each with the
  digest `test/c/host/headers.sha256` records.
- `target/`: the target description, in the layout `arbace.repr.c.host/load-host` reads (gcc's `test/c/host/` has the
  same): `abi.edn` (the ABI's types, the floating formats, the dialect's identification), `dialect.edn` (the GNU dialect's
  attributes, builtins and features as `__has_` answers them), `probes.tsv` (the names the corpora probe), and what
  `test/c/target.ae` makes of them, `predefined/STD.h` for the 13 standards and `has.tsv` (`bin/repr-c target gen`,
  `verify`); `search.txt` and `host.edn` by hand. The predefined macros are computed: the limits from the sizes, the
  floating characteristics by C23's formulas over each format (exact binary values rounded to 36 digits, nearest-even),
  the decimal ones from their digits and exponents; `bin/repr-c target oracle` finds all 13 files equal to gcc 15.2's
  `-dM -E` output and `has.tsv` equal to its answers (2026-11-07).

`bin/lib/repr-c/sysroot.bash` assembles the sysroot (`.tmp/c-target/KEY`, `ARBACE_C_SYSROOT`) and names the description
(`ARBACE_C_HOST`, which `load-host` reads over the directory its caller names).
