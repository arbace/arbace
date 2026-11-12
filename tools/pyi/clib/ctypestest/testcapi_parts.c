/* The _testcapi parts pyi's C world does not build yet (queue 269; doc/python/PYTHON-SURVEY.md, "As built: 269"):
   Modules/_testcapimodule.c's PyInit__testcapi calls each part's _PyTestCapi_Init_NAME (Modules/_testcapi/parts.h);
   lib.conf builds the parts whose C API pyi's natives and the full API's layouts give, and each other part's Init is
   a stub here that adds nothing, so the module loads and a test reaching a part not built finds no function of it
   (:testcapi, narrowed per file). A part built leaves this list (bin/pyi-clib's build fails on a doubled
   definition). Arbace's own. */

int _PyTestCapi_Init_Watchers(void *m) { (void)m; return 0; }
int _PyTestCapi_Init_Monitoring(void *m) { (void)m; return 0; }
