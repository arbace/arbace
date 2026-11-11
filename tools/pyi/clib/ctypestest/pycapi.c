/* The C API's C half in the world (queue 244, queue 262; doc/python/PYTHON-SURVEY.md, "As built: 244", "As built: 262";
   the user's 244-2 (a) and 244-3 (a)): the data CPython's Include/ declares (the singletons' structs, the type objects,
   the PyExc_* pointers), defined here so the link gives them addresses. arbace/pyctypes binds them at its start: each
   struct becomes its object's handle (a proxy's head, ob_refcnt and ob_type; the limited API reads no further), each
   PyExc_NAME pointer is set to the handle of builtins.NAME. The functions are pyctypes's natives. A static link keeps
   only what is reached, so the unused ones cost nothing. This file includes no Python.h: under the limited API the
   type objects' type is incomplete, and only their head is a proxy's. Since queue 269 the full API's macros read a
   type object's fields (Py_TYPE(o)->tp_flags, PyList_Type.tp_new): each type object has PyTypeObject's size (416
   bytes on LP64), which pyctypes fills as CPython's own. Arbace's own. */

struct pyi_proxy { long ob_refcnt; void *ob_type; };
struct pyi_type { long ob_refcnt; void *ob_type; char rest[400]; };
struct pyi_proxy _Py_NoneStruct = { 1, 0 };
struct pyi_proxy _Py_TrueStruct = { 1, 0 };
struct pyi_proxy _Py_FalseStruct = { 1, 0 };
struct pyi_proxy _Py_EllipsisObject = { 1, 0 };
struct pyi_proxy _Py_NotImplementedStruct = { 1, 0 };
struct pyi_type PyType_Type = { 1, 0 };
struct pyi_type PyBaseObject_Type = { 1, 0 };
struct pyi_type PyLong_Type = { 1, 0 };
struct pyi_type PyBool_Type = { 1, 0 };
struct pyi_type PyFloat_Type = { 1, 0 };
struct pyi_type PyComplex_Type = { 1, 0 };
struct pyi_type PyUnicode_Type = { 1, 0 };
struct pyi_type PyBytes_Type = { 1, 0 };
struct pyi_type PyByteArray_Type = { 1, 0 };
struct pyi_type PyTuple_Type = { 1, 0 };
struct pyi_type PyList_Type = { 1, 0 };
struct pyi_type PyDict_Type = { 1, 0 };
struct pyi_type PySet_Type = { 1, 0 };
struct pyi_type PyFrozenSet_Type = { 1, 0 };
struct pyi_type PySlice_Type = { 1, 0 };
struct pyi_type PyModule_Type = { 1, 0 };
struct pyi_type PyDictProxy_Type = { 1, 0 };
struct pyi_type PyRange_Type = { 1, 0 };
struct pyi_type PyMemoryView_Type = { 1, 0 };
struct pyi_type PyProperty_Type = { 1, 0 };
struct pyi_type PyCapsule_Type = { 1, 0 };
struct pyi_type PyInstanceMethod_Type = { 1, 0 };
struct pyi_type PyGen_Type = { 1, 0 };

/* Py_Version (Include/pylifecycle.h; queue 269: _testcapi reads it): Py_PACK_FULL_VERSION of 3.14.10 final */
const unsigned long Py_Version = 0x030E0AF0;

void *PyExc_ArithmeticError;
void *PyExc_AssertionError;
void *PyExc_AttributeError;
void *PyExc_BaseException;
void *PyExc_BlockingIOError;
void *PyExc_BrokenPipeError;
void *PyExc_BufferError;
void *PyExc_BytesWarning;
void *PyExc_ChildProcessError;
void *PyExc_ConnectionAbortedError;
void *PyExc_ConnectionError;
void *PyExc_ConnectionRefusedError;
void *PyExc_ConnectionResetError;
void *PyExc_DeprecationWarning;
void *PyExc_EOFError;
void *PyExc_EnvironmentError;
void *PyExc_Exception;
void *PyExc_FileExistsError;
void *PyExc_FileNotFoundError;
void *PyExc_FloatingPointError;
void *PyExc_FutureWarning;
void *PyExc_GeneratorExit;
void *PyExc_IOError;
void *PyExc_ImportError;
void *PyExc_ImportWarning;
void *PyExc_IndentationError;
void *PyExc_IndexError;
void *PyExc_InterruptedError;
void *PyExc_IsADirectoryError;
void *PyExc_KeyError;
void *PyExc_KeyboardInterrupt;
void *PyExc_LookupError;
void *PyExc_MemoryError;
void *PyExc_ModuleNotFoundError;
void *PyExc_NameError;
void *PyExc_NotADirectoryError;
void *PyExc_NotImplementedError;
void *PyExc_OSError;
void *PyExc_OverflowError;
void *PyExc_PendingDeprecationWarning;
void *PyExc_PermissionError;
void *PyExc_ProcessLookupError;
void *PyExc_RecursionError;
void *PyExc_ReferenceError;
void *PyExc_ResourceWarning;
void *PyExc_RuntimeError;
void *PyExc_RuntimeWarning;
void *PyExc_StopAsyncIteration;
void *PyExc_StopIteration;
void *PyExc_SyntaxError;
void *PyExc_SyntaxWarning;
void *PyExc_SystemError;
void *PyExc_SystemExit;
void *PyExc_TabError;
void *PyExc_TimeoutError;
void *PyExc_TypeError;
void *PyExc_UnboundLocalError;
void *PyExc_UnicodeDecodeError;
void *PyExc_UnicodeEncodeError;
void *PyExc_UnicodeError;
void *PyExc_UnicodeTranslateError;
void *PyExc_UnicodeWarning;
void *PyExc_UserWarning;
void *PyExc_ValueError;
void *PyExc_Warning;
void *PyExc_ZeroDivisionError;
void *PyExc_BaseExceptionGroup;
void *PyExc_EncodingWarning;
void *PyExc_PythonFinalizationError;
