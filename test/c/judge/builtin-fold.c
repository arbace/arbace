/* queue 154: the checker folds the builtins of the :missing-builtin class as gcc 15.2 does (constant operands) */
/* { dg-do compile } */
/* { dg-options "-std=gnu23" } */
_Static_assert (__builtin_stdc_trailing_ones ((unsigned _BitInt(65)) 7) == 3 && __builtin_clzg ((unsigned _BitInt(200)) 1) == 199);
_Static_assert (__builtin_popcountg (0xffu) == 8);
_Static_assert (__builtin_parityg ((unsigned char) 7) == 1);
_Static_assert (__builtin_clzg (1u) == 31);
_Static_assert (__builtin_clzg (0u, 42) == 42);
_Static_assert (__builtin_ctzg ((unsigned long) 0x50) == 4);
_Static_assert (__builtin_ffsg (0) == 0 && __builtin_ffsg (-8L) == 4);
_Static_assert (__builtin_clrsbg (-2) == 30);
_Static_assert (__builtin_popcountg ((unsigned _BitInt(575)) -1) == 575);
_Static_assert (__builtin_stdc_leading_zeros ((unsigned char) 1) == 7);
_Static_assert (__builtin_stdc_first_leading_one ((unsigned _BitInt(129)) 1) == 129);
_Static_assert (__builtin_stdc_bit_ceil (5u) == 8);
_Static_assert (__builtin_stdc_bit_floor ((unsigned _BitInt(65)) 1000) == 512);
_Static_assert (__builtin_stdc_rotate_left ((unsigned char) 0x81, 1) == 3);
_Static_assert (__builtin_stdc_rotate_right ((unsigned short) 1, 1) == 0x8000);
_Static_assert (__builtin_stdc_has_single_bit (64u));
enum { E = __builtin_popcountg (0x7u) };
_Static_assert (E == 3);
_Static_assert (__builtin_add_overflow_p (__INT_MAX__, 1, 0));
_Static_assert (!__builtin_add_overflow_p (__INT_MAX__, 1, 0L));
_Static_assert (__builtin_sub_overflow_p (0, 1, 0u));
_Static_assert (__builtin_mul_overflow_p (1L << 40, 1L << 40, (_BitInt(80)) 0));
_Static_assert (!__builtin_mul_overflow_p (1L << 40, 1L << 40, (_BitInt(82)) 0));
_Static_assert (__builtin_isinff (__builtin_inff ()));
_Static_assert (__builtin_isnanl (__builtin_nanl ("")));
_Static_assert (!__builtin_finite (__builtin_inf ()));
_Static_assert (__builtin_finitef (1.0f));
_Static_assert (__atomic_always_lock_free (8, 0));
_Static_assert (!__atomic_always_lock_free (16, 0));
_Static_assert (__atomic_always_lock_free (4, (int *) 0));
enum { B = __builtin_assoc_barrier (1 + 2) };
_Static_assert (B == 3);
_Static_assert (__builtin_popcountg (0xffu) == 7); /* { dg-error "static assertion failed" } */
_Static_assert (__builtin_stdc_rotate_left ((unsigned char) 0x81, 1) == 2); /* { dg-error "static assertion failed" } */
_Static_assert (__builtin_mul_overflow_p (2, 3, 0)); /* { dg-error "static assertion failed" } */
_Static_assert (__builtin_isinff (1.0f)); /* { dg-error "static assertion failed" } */
_Static_assert (__atomic_always_lock_free (16, 0)); /* { dg-error "static assertion failed" } */
_Static_assert (__builtin_assoc_barrier (1 + 2) == 4); /* { dg-error "static assertion failed" } */
_Static_assert (__builtin_stdc_bit_width ((unsigned _BitInt(300)) 1 << 299) == 299); /* { dg-error "static assertion failed" } */
