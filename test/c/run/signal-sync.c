/* ci-run verdict: exit 136. Synchronous signals caught and left by siglongjmp: an integer division by zero's SIGFPE twice, a
   store to address 8's SIGSEGV through sigaction's SA_SIGINFO handler, then SIGFPE at its default, which ends the
   program (status 128 + 8). Checks itself: sigsetjmp's values are the signals' numbers as Linux's x86-64 ABI gives them
   (SIGFPE 8, SIGSEGV 11, the handler adding 100), three handler calls; a difference returns 1 before the last division. */
#include <setjmp.h>
#include <signal.h>
#include <stdio.h>
static sigjmp_buf env;
static volatile int zero = 0, hits = 0;
static void onfpe(int s) { hits++; siglongjmp(env, s); }
static void onsegv(int s, siginfo_t *i, void *c) { (void)i; (void)c; hits++; siglongjmp(env, 100 + s); }
int main(void) { setvbuf(stdout, 0, _IONBF, 0);
  struct sigaction sa = {0};
  signal(SIGFPE, onfpe);
  int r = sigsetjmp(env, 1);
  if (r == 0) { printf("%d\n", 1 / zero); return 1; }
  printf("fpe %d\n", r);
  if (r != SIGFPE) return 1;
  r = sigsetjmp(env, 1);
  if (r == 0) { printf("%d\n", 2 / zero); return 1; }
  printf("fpe again %d\n", r);
  if (r != SIGFPE) return 1;
  sa.sa_sigaction = onsegv; sa.sa_flags = SA_SIGINFO;
  sigaction(SIGSEGV, &sa, 0);
  r = sigsetjmp(env, 1);
  if (r == 0) { *(volatile int *)8 = 1; return 1; }
  printf("segv %d hits %d\n", r, hits);
  if (r != 100 + SIGSEGV || SIGSEGV != 11 || SIGFPE != 8 || hits != 3) return 1;
  signal(SIGFPE, SIG_DFL);
  return 1 / zero;
}
