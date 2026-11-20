/* The C++ runtime's few functions libcpp's lowered C calls, for the token checks' reference (tools/ppref/ppref.cc;
   doc/cpp/CPP-SURVEY.md, "As built: pp-ref"). Arbace's own (EPL-1.0). libcpp is built as GCC builds it, with -fno-exceptions
   and -fno-rtti: operator new aborts where it would throw (GCC's xmalloc does the same), the rest is the allocator's. */

#include <stdio.h>
#include <stdlib.h>

static void *
shim_alloc (unsigned long n)
{
  void *p = malloc (n ? n : 1);
  if (!p)
    {
      fputs ("ppref: out of memory\n", stderr);
      abort ();
    }
  return p;
}

/* operator new (size_t), operator new[] (size_t) */
void *_Znwm (unsigned long n) { return shim_alloc (n); }
void *_Znam (unsigned long n) { return shim_alloc (n); }

/* operator delete (void *), operator delete[] (void *), and their sized forms */
void _ZdlPv (void *p) { free (p); }
void _ZdaPv (void *p) { free (p); }
void _ZdlPvm (void *p, unsigned long n) { (void) n; free (p); }
void _ZdaPvm (void *p, unsigned long n) { (void) n; free (p); }

/* the guards of a function's static with a dynamic initializer (the Itanium ABI's __cxa_guard_*): one thread, so the guard's
   first byte alone, as libsupc++'s guard.cc without threads */
int __cxa_guard_acquire (unsigned long long *g) { return *(unsigned char *) g == 0; }
void __cxa_guard_release (unsigned long long *g) { *(unsigned char *) g = 1; }
void __cxa_guard_abort (unsigned long long *g) { (void) g; }

/* libiberty's alloca without a compiler's (libiberty.h maps alloca to C_alloca when __GNUC__ is not defined, as the build reads
   libcpp: -U__GNUC__): heap blocks never freed (a file's preprocessing allocates little this way), C_alloca (0) nothing */
void *C_alloca (unsigned long n) { return n ? shim_alloc (n) : 0; }

/* a pure virtual function called: libsupc++'s message and abort */
void
__cxa_pure_virtual (void)
{
  fputs ("pure virtual method called\n", stderr);
  abort ();
}
