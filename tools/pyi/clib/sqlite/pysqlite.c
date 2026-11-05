/* The C side of pyi's _sqlite3 (queue 215; doc/python/PYTHON-SURVEY.md, "As built: 215"): built by ci's builder with
   SQLite 3.50.4's amalgamation into one node program, go/arbace/clib/sqlite's Nodes. The Go glue (go/arbace/pysqlite3)
   calls SQLite's functions directly, and these helpers:
   - the callbacks: SQLite calls C function pointers, so the glue's callbacks are natives of ci (Go functions the node
     program calls by name, ci.Natives, registered by the glue before the library opens), declared extern here and
     handed to SQLite by the registering wrappers below; the context pointers are the glue's handles;
   - the calls that take or give a double (arbace/clib passes integers): the double's bits as a long long;
   - sqlite3_db_config, variadic, at the one form the module uses. */

#include <string.h>
#include "sqlite3.h"

/* the natives (go/arbace/pysqlite3, callbacks.go) */
extern void pysqlite_func(sqlite3_context *ctx, int argc, sqlite3_value **argv);
extern void pysqlite_step(sqlite3_context *ctx, int argc, sqlite3_value **argv);
extern void pysqlite_final(sqlite3_context *ctx);
extern void pysqlite_value(sqlite3_context *ctx);
extern void pysqlite_inverse(sqlite3_context *ctx, int argc, sqlite3_value **argv);
extern void pysqlite_destroy(void *ctx);
extern int pysqlite_collation(void *ctx, int n1, const void *s1, int n2, const void *s2);
extern int pysqlite_authorizer(void *ctx, int action, const char *arg1, const char *arg2, const char *dbname,
                               const char *source);
extern int pysqlite_progress(void *ctx);
extern int pysqlite_trace(unsigned int type, void *ctx, void *stmt, void *sql);

/* create_function (aggregate 0) and create_aggregate (aggregate 1): sqlite3_create_function_v2 with the natives */
int pysqlite_create_function(sqlite3 *db, const char *name, int narg, int flags, void *ctx, int aggregate)
{
    if (aggregate) {
        return sqlite3_create_function_v2(db, name, narg, flags, ctx, 0, pysqlite_step, pysqlite_final,
                                          pysqlite_destroy);
    }
    return sqlite3_create_function_v2(db, name, narg, flags, ctx, pysqlite_func, 0, 0, pysqlite_destroy);
}

/* create_window_function: ctx 0 clears the function */
int pysqlite_create_window_function(sqlite3 *db, const char *name, int narg, int flags, void *ctx)
{
    if (ctx == 0) {
        return sqlite3_create_window_function(db, name, narg, flags, 0, 0, 0, 0, 0, 0);
    }
    return sqlite3_create_window_function(db, name, narg, flags, ctx, pysqlite_step, pysqlite_final, pysqlite_value,
                                          pysqlite_inverse, pysqlite_destroy);
}

/* create_collation: ctx 0 removes the collation */
int pysqlite_create_collation(sqlite3 *db, const char *name, int flags, void *ctx)
{
    if (ctx == 0) {
        return sqlite3_create_collation_v2(db, name, flags, 0, 0, 0);
    }
    return sqlite3_create_collation_v2(db, name, flags, ctx, pysqlite_collation, pysqlite_destroy);
}

/* set_authorizer, set_progress_handler, set_trace_callback: ctx 0 clears the callback */
int pysqlite_set_authorizer(sqlite3 *db, void *ctx)
{
    return sqlite3_set_authorizer(db, ctx ? pysqlite_authorizer : 0, ctx);
}

void pysqlite_set_progress_handler(sqlite3 *db, int n, void *ctx)
{
    sqlite3_progress_handler(db, n, ctx ? pysqlite_progress : 0, ctx);
}

int pysqlite_set_trace(sqlite3 *db, void *ctx)
{
    return sqlite3_trace_v2(db, SQLITE_TRACE_STMT, ctx ? pysqlite_trace : 0, ctx);
}

/* the doubles, as their bits */
long long pysqlite_column_double(sqlite3_stmt *stmt, int i)
{
    double d = sqlite3_column_double(stmt, i);
    long long b;
    memcpy(&b, &d, sizeof b);
    return b;
}

long long pysqlite_value_double(sqlite3_value *v)
{
    double d = sqlite3_value_double(v);
    long long b;
    memcpy(&b, &d, sizeof b);
    return b;
}

int pysqlite_bind_double(sqlite3_stmt *stmt, int i, long long bits)
{
    double d;
    memcpy(&d, &bits, sizeof d);
    return sqlite3_bind_double(stmt, i, d);
}

void pysqlite_result_double(sqlite3_context *ctx, long long bits)
{
    double d;
    memcpy(&d, &bits, sizeof d);
    sqlite3_result_double(ctx, d);
}

/* sqlite3_db_config(db, op, int enable, int *actual), the boolean options' form */
int pysqlite_db_config(sqlite3 *db, int op, int enable, int *actual)
{
    return sqlite3_db_config(db, op, enable, actual);
}

/* musl's libc initialization, which arbace/clib calls once (see bzip2's pybz2.c). */
void __init_libc(char **envp, char *pn);

/* A program needs an entry; the library's is never run. It names __init_libc, so the link keeps it. */
int main(void)
{
    __init_libc((char **)0, (char *)0);
    return sqlite3_libversion_number() > 0 ? 0 : 1;
}
