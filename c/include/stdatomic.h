/* Arbace's <stdatomic.h> (ISO/IEC 9899:2024 §7.17), c/README.md: the atomic types, orders and operations over the GNU
   dialect's __atomic builtins (each generic function a macro of the builtin of the same meaning; the memory orders
   numbered as the builtins take them, the predefined __ATOMIC_ macros). */
#ifndef __ARBACE_STDATOMIC_H
#define __ARBACE_STDATOMIC_H

#if defined __STDC_VERSION__ && __STDC_VERSION__ > 201710L
#define __STDC_VERSION_STDATOMIC_H__ 202311L
#endif

typedef enum memory_order {
  memory_order_relaxed = __ATOMIC_RELAXED,
  memory_order_consume = __ATOMIC_CONSUME,
  memory_order_acquire = __ATOMIC_ACQUIRE,
  memory_order_release = __ATOMIC_RELEASE,
  memory_order_acq_rel = __ATOMIC_ACQ_REL,
  memory_order_seq_cst = __ATOMIC_SEQ_CST
} memory_order;

/* 7.17.6 */
typedef _Atomic _Bool atomic_bool;
typedef _Atomic char atomic_char;
typedef _Atomic signed char atomic_schar;
typedef _Atomic unsigned char atomic_uchar;
typedef _Atomic short atomic_short;
typedef _Atomic unsigned short atomic_ushort;
typedef _Atomic int atomic_int;
typedef _Atomic unsigned int atomic_uint;
typedef _Atomic long atomic_long;
typedef _Atomic unsigned long atomic_ulong;
typedef _Atomic long long atomic_llong;
typedef _Atomic unsigned long long atomic_ullong;
typedef _Atomic __CHAR16_TYPE__ atomic_char16_t;
typedef _Atomic __CHAR32_TYPE__ atomic_char32_t;
typedef _Atomic __WCHAR_TYPE__ atomic_wchar_t;
typedef _Atomic __INT_LEAST8_TYPE__ atomic_int_least8_t;
typedef _Atomic __UINT_LEAST8_TYPE__ atomic_uint_least8_t;
typedef _Atomic __INT_LEAST16_TYPE__ atomic_int_least16_t;
typedef _Atomic __UINT_LEAST16_TYPE__ atomic_uint_least16_t;
typedef _Atomic __INT_LEAST32_TYPE__ atomic_int_least32_t;
typedef _Atomic __UINT_LEAST32_TYPE__ atomic_uint_least32_t;
typedef _Atomic __INT_LEAST64_TYPE__ atomic_int_least64_t;
typedef _Atomic __UINT_LEAST64_TYPE__ atomic_uint_least64_t;
typedef _Atomic __INT_FAST8_TYPE__ atomic_int_fast8_t;
typedef _Atomic __UINT_FAST8_TYPE__ atomic_uint_fast8_t;
typedef _Atomic __INT_FAST16_TYPE__ atomic_int_fast16_t;
typedef _Atomic __UINT_FAST16_TYPE__ atomic_uint_fast16_t;
typedef _Atomic __INT_FAST32_TYPE__ atomic_int_fast32_t;
typedef _Atomic __UINT_FAST32_TYPE__ atomic_uint_fast32_t;
typedef _Atomic __INT_FAST64_TYPE__ atomic_int_fast64_t;
typedef _Atomic __UINT_FAST64_TYPE__ atomic_uint_fast64_t;
typedef _Atomic __INTPTR_TYPE__ atomic_intptr_t;
typedef _Atomic __UINTPTR_TYPE__ atomic_uintptr_t;
typedef _Atomic __SIZE_TYPE__ atomic_size_t;
typedef _Atomic __PTRDIFF_TYPE__ atomic_ptrdiff_t;
typedef _Atomic __INTMAX_TYPE__ atomic_intmax_t;
typedef _Atomic __UINTMAX_TYPE__ atomic_uintmax_t;
#ifdef __CHAR8_TYPE__
typedef _Atomic __CHAR8_TYPE__ atomic_char8_t;
#endif

/* 7.17.5: 2, always lock-free (the predefined answers) */
#define ATOMIC_BOOL_LOCK_FREE __GCC_ATOMIC_BOOL_LOCK_FREE
#define ATOMIC_CHAR_LOCK_FREE __GCC_ATOMIC_CHAR_LOCK_FREE
#ifdef __GCC_ATOMIC_CHAR8_T_LOCK_FREE
#define ATOMIC_CHAR8_T_LOCK_FREE __GCC_ATOMIC_CHAR8_T_LOCK_FREE
#endif
#define ATOMIC_CHAR16_T_LOCK_FREE __GCC_ATOMIC_CHAR16_T_LOCK_FREE
#define ATOMIC_CHAR32_T_LOCK_FREE __GCC_ATOMIC_CHAR32_T_LOCK_FREE
#define ATOMIC_WCHAR_T_LOCK_FREE __GCC_ATOMIC_WCHAR_T_LOCK_FREE
#define ATOMIC_SHORT_LOCK_FREE __GCC_ATOMIC_SHORT_LOCK_FREE
#define ATOMIC_INT_LOCK_FREE __GCC_ATOMIC_INT_LOCK_FREE
#define ATOMIC_LONG_LOCK_FREE __GCC_ATOMIC_LONG_LOCK_FREE
#define ATOMIC_LLONG_LOCK_FREE __GCC_ATOMIC_LLONG_LOCK_FREE
#define ATOMIC_POINTER_LOCK_FREE __GCC_ATOMIC_POINTER_LOCK_FREE

/* 7.17.2: initialization (ATOMIC_VAR_INIT removed by C23) */
#if !(defined __STDC_VERSION__ && __STDC_VERSION__ > 201710L)
#define ATOMIC_VAR_INIT(value) (value)
#endif
#define atomic_init(obj, value) atomic_store_explicit((obj), (value), memory_order_relaxed)

#define kill_dependency(y) (y)

/* 7.17.4: fences */
#define atomic_thread_fence(order) __atomic_thread_fence(order)
#define atomic_signal_fence(order) __atomic_signal_fence(order)
#define atomic_is_lock_free(obj) __atomic_is_lock_free(sizeof(*(obj)), (obj))

/* 7.17.7: operations on atomic types, each on any atomic type through the generic builtins */
#define atomic_store_explicit(obj, desired, order) \
  __extension__ ({ __auto_type __arbace_p = (obj); __typeof__(*__arbace_p) __arbace_v = (desired); \
                   __atomic_store(__arbace_p, &__arbace_v, (order)); })
#define atomic_store(obj, desired) atomic_store_explicit((obj), (desired), memory_order_seq_cst)
#define atomic_load_explicit(obj, order) \
  __extension__ ({ __auto_type __arbace_p = (obj); __typeof__((void)0, *__arbace_p) __arbace_r; \
                   __atomic_load(__arbace_p, &__arbace_r, (order)); __arbace_r; })
#define atomic_load(obj) atomic_load_explicit((obj), memory_order_seq_cst)
#define atomic_exchange_explicit(obj, desired, order) \
  __extension__ ({ __auto_type __arbace_p = (obj); __typeof__((void)0, *__arbace_p) __arbace_v = (desired), __arbace_r; \
                   __atomic_exchange(__arbace_p, &__arbace_v, &__arbace_r, (order)); __arbace_r; })
#define atomic_exchange(obj, desired) atomic_exchange_explicit((obj), (desired), memory_order_seq_cst)
#define atomic_compare_exchange_strong_explicit(obj, expected, desired, success, failure) \
  __extension__ ({ __auto_type __arbace_p = (obj); __typeof__((void)0, *__arbace_p) __arbace_v = (desired); \
                   __atomic_compare_exchange(__arbace_p, (expected), &__arbace_v, 0, (success), (failure)); })
#define atomic_compare_exchange_strong(obj, expected, desired) \
  atomic_compare_exchange_strong_explicit((obj), (expected), (desired), memory_order_seq_cst, memory_order_seq_cst)
#define atomic_compare_exchange_weak_explicit(obj, expected, desired, success, failure) \
  __extension__ ({ __auto_type __arbace_p = (obj); __typeof__((void)0, *__arbace_p) __arbace_v = (desired); \
                   __atomic_compare_exchange(__arbace_p, (expected), &__arbace_v, 1, (success), (failure)); })
#define atomic_compare_exchange_weak(obj, expected, desired) \
  atomic_compare_exchange_weak_explicit((obj), (expected), (desired), memory_order_seq_cst, memory_order_seq_cst)
#define atomic_fetch_add_explicit(obj, operand, order) __atomic_fetch_add((obj), (operand), (order))
#define atomic_fetch_add(obj, operand) __atomic_fetch_add((obj), (operand), memory_order_seq_cst)
#define atomic_fetch_sub_explicit(obj, operand, order) __atomic_fetch_sub((obj), (operand), (order))
#define atomic_fetch_sub(obj, operand) __atomic_fetch_sub((obj), (operand), memory_order_seq_cst)
#define atomic_fetch_or_explicit(obj, operand, order) __atomic_fetch_or((obj), (operand), (order))
#define atomic_fetch_or(obj, operand) __atomic_fetch_or((obj), (operand), memory_order_seq_cst)
#define atomic_fetch_xor_explicit(obj, operand, order) __atomic_fetch_xor((obj), (operand), (order))
#define atomic_fetch_xor(obj, operand) __atomic_fetch_xor((obj), (operand), memory_order_seq_cst)
#define atomic_fetch_and_explicit(obj, operand, order) __atomic_fetch_and((obj), (operand), (order))
#define atomic_fetch_and(obj, operand) __atomic_fetch_and((obj), (operand), memory_order_seq_cst)

/* 7.17.8: the atomic flag */
typedef _Atomic struct { unsigned char __arbace_set; } atomic_flag;
#define ATOMIC_FLAG_INIT { 0 }
#define atomic_flag_test_and_set_explicit(obj, order) __atomic_test_and_set((void *)(obj), (order))
#define atomic_flag_test_and_set(obj) atomic_flag_test_and_set_explicit((obj), memory_order_seq_cst)
#define atomic_flag_clear_explicit(obj, order) __atomic_clear((_Bool *)(obj), (order))
#define atomic_flag_clear(obj) atomic_flag_clear_explicit((obj), memory_order_seq_cst)

#endif
