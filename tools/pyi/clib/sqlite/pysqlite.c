/* The C side of pyi's _sqlite3 (queue 215; doc/python/PYTHON-SURVEY.md, "As built: 215"): built by ci's builder with
   SQLite 3.50.4's amalgamation into one node program. The Go glue calls SQLite's functions directly; this file holds
   the program's entry. */

#include "sqlite3.h"

/* musl's libc initialization, which arbace/clib calls once (see bzip2's pybz2.c). */
void __init_libc(char **envp, char *pn);

/* A program needs an entry; the library's is never run. It names __init_libc and the API's entry points the glue
   reaches first, so the link keeps them. */
int main(void)
{
    __init_libc((char **)0, (char *)0);
    return sqlite3_libversion_number() > 0 ? 0 : 1;
}
