/* The C API's C half in the world (queue 244; doc/python/PYTHON-SURVEY.md, "As built: 244"; the user's 244-2 (a) and
   244-3 (a)): the data Python.h declares (the singletons' structs, the exception pointers), defined here so the link
   gives them addresses; arbace/pyctypes binds them at its start (the singletons' structs become their objects' handles,
   the PyExc_* pointers are set to the exception classes' handles). The functions are pyctypes's natives. Arbace's own. */
#include <Python.h>

PyObject _Py_NoneStruct = { 1, 0 };
PyObject _Py_TrueStruct = { 1, 0 };
PyObject _Py_FalseStruct = { 1, 0 };
PyObject *PyExc_TypeError;
PyObject *PyExc_ValueError;
