/* config.h of XZ Utils 5.2.5 for pyi's _lzma (queue 208; bin/pyi-clib build xz): the tarball's configure run once
   (CC=gcc sh ./configure --disable-shared --disable-threads --disable-nls --disable-xz --disable-xzdec
   --disable-lzmadec --disable-lzmainfo --disable-scripts --disable-doc, on the Mac VM's aarch64 Alpine with musl),
   its #defines kept and its comments dropped. ci builds for x86_64 LP64; what the file says of the host holds for
   it too (SIZEOF_SIZE_T 8, musl's headers), and its absences are what ci needs: no TUKLIB_FAST_UNALIGNED_ACCESS, no
   immintrin.h (the portable code paths). Threads off, as ci has one C thread. */
#define ASSUME_RAM 128
#define HAVE_CHECK_CRC32 1
#define HAVE_CHECK_CRC64 1
#define HAVE_CHECK_SHA256 1
#define HAVE_DECL_PROGRAM_INVOCATION_NAME 1
#define HAVE_DECODERS 1
#define HAVE_DECODER_ARM 1
#define HAVE_DECODER_ARMTHUMB 1
#define HAVE_DECODER_DELTA 1
#define HAVE_DECODER_IA64 1
#define HAVE_DECODER_LZMA1 1
#define HAVE_DECODER_LZMA2 1
#define HAVE_DECODER_POWERPC 1
#define HAVE_DECODER_SPARC 1
#define HAVE_DECODER_X86 1
#define HAVE_DLFCN_H 1
#define HAVE_ENCODERS 1
#define HAVE_ENCODER_ARM 1
#define HAVE_ENCODER_ARMTHUMB 1
#define HAVE_ENCODER_DELTA 1
#define HAVE_ENCODER_IA64 1
#define HAVE_ENCODER_LZMA1 1
#define HAVE_ENCODER_LZMA2 1
#define HAVE_ENCODER_POWERPC 1
#define HAVE_ENCODER_SPARC 1
#define HAVE_ENCODER_X86 1
#define HAVE_FCNTL_H 1
#define HAVE_FUTIMENS 1
#define HAVE_GETOPT_H 1
#define HAVE_GETOPT_LONG 1
#define HAVE_INTTYPES_H 1
#define HAVE_LIMITS_H 1
#define HAVE_MBRTOWC 1
#define HAVE_MEMORY_H 1
#define HAVE_MF_BT2 1
#define HAVE_MF_BT3 1
#define HAVE_MF_BT4 1
#define HAVE_MF_HC3 1
#define HAVE_MF_HC4 1
#define HAVE_OPTRESET 1
#define HAVE_POSIX_FADVISE 1
#define HAVE_STDBOOL_H 1
#define HAVE_STDINT_H 1
#define HAVE_STDLIB_H 1
#define HAVE_STRINGS_H 1
#define HAVE_STRING_H 1
#define HAVE_STRUCT_STAT_ST_ATIM_TV_NSEC 1
#define HAVE_SYS_PARAM_H 1
#define HAVE_SYS_STAT_H 1
#define HAVE_SYS_TIME_H 1
#define HAVE_SYS_TYPES_H 1
#define HAVE_UINTPTR_T 1
#define HAVE_UNISTD_H 1
#define HAVE_VISIBILITY 1
#define HAVE_WCWIDTH 1
#define HAVE__BOOL 1
#define HAVE___BUILTIN_ASSUME_ALIGNED 1
#define HAVE___BUILTIN_BSWAPXX 1
#define LT_OBJDIR ".libs/"
#define NDEBUG 1
#define PACKAGE "xz"
#define PACKAGE_BUGREPORT "lasse.collin@tukaani.org"
#define PACKAGE_NAME "XZ Utils"
#define PACKAGE_STRING "XZ Utils 5.2.5"
#define PACKAGE_TARNAME "xz"
#define PACKAGE_URL "https://tukaani.org/xz/"
#define PACKAGE_VERSION "5.2.5"
#define SIZEOF_SIZE_T 8
#define STDC_HEADERS 1
#define TUKLIB_CPUCORES_SCHED_GETAFFINITY 1
#define TUKLIB_PHYSMEM_SYSCONF 1
#define VERSION "5.2.5"
