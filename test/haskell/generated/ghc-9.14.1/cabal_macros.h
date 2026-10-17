/* The macros Cabal's cabal_macros.h defines for the packages the host's GHC 9.14.1 installs
   (ghc-pkg list), for hsc2hs (doc/haskell/HASKELL-NOTES.md, R5): MIN_VERSION_pkg, VERSION_pkg,
   MIN_TOOL_VERSION_ghc, __GLASGOW_HASKELL__. */
#ifndef __GLASGOW_HASKELL__
#define __GLASGOW_HASKELL__ 914
#endif

/* Cabal-3.16.0.0 */
#ifndef VERSION_Cabal
#define VERSION_Cabal "3.16.0.0"
#endif
#ifndef MIN_VERSION_Cabal
#define MIN_VERSION_Cabal(major1,major2,minor) (\
  (major1) <  3 || \
  (major1) == 3 && (major2) <  16 || \
  (major1) == 3 && (major2) == 16 && (minor) <= 0)
#endif

/* Cabal-syntax-3.16.0.0 */
#ifndef VERSION_Cabal_syntax
#define VERSION_Cabal_syntax "3.16.0.0"
#endif
#ifndef MIN_VERSION_Cabal_syntax
#define MIN_VERSION_Cabal_syntax(major1,major2,minor) (\
  (major1) <  3 || \
  (major1) == 3 && (major2) <  16 || \
  (major1) == 3 && (major2) == 16 && (minor) <= 0)
#endif

/* array-0.5.8.0 */
#ifndef VERSION_array
#define VERSION_array "0.5.8.0"
#endif
#ifndef MIN_VERSION_array
#define MIN_VERSION_array(major1,major2,minor) (\
  (major1) <  0 || \
  (major1) == 0 && (major2) <  5 || \
  (major1) == 0 && (major2) == 5 && (minor) <= 8)
#endif

/* base-4.22.0.0 */
#ifndef VERSION_base
#define VERSION_base "4.22.0.0"
#endif
#ifndef MIN_VERSION_base
#define MIN_VERSION_base(major1,major2,minor) (\
  (major1) <  4 || \
  (major1) == 4 && (major2) <  22 || \
  (major1) == 4 && (major2) == 22 && (minor) <= 0)
#endif

/* binary-0.8.9.3 */
#ifndef VERSION_binary
#define VERSION_binary "0.8.9.3"
#endif
#ifndef MIN_VERSION_binary
#define MIN_VERSION_binary(major1,major2,minor) (\
  (major1) <  0 || \
  (major1) == 0 && (major2) <  8 || \
  (major1) == 0 && (major2) == 8 && (minor) <= 9)
#endif

/* bytestring-0.12.2.0 */
#ifndef VERSION_bytestring
#define VERSION_bytestring "0.12.2.0"
#endif
#ifndef MIN_VERSION_bytestring
#define MIN_VERSION_bytestring(major1,major2,minor) (\
  (major1) <  0 || \
  (major1) == 0 && (major2) <  12 || \
  (major1) == 0 && (major2) == 12 && (minor) <= 2)
#endif

/* containers-0.8 */
#ifndef VERSION_containers
#define VERSION_containers "0.8"
#endif
#ifndef MIN_VERSION_containers
#define MIN_VERSION_containers(major1,major2,minor) (\
  (major1) <  0 || \
  (major1) == 0 && (major2) <  8 || \
  (major1) == 0 && (major2) == 8 && (minor) <= 0)
#endif

/* deepseq-1.5.1.0 */
#ifndef VERSION_deepseq
#define VERSION_deepseq "1.5.1.0"
#endif
#ifndef MIN_VERSION_deepseq
#define MIN_VERSION_deepseq(major1,major2,minor) (\
  (major1) <  1 || \
  (major1) == 1 && (major2) <  5 || \
  (major1) == 1 && (major2) == 5 && (minor) <= 1)
#endif

/* directory-1.3.10.0 */
#ifndef VERSION_directory
#define VERSION_directory "1.3.10.0"
#endif
#ifndef MIN_VERSION_directory
#define MIN_VERSION_directory(major1,major2,minor) (\
  (major1) <  1 || \
  (major1) == 1 && (major2) <  3 || \
  (major1) == 1 && (major2) == 3 && (minor) <= 10)
#endif

/* exceptions-0.10.11 */
#ifndef VERSION_exceptions
#define VERSION_exceptions "0.10.11"
#endif
#ifndef MIN_VERSION_exceptions
#define MIN_VERSION_exceptions(major1,major2,minor) (\
  (major1) <  0 || \
  (major1) == 0 && (major2) <  10 || \
  (major1) == 0 && (major2) == 10 && (minor) <= 11)
#endif

/* file-io-0.1.5 */
#ifndef VERSION_file_io
#define VERSION_file_io "0.1.5"
#endif
#ifndef MIN_VERSION_file_io
#define MIN_VERSION_file_io(major1,major2,minor) (\
  (major1) <  0 || \
  (major1) == 0 && (major2) <  1 || \
  (major1) == 0 && (major2) == 1 && (minor) <= 5)
#endif

/* filepath-1.5.4.0 */
#ifndef VERSION_filepath
#define VERSION_filepath "1.5.4.0"
#endif
#ifndef MIN_VERSION_filepath
#define MIN_VERSION_filepath(major1,major2,minor) (\
  (major1) <  1 || \
  (major1) == 1 && (major2) <  5 || \
  (major1) == 1 && (major2) == 5 && (minor) <= 4)
#endif

/* ghc-9.14.1 */
#ifndef VERSION_ghc
#define VERSION_ghc "9.14.1"
#endif
#ifndef MIN_VERSION_ghc
#define MIN_VERSION_ghc(major1,major2,minor) (\
  (major1) <  9 || \
  (major1) == 9 && (major2) <  14 || \
  (major1) == 9 && (major2) == 14 && (minor) <= 1)
#endif

/* ghc-bignum-1.4 */
#ifndef VERSION_ghc_bignum
#define VERSION_ghc_bignum "1.4"
#endif
#ifndef MIN_VERSION_ghc_bignum
#define MIN_VERSION_ghc_bignum(major1,major2,minor) (\
  (major1) <  1 || \
  (major1) == 1 && (major2) <  4 || \
  (major1) == 1 && (major2) == 4 && (minor) <= 0)
#endif

/* ghc-boot-9.14.1 */
#ifndef VERSION_ghc_boot
#define VERSION_ghc_boot "9.14.1"
#endif
#ifndef MIN_VERSION_ghc_boot
#define MIN_VERSION_ghc_boot(major1,major2,minor) (\
  (major1) <  9 || \
  (major1) == 9 && (major2) <  14 || \
  (major1) == 9 && (major2) == 14 && (minor) <= 1)
#endif

/* ghc-boot-th-9.14.1 */
#ifndef VERSION_ghc_boot_th
#define VERSION_ghc_boot_th "9.14.1"
#endif
#ifndef MIN_VERSION_ghc_boot_th
#define MIN_VERSION_ghc_boot_th(major1,major2,minor) (\
  (major1) <  9 || \
  (major1) == 9 && (major2) <  14 || \
  (major1) == 9 && (major2) == 14 && (minor) <= 1)
#endif

/* ghc-compact-0.1.0.0 */
#ifndef VERSION_ghc_compact
#define VERSION_ghc_compact "0.1.0.0"
#endif
#ifndef MIN_VERSION_ghc_compact
#define MIN_VERSION_ghc_compact(major1,major2,minor) (\
  (major1) <  0 || \
  (major1) == 0 && (major2) <  1 || \
  (major1) == 0 && (major2) == 1 && (minor) <= 0)
#endif

/* ghc-experimental-9.1401.0 */
#ifndef VERSION_ghc_experimental
#define VERSION_ghc_experimental "9.1401.0"
#endif
#ifndef MIN_VERSION_ghc_experimental
#define MIN_VERSION_ghc_experimental(major1,major2,minor) (\
  (major1) <  9 || \
  (major1) == 9 && (major2) <  1401 || \
  (major1) == 9 && (major2) == 1401 && (minor) <= 0)
#endif

/* ghc-heap-9.14.1 */
#ifndef VERSION_ghc_heap
#define VERSION_ghc_heap "9.14.1"
#endif
#ifndef MIN_VERSION_ghc_heap
#define MIN_VERSION_ghc_heap(major1,major2,minor) (\
  (major1) <  9 || \
  (major1) == 9 && (major2) <  14 || \
  (major1) == 9 && (major2) == 14 && (minor) <= 1)
#endif

/* ghc-internal-9.1401.0 */
#ifndef VERSION_ghc_internal
#define VERSION_ghc_internal "9.1401.0"
#endif
#ifndef MIN_VERSION_ghc_internal
#define MIN_VERSION_ghc_internal(major1,major2,minor) (\
  (major1) <  9 || \
  (major1) == 9 && (major2) <  1401 || \
  (major1) == 9 && (major2) == 1401 && (minor) <= 0)
#endif

/* ghc-platform-0.1.0.0 */
#ifndef VERSION_ghc_platform
#define VERSION_ghc_platform "0.1.0.0"
#endif
#ifndef MIN_VERSION_ghc_platform
#define MIN_VERSION_ghc_platform(major1,major2,minor) (\
  (major1) <  0 || \
  (major1) == 0 && (major2) <  1 || \
  (major1) == 0 && (major2) == 1 && (minor) <= 0)
#endif

/* ghc-prim-0.13.1 */
#ifndef VERSION_ghc_prim
#define VERSION_ghc_prim "0.13.1"
#endif
#ifndef MIN_VERSION_ghc_prim
#define MIN_VERSION_ghc_prim(major1,major2,minor) (\
  (major1) <  0 || \
  (major1) == 0 && (major2) <  13 || \
  (major1) == 0 && (major2) == 13 && (minor) <= 1)
#endif

/* ghc-toolchain-0.1.0.0 */
#ifndef VERSION_ghc_toolchain
#define VERSION_ghc_toolchain "0.1.0.0"
#endif
#ifndef MIN_VERSION_ghc_toolchain
#define MIN_VERSION_ghc_toolchain(major1,major2,minor) (\
  (major1) <  0 || \
  (major1) == 0 && (major2) <  1 || \
  (major1) == 0 && (major2) == 1 && (minor) <= 0)
#endif

/* ghci-9.14.1 */
#ifndef VERSION_ghci
#define VERSION_ghci "9.14.1"
#endif
#ifndef MIN_VERSION_ghci
#define MIN_VERSION_ghci(major1,major2,minor) (\
  (major1) <  9 || \
  (major1) == 9 && (major2) <  14 || \
  (major1) == 9 && (major2) == 14 && (minor) <= 1)
#endif

/* haddock-api-2.33.0 */
#ifndef VERSION_haddock_api
#define VERSION_haddock_api "2.33.0"
#endif
#ifndef MIN_VERSION_haddock_api
#define MIN_VERSION_haddock_api(major1,major2,minor) (\
  (major1) <  2 || \
  (major1) == 2 && (major2) <  33 || \
  (major1) == 2 && (major2) == 33 && (minor) <= 0)
#endif

/* haddock-library-1.11.0 */
#ifndef VERSION_haddock_library
#define VERSION_haddock_library "1.11.0"
#endif
#ifndef MIN_VERSION_haddock_library
#define MIN_VERSION_haddock_library(major1,major2,minor) (\
  (major1) <  1 || \
  (major1) == 1 && (major2) <  11 || \
  (major1) == 1 && (major2) == 11 && (minor) <= 0)
#endif

/* haskeline-0.8.3.0 */
#ifndef VERSION_haskeline
#define VERSION_haskeline "0.8.3.0"
#endif
#ifndef MIN_VERSION_haskeline
#define MIN_VERSION_haskeline(major1,major2,minor) (\
  (major1) <  0 || \
  (major1) == 0 && (major2) <  8 || \
  (major1) == 0 && (major2) == 8 && (minor) <= 3)
#endif

/* hpc-0.7.0.2 */
#ifndef VERSION_hpc
#define VERSION_hpc "0.7.0.2"
#endif
#ifndef MIN_VERSION_hpc
#define MIN_VERSION_hpc(major1,major2,minor) (\
  (major1) <  0 || \
  (major1) == 0 && (major2) <  7 || \
  (major1) == 0 && (major2) == 7 && (minor) <= 0)
#endif

/* integer-gmp-1.1 */
#ifndef VERSION_integer_gmp
#define VERSION_integer_gmp "1.1"
#endif
#ifndef MIN_VERSION_integer_gmp
#define MIN_VERSION_integer_gmp(major1,major2,minor) (\
  (major1) <  1 || \
  (major1) == 1 && (major2) <  1 || \
  (major1) == 1 && (major2) == 1 && (minor) <= 0)
#endif

/* mtl-2.3.1 */
#ifndef VERSION_mtl
#define VERSION_mtl "2.3.1"
#endif
#ifndef MIN_VERSION_mtl
#define MIN_VERSION_mtl(major1,major2,minor) (\
  (major1) <  2 || \
  (major1) == 2 && (major2) <  3 || \
  (major1) == 2 && (major2) == 3 && (minor) <= 1)
#endif

/* os-string-2.0.8 */
#ifndef VERSION_os_string
#define VERSION_os_string "2.0.8"
#endif
#ifndef MIN_VERSION_os_string
#define MIN_VERSION_os_string(major1,major2,minor) (\
  (major1) <  2 || \
  (major1) == 2 && (major2) <  0 || \
  (major1) == 2 && (major2) == 0 && (minor) <= 8)
#endif

/* parsec-3.1.18.0 */
#ifndef VERSION_parsec
#define VERSION_parsec "3.1.18.0"
#endif
#ifndef MIN_VERSION_parsec
#define MIN_VERSION_parsec(major1,major2,minor) (\
  (major1) <  3 || \
  (major1) == 3 && (major2) <  1 || \
  (major1) == 3 && (major2) == 1 && (minor) <= 18)
#endif

/* pretty-1.1.3.6 */
#ifndef VERSION_pretty
#define VERSION_pretty "1.1.3.6"
#endif
#ifndef MIN_VERSION_pretty
#define MIN_VERSION_pretty(major1,major2,minor) (\
  (major1) <  1 || \
  (major1) == 1 && (major2) <  1 || \
  (major1) == 1 && (major2) == 1 && (minor) <= 3)
#endif

/* process-1.6.26.1 */
#ifndef VERSION_process
#define VERSION_process "1.6.26.1"
#endif
#ifndef MIN_VERSION_process
#define MIN_VERSION_process(major1,major2,minor) (\
  (major1) <  1 || \
  (major1) == 1 && (major2) <  6 || \
  (major1) == 1 && (major2) == 6 && (minor) <= 26)
#endif

/* rts-1.0.3 */
#ifndef VERSION_rts
#define VERSION_rts "1.0.3"
#endif
#ifndef MIN_VERSION_rts
#define MIN_VERSION_rts(major1,major2,minor) (\
  (major1) <  1 || \
  (major1) == 1 && (major2) <  0 || \
  (major1) == 1 && (major2) == 0 && (minor) <= 3)
#endif

/* semaphore-compat-1.0.0 */
#ifndef VERSION_semaphore_compat
#define VERSION_semaphore_compat "1.0.0"
#endif
#ifndef MIN_VERSION_semaphore_compat
#define MIN_VERSION_semaphore_compat(major1,major2,minor) (\
  (major1) <  1 || \
  (major1) == 1 && (major2) <  0 || \
  (major1) == 1 && (major2) == 0 && (minor) <= 0)
#endif

/* stm-2.5.3.1 */
#ifndef VERSION_stm
#define VERSION_stm "2.5.3.1"
#endif
#ifndef MIN_VERSION_stm
#define MIN_VERSION_stm(major1,major2,minor) (\
  (major1) <  2 || \
  (major1) == 2 && (major2) <  5 || \
  (major1) == 2 && (major2) == 5 && (minor) <= 3)
#endif

/* system-cxx-std-lib-1.0 */
#ifndef VERSION_system_cxx_std_lib
#define VERSION_system_cxx_std_lib "1.0"
#endif
#ifndef MIN_VERSION_system_cxx_std_lib
#define MIN_VERSION_system_cxx_std_lib(major1,major2,minor) (\
  (major1) <  1 || \
  (major1) == 1 && (major2) <  0 || \
  (major1) == 1 && (major2) == 0 && (minor) <= 0)
#endif

/* template-haskell-2.24.0.0 */
#ifndef VERSION_template_haskell
#define VERSION_template_haskell "2.24.0.0"
#endif
#ifndef MIN_VERSION_template_haskell
#define MIN_VERSION_template_haskell(major1,major2,minor) (\
  (major1) <  2 || \
  (major1) == 2 && (major2) <  24 || \
  (major1) == 2 && (major2) == 24 && (minor) <= 0)
#endif

/* template-haskell-lift-0.1.0.0 */
#ifndef VERSION_template_haskell_lift
#define VERSION_template_haskell_lift "0.1.0.0"
#endif
#ifndef MIN_VERSION_template_haskell_lift
#define MIN_VERSION_template_haskell_lift(major1,major2,minor) (\
  (major1) <  0 || \
  (major1) == 0 && (major2) <  1 || \
  (major1) == 0 && (major2) == 1 && (minor) <= 0)
#endif

/* template-haskell-quasiquoter-0.1.0.0 */
#ifndef VERSION_template_haskell_quasiquoter
#define VERSION_template_haskell_quasiquoter "0.1.0.0"
#endif
#ifndef MIN_VERSION_template_haskell_quasiquoter
#define MIN_VERSION_template_haskell_quasiquoter(major1,major2,minor) (\
  (major1) <  0 || \
  (major1) == 0 && (major2) <  1 || \
  (major1) == 0 && (major2) == 1 && (minor) <= 0)
#endif

/* terminfo-0.4.1.7 */
#ifndef VERSION_terminfo
#define VERSION_terminfo "0.4.1.7"
#endif
#ifndef MIN_VERSION_terminfo
#define MIN_VERSION_terminfo(major1,major2,minor) (\
  (major1) <  0 || \
  (major1) == 0 && (major2) <  4 || \
  (major1) == 0 && (major2) == 4 && (minor) <= 1)
#endif

/* text-2.1.3 */
#ifndef VERSION_text
#define VERSION_text "2.1.3"
#endif
#ifndef MIN_VERSION_text
#define MIN_VERSION_text(major1,major2,minor) (\
  (major1) <  2 || \
  (major1) == 2 && (major2) <  1 || \
  (major1) == 2 && (major2) == 1 && (minor) <= 3)
#endif

/* time-1.15 */
#ifndef VERSION_time
#define VERSION_time "1.15"
#endif
#ifndef MIN_VERSION_time
#define MIN_VERSION_time(major1,major2,minor) (\
  (major1) <  1 || \
  (major1) == 1 && (major2) <  15 || \
  (major1) == 1 && (major2) == 15 && (minor) <= 0)
#endif

/* transformers-0.6.1.2 */
#ifndef VERSION_transformers
#define VERSION_transformers "0.6.1.2"
#endif
#ifndef MIN_VERSION_transformers
#define MIN_VERSION_transformers(major1,major2,minor) (\
  (major1) <  0 || \
  (major1) == 0 && (major2) <  6 || \
  (major1) == 0 && (major2) == 6 && (minor) <= 1)
#endif

/* unix-2.8.8.0 */
#ifndef VERSION_unix
#define VERSION_unix "2.8.8.0"
#endif
#ifndef MIN_VERSION_unix
#define MIN_VERSION_unix(major1,major2,minor) (\
  (major1) <  2 || \
  (major1) == 2 && (major2) <  8 || \
  (major1) == 2 && (major2) == 8 && (minor) <= 8)
#endif

/* xhtml-3000.2.2.1 */
#ifndef VERSION_xhtml
#define VERSION_xhtml "3000.2.2.1"
#endif
#ifndef MIN_VERSION_xhtml
#define MIN_VERSION_xhtml(major1,major2,minor) (\
  (major1) <  3000 || \
  (major1) == 3000 && (major2) <  2 || \
  (major1) == 3000 && (major2) == 2 && (minor) <= 2)
#endif

/* tool ghc-9.14.1 */
#ifndef MIN_TOOL_VERSION_ghc
#define MIN_TOOL_VERSION_ghc(major1,major2,minor) (\
  (major1) <  9 || \
  (major1) == 9 && (major2) <  14 || \
  (major1) == 9 && (major2) == 14 && (minor) <= 1)
#endif
