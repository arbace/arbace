/* The _testcapi parts pyi's C world does not build yet (queue 269; doc/python/PYTHON-SURVEY.md, "As built: 269"):
   Modules/_testcapimodule.c's PyInit__testcapi calls each part's _PyTestCapi_Init_NAME (Modules/_testcapi/parts.h);
   lib.conf builds the parts whose C API pyi's natives and the full API's layouts give, and each other part's Init is
   a stub here that adds nothing, so the module loads and a test reaching a part not built finds no function of it
   (:testcapi, narrowed per file). A part built leaves this list (bin/pyi-clib's build fails on a doubled
   definition). Arbace's own. */

int _PyTestCapi_Init_Vectorcall(void *m) { (void)m; return 0; }
int _PyTestCapi_Init_Heaptype(void *m) { (void)m; return 0; }
int _PyTestCapi_Init_Abstract(void *m) { (void)m; return 0; }
int _PyTestCapi_Init_Unicode(void *m) { (void)m; return 0; }
int _PyTestCapi_Init_GetArgs(void *m) { (void)m; return 0; }
int _PyTestCapi_Init_DateTime(void *m) { (void)m; return 0; }
int _PyTestCapi_Init_Docstring(void *m) { (void)m; return 0; }
int _PyTestCapi_Init_Mem(void *m) { (void)m; return 0; }
int _PyTestCapi_Init_Watchers(void *m) { (void)m; return 0; }
int _PyTestCapi_Init_Long(void *m) { (void)m; return 0; }
int _PyTestCapi_Init_Complex(void *m) { (void)m; return 0; }
int _PyTestCapi_Init_Numbers(void *m) { (void)m; return 0; }
int _PyTestCapi_Init_Dict(void *m) { (void)m; return 0; }
int _PyTestCapi_Init_Set(void *m) { (void)m; return 0; }
int _PyTestCapi_Init_Structmember(void *m) { (void)m; return 0; }
int _PyTestCapi_Init_Exceptions(void *m) { (void)m; return 0; }
int _PyTestCapi_Init_Code(void *m) { (void)m; return 0; }
int _PyTestCapi_Init_Buffer(void *m) { (void)m; return 0; }
int _PyTestCapi_Init_PyAtomic(void *m) { (void)m; return 0; }
int _PyTestCapi_Init_Run(void *m) { (void)m; return 0; }
int _PyTestCapi_Init_File(void *m) { (void)m; return 0; }
int _PyTestCapi_Init_Codec(void *m) { (void)m; return 0; }
int _PyTestCapi_Init_Immortal(void *m) { (void)m; return 0; }
int _PyTestCapi_Init_GC(void *m) { (void)m; return 0; }
int _PyTestCapi_Init_Hash(void *m) { (void)m; return 0; }
int _PyTestCapi_Init_Time(void *m) { (void)m; return 0; }
int _PyTestCapi_Init_Monitoring(void *m) { (void)m; return 0; }
int _PyTestCapi_Init_Object(void *m) { (void)m; return 0; }
int _PyTestCapi_Init_Config(void *m) { (void)m; return 0; }
int _PyTestCapi_Init_Import(void *m) { (void)m; return 0; }
int _PyTestCapi_Init_Frame(void *m) { (void)m; return 0; }
int _PyTestCapi_Init_Type(void *m) { (void)m; return 0; }
int _PyTestCapi_Init_Function(void *m) { (void)m; return 0; }
int _PyTestCapi_Init_Weakref(void *m) { (void)m; return 0; }
