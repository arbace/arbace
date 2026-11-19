/* hsi's RTS OS-threads API (queue 430): the functions of rts/OSThreads.h a program's own C calls (IncallAffinity: the
   condition variables, mutexes and OS threads of GHC 9.14.1's rts/posix/OSThreads.c, which GHC's RTS defines and a test's C
   links against), over musl's pthreads as there; linked by bin/hsi cnodes when the test's C names one of them. The
   threads are ci's (crt's scheduler); a thread's in-calls are hsi's rts_inCall. */
#include <pthread.h>
#include <sched.h>
#include <stdbool.h>
#include <stdlib.h>

/* rts/include/rts/OSThreads.h's posix types (no timedwait clock: HAVE_PTHREAD_CONDATTR_SETCLOCK unset in this world) */
typedef struct { pthread_cond_t cond; } Condition;
typedef pthread_mutex_t Mutex;
typedef pthread_t OSThreadId;
typedef void *OSThreadProc(void *);

void initCondition(Condition *c) { pthread_cond_init(&c->cond, NULL); }
void closeCondition(Condition *c) { pthread_cond_destroy(&c->cond); }
void broadcastCondition(Condition *c) { pthread_cond_broadcast(&c->cond); }
void signalCondition(Condition *c) { pthread_cond_signal(&c->cond); }
void waitCondition(Condition *c, Mutex *m) { pthread_cond_wait(&c->cond, m); }

void initMutex(Mutex *m)
{
    /* OSThreads.c: an error-checking mutex (a recursive lock is the RTS's error, not a hang) */
    pthread_mutexattr_t attr;
    pthread_mutexattr_init(&attr);
    pthread_mutexattr_settype(&attr, PTHREAD_MUTEX_ERRORCHECK);
    pthread_mutex_init(m, &attr);
    pthread_mutexattr_destroy(&attr);
}
void closeMutex(Mutex *m) { pthread_mutex_destroy(m); }

OSThreadId osThreadId(void) { return pthread_self(); }
void yieldThread(void) { sched_yield(); }
void shutdownThread(void) { pthread_exit(NULL); }

/* createOSThread: a detached thread running startProc(param), 0 on success (OSThreads.c's) */
int createOSThread(OSThreadId *pId, const char *name, OSThreadProc *startProc, void *param)
{
    (void)name;
    int r = pthread_create(pId, NULL, startProc, param);
    if (r == 0) pthread_detach(*pId);
    return r;
}
int createAttachedOSThread(OSThreadId *pId, const char *name, OSThreadProc *startProc, void *param)
{
    (void)name;
    return pthread_create(pId, NULL, startProc, param);
}
void joinOSThread(OSThreadId id) { pthread_join(id, NULL); }
