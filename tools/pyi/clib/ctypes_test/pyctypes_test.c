/* The C side of pyi's ctypes world (queue 244; doc/python/PYTHON-SURVEY.md, "As built: 244"): built by ci's builder
   with CPython's Modules/_ctypes/_ctypes_test.c into one node program, go/arbace/clib/ctypes_test's Nodes, linked with
   musl 1.2.6. A static link keeps only what is reached, so this file names what a ctypes program may look up in the C
   library by name (CDLL(None), find_library("c"), find_library("m")): the functions Lib/test/test_ctypes and
   Lib/ctypes call there, kept by the table pyctypes_libc. The world's set is the survey's PYS-CT2. */

#include <stddef.h>
#include <stdlib.h>
#include <stdio.h>
#include <string.h>
#include <wchar.h>
#include <ctype.h>
#include <math.h>
#include <fcntl.h>
#include <unistd.h>
#include <errno.h>
#include <time.h>

void *pyctypes_libc[] = {
    (void *)qsort, (void *)open, (void *)close, (void *)read, (void *)write, (void *)atan2, (void *)sqrt, (void *)pow,
    (void *)strlen, (void *)strchr, (void *)strcpy, (void *)strncpy, (void *)strcmp, (void *)strtok, (void *)strtol,
    (void *)printf, (void *)sprintf, (void *)snprintf, (void *)puts, (void *)malloc, (void *)calloc, (void *)realloc,
    (void *)free, (void *)memset, (void *)memmove, (void *)memcpy, (void *)memcmp, (void *)wcslen, (void *)labs,
    (void *)abs, (void *)atoi, (void *)toupper, (void *)getpid, (void *)time, (void *)getenv, (void *)__errno_location,
};

/* musl's libc initialization (src/env/__libc_start_main.c): arbace/clib calls it once, before the world's first
   call, as __libc_start_main would (the thread pointer, the auxiliary vector). */
void __init_libc(char **envp, char *pn);

/* A program needs an entry; the world's is never run. It names __init_libc and the table, so the link keeps them. */
int main(void)
{
    __init_libc((char **)0, (char *)0);
    return pyctypes_libc[0] != 0;
}
