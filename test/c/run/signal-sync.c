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
  r = sigsetjmp(env, 1);
  if (r == 0) { printf("%d\n", 2 / zero); return 1; }
  printf("fpe again %d\n", r);
  sa.sa_sigaction = onsegv; sa.sa_flags = SA_SIGINFO;
  sigaction(SIGSEGV, &sa, 0);
  r = sigsetjmp(env, 1);
  if (r == 0) { *(volatile int *)8 = 1; return 1; }
  printf("segv %d hits %d\n", r, hits);
  signal(SIGFPE, SIG_DFL);
  return 1 / zero;
}
