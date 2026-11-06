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
   reference natively, null on musl (which has none), so std falls back on its own list run by a pthread key's destructor:
   musl's pthread_exit runs it as a thread other than the main one exits, and the main thread never runs it at exit. ci calls
   a weak undefined function rather than reading it null (a ci gap, RES31): this port keeps natively's behaviour with the same
   mechanism, a list per thread (last registered first run, a destructor registering more run too) held by a pthread key whose
   destructor runs it (since RE6: ci runs threads). */
typedef unsigned pthread_key_t;
typedef int pthread_once_t;
int pthread_key_create(pthread_key_t *, void (*)(void *));
int pthread_once(pthread_once_t *, void (*)(void));
void *pthread_getspecific(pthread_key_t);
int pthread_setspecific(pthread_key_t, const void *);
void *malloc(unsigned long);
void free(void *);
struct rsi_tls_dtor { void (*dtor)(void *); void *obj; struct rsi_tls_dtor *next; };
static pthread_key_t rsi_tls_key;
static pthread_once_t rsi_tls_once;
static void rsi_tls_run(void *head) {
  struct rsi_tls_dtor *d = head;
  while (d) {
    pthread_setspecific(rsi_tls_key, d->next);
    d->dtor(d->obj);
    free(d);
    d = pthread_getspecific(rsi_tls_key);
  }
}
static void rsi_tls_init(void) { pthread_key_create(&rsi_tls_key, rsi_tls_run); }
int __cxa_thread_atexit_impl(void (*dtor)(void *), void *obj, void *dso) {
  struct rsi_tls_dtor *d;
  (void)dso;
  pthread_once(&rsi_tls_once, rsi_tls_init);
  d = malloc(sizeof *d);
  if (!d) return -1;
  d->dtor = dtor; d->obj = obj; d->next = pthread_getspecific(rsi_tls_key);
  pthread_setspecific(rsi_tls_key, d);
  return 0;
}
