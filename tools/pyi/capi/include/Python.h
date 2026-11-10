/* Python.h: Arbace's own clean-room header of CPython's C API (queue 244; doc/python/PYTHON-SURVEY.md, "As built:
   244"), for extension modules built by ci (Arbace's C interpreter) and run beside pyi. Written from the C API's
   documentation (docs.python.org/3.14/c-api: "Objects, Types and Reference Counts", "Common Object Structures",
   "Module Objects", "Initializing Modules", "Parsing arguments", "Exception Handling", "Stable ABI"), not from
   CPython's headers. The limited API's shape: PyObject is its documented head (ob_refcnt, ob_type), every other
   object opaque, every operation a function; Py_INCREF and Py_DECREF are the stable ABI's Py_IncRef and Py_DecRef.
   This first slice declares what Modules/_ctypes/_ctypes_test.c reaches; the functions are pyi's (the boundary of
   the survey's design, PYS-CT3). */
#ifndef Py_PYTHON_H
#define Py_PYTHON_H

#include "pyconfig.h"
#include <stddef.h>
#include <stdarg.h>
#include <stdint.h>
#include <inttypes.h>
#include <errno.h>
#include <assert.h>
#include <limits.h>
#include <string.h>
#include <stdlib.h>
#include <stdio.h>
#include <wchar.h>

#ifdef __cplusplus
extern "C" {
#endif

#define PY_MAJOR_VERSION 3
#define PY_MINOR_VERSION 14
#define PY_VERSION_HEX 0x030E0AF0

#define Py_EXPORTED_SYMBOL __attribute__((visibility("default")))
#define PyAPI_FUNC(RTYPE) extern RTYPE
#define PyAPI_DATA(RTYPE) extern RTYPE
#define PyMODINIT_FUNC PyObject *

typedef long Py_ssize_t;
typedef Py_ssize_t Py_hash_t;

/* the object's head: "Objects, Types and Reference Counts", "PyObject" */
typedef struct _typeobject PyTypeObject;
typedef struct _object {
    Py_ssize_t ob_refcnt;
    PyTypeObject *ob_type;
} PyObject;
typedef struct {
    PyObject ob_base;
    Py_ssize_t ob_size;
} PyVarObject;

#define PyObject_HEAD PyObject ob_base;
#define PyObject_VAR_HEAD PyVarObject ob_base;

PyAPI_FUNC(void) Py_IncRef(PyObject *);
PyAPI_FUNC(void) Py_DecRef(PyObject *);
#define Py_INCREF(op) Py_IncRef((PyObject *)(op))
#define Py_DECREF(op) Py_DecRef((PyObject *)(op))
#define Py_XINCREF(op) do { PyObject *_py_x = (PyObject *)(op); if (_py_x) Py_IncRef(_py_x); } while (0)
#define Py_XDECREF(op) do { PyObject *_py_x = (PyObject *)(op); if (_py_x) Py_DecRef(_py_x); } while (0)
#define Py_CLEAR(op) do { PyObject *_py_t = (PyObject *)(op); if (_py_t) { (op) = NULL; Py_DecRef(_py_t); } } while (0)
PyAPI_FUNC(PyObject *) Py_NewRef(PyObject *);
PyAPI_FUNC(PyObject *) Py_XNewRef(PyObject *);
#define Py_REFCNT(op) (((PyObject *)(op))->ob_refcnt)
#define Py_TYPE(op) (((PyObject *)(op))->ob_type)

PyAPI_DATA(PyObject) _Py_NoneStruct;
PyAPI_DATA(PyObject) _Py_TrueStruct;
PyAPI_DATA(PyObject) _Py_FalseStruct;
#define Py_None (&_Py_NoneStruct)
#define Py_True (&_Py_TrueStruct)
#define Py_False (&_Py_FalseStruct)
#define Py_RETURN_NONE return Py_NewRef(Py_None)
#define Py_RETURN_TRUE return Py_NewRef(Py_True)
#define Py_RETURN_FALSE return Py_NewRef(Py_False)

/* "Common Object Structures": functions and methods */
typedef PyObject *(*PyCFunction)(PyObject *, PyObject *);
typedef struct PyMethodDef {
    const char *ml_name;
    PyCFunction ml_meth;
    int ml_flags;
    const char *ml_doc;
} PyMethodDef;
#define METH_VARARGS 0x0001
#define METH_KEYWORDS 0x0002
#define METH_NOARGS 0x0004
#define METH_O 0x0008
#define METH_CLASS 0x0010
#define METH_STATIC 0x0020
#define METH_COEXIST 0x0040
#define METH_FASTCALL 0x0080
#define METH_METHOD 0x0200

/* "Module Objects", "Initializing Modules": multi-phase initialization */
typedef struct PyModuleDef_Base {
    PyObject ob_base;
    PyObject *(*m_init)(void);
    Py_ssize_t m_index;
    PyObject *m_copy;
} PyModuleDef_Base;
#define PyModuleDef_HEAD_INIT { { 1, NULL }, NULL, 0, NULL }
typedef struct PyModuleDef_Slot {
    int slot;
    void *value;
} PyModuleDef_Slot;
typedef int (*traverseproc)(PyObject *, int (*)(PyObject *, void *), void *);
typedef int (*inquiry)(PyObject *);
typedef void (*freefunc)(void *);
typedef struct PyModuleDef {
    PyModuleDef_Base m_base;
    const char *m_name;
    const char *m_doc;
    Py_ssize_t m_size;
    PyMethodDef *m_methods;
    PyModuleDef_Slot *m_slots;
    traverseproc m_traverse;
    inquiry m_clear;
    freefunc m_free;
} PyModuleDef;
#define Py_mod_create 1
#define Py_mod_exec 2
#define Py_mod_multiple_interpreters 3
#define Py_mod_gil 4
#define Py_MOD_MULTIPLE_INTERPRETERS_NOT_SUPPORTED ((void *)0)
#define Py_MOD_MULTIPLE_INTERPRETERS_SUPPORTED ((void *)1)
#define Py_MOD_PER_INTERPRETER_GIL_SUPPORTED ((void *)2)
#define Py_MOD_GIL_USED ((void *)0)
#define Py_MOD_GIL_NOT_USED ((void *)1)
PyAPI_FUNC(PyObject *) PyModuleDef_Init(PyModuleDef *);

/* "Parsing arguments and building values" */
PyAPI_FUNC(int) PyArg_ParseTuple(PyObject *, const char *, ...);

/* "Integer Objects" */
PyAPI_FUNC(PyObject *) PyLong_FromLong(long);
PyAPI_FUNC(PyObject *) PyLong_FromLongLong(long long);
PyAPI_FUNC(PyObject *) PyLong_FromUnsignedLongLong(unsigned long long);

/* "List Objects", "Bytes Objects", "Unicode Objects" */
PyAPI_FUNC(PyObject *) PyList_New(Py_ssize_t);
PyAPI_FUNC(int) PyList_Append(PyObject *, PyObject *);
PyAPI_FUNC(PyObject *) PyBytes_FromStringAndSize(const char *, Py_ssize_t);
PyAPI_FUNC(PyObject *) PyUnicode_FromString(const char *);
PyAPI_FUNC(int) PyUnicode_CompareWithASCIIString(PyObject *, const char *);
PyAPI_FUNC(int) PyType_HasFeature(PyTypeObject *, unsigned long);
PyAPI_FUNC(unsigned long) PyType_GetFlags(PyTypeObject *);
#define Py_TPFLAGS_UNICODE_SUBCLASS (1UL << 28)
#define PyUnicode_Check(op) ((PyType_GetFlags(Py_TYPE(op)) & Py_TPFLAGS_UNICODE_SUBCLASS) != 0)

/* "Exception Handling" */
PyAPI_DATA(PyObject *) PyExc_TypeError;
PyAPI_DATA(PyObject *) PyExc_ValueError;
PyAPI_FUNC(void) PyErr_SetString(PyObject *, const char *);
PyAPI_FUNC(PyObject *) PyErr_Format(PyObject *, const char *, ...);

#ifdef __cplusplus
}
#endif
#endif
