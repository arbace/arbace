/* ci-run verdict: exit 134. abort() called on a thread other than the main one ends the process by SIGABRT, as musl's
   abort does on Linux (raise's tkill names the calling thread's id, not the process's): status 134, whichever thread
   calls it. ci once answered that tkill ESRCH, so abort went on to a_crash (status 139): n-ci-thread, 2026-11-17, the
   llvmi row cpp.runtime.thread-throw's and bin/cppi check's runtime/thread-throw.cpp's cause. Prints "main", then
   "thread"; a return from the join (main's status 1) or from abort (the thread's 2) fails the verdict. */
#include <pthread.h>
#include <stdio.h>
#include <stdlib.h>
static void *run(void *arg) {
  (void)arg;
  printf("thread\n");
  fflush(stdout);
  abort();
  return (void *)2;
}
int main(void) {
  pthread_t t;
  printf("main\n");
  fflush(stdout);
  if (pthread_create(&t, 0, run, 0) != 0) return 3;
  void *r;
  pthread_join(t, &r);
  return 1;
}
