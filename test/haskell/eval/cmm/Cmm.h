/* hsi's Cmm.h (doc/haskell/HASKELL-EVAL-SURVEY.md, "HES67's design: Cmm on hsi"): the header every .cmm includes, the subset of
 * GHC 9.14.1's rts/include/Cmm.h (and of MachDeps.h, ghcconfig.h and DerivedConstants.h, which the build generates) that hsi's
 * Cmm (arbace.repr.haskell.cmm, go/arbace/hs/cmm.ae) has a meaning for, for x86_64-linux as GHC configures it: the definitions
 * below are Cmm.h's own for that platform, in its order; a macro this file lacks is a Cmm name the reader resolves as GHC's
 * parser does (a label), so a test that needs more of the RTS's macros fails at its use, by name.
 *
 * From GHC 9.14.1's source tarball (the pin of tools/repr-haskell's corpus), rts/include/Cmm.h:
 * (c) The University of Glasgow 2004-2013, under the Glasgow Haskell Compiler License (LICENSE.md). */

#pragma once

#define CMINUSMINUS 1

#include "MachDeps.h"
#define TABLES_NEXT_TO_CODE 1
#define x86_64_HOST_ARCH 1
#define linux_HOST_OS 1

/* Types */
#define I8  bits8
#define I16 bits16
#define I32 bits32
#define I64 bits64
#define P_  gcptr
#define W_ bits64
#define TAG_BITS                3
#define TAG_MASK ((1 << TAG_BITS) - 1)
#define UNTAG(p) (p & ~TAG_MASK)
#define GETTAG(p) (p & TAG_MASK)
#define CInt bits32
#define CLong bits64
#define CBool bits8
#define F_   float32
#define D_   float64
#define L_   bits64
#define V16_ vec128
#define V32_ vec256
#define V64_ vec512
#define SIZEOF_StgDouble 8
#define SIZEOF_StgWord64 8

/* Misc useful stuff */
#define ccall foreign "C"
#define NULL (0::W_)
#define STRING(name,str)                        \
  section "rodata" {                            \
        name : bits8[] str;                     \
  }                                             \

#define RET_LBL(f) f##_info
#define ENTRY_LBL(f) f##_info

/* Byte/word macros */
#define SIZEOF_W  SIZEOF_VOID_P
#define W_MASK    (SIZEOF_W-1)
#define W_SHIFT 3
#define WDS(n) ((n)*SIZEOF_W)
#define BYTES_TO_WDS(n) ((n) / SIZEOF_W)
#define ROUNDUP_BYTES_TO_WDS(n) (((n) + SIZEOF_W - 1) / SIZEOF_W)
#define TO_I64(x) (x)
#define TO_W_(x) %sx64(x)
#define TO_ZXW_(x) %zx64(x)
#define HALF_W_(x) %lobits32(x)
#define W_TO_INT(x) %lobits32(x)
#define W_TO_LONG(x) (x)

/* Atomic memory operations */
#define cmpxchgW cmpxchg64
#define xchgW xchg64

/* Heap/stack access, and adjusting the heap/stack pointers */
#define Sp(n)  W_[Sp + WDS(n)]
#define Hp(n)  W_[Hp + WDS(n)]
#define Sp_adj(n) Sp = Sp + WDS(n)  /* pronounced "spadge" */
#define Hp_adj(n) Hp = Hp + WDS(n)

/* Assertions and Debuggery */
#if defined(DEBUG) || defined(USE_ASSERTS_ALL_WAYS)
#define ASSERTS_ENABLED 1
#else
#undef ASSERTS_ENABLED
#endif

#if defined(ASSERTS_ENABLED)
#define ASSERT(predicate)                       \
        if (predicate) {                        \
            /*null*/;                           \
        } else {                                \
            foreign "C" _assertFail(__FILE__, __LINE__) never returns; \
        }
#else
#define ASSERT(p) /* nothing */
#endif

#if defined(DEBUG)
#define DEBUG_ONLY(s) s
#else
#define DEBUG_ONLY(s) /* nothing */
#endif

/* Returning */
#define RET_R1(x) jump %ENTRY_CODE(Sp(0)) [R1]

/* DerivedConstants.h's closures hsi lays out (go/arbace/hs/cmm.ae's object windows): the header a word (no profiling), a byte
 * array's size after it, its payload at SIZEOF_StgArrBytes */
#define SIZEOF_StgHeader 8
#define OFFSET_StgHeader_info 0
#define REP_StgHeader_info b64
#define StgHeader_info(__ptr__) REP_StgHeader_info[__ptr__+OFFSET_StgHeader_info]
#define SIZEOF_StgArrBytes 16
#define OFFSET_StgArrBytes_bytes 8
#define REP_StgArrBytes_bytes b64
#define StgArrBytes_bytes(__ptr__) REP_StgArrBytes_bytes[__ptr__+OFFSET_StgArrBytes_bytes]
#define BYTE_ARR_CTS(arr)  ((arr) + SIZEOF_StgArrBytes)
#define BYTE_ARR_WDS(arr) ROUNDUP_BYTES_TO_WDS(StgArrBytes_bytes(arr))

/* rts/storage/ClosureTypes.h (the closure types' numbers, as INFO_TABLE's arguments take them) */
#define INVALID_OBJECT                0
#define CONSTR                        1
#define CONSTR_1_0                    2
#define CONSTR_0_1                    3
#define CONSTR_2_0                    4
#define CONSTR_1_1                    5
#define CONSTR_0_2                    6
#define CONSTR_NOCAF                  7
#define FUN                           8
#define FUN_1_0                       9
#define FUN_0_1                       10
#define FUN_2_0                       11
#define FUN_1_1                       12
#define FUN_0_2                       13
#define FUN_STATIC                    14
#define THUNK                         15
#define THUNK_1_0                     16
#define THUNK_0_1                     17
#define THUNK_2_0                     18
#define THUNK_1_1                     19
#define THUNK_0_2                     20
#define THUNK_STATIC                  21
#define THUNK_SELECTOR                22
#define BCO                           23
#define AP                            24
#define PAP                           25
#define AP_STACK                      26
#define IND                           27
#define IND_STATIC                    28
#define RET_BCO                       29
#define RET_SMALL                     30
#define RET_BIG                       31
#define RET_FUN                       32
#define UPDATE_FRAME                  33
#define CATCH_FRAME                   34
#define UNDERFLOW_FRAME               35
#define STOP_FRAME                    36
#define BLOCKING_QUEUE                37
#define BLACKHOLE                     38
#define MVAR_CLEAN                    39
#define MVAR_DIRTY                    40
#define TVAR                          41
#define ARR_WORDS                     42
#define MUT_ARR_PTRS_CLEAN            43
#define MUT_ARR_PTRS_DIRTY            44
#define MUT_ARR_PTRS_FROZEN_DIRTY     45
#define MUT_ARR_PTRS_FROZEN_CLEAN     46
#define MUT_VAR_CLEAN                 47
#define MUT_VAR_DIRTY                 48
#define WEAK                          49
#define PRIM                          50
#define MUT_PRIM                      51
#define TSO                           52
#define STACK                         53
#define TREC_CHUNK                    54
#define ATOMICALLY_FRAME              55
#define CATCH_RETRY_FRAME             56
#define CATCH_STM_FRAME               57
#define WHITEHOLE                     58
#define SMALL_MUT_ARR_PTRS_CLEAN      59
#define SMALL_MUT_ARR_PTRS_DIRTY      60
#define SMALL_MUT_ARR_PTRS_FROZEN_DIRTY 61
#define SMALL_MUT_ARR_PTRS_FROZEN_CLEAN 62
#define COMPACT_NFDATA                63
#define CONTINUATION                  64
#define ANN_FRAME                     65
#define N_CLOSURE_TYPES               66
