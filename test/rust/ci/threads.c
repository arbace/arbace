#include <pthread.h>
#include <stdio.h>
#include <stdlib.h>
#include <unistd.h>
#include <time.h>
_Thread_local int tl = 7;
static pthread_mutex_t mu = PTHREAD_MUTEX_INITIALIZER;
static pthread_cond_t cv = PTHREAD_COND_INITIALIZER;
static int counter, ready;
static void *worker(void *arg) {
  long id = (long)arg;
  tl += (int)id;
  for (int i = 0; i < 1000; i++) { pthread_mutex_lock(&mu); counter++; pthread_mutex_unlock(&mu); }
  pthread_mutex_lock(&mu); ready++; pthread_cond_signal(&cv); pthread_mutex_unlock(&mu);
  return (void *)(long)(tl * 10);
}
static void *sleeper(void *a) { struct timespec ts = {0, 20000000}; nanosleep(&ts, 0); return (void*)5; }
int main(void) {
  pthread_t t[4];
  for (long i = 0; i < 4; i++) pthread_create(&t[i], 0, worker, (void *)i);
  pthread_mutex_lock(&mu); while (ready < 4) pthread_cond_wait(&cv, &mu); pthread_mutex_unlock(&mu);
  for (int i = 0; i < 4; i++) { void *r; pthread_join(t[i], &r); printf("joined %d -> %ld\n", i, (long)r); }
  printf("counter %d tl %d\n", counter, tl);
  pthread_t s; pthread_create(&s, 0, sleeper, 0); pthread_detach(s);
  pthread_t s2; void *r2; pthread_create(&s2, 0, sleeper, 0); pthread_join(s2, &r2); printf("sleeper %ld\n", (long)r2);
  struct timespec ts = {0, 50000000}; nanosleep(&ts, 0);
  printf("done\n");
  return 3;
}
