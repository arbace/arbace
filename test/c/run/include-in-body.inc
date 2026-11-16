/* include-in-body.c's statements (Arbace's own, EPL-1.0), included inside its function bodies as CPython's
   Modules/_testcapimodule.c includes _testcapi_feature_macros.inc */
#ifdef HAVE_ONE
    res = add(result, "HAVE_ONE", 1);
#else
    res = add(result, "HAVE_ONE", 0);
#endif
if (res) {
    return -1;
}

#ifdef HAVE_TWO
    res = add(result, "HAVE_TWO", 1);
#else
    res = add(result, "HAVE_TWO", 0);
#endif
if (res) {
    return -1;
}
