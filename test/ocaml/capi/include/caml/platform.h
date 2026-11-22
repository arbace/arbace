/* oi's C API: Arbace's own clean-room <caml/platform.h> (test/ocaml/capi/include/caml/mlvalues.h's notes; queue 530): what a stub that
   starts its own threads needs of it, POSIX threads' headers and the return type of a thread's function (a pthread's void *). */
#ifndef CAML_PLATFORM_H
#define CAML_PLATFORM_H
#include <pthread.h>
#include <errno.h>
#include <string.h>
#include <stdbool.h>
#include <stdio.h>
#include <stdlib.h>
#include "config.h"
#include "mlvalues.h"
#define CAML_THREAD_FUNCTION void *
#endif
