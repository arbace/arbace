/* ci's ports for rsi (doc/rust/EVAL.md §9, bin/rsi runtime-ci): the builtins the lowered C calls that ci's back end does not
   compute, as functions of their name, linked before std's lowered units. __builtin_ldexpl: the bounds of llvm.fpto{s,u}i.sat
   (EVAL.md §5), powers of two exact in ci's 8-byte long double (CS35). */
long double ldexpl(long double, int);
long double __builtin_ldexpl(long double x, int e) { return ldexpl(x, e); }

/* __builtin_powi, __builtin_powif: llvm.powi's (EVAL.md §5), as compiler_builtins' __powidf2 and __powisf2 compute it (the
   native build's call): repeated squaring, the reciprocal for a negative exponent. */
double __builtin_powi(double a, int b) {
  const int recip = b < 0;
  double r = 1;
  while (1) { if (b & 1) r *= a; b /= 2; if (b == 0) break; a *= a; }
  return recip ? 1 / r : r;
}
float __builtin_powif(float a, int b) {
  const int recip = b < 0;
  float r = 1;
  while (1) { if (b & 1) r *= a; b /= 2; if (b == 0) break; a *= a; }
  return recip ? 1 / r : r;
}

/* __builtin_fmod, __builtin_fmodf: frem's (the lowering's), musl's fmod and fmodf. */
double fmod(double, double);
float fmodf(float, float);
double __builtin_fmod(double x, double y) { return fmod(x, y); }
float __builtin_fmodf(float x, float y) { return fmodf(x, y); }

/* __cxa_thread_atexit_impl: std's thread-local destructors' registration (sys/thread_local/destructors/linux_like.rs), a weak
   reference natively, null on musl (which has none), so std falls back on a pthread key whose destructors the main thread
   never runs at exit. ci calls a weak undefined function rather than reading it null (a ci gap): this port registers nothing
   and returns success, so the main thread's destructors do not run either, as natively. */
int __cxa_thread_atexit_impl(void (*dtor)(void *), void *obj, void *dso) { (void)dtor; (void)obj; (void)dso; return 0; }
