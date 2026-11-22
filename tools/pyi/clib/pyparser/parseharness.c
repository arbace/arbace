/* parseharness.c: Arbace's own harness around CPython 3.14.10's parser (queue 494; py-own-judge-1 (a), the user,
   2026-11-20): the parser's sources from the pinned tarball, unmodified (Parser/parser.c, pegen.c, pegen_errors.c,
   action_helpers.c, string_parser.c, peg_api.c; Python/Python-ast.c, asdl.c, pyarena.c, future.c, ast_preprocess.c;
   the tokenizer as queue 489 built it, with Parser/tokenizer/string_tokenizer.c and utf8_tokenizer.c; Python/mystrtoul.c),
   built by ci's builder (bin/pyi-clib build pyparser) into one node program with this file, which gives them the C API
   they call (PyMem, the exceptions as instances with SyntaxError's and UnicodeDecodeError's attributes, str and bytes in
   CPython's own layouts, the numbers, _tokenize's module machinery, unicodedata's normalize and \N{...}'s names: its
   code copied, its tables read from the tarball's Modules/*_db.h at start) and runs what tools/pydump/pydump.py tree
   runs over each file: ast.parse (compile()'s PyCF_ONLY_AST path, Py_CompileStringObject: the parser, then
   _PyFuture_FromAST and _PyAST_Preprocess), again with type_comments=True, and for --full tokenize.tokenize's stream
   (_tokenize.TokenizerIter), writing pydump.py's Writer's forms from the C tree itself: parse-check's reference without
   CPython (bin/repr-python parse-check, REPR_PYTHON_PARSE_REF=own).

   usage: parseharness --base DIR --list FILE --out DIR --encodings DIR [--tsv FILE] [--full]
     every file of the list (paths relative to DIR) written as OUT/REL.ae in pydump.py tree's format (--full: tree
     --full's); DIR of --encodings is the tarball's Lib/encodings (detect_encoding's codecs, as in queue 489's harness).
     The TSV, one line a file: REL STATUS (ok, error, helper-error). Unicodedata's tables: DIR/../../Modules.

   Except where it is copied from CPython (marked; PSF license, LICENSE.md) this file is Arbace's. */
#include "Python.h"
#include "pycore_runtime.h"
#include "pycore_token.h"           // _PyParser_TokenNames
#include "pycore_unicodeobject.h"
#include "pycore_pystate.h"
#include "pycore_parser.h"          // _PyParser_ASTFromString
#include "pycore_ast.h"
#include "pycore_pyarena.h"
#include "pycore_compile.h"         // _PyFuture_FromAST
#include "pycore_symtable.h"
#include "pycore_c_array.h"
#include "pegen.h"
#include "errcode.h"
#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <stdarg.h>
#include <sys/stat.h>
#include <errno.h>
#include <stdbool.h>

int _PyFuture_FromAST(mod_ty mod, PyObject *filename, _PyFutureFeatures *futures);
int _PyAST_Preprocess(mod_ty mod, PyArena *arena, PyObject *filename, int optimize, int ff_features,
                      int syntax_check_only, int enable_warnings);

/* ---------------------------------------------------------------- the runtime's data */

_PyRuntimeState _PyRuntime;     /* the parser's dummy_name and _Py_STR(empty) are set up in main; &_Py_ID(...) names */

static void fatal(const char *msg) { fprintf(stderr, "parseharness: %s\n", msg); exit(70); }
#define STUB(ret, name, params) ret name params { fatal("not given by the harness: " #name); return (ret)0; }
#define STUBV(name, params) void name params { fatal("not given by the harness: " #name); }

static void obj_free(PyObject *op) { free(op); }

PyTypeObject PyType_Type = { PyVarObject_HEAD_INIT(&PyType_Type, 0) .tp_name = "type",
                             .tp_flags = Py_TPFLAGS_TYPE_SUBCLASS };
PyTypeObject PyBaseObject_Type = { PyVarObject_HEAD_INIT(&PyType_Type, 0) .tp_name = "object" };
static void str_dealloc(PyObject *op);
PyTypeObject PyUnicode_Type = { PyVarObject_HEAD_INIT(&PyType_Type, 0) .tp_name = "str",
                                .tp_flags = Py_TPFLAGS_UNICODE_SUBCLASS, .tp_dealloc = str_dealloc };
PyTypeObject PyBytes_Type = { PyVarObject_HEAD_INIT(&PyType_Type, 0) .tp_name = "bytes",
                              .tp_flags = Py_TPFLAGS_BYTES_SUBCLASS, .tp_dealloc = obj_free };
static void tuple_dealloc(PyObject *op);
PyTypeObject PyTuple_Type = { PyVarObject_HEAD_INIT(&PyType_Type, 0) .tp_name = "tuple",
                              .tp_flags = Py_TPFLAGS_TUPLE_SUBCLASS, .tp_dealloc = tuple_dealloc };
static void list_dealloc(PyObject *op);
PyTypeObject PyList_Type = { PyVarObject_HEAD_INIT(&PyType_Type, 0) .tp_name = "list",
                             .tp_flags = Py_TPFLAGS_LIST_SUBCLASS, .tp_dealloc = list_dealloc };
static void int_dealloc(PyObject *op);
PyTypeObject PyLong_Type = { PyVarObject_HEAD_INIT(&PyType_Type, 0) .tp_name = "int",
                             .tp_flags = Py_TPFLAGS_LONG_SUBCLASS, .tp_dealloc = int_dealloc };
PyTypeObject PyBool_Type = { PyVarObject_HEAD_INIT(&PyType_Type, 0) .tp_name = "bool", .tp_base = &PyLong_Type };
PyTypeObject PyFloat_Type = { PyVarObject_HEAD_INIT(&PyType_Type, 0) .tp_name = "float", .tp_dealloc = obj_free };
PyTypeObject PyComplex_Type = { PyVarObject_HEAD_INIT(&PyType_Type, 0) .tp_name = "complex", .tp_dealloc = obj_free };
PyTypeObject PyDict_Type = { PyVarObject_HEAD_INIT(&PyType_Type, 0) .tp_name = "dict",
                             .tp_flags = Py_TPFLAGS_DICT_SUBCLASS };
PyTypeObject PyModule_Type = { PyVarObject_HEAD_INIT(&PyType_Type, 0) .tp_name = "module" };
PyTypeObject PyCFunction_Type = { PyVarObject_HEAD_INIT(&PyType_Type, 0) .tp_name = "builtin_function_or_method",
                                  .tp_dealloc = obj_free };
PyTypeObject PyEllipsis_Type = { PyVarObject_HEAD_INIT(&PyType_Type, 0) .tp_name = "ellipsis" };
/* named by Python-ast.c's type machinery, which the parser does not run */
PyTypeObject Py_GenericAliasType = { PyVarObject_HEAD_INIT(&PyType_Type, 0) .tp_name = "types.GenericAlias" };
PyTypeObject _PyUnion_Type = { PyVarObject_HEAD_INIT(&PyType_Type, 0) .tp_name = "typing.Union" };
static PyTypeObject Readline_Type = { PyVarObject_HEAD_INIT(&PyType_Type, 0) .tp_name = "readline" };

PyObject _Py_NoneStruct = { .ob_refcnt = _Py_STATIC_IMMORTAL_INITIAL_REFCNT, .ob_type = &PyBaseObject_Type };
PyObject _Py_EllipsisObject = { .ob_refcnt = _Py_STATIC_IMMORTAL_INITIAL_REFCNT, .ob_type = &PyEllipsis_Type };
PyLongObject _Py_TrueStruct = { PyObject_HEAD_INIT(&PyBool_Type) };
PyLongObject _Py_FalseStruct = { PyObject_HEAD_INIT(&PyBool_Type) };

/* data the headers' inline functions name: PyByteArray_AS_STRING's empty buffer, and Py_UNICODE_ISSPACE's table,
   Objects/unicodeobject.c's _Py_ascii_whitespace (CPython 3.14.10; PSF license): 1 for 0x09-0x0D and 0x1C-0x20 */
char _PyByteArray_empty_string[] = "";
const unsigned char _Py_ascii_whitespace[] = {
    0, 0, 0, 0, 0, 0, 0, 0, 0, 1, 1, 1, 1, 1, 0, 0,
    0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1, 1, 1, 1,
    1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
    0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
    0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
    0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
    0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
    0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
};

/* Objects/longobject.c's _PyLong_DigitValue (CPython 3.14.10; PSF license): Python/mystrtoul.c's digits */
unsigned char _PyLong_DigitValue[256] = {
    37, 37, 37, 37, 37, 37, 37, 37, 37, 37, 37, 37, 37, 37, 37, 37,
    37, 37, 37, 37, 37, 37, 37, 37, 37, 37, 37, 37, 37, 37, 37, 37,
    37, 37, 37, 37, 37, 37, 37, 37, 37, 37, 37, 37, 37, 37, 37, 37,
    0,  1,  2,  3,  4,  5,  6,  7,  8,  9,  37, 37, 37, 37, 37, 37,
    37, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 20, 21, 22, 23, 24,
    25, 26, 27, 28, 29, 30, 31, 32, 33, 34, 35, 37, 37, 37, 37, 37,
    37, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 20, 21, 22, 23, 24,
    25, 26, 27, 28, 29, 30, 31, 32, 33, 34, 35, 37, 37, 37, 37, 37,
    37, 37, 37, 37, 37, 37, 37, 37, 37, 37, 37, 37, 37, 37, 37, 37,
    37, 37, 37, 37, 37, 37, 37, 37, 37, 37, 37, 37, 37, 37, 37, 37,
    37, 37, 37, 37, 37, 37, 37, 37, 37, 37, 37, 37, 37, 37, 37, 37,
    37, 37, 37, 37, 37, 37, 37, 37, 37, 37, 37, 37, 37, 37, 37, 37,
    37, 37, 37, 37, 37, 37, 37, 37, 37, 37, 37, 37, 37, 37, 37, 37,
    37, 37, 37, 37, 37, 37, 37, 37, 37, 37, 37, 37, 37, 37, 37, 37,
    37, 37, 37, 37, 37, 37, 37, 37, 37, 37, 37, 37, 37, 37, 37, 37,
    37, 37, 37, 37, 37, 37, 37, 37, 37, 37, 37, 37, 37, 37, 37, 37,
};

/* the exceptions: a type each, its base as CPython's hierarchy has it (the part the sources ask) */
static void exc_dealloc(PyObject *op);
#define EXC(var, name, base) \
    static PyTypeObject var##_T = { PyVarObject_HEAD_INIT(&PyType_Type, 0) .tp_name = name, \
                                    .tp_flags = Py_TPFLAGS_BASE_EXC_SUBCLASS | Py_TPFLAGS_TYPE_SUBCLASS, .tp_base = base, \
                                    .tp_dealloc = exc_dealloc }; \
    PyObject *PyExc_##var = (PyObject *)&var##_T;
EXC(BaseException, "BaseException", NULL)
EXC(Exception, "Exception", &BaseException_T)
EXC(KeyboardInterrupt, "KeyboardInterrupt", &BaseException_T)
EXC(StopIteration, "StopIteration", &Exception_T)
EXC(SyntaxError, "SyntaxError", &Exception_T)
EXC(IndentationError, "IndentationError", &SyntaxError_T)
EXC(TabError, "TabError", &IndentationError_T)
EXC(TypeError, "TypeError", &Exception_T)
EXC(ValueError, "ValueError", &Exception_T)
EXC(UnicodeError, "UnicodeError", &ValueError_T)
EXC(UnicodeDecodeError, "UnicodeDecodeError", &UnicodeError_T)
EXC(LookupError, "LookupError", &Exception_T)
EXC(MemoryError, "MemoryError", &Exception_T)
EXC(SystemError, "SystemError", &Exception_T)
EXC(RuntimeError, "RuntimeError", &Exception_T)
EXC(RecursionError, "RecursionError", &RuntimeError_T)
EXC(OverflowError, "OverflowError", &Exception_T)
EXC(Warning, "Warning", &Exception_T)
EXC(SyntaxWarning, "SyntaxWarning", &Warning_T)
EXC(DeprecationWarning, "DeprecationWarning", &Warning_T)
PyTypeObject _PyExc_IncompleteInputError = { PyVarObject_HEAD_INIT(&PyType_Type, 0) .tp_name = "_IncompleteInputError",
    .tp_flags = Py_TPFLAGS_BASE_EXC_SUBCLASS | Py_TPFLAGS_TYPE_SUBCLASS, .tp_base = &SyntaxError_T, .tp_dealloc = exc_dealloc };

/* ---------------------------------------------------------------- memory and objects */

void *PyMem_Malloc(size_t n) { return malloc(n ? n : 1); }
void *PyMem_Calloc(size_t n, size_t s) { return calloc(n ? n : 1, s ? s : 1); }
void *PyMem_Realloc(void *p, size_t n) { return realloc(p, n ? n : 1); }
void PyMem_Free(void *p) { free(p); }

void _Py_NO_RETURN _Py_FatalErrorFunc(const char *func, const char *msg) { (void)func; fatal(msg); }
void _Py_NO_RETURN (Py_FatalError)(const char *msg) { fatal(msg); }

void _Py_Dealloc(PyObject *op)
{
    destructor d = Py_TYPE(op)->tp_dealloc;
    if (d) { d(op); } else { free(op); }
}

static PyObject *obj_new(PyTypeObject *t, size_t size)
{
    PyObject *op = calloc(1, size);
    if (!op) { fatal("out of memory"); }
    op->ob_refcnt = 1;
    op->ob_type = t;
    return op;
}

/* int: its value when it fits a long long, else the digits' text (a literal PyLong_FromString read); the parser only
   boxes them and the writer asks the type */
typedef struct { PyObject_HEAD long long v; char *big; } IntObject;
static void int_dealloc(PyObject *op) { free(((IntObject *)op)->big); free(op); }
static PyObject *int_new(long long v) { IntObject *o = (IntObject *)obj_new(&PyLong_Type, sizeof(IntObject)); o->v = v; return (PyObject *)o; }
PyObject *PyLong_FromLong(long v) { return int_new(v); }
PyObject *PyLong_FromSsize_t(Py_ssize_t v) { return int_new(v); }
PyObject *PyBool_FromLong(long v) { PyObject *r = v ? Py_True : Py_False; return Py_NewRef(r); }

typedef struct { PyObject_HEAD double v; } FloatObject;
PyObject *PyFloat_FromDouble(double v) { FloatObject *o = (FloatObject *)obj_new(&PyFloat_Type, sizeof(FloatObject)); o->v = v; return (PyObject *)o; }
typedef struct { PyObject_HEAD Py_complex v; } ComplexObject;
PyObject *PyComplex_FromCComplex(Py_complex v) { ComplexObject *o = (ComplexObject *)obj_new(&PyComplex_Type, sizeof(ComplexObject)); o->v = v; return (PyObject *)o; }

/* PyLong_FromString(s, NULL, 0) as parsenumber calls it: a literal the tokenizer took (no underscores left), too big
   for a long; Objects/longobject.c's limit on decimal digits (sys.int_info.default_max_str_digits, 4300) */
PyObject *PyLong_FromString(const char *s, char **pend, int base)
{
    (void)base;
    const char *p = s;
    int binary = 0;
    if (p[0] == '0' && (p[1] == 'x' || p[1] == 'X' || p[1] == 'o' || p[1] == 'O' || p[1] == 'b' || p[1] == 'B')) { binary = 1; p += 2; }
    Py_ssize_t digits = 0;
    for (const char *q = p; *q; q++) { if (*q != '_') { digits++; } }
    if (!binary && digits > 4300) {
        PyErr_Format(PyExc_ValueError, "Exceeds the limit (%d digits) for integer string conversion: value has %zd digits; "
                     "use sys.set_int_max_str_digits() to increase the limit", 4300, digits);
        return NULL;
    }
    IntObject *o = (IntObject *)int_new(0);
    o->big = strdup(s);
    if (pend) { *pend = (char *)s + strlen(s); }
    return (PyObject *)o;
}

/* Python/pystrtod.c's PyOS_string_to_double as parsenumber calls it (no overflow exception: an infinity) */
double PyOS_string_to_double(const char *s, char **endptr, PyObject *overflow_exception)
{
    (void)overflow_exception;
    char *end;
    double x = strtod(s, &end);
    if (endptr) { *endptr = end; }
    else if (end == s || *end) {
        PyErr_Format(PyExc_ValueError, "could not convert string to float: '%.200s'", s);
        return -1.0;
    }
    return x;
}

/* bytes: CPython's layout (PyBytes_AS_STRING and PyBytes_GET_SIZE read it in place) */
static PyObject *bytes_new(const char *s, Py_ssize_t n)
{
    PyBytesObject *b = (PyBytesObject *)obj_new(&PyBytes_Type, offsetof(PyBytesObject, ob_sval) + n + 1);
    Py_SET_SIZE(b, n);
    b->ob_shash = -1;
    if (s) { memcpy(b->ob_sval, s, n); }
    b->ob_sval[n] = 0;
    return (PyObject *)b;
}
PyObject *PyBytes_FromStringAndSize(const char *s, Py_ssize_t n) { return bytes_new(s, n); }
char *PyBytes_AsString(PyObject *o) { return PyBytes_AS_STRING(o); }
Py_ssize_t PyBytes_Size(PyObject *o) { return PyBytes_GET_SIZE(o); }
int PyBytes_AsStringAndSize(PyObject *o, char **s, Py_ssize_t *len)
{
    *s = PyBytes_AS_STRING(o);
    if (len) { *len = PyBytes_GET_SIZE(o); }
    else if ((Py_ssize_t)strlen(*s) != PyBytes_GET_SIZE(o)) { PyErr_SetString(PyExc_ValueError, "embedded null byte"); return -1; }
    return 0;
}
void PyBytes_Concat(PyObject **pv, PyObject *w)
{
    PyObject *v = *pv;
    if (!v) { return; }
    if (!w) { Py_CLEAR(*pv); return; }
    Py_ssize_t a = PyBytes_GET_SIZE(v), b = PyBytes_GET_SIZE(w);
    PyObject *r = bytes_new(NULL, a + b);
    memcpy(PyBytes_AS_STRING(r), PyBytes_AS_STRING(v), a);
    memcpy(PyBytes_AS_STRING(r) + a, PyBytes_AS_STRING(w), b);
    Py_DECREF(v);
    *pv = r;
}

/* tuples and lists: CPython's layouts */
static PyObject *tuple_new(Py_ssize_t n)
{
    PyTupleObject *t = (PyTupleObject *)obj_new(&PyTuple_Type, offsetof(PyTupleObject, ob_item) + (n ? n : 1) * sizeof(PyObject *));
    Py_SET_SIZE(t, n);
    return (PyObject *)t;
}
static void tuple_dealloc(PyObject *op)
{
    PyTupleObject *t = (PyTupleObject *)op;
    for (Py_ssize_t i = 0; i < Py_SIZE(t); i++) { Py_XDECREF(t->ob_item[i]); }
    free(op);
}
PyObject *PyTuple_New(Py_ssize_t n) { return tuple_new(n); }
PyObject *PyTuple_Pack(Py_ssize_t n, ...)
{
    va_list ap;
    PyObject *t = tuple_new(n);
    va_start(ap, n);
    for (Py_ssize_t i = 0; i < n; i++) {
        PyObject *o = va_arg(ap, PyObject *);
        Py_INCREF(o);
        ((PyTupleObject *)t)->ob_item[i] = o;
    }
    va_end(ap);
    return t;
}
PyObject *PyList_New(Py_ssize_t n)
{
    PyListObject *l = (PyListObject *)obj_new(&PyList_Type, sizeof(PyListObject));
    Py_SET_SIZE(l, n);
    l->allocated = n;
    l->ob_item = n ? calloc(n, sizeof(PyObject *)) : NULL;
    return (PyObject *)l;
}
static void list_dealloc(PyObject *op)
{
    PyListObject *l = (PyListObject *)op;
    for (Py_ssize_t i = 0; i < Py_SIZE(l); i++) { Py_XDECREF(l->ob_item[i]); }
    free(l->ob_item);
    free(op);
}
int PyList_Append(PyObject *op, PyObject *v)
{
    PyListObject *l = (PyListObject *)op;
    if (Py_SIZE(l) == l->allocated) {
        l->allocated = l->allocated ? l->allocated * 2 : 8;
        l->ob_item = realloc(l->ob_item, l->allocated * sizeof(PyObject *));
    }
    l->ob_item[Py_SIZE(l)] = Py_NewRef(v);
    Py_SET_SIZE(l, Py_SIZE(l) + 1);
    return 0;
}

/* ---------------------------------------------------------------- str in CPython's compact layout */

static PyObject *str_new(Py_ssize_t size, Py_UCS4 maxchar)
{
    int ascii = maxchar < 128, kind;
    size_t struct_size, char_size;
    if (maxchar < 256) { kind = PyUnicode_1BYTE_KIND; char_size = 1; }
    else if (maxchar < 65536) { kind = PyUnicode_2BYTE_KIND; char_size = 2; }
    else { kind = PyUnicode_4BYTE_KIND; char_size = 4; }
    struct_size = ascii ? sizeof(PyASCIIObject) : sizeof(PyCompactUnicodeObject);
    PyObject *op = obj_new(&PyUnicode_Type, struct_size + (size + 1) * char_size);
    PyASCIIObject *a = (PyASCIIObject *)op;
    a->length = size;
    a->hash = -1;
    a->state.interned = 0;
    a->state.kind = kind;
    a->state.compact = 1;
    a->state.ascii = ascii;
    a->state.statically_allocated = 0;
    if (!ascii) {
        ((PyCompactUnicodeObject *)op)->utf8 = NULL;
        ((PyCompactUnicodeObject *)op)->utf8_length = 0;
    }
    return op;
}

static void str_dealloc(PyObject *op)
{
    PyASCIIObject *a = (PyASCIIObject *)op;
    if (!a->state.ascii) { free(((PyCompactUnicodeObject *)op)->utf8); }
    free(op);
}

/* a str from code points */
static PyObject *str_from_ucs4(const Py_UCS4 *u, Py_ssize_t n)
{
    Py_UCS4 m = 0;
    for (Py_ssize_t i = 0; i < n; i++) { if (u[i] > m) { m = u[i]; } }
    PyObject *s = str_new(n, m);
    int kind = PyUnicode_KIND(s);
    void *data = PyUnicode_DATA(s);
    for (Py_ssize_t i = 0; i < n; i++) { PyUnicode_WRITE(kind, data, i, u[i]); }
    PyUnicode_WRITE(kind, data, n, 0);
    return s;
}
static PyObject *str_ascii(const char *s) { Py_ssize_t n = (Py_ssize_t)strlen(s); PyObject *r = str_new(n, 0); memcpy(PyUnicode_DATA(r), s, n + 1); return r; }

/* ---------------------------------------------------------------- the exceptions as instances */

/* one layout for every exception: CPython's PySyntaxErrorObject first (pegen.c sets its metadata in place; the
   harness reads only that field of it), then args and the attributes of SyntaxError and UnicodeDecodeError (NULL: None) */
typedef struct {
    PySyntaxErrorObject base;
    PyObject *args;
    PyObject *msg, *filename, *lineno, *offset, *text, *end_lineno, *end_offset;
    PyObject *encoding, *object, *reason;
    Py_ssize_t start, end;
} ExcObject;

static void exc_dealloc(PyObject *op)
{
    ExcObject *e = (ExcObject *)op;
    Py_XDECREF(e->args); Py_XDECREF(e->msg); Py_XDECREF(e->filename); Py_XDECREF(e->lineno); Py_XDECREF(e->offset);
    Py_XDECREF(e->text); Py_XDECREF(e->end_lineno); Py_XDECREF(e->end_offset); Py_XDECREF(e->encoding);
    Py_XDECREF(e->object); Py_XDECREF(e->reason); Py_XDECREF(e->base.metadata);
    free(op);
}

static int subclass(PyTypeObject *a, PyTypeObject *b) { for (; a; a = a->tp_base) { if (a == b) { return 1; } } return 0; }
int PyType_IsSubtype(PyTypeObject *a, PyTypeObject *b) { return subclass(a, b); }
static int is_exc_type(PyObject *t) { return Py_TYPE(t) == &PyType_Type && subclass((PyTypeObject *)t, &BaseException_T); }
static int is_exc(PyObject *o) { return o && subclass(Py_TYPE(o), &BaseException_T); }

static PyObject *none_or(PyObject *o) { return (o && o != Py_None) ? Py_NewRef(o) : NULL; }

/* TYPE(VALUE) as PyErr_SetObject's normalization makes it: VALUE a tuple is the arguments, None none, another the one;
   SyntaxError's __init__ reads (msg, (filename, lineno, offset, text[, end_lineno, end_offset])) */
static PyObject *exc_make(PyObject *type, PyObject *value)
{
    ExcObject *e = (ExcObject *)obj_new((PyTypeObject *)type, sizeof(ExcObject));
    if (!value || value == Py_None) { e->args = tuple_new(0); }
    else if (Py_TYPE(value) == &PyTuple_Type) { e->args = Py_NewRef(value); }
    else { e->args = PyTuple_Pack(1, value); }
    Py_ssize_t n = PyTuple_GET_SIZE(e->args);
    if (subclass((PyTypeObject *)type, &SyntaxError_T)) {
        if (n >= 1) { e->msg = Py_NewRef(PyTuple_GET_ITEM(e->args, 0)); }
        if (n == 2) {
            PyObject *info = PyTuple_GET_ITEM(e->args, 1);
            if (Py_TYPE(info) == &PyTuple_Type && PyTuple_GET_SIZE(info) >= 4) {
                e->filename = none_or(PyTuple_GET_ITEM(info, 0));
                e->lineno = none_or(PyTuple_GET_ITEM(info, 1));
                e->offset = none_or(PyTuple_GET_ITEM(info, 2));
                e->text = none_or(PyTuple_GET_ITEM(info, 3));
                if (PyTuple_GET_SIZE(info) >= 5) { e->end_lineno = none_or(PyTuple_GET_ITEM(info, 4)); }
                if (PyTuple_GET_SIZE(info) >= 6) { e->end_offset = none_or(PyTuple_GET_ITEM(info, 5)); }
            }
        }
    }
    return (PyObject *)e;
}

static PyThreadState the_tstate;
PyThreadState *PyThreadState_Get(void) { return &the_tstate; }
PyThreadState *_PyThreadState_GetCurrent(void) { return &the_tstate; }
#define CUR (the_tstate.current_exception)

PyObject *PyErr_GetRaisedException(void) { PyObject *e = CUR; CUR = NULL; return e; }
void PyErr_SetRaisedException(PyObject *exc) { PyObject *old = CUR; CUR = exc; Py_XDECREF(old); }
void PyErr_SetObject(PyObject *t, PyObject *v)
{
    PyObject *e = (v && is_exc(v) && subclass(Py_TYPE(v), (PyTypeObject *)t)) ? Py_NewRef(v) : exc_make(t, v);
    PyErr_SetRaisedException(e);
}
void PyErr_Restore(PyObject *t, PyObject *v, PyObject *tb)
{
    Py_XDECREF(tb);
    if (!t) { Py_XDECREF(v); PyErr_SetRaisedException(NULL); return; }
    PyErr_SetObject(t, v);
    Py_DECREF(t); Py_XDECREF(v);
}
void PyErr_SetNone(PyObject *t) { PyErr_SetObject(t, NULL); }
void PyErr_SetString(PyObject *t, const char *msg) { PyObject *v = PyUnicode_FromString(msg); PyErr_SetObject(t, v); Py_DECREF(v); }
PyObject *PyErr_Format(PyObject *t, const char *f, ...)
{
    va_list ap;
    va_start(ap, f);
    PyObject *v = PyUnicode_FromFormatV(f, ap);
    va_end(ap);
    PyErr_SetObject(t, v);
    Py_DECREF(v);
    return NULL;
}
PyObject *PyErr_NoMemory(void) { PyErr_SetNone(PyExc_MemoryError); return NULL; }
PyObject *PyErr_Occurred(void) { return CUR ? (PyObject *)Py_TYPE(CUR) : NULL; }
void PyErr_Clear(void) { PyErr_SetRaisedException(NULL); }
void PyErr_Fetch(PyObject **t, PyObject **v, PyObject **tb)
{
    PyObject *e = PyErr_GetRaisedException();
    *t = e ? Py_NewRef((PyObject *)Py_TYPE(e)) : NULL;
    *v = e;
    *tb = NULL;
}
int PyErr_GivenExceptionMatches(PyObject *given, PyObject *exc)
{
    if (!given || !exc) { return 0; }
    if (Py_TYPE(exc) == &PyTuple_Type) {
        for (Py_ssize_t i = 0; i < PyTuple_GET_SIZE(exc); i++) { if (PyErr_GivenExceptionMatches(given, PyTuple_GET_ITEM(exc, i))) { return 1; } }
        return 0;
    }
    PyTypeObject *g = is_exc_type(given) ? (PyTypeObject *)given : Py_TYPE(given);
    return subclass(g, (PyTypeObject *)exc);
}
int PyErr_ExceptionMatches(PyObject *exc) { return PyErr_GivenExceptionMatches(PyErr_Occurred(), exc); }
void _PyErr_BadInternalCall(const char *filename, int lineno)
{
    (void)filename; (void)lineno;
    PyErr_SetString(PyExc_SystemError, "bad argument to internal function");
}

/* warnings: pydump.py runs with warnings.simplefilter("ignore") */
int PyErr_WarnExplicitObject(PyObject *category, PyObject *message, PyObject *filename, int lineno, PyObject *module, PyObject *registry)
{
    (void)category; (void)message; (void)filename; (void)lineno; (void)module; (void)registry;
    return 0;
}
int PyErr_WarnFormat(PyObject *category, Py_ssize_t stack_level, const char *format, ...)
{
    (void)category; (void)stack_level; (void)format;
    return 0;
}
int _PyErr_EmitSyntaxWarning(PyObject *msg, PyObject *filename, int lineno, int col_offset, int end_lineno, int end_col_offset)
{
    (void)msg; (void)filename; (void)lineno; (void)col_offset; (void)end_lineno; (void)end_col_offset;
    return 0;
}

/* Python/errors.c's PyErr_SyntaxLocationObjectEx: lineno, offset, end_lineno, end_offset (None when negative) set on
   the raised exception; its text from PyErr_ProgramTextObject(filename, lineno), which reads the file of that name in
   CPython's working directory: none here (the name is the corpus's relative path, or "<string>"), the text kept; msg
   set from str() when unset, for a subclass */
static void syntax_location(PyObject *filename, int lineno, int col_offset, int end_lineno, int end_col_offset)
{
    (void)filename;
    PyObject *exc = CUR;
    if (!exc) { return; }
    ExcObject *e = (ExcObject *)exc;
    Py_XSETREF(e->lineno, int_new(lineno));
    Py_XSETREF(e->offset, col_offset >= 0 ? int_new(col_offset) : NULL);
    Py_XSETREF(e->end_lineno, end_lineno >= 0 ? int_new(end_lineno) : NULL);
    Py_XSETREF(e->end_offset, end_col_offset >= 0 ? int_new(end_col_offset) : NULL);
    if (filename) { Py_XSETREF(e->filename, Py_NewRef(filename)); }
}
void PyErr_SyntaxLocationObject(PyObject *filename, int lineno, int col_offset) { syntax_location(filename, lineno, col_offset, lineno, -1); }
void PyErr_RangedSyntaxLocationObject(PyObject *filename, int lineno, int col_offset, int end_lineno, int end_col_offset)
{
    syntax_location(filename, lineno, col_offset, end_lineno, end_col_offset);
}

/* Python/errors.c's _PyErr_ProgramDecodedTextObject: the line LINENO of the file FILENAME in CPython's working
   directory; pydump.py's files are named by their path relative to the corpus, which is not CPython's directory: none */
PyObject *_PyErr_ProgramDecodedTextObject(PyObject *filename, int lineno, const char *encoding)
{
    (void)filename; (void)lineno; (void)encoding;
    return NULL;
}

/* setattr on an exception: the attributes its sources set (the names are the runtime's static identifiers) */
int PyObject_SetAttr(PyObject *o, PyObject *name, PyObject *v)
{
    if (!is_exc(o)) { return 0; }
    ExcObject *e = (ExcObject *)o;
    PyObject **slot = NULL;
    if (name == &_Py_ID(lineno)) { slot = &e->lineno; }
    else if (name == &_Py_ID(offset)) { slot = &e->offset; }
    else if (name == &_Py_ID(end_lineno)) { slot = &e->end_lineno; }
    else if (name == &_Py_ID(end_offset)) { slot = &e->end_offset; }
    else if (name == &_Py_ID(text)) { slot = &e->text; }
    else if (name == &_Py_ID(filename)) { slot = &e->filename; }
    else if (name == &_Py_ID(msg)) { slot = &e->msg; }
    if (slot) { Py_XSETREF(*slot, none_or(v)); }
    return 0;
}
int PyObject_SetAttrString(PyObject *o, const char *name, PyObject *v) { (void)o; (void)name; (void)v; return 0; }

/* ---------------------------------------------------------------- UTF-8 */

/* the length of a valid UTF-8 sequence at s (out its code point), else minus the length of the maximal subpart CPython's
   decoder replaces as one U+FFFD, with its reason; *eod set when the subpart runs to the data's end */
static Py_ssize_t utf8_seq(const unsigned char *s, Py_ssize_t n, Py_UCS4 *out, const char **reason)
{
    unsigned c = s[0];
    if (c < 0x80) { *out = c; return 1; }
    int need; Py_UCS4 cp; unsigned lo = 0x80, hi = 0xBF;
    if (c >= 0xC2 && c <= 0xDF) { need = 1; cp = c & 0x1F; }
    else if (c >= 0xE0 && c <= 0xEF) { need = 2; cp = c & 0x0F; if (c == 0xE0) { lo = 0xA0; } else if (c == 0xED) { hi = 0x9F; } }
    else if (c >= 0xF0 && c <= 0xF4) { need = 3; cp = c & 0x07; if (c == 0xF0) { lo = 0x90; } else if (c == 0xF4) { hi = 0x8F; } }
    else { if (reason) { *reason = "invalid start byte"; } return -1; }
    for (int i = 1; i <= need; i++) {
        if (i >= n) { if (reason) { *reason = "unexpected end of data"; } return -i; }
        unsigned d = s[i];
        if (d < lo || d > hi) { if (reason) { *reason = "invalid continuation byte"; } return -i; }
        lo = 0x80; hi = 0xBF;
        cp = (cp << 6) | (d & 0x3F);
    }
    *out = cp;
    return need + 1;
}

static PyObject *unicode_decode_error(const char *encoding, const char *s, Py_ssize_t n, Py_ssize_t start, Py_ssize_t end, const char *reason)
{
    ExcObject *e = (ExcObject *)obj_new(&UnicodeDecodeError_T, sizeof(ExcObject));
    e->encoding = str_ascii(encoding);
    e->object = bytes_new(s, n);
    e->start = start;
    e->end = end;
    e->reason = str_ascii(reason);
    e->args = PyTuple_Pack(5, e->encoding, e->object, Py_None, Py_None, e->reason);
    return (PyObject *)e;
}

/* UTF-8's decoder: errors "strict" (NULL) or "replace", one U+FFFD a maximal subpart as CPython's decoder gives it;
   strict: a UnicodeDecodeError raised, NULL; CONSUMED: the stateful decoder's (an incomplete sequence at the end left) */
static PyObject *decode_utf8(const char *s, Py_ssize_t n, const char *errors, Py_ssize_t *consumed)
{
    Py_UCS4 *u = malloc((n + 1) * sizeof(Py_UCS4));
    Py_ssize_t k = 0, i = 0;
    while (i < n) {
        Py_UCS4 cp;
        const char *reason;
        Py_ssize_t l = utf8_seq((const unsigned char *)s + i, n - i, &cp, &reason);
        if (l > 0) { u[k++] = cp; i += l; continue; }
        if (consumed && i - l >= n && !strcmp(reason, "unexpected end of data")) { break; }
        if (errors == NULL || strcmp(errors, "replace") != 0) {
            free(u);
            Py_ssize_t end = !strcmp(reason, "unexpected end of data") ? n : (!strcmp(reason, "invalid start byte") ? i + 1 : i - l);
            PyObject *exc = unicode_decode_error("utf-8", s, n, i, end, reason);
            PyErr_SetRaisedException(exc);
            return NULL;
        }
        u[k++] = 0xFFFD; i += -l;
    }
    if (consumed) { *consumed = i; }
    PyObject *r = str_from_ucs4(u, k);
    free(u);
    return r;
}

PyObject *PyUnicode_DecodeUTF8(const char *s, Py_ssize_t n, const char *errors) { return decode_utf8(s, n, errors, NULL); }
PyObject *PyUnicode_DecodeUTF8Stateful(const char *s, Py_ssize_t n, const char *errors, Py_ssize_t *consumed) { return decode_utf8(s, n, errors, consumed); }
PyObject *PyUnicode_FromStringAndSize(const char *s, Py_ssize_t n) { return decode_utf8(s, n, NULL, NULL); }
PyObject *PyUnicode_FromString(const char *s) { return decode_utf8(s, (Py_ssize_t)strlen(s), NULL, NULL); }
PyObject *PyUnicode_InternFromString(const char *s) { return PyUnicode_FromString(s); }
void _PyUnicode_InternImmortal(PyInterpreterState *interp, PyObject **p) { (void)interp; (void)p; }
PyObject *PyUnicode_FromKindAndData(int kind, const void *buffer, Py_ssize_t size)
{
    Py_UCS4 *u = malloc((size + 1) * sizeof(Py_UCS4));
    for (Py_ssize_t i = 0; i < size; i++) { u[i] = PyUnicode_READ(kind, buffer, i); }
    PyObject *r = str_from_ucs4(u, size);
    free(u);
    return r;
}
PyObject *PyUnicode_FromObject(PyObject *o)
{
    if (Py_TYPE(o) == &PyUnicode_Type) { return Py_NewRef(o); }
    PyErr_SetString(PyExc_TypeError, "Can't convert object to str implicitly");
    return NULL;
}

/* the UTF-8 of a str (cached in a compact non-ASCII object, as CPython does) */
static const char *str_utf8(PyObject *op, Py_ssize_t *size)
{
    PyASCIIObject *a = (PyASCIIObject *)op;
    if (a->state.ascii) {
        if (size) { *size = a->length; }
        return (const char *)PyUnicode_DATA(op);
    }
    PyCompactUnicodeObject *c = (PyCompactUnicodeObject *)op;
    if (!c->utf8) {
        int kind = PyUnicode_KIND(op);
        const void *data = PyUnicode_DATA(op);
        Py_ssize_t n = a->length, k = 0;
        char *b = malloc(n * 4 + 1);
        for (Py_ssize_t i = 0; i < n; i++) {
            Py_UCS4 ch = PyUnicode_READ(kind, data, i);
            if (ch < 0x80) { b[k++] = (char)ch; }
            else if (ch < 0x800) { b[k++] = (char)(0xC0 | (ch >> 6)); b[k++] = (char)(0x80 | (ch & 0x3F)); }
            else if (ch < 0x10000) {
                b[k++] = (char)(0xE0 | (ch >> 12)); b[k++] = (char)(0x80 | ((ch >> 6) & 0x3F)); b[k++] = (char)(0x80 | (ch & 0x3F));
            } else {
                b[k++] = (char)(0xF0 | (ch >> 18)); b[k++] = (char)(0x80 | ((ch >> 12) & 0x3F));
                b[k++] = (char)(0x80 | ((ch >> 6) & 0x3F)); b[k++] = (char)(0x80 | (ch & 0x3F));
            }
        }
        b[k] = 0;
        c->utf8 = b;
        c->utf8_length = k;
    }
    if (size) { *size = c->utf8_length; }
    return c->utf8;
}

const char *PyUnicode_AsUTF8AndSize(PyObject *op, Py_ssize_t *size) { return str_utf8(op, size); }
const char *PyUnicode_AsUTF8(PyObject *op) { return str_utf8(op, NULL); }
PyObject *PyUnicode_AsUTF8String(PyObject *op) { Py_ssize_t n; const char *s = str_utf8(op, &n); return bytes_new(s, n); }

PyObject *PyUnicode_Substring(PyObject *op, Py_ssize_t start, Py_ssize_t end)
{
    Py_ssize_t len = PyUnicode_GET_LENGTH(op);
    if (end > len) { end = len; }
    if (start > end) { start = end; }
    int kind = PyUnicode_KIND(op);
    const void *data = PyUnicode_DATA(op);
    Py_UCS4 *u = malloc((end - start + 1) * sizeof(Py_UCS4));
    for (Py_ssize_t i = start; i < end; i++) { u[i - start] = PyUnicode_READ(kind, data, i); }
    PyObject *r = str_from_ucs4(u, end - start);
    free(u);
    return r;
}

int PyUnicode_CompareWithASCIIString(PyObject *a, const char *s)
{
    Py_ssize_t na = PyUnicode_GET_LENGTH(a), i;
    for (i = 0; i < na && s[i]; i++) {
        Py_UCS4 x = PyUnicode_READ_CHAR(a, i), y = (unsigned char)s[i];
        if (x != y) { return x < y ? -1 : 1; }
    }
    if (i < na) { return 1; }
    return s[i] ? -1 : 0;
}
int _PyUnicode_EqualToASCIIString(PyObject *a, const char *s) { return PyUnicode_CompareWithASCIIString(a, s) == 0; }

/* Objects/unicodeobject.c's _PyUnicode_ScanIdentifier, copied (CPython 3.14.10; PSF license) */
Py_ssize_t
_PyUnicode_ScanIdentifier(PyObject *self)
{
    Py_ssize_t i;
    Py_ssize_t len = PyUnicode_GET_LENGTH(self);
    if (len == 0) {
        /* an empty string is not a valid identifier */
        return 0;
    }

    int kind = PyUnicode_KIND(self);
    const void *data = PyUnicode_DATA(self);
    Py_UCS4 ch = PyUnicode_READ(kind, data, 0);
    if (!_PyUnicode_IsXidStart(ch) && ch != 0x5F /* LOW LINE */) {
        return 0;
    }

    for (i = 1; i < len; i++) {
        ch = PyUnicode_READ(kind, data, i);
        if (!_PyUnicode_IsXidContinue(ch)) {
            return i;
        }
    }
    return i;
}

/* ---------------------------------------------------------------- PyUnicode_FromFormat (the sources' formats) */

typedef struct { Py_UCS4 *u; Py_ssize_t n, cap; } Buf;
static void buf_put(Buf *b, Py_UCS4 c)
{
    if (b->n == b->cap) { b->cap = b->cap ? b->cap * 2 : 64; b->u = realloc(b->u, b->cap * sizeof(Py_UCS4)); }
    b->u[b->n++] = c;
}
static void buf_str(Buf *b, PyObject *t)
{
    int kind = PyUnicode_KIND(t);
    const void *data = PyUnicode_DATA(t);
    for (Py_ssize_t i = 0; i < PyUnicode_GET_LENGTH(t); i++) { buf_put(b, PyUnicode_READ(kind, data, i)); }
}
static void buf_utf8(Buf *b, const char *s, Py_ssize_t n)
{
    PyObject *t = decode_utf8(s, n, "replace", NULL);
    buf_str(b, t);
    Py_DECREF(t);
}
static PyObject *exc_str(PyObject *e);
static void buf_repr(Buf *b, PyObject *o);

PyObject *PyUnicode_FromFormatV(const char *f, va_list ap)
{
    Buf b = {0};
    for (; *f; f++) {
        if (*f != '%') { buf_put(&b, (unsigned char)*f); continue; }
        f++;
        if (*f == '%') { buf_put(&b, '%'); continue; }
        int zero = 0, width = -1, prec = -1, lng = 0, size_t_ = 0;
        if (*f == '0') { zero = 1; f++; }
        if (*f >= '0' && *f <= '9') { width = 0; while (*f >= '0' && *f <= '9') { width = width * 10 + (*f++ - '0'); } }
        if (*f == '.') {
            f++;
            if (*f == '*') { prec = va_arg(ap, int); f++; }
            else { prec = 0; while (*f >= '0' && *f <= '9') { prec = prec * 10 + (*f++ - '0'); } }
        }
        if (*f == 'l') { lng = 1; f++; if (*f == 'l') { lng = 2; f++; } }
        else if (*f == 'z') { size_t_ = 1; f++; }
        char tmp[64];
        switch (*f) {
        case 'c': buf_put(&b, (Py_UCS4)va_arg(ap, int)); break;
        case 'd': case 'i': case 'u': case 'x': case 'X': case 'o': {
            long long v; unsigned long long uv;
            int sgn = *f == 'd' || *f == 'i';
            if (size_t_) { v = sgn ? (long long)va_arg(ap, Py_ssize_t) : (long long)va_arg(ap, size_t); }
            else if (lng == 2) { v = sgn ? va_arg(ap, long long) : (long long)va_arg(ap, unsigned long long); }
            else if (lng == 1) { v = sgn ? va_arg(ap, long) : (long long)va_arg(ap, unsigned long); }
            else { v = sgn ? va_arg(ap, int) : (long long)va_arg(ap, unsigned int); }
            uv = (unsigned long long)v;
            int p = (prec >= 0 ? prec : (zero ? width : -1));
            if (*f == 'x') { snprintf(tmp, sizeof tmp, "%0*llx", p > 0 ? p : 1, uv); }
            else if (*f == 'X') { snprintf(tmp, sizeof tmp, "%0*llX", p > 0 ? p : 1, uv); }
            else if (*f == 'o') { snprintf(tmp, sizeof tmp, "%0*llo", p > 0 ? p : 1, uv); }
            else if (*f == 'u') { snprintf(tmp, sizeof tmp, "%0*llu", p > 0 ? p : 1, uv); }
            else { snprintf(tmp, sizeof tmp, "%0*lld", p > 0 ? p : 1, v); }
            for (int k = (int)strlen(tmp); k < width; k++) { buf_put(&b, ' '); }
            for (char *q = tmp; *q; q++) { buf_put(&b, (unsigned char)*q); }
            break;
        }
        case 's': {
            const char *s = va_arg(ap, const char *);
            Py_ssize_t n = (Py_ssize_t)strlen(s);
            if (prec >= 0 && prec < n) { n = prec; }
            buf_utf8(&b, s, n);
            break;
        }
        case 'U': buf_str(&b, va_arg(ap, PyObject *)); break;
        case 'R': buf_repr(&b, va_arg(ap, PyObject *)); break;
        case 'S': {
            PyObject *o = va_arg(ap, PyObject *);
            if (Py_TYPE(o) == &PyUnicode_Type) { buf_str(&b, o); }
            else { PyObject *s = exc_str(o); buf_str(&b, s); Py_DECREF(s); }
            break;
        }
        case 'V': {
            PyObject *o = va_arg(ap, PyObject *);
            const char *s = va_arg(ap, const char *);
            if (o) { buf_str(&b, o); } else { buf_utf8(&b, s, (Py_ssize_t)strlen(s)); }
            break;
        }
        default: fprintf(stderr, "parseharness: format %%%c in \"%s\"\n", *f, f); fatal("PyUnicode_FromFormat: a format the harness does not give");
        }
    }
    PyObject *r = str_from_ucs4(b.u, b.n);
    free(b.u);
    return r;
}

PyObject *PyUnicode_FromFormat(const char *f, ...)
{
    va_list ap;
    va_start(ap, f);
    PyObject *r = PyUnicode_FromFormatV(f, ap);
    va_end(ap);
    return r;
}

/* str() of an exception: Objects/exceptions.c's BaseException_str (one argument its str, none "") and
   UnicodeDecodeError_str; the parser formats them into its "(unicode error) ..." and "(value error) ..." */
static PyObject *exc_str(PyObject *o)
{
    if (Py_TYPE(o) == &PyUnicode_Type) { return Py_NewRef(o); }
    if (!is_exc(o)) { fatal("str() of an object the harness does not give"); }
    ExcObject *e = (ExcObject *)o;
    if (Py_TYPE(o) == &UnicodeDecodeError_T && e->object) {
        Py_ssize_t len = PyBytes_GET_SIZE(e->object);
        if (e->start >= 0 && e->start < len && e->end >= 0 && e->end <= len && e->end == e->start + 1) {
            int bad = (int)(PyBytes_AS_STRING(e->object)[e->start] & 0xff);
            return PyUnicode_FromFormat("'%U' codec can't decode byte 0x%02x in position %zd: %U", e->encoding, bad, e->start, e->reason);
        }
        return PyUnicode_FromFormat("'%U' codec can't decode bytes in position %zd-%zd: %U", e->encoding, e->start, e->end - 1, e->reason);
    }
    Py_ssize_t n = PyTuple_GET_SIZE(e->args);
    if (n == 0) { return str_ascii(""); }
    if (n == 1) { return exc_str(PyTuple_GET_ITEM(e->args, 0)); }
    fatal("str() of an exception of several arguments");
    return NULL;
}
PyObject *PyObject_Str(PyObject *o) { return exc_str(o); }

/* ---------------------------------------------------------------- PyUnicodeWriter (action_helpers.c's concatenations) */

PyUnicodeWriter *PyUnicodeWriter_Create(Py_ssize_t length) { (void)length; return (PyUnicodeWriter *)calloc(1, sizeof(Buf)); }
int PyUnicodeWriter_WriteStr(PyUnicodeWriter *w, PyObject *o)
{
    if (Py_TYPE(o) != &PyUnicode_Type) { fatal("PyUnicodeWriter_WriteStr: not a str"); }
    buf_str((Buf *)w, o);
    return 0;
}
PyObject *PyUnicodeWriter_Finish(PyUnicodeWriter *w) { Buf *b = (Buf *)w; PyObject *r = str_from_ucs4(b->u, b->n); free(b->u); free(b); return r; }
void PyUnicodeWriter_Discard(PyUnicodeWriter *w) { if (w) { free(((Buf *)w)->u); free(w); } }

/* ---------------------------------------------------------------- the codecs (queue 489's harness's) */

static const char *encodings_dir;   /* the tarball's Lib/encodings */

/* encodings.normalize_encoding, lower-cased (codecs.lookup's own normalization) */
static void normalize(const char *in, char *out, size_t cap)
{
    size_t k = 0; int punct = 0;
    for (const char *p = in; *p && k + 2 < cap; p++) {
        unsigned char c = (unsigned char)*p;
        if ((c >= 'A' && c <= 'Z') || (c >= 'a' && c <= 'z') || (c >= '0' && c <= '9') || c == '.' || c >= 128) {
            if (punct && k) { out[k++] = '_'; }
            out[k++] = (char)((c >= 'A' && c <= 'Z') ? c + 32 : c);
            punct = 0;
        } else {
            punct = 1;
        }
    }
    out[k] = 0;
}

static char *slurp(const char *path, Py_ssize_t *len)
{
    FILE *fp = fopen(path, "rb");
    if (!fp) { return NULL; }
    size_t cap = 65536, n = 0, r;
    char *b = malloc(cap + 1);
    while ((r = fread(b + n, 1, cap - n, fp)) > 0) {
        n += r;
        if (n == cap) { cap *= 2; b = realloc(b, cap + 1); }
    }
    fclose(fp);
    b[n] = 0;
    if (len) { *len = (Py_ssize_t)n; }
    return b;
}

/* aliases.py's entry for NAME ('NAME' : 'MODULE',), its module into MOD */
static int alias_of(const char *name, char *mod, size_t cap)
{
    char path[4096];
    snprintf(path, sizeof path, "%s/aliases.py", encodings_dir);
    char *text = slurp(path, NULL);
    if (!text) { return 0; }
    size_t nl = strlen(name);
    int found = 0;
    for (char *p = text; (p = strchr(p, '\'')) != NULL; ) {
        char *q = strchr(p + 1, '\'');
        if (!q) { break; }
        char *r = q + 1;
        while (*r == ' ' || *r == '\t') { r++; }
        if (*r == ':' && (size_t)(q - p - 1) == nl && memcmp(p + 1, name, nl) == 0) {
            r++;
            while (*r == ' ' || *r == '\t') { r++; }
            if (*r == '\'') {
                char *e = strchr(r + 1, '\'');
                if (e && (size_t)(e - r - 1) < cap) { memcpy(mod, r + 1, e - r - 1); mod[e - r - 1] = 0; found = 1; }
            }
            break;
        }
        p = (*r == ':') ? r : q + 1;
    }
    free(text);
    return found;
}

/* codecs.lookup: the module encodings.search_function imports for NAME into MOD; 0: LookupError */
static int codec_module(const char *name, char *mod, size_t cap)
{
    char norm[256], cand[2][256];
    int nc = 0;
    normalize(name, norm, sizeof norm);
    char alt[256];
    snprintf(alt, sizeof alt, "%s", norm);
    for (char *p = alt; *p; p++) { if (*p == '.') { *p = '_'; } }
    if (alias_of(norm, cand[nc], sizeof cand[0]) || alias_of(alt, cand[nc], sizeof cand[0])) { nc++; }
    snprintf(cand[nc++], sizeof cand[0], "%s", norm);
    for (int i = 0; i < nc; i++) {
        if (!cand[i][0] || strchr(cand[i], '.')) { continue; }
        /* the modules a Linux CPython cannot import (mbcs and oem need Windows' codecs) */
        if (!strcmp(cand[i], "mbcs") || !strcmp(cand[i], "oem")) { continue; }
        char path[4096];
        struct stat st;
        snprintf(path, sizeof path, "%s/%s.py", encodings_dir, cand[i]);
        if (stat(path, &st) == 0) { snprintf(mod, cap, "%s", cand[i]); return 1; }
    }
    return 0;
}

static Py_UCS4 hexval(const char *p, int n)
{
    Py_UCS4 v = 0;
    for (int i = 0; i < n; i++) {
        char c = p[i];
        v = v * 16 + (Py_UCS4)(c >= 'a' ? c - 'a' + 10 : c >= 'A' ? c - 'A' + 10 : c - '0');
    }
    return v;
}

/* a charmap codec's decoding table from its module's source (gencodec.py's decoding_table: one string literal a
   line); 1 with the 256 entries, 0 when the module is no charmap codec */
static int charmap_table(const char *mod, Py_UCS4 table[256])
{
    char path[4096];
    snprintf(path, sizeof path, "%s/%s.py", encodings_dir, mod);
    char *text = slurp(path, NULL);
    if (!text) { return 0; }
    char *p = strstr(text, "\ndecoding_table = (");
    int n = 0;
    if (p) {
        p = strchr(p + 1, '\n');
        while (p && n < 256) {
            p++;
            char *q = p;
            while (*q == ' ') { q++; }
            if (*q == ')') { break; }
            if (*q != '\'' && *q != '"') { p = strchr(p, '\n'); continue; }   /* '...', or "'" for the apostrophe */
            q++;
            Py_UCS4 c;
            if (*q == '\\') {
                q++;
                switch (*q) {
                case 'x': c = hexval(q + 1, 2); break;
                case 'u': c = hexval(q + 1, 4); break;
                case 't': c = '\t'; break;
                case 'n': c = '\n'; break;
                case 'r': c = '\r'; break;
                case '\\': c = '\\'; break;
                case '\'': c = '\''; break;
                default: free(text); return 0;
                }
            } else {
                Py_ssize_t l = utf8_seq((const unsigned char *)q, 4, &c, NULL);
                if (l <= 0) { free(text); return 0; }
            }
            table[n++] = c;
            p = strchr(p, '\n');
        }
    }
    free(text);
    return n == 256;
}

/* the decoder of a codec module: errors NULL (strict) or "replace"; *refused set when the harness has none */
static PyObject *decode_with(const char *mod, const char *s, Py_ssize_t n, const char *errors, int *refused)
{
    int replace = errors && !strcmp(errors, "replace");
    *refused = 0;
    if (!strcmp(mod, "utf_8")) { return decode_utf8(s, n, errors, NULL); }
    if (!strcmp(mod, "utf_8_sig")) {
        if (n >= 3 && !memcmp(s, "\xef\xbb\xbf", 3)) { s += 3; n -= 3; }
        return decode_utf8(s, n, errors, NULL);
    }
    Py_UCS4 table[256];
    if (!strcmp(mod, "latin_1")) { for (int i = 0; i < 256; i++) { table[i] = i; } }
    else if (!strcmp(mod, "ascii")) { for (int i = 0; i < 256; i++) { table[i] = i < 128 ? (Py_UCS4)i : 0xFFFE; } }
    else if (!charmap_table(mod, table)) { *refused = 1; return NULL; }
    Py_UCS4 *u = malloc((n + 1) * sizeof(Py_UCS4));
    for (Py_ssize_t i = 0; i < n; i++) {
        Py_UCS4 c = table[(unsigned char)s[i]];
        if (c == 0xFFFE) {
            if (!replace) {
                free(u);
                PyErr_SetRaisedException(unicode_decode_error(!strcmp(mod, "ascii") ? "ascii" : "charmap", s, n, i, i + 1,
                                                              !strcmp(mod, "ascii") ? "ordinal not in range(128)" : "character maps to <undefined>"));
                return NULL;
            }
            c = 0xFFFD;
        }
        u[i] = c;
    }
    PyObject *r = str_from_ucs4(u, n);
    free(u);
    return r;
}

static int harness_refused;     /* a codec the harness has no decoder for: the file's dump is the harness's refusal */
static char harness_refused_codec[256];

PyObject *PyUnicode_Decode(const char *s, Py_ssize_t n, const char *encoding, const char *errors)
{
    char mod[256];
    if (!codec_module(encoding, mod, sizeof mod)) {
        PyErr_Format(PyExc_LookupError, "unknown encoding: %s", encoding);
        return NULL;
    }
    int refused;
    PyObject *r = decode_with(mod, s, n, errors, &refused);
    if (refused) {
        harness_refused = 1;
        snprintf(harness_refused_codec, sizeof harness_refused_codec, "%s", mod);
        PyErr_Format(PyExc_SystemError, "the harness has no decoder for %s", mod);
    }
    return r;
}

/* ---------------------------------------------------------------- the escape decoders */

static int ud_getcode(const char *name, int namelen, Py_UCS4 *code, int with_named_seq);

/* Objects/unicodeobject.c's _PyUnicode_DecodeUnicodeEscapeInternal2 (CPython 3.14.10; PSF license), copied with the
   writer a buffer of code points and the "strict" error handler's UnicodeDecodeError raised in place (errors NULL,
   consumed NULL: as string_parser.c calls it) */
PyObject *
_PyUnicode_DecodeUnicodeEscapeInternal2(const char *s,
                               Py_ssize_t size,
                               const char *errors,
                               Py_ssize_t *consumed,
                               int *first_invalid_escape_char,
                               const char **first_invalid_escape_ptr)
{
    const char *starts = s;
    const char *initial_starts = starts;
    const char *end;
    Buf w = {0};
    (void)errors;

    *first_invalid_escape_char = -1;
    *first_invalid_escape_ptr = NULL;
    if (consumed) { fatal("_PyUnicode_DecodeUnicodeEscapeInternal2: consumed"); }

    end = s + size;
    while (s < end) {
        unsigned char c = (unsigned char) *s++;
        Py_UCS4 ch;
        int count;
        const char *message;

        if (c != '\\') {
            buf_put(&w, c);
            continue;
        }

        Py_ssize_t startinpos = s - starts - 1;
        if (s >= end) {
            message = "\\ at end of string";
            goto error;
        }
        c = (unsigned char) *s++;

        switch (c) {
        case '\n': continue;
        case '\\': buf_put(&w, '\\'); continue;
        case '\'': buf_put(&w, '\''); continue;
        case '\"': buf_put(&w, '\"'); continue;
        case 'b': buf_put(&w, '\b'); continue;
        case 'f': buf_put(&w, '\014'); continue;
        case 't': buf_put(&w, '\t'); continue;
        case 'n': buf_put(&w, '\n'); continue;
        case 'r': buf_put(&w, '\r'); continue;
        case 'v': buf_put(&w, '\013'); continue;
        case 'a': buf_put(&w, '\007'); continue;

        case '0': case '1': case '2': case '3':
        case '4': case '5': case '6': case '7':
            ch = c - '0';
            if (s < end && '0' <= *s && *s <= '7') {
                ch = (ch<<3) + *s++ - '0';
                if (s < end && '0' <= *s && *s <= '7') {
                    ch = (ch<<3) + *s++ - '0';
                }
            }
            if (ch > 0377) {
                if (*first_invalid_escape_char == -1) {
                    *first_invalid_escape_char = ch;
                    if (starts == initial_starts) {
                        *first_invalid_escape_ptr = s - 3;
                    }
                }
            }
            buf_put(&w, ch);
            continue;

        case 'x':
            count = 2;
            message = "truncated \\xXX escape";
            goto hexescape;
        case 'u':
            count = 4;
            message = "truncated \\uXXXX escape";
            goto hexescape;
        case 'U':
            count = 8;
            message = "truncated \\UXXXXXXXX escape";
        hexescape:
            for (ch = 0; count; ++s, --count) {
                if (s >= end) {
                    goto error;
                }
                c = (unsigned char)*s;
                ch <<= 4;
                if (c >= '0' && c <= '9') {
                    ch += c - '0';
                }
                else if (c >= 'a' && c <= 'f') {
                    ch += c - ('a' - 10);
                }
                else if (c >= 'A' && c <= 'F') {
                    ch += c - ('A' - 10);
                }
                else {
                    goto error;
                }
            }
            if (ch > 0x10ffff) {
                message = "illegal Unicode character";
                goto error;
            }
            buf_put(&w, ch);
            continue;

        case 'N':
            message = "malformed \\N character escape";
            if (s >= end) {
                goto error;
            }
            if (*s == '{') {
                const char *start = ++s;
                size_t namelen;
                while (s < end && *s != '}')
                    s++;
                if (s >= end) {
                    goto error;
                }
                namelen = s - start;
                if (namelen) {
                    s++;
                    ch = 0xffffffff;
                    if (namelen <= INT_MAX &&
                        ud_getcode(start, (int)namelen, &ch, 0)) {
                        buf_put(&w, ch);
                        continue;
                    }
                    message = "unknown Unicode character name";
                }
            }
            goto error;

        default:
            if (*first_invalid_escape_char == -1) {
                *first_invalid_escape_char = c;
                if (starts == initial_starts) {
                    *first_invalid_escape_ptr = s - 1;
                }
            }
            buf_put(&w, '\\');
            buf_put(&w, c);
            continue;
        }

      error:;
        Py_ssize_t endinpos = s-starts;
        free(w.u);
        PyErr_SetRaisedException(unicode_decode_error("unicodeescape", starts, size, startinpos, endinpos, message));
        return NULL;
    }
    PyObject *r = str_from_ucs4(w.u, w.n);
    free(w.u);
    return r;
}

/* Objects/bytesobject.c's _PyBytes_DecodeEscape2 (CPython 3.14.10; PSF license), copied with the writer a buffer of
   the input's length (the output is never longer) and errors NULL (strict) */
PyObject *_PyBytes_DecodeEscape2(const char *s,
                                Py_ssize_t len,
                                const char *errors,
                                int *first_invalid_escape_char,
                                const char **first_invalid_escape_ptr)
{
    int c;
    char *p, *buf;
    const char *end;
    (void)errors;

    buf = p = malloc(len + 1);
    *first_invalid_escape_char = -1;
    *first_invalid_escape_ptr = NULL;

    end = s + len;
    while (s < end) {
        if (*s != '\\') {
            *p++ = *s++;
            continue;
        }

        s++;
        if (s == end) {
            PyErr_SetString(PyExc_ValueError,
                            "Trailing \\ in string");
            goto failed;
        }

        switch (*s++) {
        case '\n': break;
        case '\\': *p++ = '\\'; break;
        case '\'': *p++ = '\''; break;
        case '\"': *p++ = '\"'; break;
        case 'b': *p++ = '\b'; break;
        case 'f': *p++ = '\014'; break;
        case 't': *p++ = '\t'; break;
        case 'n': *p++ = '\n'; break;
        case 'r': *p++ = '\r'; break;
        case 'v': *p++ = '\013'; break;
        case 'a': *p++ = '\007'; break;
        case '0': case '1': case '2': case '3':
        case '4': case '5': case '6': case '7':
            c = s[-1] - '0';
            if (s < end && '0' <= *s && *s <= '7') {
                c = (c<<3) + *s++ - '0';
                if (s < end && '0' <= *s && *s <= '7')
                    c = (c<<3) + *s++ - '0';
            }
            if (c > 0377) {
                if (*first_invalid_escape_char == -1) {
                    *first_invalid_escape_char = c;
                    *first_invalid_escape_ptr = s - 3;
                }
            }
            *p++ = c;
            break;
        case 'x':
            if (s+1 < end) {
                int digit1, digit2;
                digit1 = _PyLong_DigitValue[Py_CHARMASK(s[0])];
                digit2 = _PyLong_DigitValue[Py_CHARMASK(s[1])];
                if (digit1 < 16 && digit2 < 16) {
                    *p++ = (unsigned char)((digit1 << 4) + digit2);
                    s += 2;
                    break;
                }
            }
            PyErr_Format(PyExc_ValueError,
                         "invalid \\x escape at position %zd",
                         s - 2 - (end - len));
            goto failed;

        default:
            if (*first_invalid_escape_char == -1) {
                *first_invalid_escape_char = (unsigned char)s[-1];
                *first_invalid_escape_ptr = s - 1;
            }
            *p++ = '\\';
            s--;
        }
    }
    {
        PyObject *r = bytes_new(buf, p - buf);
        free(buf);
        return r;
    }
  failed:
    free(buf);
    return NULL;
}

/* ---------------------------------------------------------------- copied helpers */

/* Python/ast.c's _PyAST_GetDocString (CPython 3.14.10; PSF license) */
PyObject *
_PyAST_GetDocString(asdl_stmt_seq *body)
{
    if (!asdl_seq_LEN(body)) {
        return NULL;
    }
    stmt_ty st = asdl_seq_GET(body, 0);
    if (st->kind != Expr_kind) {
        return NULL;
    }
    expr_ty e = st->v.Expr.value;
    if (e->kind == Constant_kind && PyUnicode_CheckExact(e->v.Constant.value)) {
        return e->v.Constant.value;
    }
    return NULL;
}

/* Python/codegen.c's _Py_CArray_* (CPython 3.14.10; PSF license) */
int
_Py_CArray_Init(_Py_c_array_t* array, int item_size, int initial_num_entries) {
    memset(array, 0, sizeof(_Py_c_array_t));
    array->item_size = item_size;
    array->initial_num_entries = initial_num_entries;
    return 0;
}

void
_Py_CArray_Fini(_Py_c_array_t* array)
{
    if (array->array) {
        PyMem_Free(array->array);
        array->allocated_entries = 0;
    }
}

int
_Py_CArray_EnsureCapacity(_Py_c_array_t *c_array, int idx)
{
    void *arr = c_array->array;
    int alloc = c_array->allocated_entries;
    if (arr == NULL) {
        int new_alloc = c_array->initial_num_entries;
        if (idx >= new_alloc) {
            new_alloc = idx + c_array->initial_num_entries;
        }
        arr = PyMem_Calloc(new_alloc, c_array->item_size);
        if (arr == NULL) {
            PyErr_NoMemory();
            return -1;
        }
        alloc = new_alloc;
    }
    else if (idx >= alloc) {
        size_t oldsize = alloc * c_array->item_size;
        int new_alloc = alloc << 1;
        if (idx >= new_alloc) {
            new_alloc = idx + c_array->initial_num_entries;
        }
        size_t newsize = new_alloc * c_array->item_size;

        if (oldsize > (SIZE_MAX >> 1)) {
            PyErr_NoMemory();
            return -1;
        }

        void *tmp = PyMem_Realloc(arr, newsize);
        if (tmp == NULL) {
            PyErr_NoMemory();
            return -1;
        }
        arr = tmp;
        memset((char *)arr + oldsize, 0, newsize - oldsize);
        alloc = new_alloc;
    }

    c_array->array = arr;
    c_array->allocated_entries = alloc;
    return 0;
}

/* ---------------------------------------------------------------- the runtime's other answers */

int PySys_Audit(const char *event, const char *format, ...) { (void)event; (void)format; return 0; }
/* the C stack: the harness sets no limit below the parser's own MAXSTACK (CPython's depends on its host's stack) */
int _Py_ReachedRecursionLimitWithMargin(PyThreadState *tstate, int margin_count) { (void)tstate; (void)margin_count; return 0; }
int Py_EnterRecursiveCall(const char *where) { (void)where; return 0; }
void Py_LeaveRecursiveCall(void) {}
int _Py_CheckRecursiveCall(PyThreadState *tstate, const char *where) { (void)tstate; (void)where; return 0; }
/* python3 -I -S -B: no -O */
static PyConfig the_config;
const PyConfig *_Py_GetConfig(void) { return &the_config; }
const char *_PyType_Name(PyTypeObject *t)
{
    const char *s = strrchr(t->tp_name, '.');
    return s ? s + 1 : t->tp_name;
}
PyObject *Py_GetConstant(unsigned int id)
{
    static PyObject *empty_bytes;
    switch (id) {
    case Py_CONSTANT_NONE: return Py_None;
    case Py_CONSTANT_TRUE: return Py_True;
    case Py_CONSTANT_FALSE: return Py_False;
    case Py_CONSTANT_ELLIPSIS: return Py_Ellipsis;
    case Py_CONSTANT_EMPTY_STR: return Py_NewRef(&_Py_STR(empty));
    case Py_CONSTANT_EMPTY_BYTES:
        if (!empty_bytes) { empty_bytes = bytes_new(NULL, 0); empty_bytes->ob_refcnt = _Py_IMMORTAL_INITIAL_REFCNT; }
        return empty_bytes;
    default: fatal("Py_GetConstant"); return NULL;
    }
}
PyObject *Py_GetConstantBorrowed(unsigned int id) { return Py_GetConstant(id); }
int PyObject_IsTrue(PyObject *o) { return o == Py_True; }
PyObject *PyObject_SelfIter(PyObject *o) { return Py_NewRef(o); }
PyObject *PyObject_GenericGetAttr(PyObject *o, PyObject *name) { (void)o; (void)name; fatal("getattr"); return NULL; }

/* Python/pystrcmp.c's PyOS_mystrnicmp, copied (CPython 3.14.10; PSF license) */
int PyOS_mystrnicmp(const char *s1, const char *s2, Py_ssize_t size)
{
    const unsigned char *p1, *p2;
    if (size == 0) { return 0; }
    p1 = (const unsigned char *)s1;
    p2 = (const unsigned char *)s2;
    for (; (--size > 0) && *p1 && *p2 && (Py_TOLOWER(*p1) == Py_TOLOWER(*p2)); p1++, p2++) { ; }
    return Py_TOLOWER(*p1) - Py_TOLOWER(*p2);
}

/* ---------------------------------------------------------------- modules: _tokenize's */

typedef struct { PyObject_HEAD PyModuleDef *def; void *state; } ModuleObject;
typedef struct { PyObject_HEAD PyMethodDef *ml; PyObject *self; } CFunctionObject;

PyObject *PyModuleDef_Init(PyModuleDef *def) { return (PyObject *)def; }
void *PyModule_GetState(PyObject *m) { return ((ModuleObject *)m)->state; }
int PyModule_AddType(PyObject *m, PyTypeObject *t) { (void)m; (void)t; return 0; }

static PyObject *generic_alloc(PyTypeObject *t, Py_ssize_t n) { (void)n; return obj_new(t, t->tp_basicsize); }
static void generic_free(void *p) { free(p); }

PyObject *PyType_FromModuleAndSpec(PyObject *m, PyType_Spec *spec, PyObject *bases)
{
    (void)m; (void)bases;
    PyTypeObject *t = calloc(1, sizeof(PyHeapTypeObject));
    t->ob_base.ob_base.ob_refcnt = _Py_IMMORTAL_INITIAL_REFCNT;
    t->ob_base.ob_base.ob_type = &PyType_Type;
    t->tp_name = spec->name;
    t->tp_basicsize = spec->basicsize;
    t->tp_flags = spec->flags | Py_TPFLAGS_HEAPTYPE;
    t->tp_alloc = generic_alloc;
    t->tp_free = generic_free;
    for (PyType_Slot *s = spec->slots; s->slot; s++) {
        switch (s->slot) {
        case Py_tp_new: t->tp_new = (newfunc)s->pfunc; break;
        case Py_tp_dealloc: t->tp_dealloc = (destructor)s->pfunc; break;
        case Py_tp_iternext: t->tp_iternext = (iternextfunc)s->pfunc; break;
        case Py_tp_iter: t->tp_iter = (getiterfunc)s->pfunc; break;
        case Py_tp_getattro: t->tp_getattro = (getattrofunc)s->pfunc; break;
        default: break;
        }
    }
    return (PyObject *)t;
}
PyObject *PyType_FromSpec(PyType_Spec *spec) { return PyType_FromModuleAndSpec(NULL, spec, NULL); }
void PyObject_GC_UnTrack(void *op) { (void)op; }
void PyObject_GC_Del(void *op) { free(op); }

void _PyArg_BadArgument(const char *fname, const char *displayname, const char *expected, PyObject *arg)
{
    (void)fname; (void)displayname; (void)expected; (void)arg;
    PyErr_SetString(PyExc_TypeError, "bad argument");
}

/* the module of a definition run as its import does: its exec slots */
static ModuleObject *module_exec(PyModuleDef *def)
{
    ModuleObject *m = (ModuleObject *)obj_new(&PyModule_Type, sizeof(ModuleObject));
    m->def = def;
    m->state = def->m_size > 0 ? calloc(1, def->m_size) : NULL;
    for (PyModuleDef_Slot *s = def->m_slots; s && s->slot; s++) {
        if (s->slot == Py_mod_exec && ((int (*)(PyObject *))s->value)((PyObject *)m) < 0) { fatal("a module's exec failed"); }
    }
    return m;
}

static PyObject *ud_normalize(PyObject *self, PyObject *const *args, Py_ssize_t nargs);
static PyMethodDef ud_normalize_def = { "normalize", (PyCFunction)(void (*)(void))ud_normalize, METH_FASTCALL, NULL };

/* unicodedata.normalize, as pegen.c's init_normalization imports it: the harness's port (below) */
PyObject *PyImport_ImportModuleAttrString(const char *modname, const char *attrname)
{
    if (strcmp(modname, "unicodedata") || strcmp(attrname, "normalize")) { fatal("an import the harness does not give"); }
    CFunctionObject *f = (CFunctionObject *)obj_new(&PyCFunction_Type, sizeof(CFunctionObject));
    f->ml = &ud_normalize_def;
    f->self = NULL;
    return (PyObject *)f;
}
PyObject *PyObject_Vectorcall(PyObject *callable, PyObject *const *args, size_t nargsf, PyObject *kwnames)
{
    if (Py_TYPE(callable) != &PyCFunction_Type || kwnames) { fatal("PyObject_Vectorcall: not the harness's function"); }
    CFunctionObject *f = (CFunctionObject *)callable;
    if (!(f->ml->ml_flags & METH_FASTCALL)) { fatal("PyObject_Vectorcall: not METH_FASTCALL"); }
    return ((PyObject *(*)(PyObject *, PyObject *const *, Py_ssize_t))(void (*)(void))f->ml->ml_meth)(f->self, args, PyVectorcall_NARGS(nargsf));
}

/* the iterator's constructor arguments: the clinic's keyword-only extra_tokens and encoding */
typedef struct { PyDictObject d; PyObject *extra_tokens, *encoding; } Kwargs;

PyObject *const *(_PyArg_UnpackKeywords)(PyObject *const *args, Py_ssize_t nargs, PyObject *kwargs, PyObject *kwnames,
                                         struct _PyArg_Parser *parser, int minpos, int maxpos, int minkw, int varpos,
                                         PyObject **buf)
{
    (void)kwnames; (void)parser; (void)minpos; (void)maxpos; (void)minkw; (void)varpos;
    if (nargs != 1 || !kwargs) { fatal("_PyArg_UnpackKeywords: the harness passes readline and both keywords"); }
    buf[0] = args[0];
    buf[1] = ((Kwargs *)kwargs)->extra_tokens;
    buf[2] = ((Kwargs *)kwargs)->encoding;
    return buf;
}

/* ---------------------------------------------------------------- Py_BuildValue (the formats the sources pass) */

static PyObject *build1(const char **f, va_list *ap);
static PyObject *build_tuple(const char **f, va_list *ap, char close)
{
    PyObject *items[16];
    int n = 0;
    while (**f && **f != close) { items[n++] = build1(f, ap); }
    if (**f) { (*f)++; }
    PyObject *t = tuple_new(n);
    for (int i = 0; i < n; i++) { ((PyTupleObject *)t)->ob_item[i] = items[i]; }
    return t;
}
static PyObject *build1(const char **f, va_list *ap)
{
    char c = *(*f)++;
    PyObject *o;
    switch (c) {
    case '(': return build_tuple(f, ap, ')');
    case 'i': return int_new(va_arg(*ap, int));
    case 'n': return int_new(va_arg(*ap, Py_ssize_t));
    case 'O': o = va_arg(*ap, PyObject *); if (!o) { fatal("Py_BuildValue: NULL"); } Py_INCREF(o); return o;
    case 'N': o = va_arg(*ap, PyObject *); if (!o) { fatal("Py_BuildValue: NULL"); } return o;
    default: fatal("Py_BuildValue: a format the harness does not give"); return NULL;
    }
}
PyObject *Py_BuildValue(const char *f, ...)
{
    va_list ap;
    va_start(ap, f);
    PyObject *r = build1(&f, &ap);
    va_end(ap);
    return r;
}

/* ---------------------------------------------------------------- readline: the file's lines as bytes */

typedef struct { PyObject_HEAD char **lines; Py_ssize_t *lens; Py_ssize_t n, i; } ReadlineObject;

PyObject *PyObject_CallNoArgs(PyObject *f)
{
    if (Py_TYPE(f) != &Readline_Type) { fatal("PyObject_CallNoArgs: not the harness's readline"); }
    ReadlineObject *r = (ReadlineObject *)f;
    if (r->i >= r->n) { PyErr_SetNone(PyExc_StopIteration); return NULL; }
    PyObject *b = bytes_new(r->lines[r->i], r->lens[r->i]);
    r->i++;
    return b;
}

/* ---------------------------------------------------------------- what only the unused parts name */

/* Python-ast.c's ast2obj/obj2ast and its types, unicodedata's other functions, the file tokenizer: never run by the
   harness (each a fatal error if it were) */
STUB(PyObject *, PyDict_New, (void))
STUB(int, PyDict_GetItemRef, (PyObject *p, PyObject *k, PyObject **r))
STUB(PyObject *, PyDict_GetItemWithError, (PyObject *p, PyObject *k))
STUB(int, PyDict_Next, (PyObject *p, Py_ssize_t *pos, PyObject **k, PyObject **v))
STUB(int, PyDict_SetItem, (PyObject *p, PyObject *k, PyObject *v))
STUB(int, PyDict_SetItemString, (PyObject *p, const char *k, PyObject *v))
STUB(int, PyDict_Update, (PyObject *a, PyObject *b))
STUB(PyObject *, PyList_AsTuple, (PyObject *l))
STUB(int, PyList_Sort, (PyObject *l))
STUB(int, PyLong_AsInt, (PyObject *o))
STUB(int, PyModule_AddIntConstant, (PyObject *m, const char *n, long v))
STUB(int, PyModule_AddObjectRef, (PyObject *m, const char *n, PyObject *v))
STUB(PyObject *, PyObject_Call, (PyObject *c, PyObject *a, PyObject *k))
STUB(PyObject *, PyObject_CallFunction, (PyObject *c, const char *f, ...))
STUB(PyObject *, PyObject_GenericGetDict, (PyObject *o, void *c))
STUB(int, PyObject_GenericSetAttr, (PyObject *o, PyObject *n, PyObject *v))
STUB(int, PyObject_GenericSetDict, (PyObject *o, PyObject *v, void *c))
STUB(PyObject *, PyObject_GetAttr, (PyObject *o, PyObject *n))
STUB(int, PyObject_GetOptionalAttr, (PyObject *o, PyObject *n, PyObject **r))
STUB(int, PyObject_IsInstance, (PyObject *o, PyObject *c))
STUB(PyObject *, PyObject_Repr, (PyObject *o))
STUB(PyObject *, PyObject_VectorcallMethod, (PyObject *n, PyObject *const *a, size_t f, PyObject *k))
STUB(int, PySequence_Contains, (PyObject *s, PyObject *o))
STUB(PyObject *, PySequence_GetItem, (PyObject *s, Py_ssize_t i))
STUB(PyObject *, PySequence_List, (PyObject *s))
STUB(Py_ssize_t, PySequence_Size, (PyObject *s))
STUB(int, PySet_Discard, (PyObject *s, PyObject *k))
STUB(PyObject *, PySet_New, (PyObject *i))
STUB(Py_ssize_t, PySet_Size, (PyObject *s))
STUB(PyObject *, PyType_GenericAlloc, (PyTypeObject *t, Py_ssize_t n))
STUB(PyObject *, PyType_GenericNew, (PyTypeObject *t, PyObject *a, PyObject *k))
STUB(void *, PyType_GetSlot, (PyTypeObject *t, int s))
STUB(int, PyUnicodeWriter_WriteASCII, (PyUnicodeWriter *w, const char *s, Py_ssize_t n))
STUB(int, PyUnicodeWriter_WriteChar, (PyUnicodeWriter *w, Py_UCS4 c))
STUB(int, PyUnicodeWriter_WriteRepr, (PyUnicodeWriter *w, PyObject *o))
STUB(int, PyUnicodeWriter_WriteUTF8, (PyUnicodeWriter *w, const char *s, Py_ssize_t n))
STUB(PyObject *, PyUnicode_Join, (PyObject *s, PyObject *i))
STUB(PyObject *, PyUnicode_Replace, (PyObject *s, PyObject *a, PyObject *b, Py_ssize_t n))
STUB(PyObject *, PyNumber_Add, (PyObject *a, PyObject *b))
STUB(PyObject *, PyNumber_Subtract, (PyObject *a, PyObject *b))
STUB(PyObject *, PyNumber_Negative, (PyObject *a))
STUB(PyObject *, Py_GenericAlias, (PyObject *a, PyObject *b))
STUB(int, Py_ReprEnter, (PyObject *o))
STUBV(Py_ReprLeave, (PyObject *o))
STUB(int, _PyArg_NoPositional, (const char *n, PyObject *a))
STUB(int, _PyOnceFlag_CallOnceSlow, (_PyOnceFlag *f, _Py_once_fn_t *fn, void *a))
STUB(int, _PySet_NextEntry, (PyObject *s, Py_ssize_t *p, PyObject **k, Py_hash_t *h))
STUB(int, _PySet_Update, (PyObject *s, PyObject *i))
STUB(PyObject *, _Py_union_type_or, (PyObject *a, PyObject *b))
STUB(struct tok_state *, _PyTokenizer_FromFile, (FILE *fp, const char *enc, const char *ps1, const char *ps2))

/* ================================================================ pydump.py's Writer, over the C tree */

typedef struct { char *p; size_t n, cap; } SB;
static void sb_put(SB *b, const char *s, size_t n)
{
    if (b->n + n + 1 > b->cap) { while (b->n + n + 1 > b->cap) { b->cap = b->cap ? b->cap * 2 : 256; } b->p = realloc(b->p, b->cap); }
    memcpy(b->p + b->n, s, n);
    b->n += n;
    b->p[b->n] = 0;
}
static void sb_s(SB *b, const char *s) { sb_put(b, s, strlen(s)); }
static void sb_cp(SB *b, Py_UCS4 ch)
{
    char t[4]; int k = 0;
    if (ch < 0x80) { t[k++] = (char)ch; }
    else if (ch < 0x800) { t[k++] = (char)(0xC0 | (ch >> 6)); t[k++] = (char)(0x80 | (ch & 0x3F)); }
    else if (ch < 0x10000) { t[k++] = (char)(0xE0 | (ch >> 12)); t[k++] = (char)(0x80 | ((ch >> 6) & 0x3F)); t[k++] = (char)(0x80 | (ch & 0x3F)); }
    else { t[k++] = (char)(0xF0 | (ch >> 18)); t[k++] = (char)(0x80 | ((ch >> 12) & 0x3F)); t[k++] = (char)(0x80 | ((ch >> 6) & 0x3F)); t[k++] = (char)(0x80 | (ch & 0x3F)); }
    sb_put(b, t, k);
}
static char *sb_done(SB *b) { if (!b->p) { sb_put(b, "", 0); } return b->p; }
static char *xs(const char *s) { return strdup(s); }
static char *xf(const char *fmt, ...)
{
    char tmp[256];
    va_list ap;
    va_start(ap, fmt);
    vsnprintf(tmp, sizeof tmp, fmt, ap);
    va_end(ap);
    return strdup(tmp);
}

/* the file: its decoded text as code points and its lines' starts (Source) */
static Py_UCS4 *T;
static Py_ssize_t TN;
static Py_ssize_t *starts;
static Py_ssize_t nstarts;
static int full;
static char *wfail;                 /* the Writer's own ValueError: the file's dump is a HelperError */

static void fail(const char *fmt, ...)
{
    if (wfail) { return; }
    char tmp[512];
    va_list ap;
    va_start(ap, fmt);
    vsnprintf(tmp, sizeof tmp, fmt, ap);
    va_end(ap);
    wfail = strdup(tmp);
}

static Py_UCS4 at(Py_ssize_t k)
{
    if (k < 0 || k >= TN) { fail("IndexError: string index out of range"); return 0; }
    return T[k];
}
static int startswith(Py_ssize_t k, const char *s)
{
    for (Py_ssize_t i = 0; s[i]; i++) { if (k + i >= TN || T[k + i] != (Py_UCS4)(unsigned char)s[i]) { return 0; } }
    return 1;
}
static int utf8len(Py_UCS4 c) { return c < 0x80 ? 1 : c < 0x800 ? 2 : c < 0x10000 ? 3 : 4; }

/* Source.index: the ast's (line, UTF-8 byte column) as an index into the text */
static Py_ssize_t sindex(int line, int col)
{
    if (line < 1 || line > nstarts) { fail("IndexError: list index out of range"); return 0; }
    Py_ssize_t a = starts[line - 1], z = line < nstarts ? starts[line] : TN;
    int ascii = 1;
    for (Py_ssize_t i = a; i < z; i++) { if (T[i] >= 0x80) { ascii = 0; break; } }
    if (ascii) { return a + col; }
    Py_ssize_t b = 0, c = 0;
    for (Py_ssize_t i = a; i < z && b < col; i++) { b += utf8len(T[i]); c++; }
    return a + c;
}
/* Source.cindex: tokenize's (line, character column) */
static Py_ssize_t cindex(Py_ssize_t line, Py_ssize_t col) { return line <= nstarts ? starts[line - 1] + col : TN; }

/* qstr over the text's code points, a str object's, a C string's (UTF-8) */
static char *qstr_u(const Py_UCS4 *u, Py_ssize_t n)
{
    SB b = {0};
    sb_s(&b, "\"");
    for (Py_ssize_t i = 0; i < n; i++) {
        if (u[i] == '\\') { sb_s(&b, "\\\\"); }
        else if (u[i] == '"') { sb_s(&b, "\\\""); }
        else { sb_cp(&b, u[i]); }
    }
    sb_s(&b, "\"");
    return sb_done(&b);
}
/* the text's slice [a:z] as Python slices it (each end clamped to the text) */
static char *qstr_t(Py_ssize_t a, Py_ssize_t z)
{
    if (a > TN) { a = TN; }
    if (z > TN) { z = TN; }
    if (z < a) { z = a; }
    return qstr_u(T + a, z - a);
}
static char *qstr_c(const char *s)
{
    SB b = {0};
    sb_s(&b, "\"");
    for (; *s; s++) {
        if (*s == '\\') { sb_s(&b, "\\\\"); }
        else if (*s == '"') { sb_s(&b, "\\\""); }
        else { sb_put(&b, s, 1); }
    }
    sb_s(&b, "\"");
    return sb_done(&b);
}
static char *qstr_o(PyObject *s) { return qstr_c(PyUnicode_AsUTF8(s)); }

static int not_symbol(const char *s) { return !strcmp(s, "nil") || !strcmp(s, "true") || !strcmp(s, "false"); }
static char *ident(PyObject *name)
{
    const char *s = PyUnicode_AsUTF8(name);
    return not_symbol(s) ? qstr_c(s) : xs(s);
}

/* form(head, *fields, opts, meta): the fields NULL as nil, trailing NULLs dropped; an opt whose value is NULL left out;
   the meta map when there is one. Every string it takes it frees. */
typedef struct {
    const char *head;
    char *f[32]; int nf;
    const char *ok[16]; char *ov[16]; int no;
    const char *mk[8]; char *mv[8]; int nm;
} F;
static void F_f(F *x, char *v) { x->f[x->nf++] = v; }
static void F_o(F *x, const char *k, char *v) { x->ok[x->no] = k; x->ov[x->no++] = v; }
static void F_m(F *x, const char *k, char *v) { x->mk[x->nm] = k; x->mv[x->nm++] = v; }
static char *form(F *x)
{
    SB b = {0};
    sb_s(&b, "(");
    sb_s(&b, x->head);
    int nf = x->nf;
    while (nf > 0 && !x->f[nf - 1]) { nf--; }
    for (int i = 0; i < nf; i++) { sb_s(&b, " "); sb_s(&b, x->f[i] ? x->f[i] : "nil"); free(x->f[i]); }
    for (int i = 0; i < x->no; i++) {
        if (x->ov[i]) { sb_s(&b, " "); sb_s(&b, x->ok[i]); sb_s(&b, " "); sb_s(&b, x->ov[i]); free(x->ov[i]); }
    }
    if (x->nm) {
        sb_s(&b, " {");
        for (int i = 0; i < x->nm; i++) {
            if (i) { sb_s(&b, " "); }
            sb_s(&b, x->mk[i]); sb_s(&b, " "); sb_s(&b, x->mv[i]); free(x->mv[i]);
        }
        sb_s(&b, "}");
    }
    sb_s(&b, ")");
    return sb_done(&b);
}

/* vec over strings it frees */
typedef struct { char **v; int n, cap; } V;
static void V_add(V *x, char *s) { if (x->n == x->cap) { x->cap = x->cap ? x->cap * 2 : 8; x->v = realloc(x->v, x->cap * sizeof(char *)); } x->v[x->n++] = s; }
static char *vec(V *x)
{
    SB b = {0};
    sb_s(&b, "[");
    for (int i = 0; i < x->n; i++) { if (i) { sb_s(&b, " "); } sb_s(&b, x->v[i]); free(x->v[i]); }
    sb_s(&b, "]");
    free(x->v);
    return sb_done(&b);
}

/* the type comments of the type_comments parse: (type name, lineno, col_offset) -> comment */
typedef struct { const char *kind; int line, col; PyObject *c; } TC;
static TC *tcs;
static int ntc, captc;
static void tc_add(const char *kind, int line, int col, PyObject *c)
{
    if (!c) { return; }
    if (ntc == captc) { captc = captc ? captc * 2 : 16; tcs = realloc(tcs, captc * sizeof(TC)); }
    tcs[ntc].kind = kind; tcs[ntc].line = line; tcs[ntc].col = col; tcs[ntc].c = c; ntc++;
}
static PyObject *tc_get(const char *kind, int line, int col)
{
    for (int i = ntc - 1; i >= 0; i--) { if (tcs[i].line == line && tcs[i].col == col && !strcmp(tcs[i].kind, kind)) { return tcs[i].c; } }
    return NULL;
}
static void tc_args(arguments_ty a)
{
    asdl_arg_seq *ss[3] = { a->posonlyargs, a->args, a->kwonlyargs };
    for (int j = 0; j < 3; j++) {
        for (Py_ssize_t i = 0; i < asdl_seq_LEN(ss[j]); i++) { arg_ty x = asdl_seq_GET(ss[j], i); tc_add("arg", x->lineno, x->col_offset, x->type_comment); }
    }
    if (a->vararg) { tc_add("arg", a->vararg->lineno, a->vararg->col_offset, a->vararg->type_comment); }
    if (a->kwarg) { tc_add("arg", a->kwarg->lineno, a->kwarg->col_offset, a->kwarg->type_comment); }
}
static void tc_body(asdl_stmt_seq *body);
static void tc_stmt(stmt_ty s)
{
    switch (s->kind) {
    case FunctionDef_kind: tc_add("FunctionDef", s->lineno, s->col_offset, s->v.FunctionDef.type_comment); tc_args(s->v.FunctionDef.args); tc_body(s->v.FunctionDef.body); break;
    case AsyncFunctionDef_kind: tc_add("AsyncFunctionDef", s->lineno, s->col_offset, s->v.AsyncFunctionDef.type_comment); tc_args(s->v.AsyncFunctionDef.args); tc_body(s->v.AsyncFunctionDef.body); break;
    case ClassDef_kind: tc_body(s->v.ClassDef.body); break;
    case Assign_kind: tc_add("Assign", s->lineno, s->col_offset, s->v.Assign.type_comment); break;
    case For_kind: tc_add("For", s->lineno, s->col_offset, s->v.For.type_comment); tc_body(s->v.For.body); tc_body(s->v.For.orelse); break;
    case AsyncFor_kind: tc_add("AsyncFor", s->lineno, s->col_offset, s->v.AsyncFor.type_comment); tc_body(s->v.AsyncFor.body); tc_body(s->v.AsyncFor.orelse); break;
    case While_kind: tc_body(s->v.While.body); tc_body(s->v.While.orelse); break;
    case If_kind: tc_body(s->v.If.body); tc_body(s->v.If.orelse); break;
    case With_kind: tc_add("With", s->lineno, s->col_offset, s->v.With.type_comment); tc_body(s->v.With.body); break;
    case AsyncWith_kind: tc_add("AsyncWith", s->lineno, s->col_offset, s->v.AsyncWith.type_comment); tc_body(s->v.AsyncWith.body); break;
    case Match_kind:
        for (Py_ssize_t i = 0; i < asdl_seq_LEN(s->v.Match.cases); i++) { tc_body(asdl_seq_GET(s->v.Match.cases, i)->body); }
        break;
    case Try_kind: case TryStar_kind: {
        asdl_stmt_seq *b = s->kind == Try_kind ? s->v.Try.body : s->v.TryStar.body;
        asdl_excepthandler_seq *h = s->kind == Try_kind ? s->v.Try.handlers : s->v.TryStar.handlers;
        tc_body(b);
        for (Py_ssize_t i = 0; i < asdl_seq_LEN(h); i++) { tc_body(asdl_seq_GET(h, i)->v.ExceptHandler.body); }
        tc_body(s->kind == Try_kind ? s->v.Try.orelse : s->v.TryStar.orelse);
        tc_body(s->kind == Try_kind ? s->v.Try.finalbody : s->v.TryStar.finalbody);
        break;
    }
    default: break;
    }
}
static void tc_body(asdl_stmt_seq *body) { for (Py_ssize_t i = 0; i < asdl_seq_LEN(body); i++) { tc_stmt(asdl_seq_GET(body, i)); } }

/* posit: TEXT with the node's position in its trailing map (mode :full); a short spelling first made long */
static char *posit(char *text, int l, int c, int el, int ec, const char *shrt)
{
    if (!full) { return text; }
    char pos[128];
    snprintf(pos, sizeof pos, ":pos [%d %d %d %d]", l, c, el, ec);
    SB b = {0};
    if (text[0] != '(') {
        char *ctx = NULL, *t = text;
        if (!strncmp(t, "^:", 2)) {
            char *sp = strchr(t + 2, ' ');
            ctx = strndup(t + 2, sp - (t + 2));
            t = sp + 1;
        }
        const char *head = shrt;
        char *body;
        int plain = 1;
        for (char *q = t; *q; q++) { if (*q < '0' || *q > '9') { plain = 0; } }
        if (plain && t[0] && (t[0] != '0' || !t[1])) { plain = 1; } else { plain = 0; }
        if (!strcmp(head, "const") && plain) { head = "int"; body = qstr_c(t); }
        else { if (!strcmp(head, "const") && t[0] == '"') { head = "str"; } body = xs(t); }
        sb_s(&b, "("); sb_s(&b, head); sb_s(&b, " "); sb_s(&b, body); sb_s(&b, ")");
        free(body);
        if (ctx) {
            char pos2[160];
            snprintf(pos2, sizeof pos2, ":ctx :%s %s", ctx, pos);
            snprintf(pos, sizeof pos, "%s", pos2);
            free(ctx);
        }
        free(text);
        text = sb_done(&b);
        b = (SB){0};
    }
    size_t n = strlen(text);
    if (n >= 2 && !strcmp(text + n - 2, "})")) { sb_put(&b, text, n - 2); sb_s(&b, " "); sb_s(&b, pos); sb_s(&b, "})"); }
    else { sb_put(&b, text, n - 1); sb_s(&b, " {"); sb_s(&b, pos); sb_s(&b, "})"); }
    free(text);
    return sb_done(&b);
}

static char *expr(expr_ty n);
static char *stmt(stmt_ty n, int elif);
static char *pattern(pattern_ty p);

static char *body(asdl_stmt_seq *ss)
{
    V v = {0};
    for (Py_ssize_t i = 0; i < asdl_seq_LEN(ss); i++) { V_add(&v, stmt(asdl_seq_GET(ss, i), 0)); }
    return vec(&v);
}
static char *exprs(asdl_expr_seq *es)
{
    V v = {0};
    for (Py_ssize_t i = 0; i < asdl_seq_LEN(es); i++) { V_add(&v, expr(asdl_seq_GET(es, i))); }
    return vec(&v);
}
static char *orelse(stmt_ty n, asdl_stmt_seq *o)
{
    if (!asdl_seq_LEN(o)) { return NULL; }
    if (n->kind == If_kind && asdl_seq_LEN(o) == 1) {
        stmt_ty e = asdl_seq_GET(o, 0);
        if (e->kind == If_kind && startswith(sindex(e->lineno, e->col_offset), "elif")) {
            V v = {0};
            V_add(&v, stmt(e, 1));
            return vec(&v);
        }
    }
    return body(o);
}
static void meta_tc(F *x, const char *kind, stmt_ty n, int elif)
{
    if (elif) { F_m(x, ":elif", xs("true")); }
    PyObject *c = tc_get(kind, n->lineno, n->col_offset);
    if (c) { F_m(x, ":type-comment", qstr_o(c)); }
}
static void meta_x(F *x, int elif) { if (elif) { F_m(x, ":elif", xs("true")); } }

static const char *BINOP[] = { "?", "+", "-", "*", "mat-mult", "/", "%", "**", "<<", ">>", "bit-or", "bit-xor", "bit-and", "floor-div" };
static const char *UNARY[] = { "?", "bit-not", "not", "+", "-" };
static const char *CMP[] = { "?", "==", "!=", "<", "<=", ">", ">=", "is", "is-not", "in", "not-in" };

static char *arg(arg_ty a)
{
    PyObject *c = tc_get("arg", a->lineno, a->col_offset);
    char *t;
    if (!a->annotation && !c) {
        const char *s = PyUnicode_AsUTF8(a->arg);
        if (!not_symbol(s)) { t = xs(s); }
        else { F x = {.head = "arg"}; F_f(&x, qstr_c(s)); t = form(&x); }
    } else {
        F x = {.head = "arg"};
        F_f(&x, ident(a->arg));
        F_f(&x, a->annotation ? expr(a->annotation) : NULL);
        if (c) { F_m(&x, ":type-comment", qstr_o(c)); }
        t = form(&x);
    }
    return posit(t, a->lineno, a->col_offset, a->end_lineno, a->end_col_offset, "arg");
}
static char *args_vec(asdl_arg_seq *s)
{
    if (!asdl_seq_LEN(s)) { return NULL; }
    V v = {0};
    for (Py_ssize_t i = 0; i < asdl_seq_LEN(s); i++) { V_add(&v, arg(asdl_seq_GET(s, i))); }
    return vec(&v);
}
static char *arguments(arguments_ty a)
{
    char *kwd = NULL;
    int any = 0;
    for (Py_ssize_t i = 0; i < asdl_seq_LEN(a->kw_defaults); i++) { if (asdl_seq_GET(a->kw_defaults, i)) { any = 1; } }
    if (any) {
        V v = {0};
        for (Py_ssize_t i = 0; i < asdl_seq_LEN(a->kw_defaults); i++) {
            expr_ty d = asdl_seq_GET(a->kw_defaults, i);
            V_add(&v, d ? expr(d) : xs("nil"));
        }
        kwd = vec(&v);
    }
    F x = {.head = "arguments"};
    F_o(&x, ":posonly", args_vec(a->posonlyargs));
    F_o(&x, ":args", args_vec(a->args));
    F_o(&x, ":vararg", a->vararg ? arg(a->vararg) : NULL);
    F_o(&x, ":kwonly", args_vec(a->kwonlyargs));
    F_o(&x, ":kw-defaults", kwd);
    F_o(&x, ":kwarg", a->kwarg ? arg(a->kwarg) : NULL);
    F_o(&x, ":defaults", asdl_seq_LEN(a->defaults) ? exprs(a->defaults) : NULL);
    return form(&x);
}
static char *keyword(keyword_ty k)
{
    F x = {.head = "keyword"};
    F_f(&x, k->arg ? ident(k->arg) : xs("nil"));
    F_f(&x, expr(k->value));
    return posit(form(&x), k->lineno, k->col_offset, k->end_lineno, k->end_col_offset, NULL);
}
static char *keywords(asdl_keyword_seq *ks)
{
    if (!asdl_seq_LEN(ks)) { return NULL; }
    V v = {0};
    for (Py_ssize_t i = 0; i < asdl_seq_LEN(ks); i++) { V_add(&v, keyword(asdl_seq_GET(ks, i))); }
    return vec(&v);
}
static char *alias(alias_ty a)
{
    char *t;
    if (!a->asname) {
        const char *s = PyUnicode_AsUTF8(a->name);
        if (!not_symbol(s)) { t = xs(s); }
        else { F x = {.head = "alias"}; F_f(&x, qstr_c(s)); t = form(&x); }
    } else {
        F x = {.head = "alias"};
        F_f(&x, ident(a->name));
        F_o(&x, ":as", ident(a->asname));
        t = form(&x);
    }
    return posit(t, a->lineno, a->col_offset, a->end_lineno, a->end_col_offset, "alias");
}
static char *aliases(asdl_alias_seq *as)
{
    V v = {0};
    for (Py_ssize_t i = 0; i < asdl_seq_LEN(as); i++) { V_add(&v, alias(asdl_seq_GET(as, i))); }
    return vec(&v);
}
static char *idents(asdl_identifier_seq *s)
{
    V v = {0};
    for (Py_ssize_t i = 0; i < asdl_seq_LEN(s); i++) { V_add(&v, ident(asdl_seq_GET(s, i))); }
    return vec(&v);
}
static char *tparams(asdl_type_param_seq *ps)
{
    if (!asdl_seq_LEN(ps)) { return NULL; }
    V v = {0};
    for (Py_ssize_t i = 0; i < asdl_seq_LEN(ps); i++) {
        type_param_ty p = asdl_seq_GET(ps, i);
        F x = {0};
        if (p->kind == TypeVar_kind) {
            char *d = p->v.TypeVar.default_value ? expr(p->v.TypeVar.default_value) : NULL;
            x.head = "type-var";
            F_f(&x, ident(p->v.TypeVar.name));
            F_o(&x, ":bound", p->v.TypeVar.bound ? expr(p->v.TypeVar.bound) : NULL);
            F_o(&x, ":default", d);
        } else if (p->kind == ParamSpec_kind) {
            char *d = p->v.ParamSpec.default_value ? expr(p->v.ParamSpec.default_value) : NULL;
            x.head = "param-spec";
            F_f(&x, ident(p->v.ParamSpec.name));
            F_o(&x, ":default", d);
        } else {
            char *d = p->v.TypeVarTuple.default_value ? expr(p->v.TypeVarTuple.default_value) : NULL;
            x.head = "type-var-tuple";
            F_f(&x, ident(p->v.TypeVarTuple.name));
            F_o(&x, ":default", d);
        }
        V_add(&v, posit(form(&x), p->lineno, p->col_offset, p->end_lineno, p->end_col_offset, NULL));
    }
    return vec(&v);
}
static char *withitems(asdl_withitem_seq *s)
{
    V v = {0};
    for (Py_ssize_t i = 0; i < asdl_seq_LEN(s); i++) {
        withitem_ty w = asdl_seq_GET(s, i);
        F x = {.head = "withitem"};
        F_f(&x, expr(w->context_expr));
        F_f(&x, w->optional_vars ? expr(w->optional_vars) : NULL);
        V_add(&v, form(&x));
    }
    return vec(&v);
}
static char *patterns(asdl_pattern_seq *s)
{
    V v = {0};
    for (Py_ssize_t i = 0; i < asdl_seq_LEN(s); i++) { V_add(&v, pattern(asdl_seq_GET(s, i))); }
    return vec(&v);
}
static char *pattern(pattern_ty p)
{
    F x = {0};
    switch (p->kind) {
    case MatchValue_kind: x.head = "match-value"; F_f(&x, expr(p->v.MatchValue.value)); break;
    case MatchSingleton_kind: {
        PyObject *v = p->v.MatchSingleton.value;
        x.head = "match-singleton";
        F_f(&x, xs(v == Py_None ? "None" : v == Py_True ? "True" : "False"));
        break;
    }
    case MatchSequence_kind: x.head = "match-sequence"; F_f(&x, patterns(p->v.MatchSequence.patterns)); break;
    case MatchMapping_kind:
        x.head = "match-mapping";
        F_f(&x, exprs(p->v.MatchMapping.keys));
        F_f(&x, patterns(p->v.MatchMapping.patterns));
        F_o(&x, ":rest", p->v.MatchMapping.rest ? ident(p->v.MatchMapping.rest) : NULL);
        break;
    case MatchClass_kind:
        x.head = "match-class";
        F_f(&x, expr(p->v.MatchClass.cls));
        F_f(&x, patterns(p->v.MatchClass.patterns));
        F_o(&x, ":kwd-attrs", asdl_seq_LEN(p->v.MatchClass.kwd_attrs) ? idents(p->v.MatchClass.kwd_attrs) : NULL);
        F_o(&x, ":kwd-patterns", asdl_seq_LEN(p->v.MatchClass.kwd_patterns) ? patterns(p->v.MatchClass.kwd_patterns) : NULL);
        break;
    case MatchStar_kind: x.head = "match-star"; F_f(&x, p->v.MatchStar.name ? ident(p->v.MatchStar.name) : NULL); break;
    case MatchAs_kind:
        x.head = "match-as";
        F_f(&x, p->v.MatchAs.pattern ? pattern(p->v.MatchAs.pattern) : NULL);
        F_f(&x, p->v.MatchAs.name ? ident(p->v.MatchAs.name) : NULL);
        break;
    case MatchOr_kind: x.head = "match-or"; F_f(&x, patterns(p->v.MatchOr.patterns)); break;
    default: fail("ValueError: unknown pattern"); return xs("?");
    }
    return posit(form(&x), p->lineno, p->col_offset, p->end_lineno, p->end_col_offset, NULL);
}
static char *handlers(asdl_excepthandler_seq *hs)
{
    V v = {0};
    for (Py_ssize_t i = 0; i < asdl_seq_LEN(hs); i++) {
        excepthandler_ty h = asdl_seq_GET(hs, i);
        F x = {.head = "except-handler"};
        F_f(&x, body(h->v.ExceptHandler.body));
        F_o(&x, ":type", h->v.ExceptHandler.type ? expr(h->v.ExceptHandler.type) : NULL);
        F_o(&x, ":name", h->v.ExceptHandler.name ? ident(h->v.ExceptHandler.name) : NULL);
        V_add(&v, posit(form(&x), h->lineno, h->col_offset, h->end_lineno, h->end_col_offset, NULL));
    }
    return vec(&v);
}

static char *stmt(stmt_ty n, int elif)
{
    F x = {0};
    switch (n->kind) {
    case FunctionDef_kind: case AsyncFunctionDef_kind: {
        int a = n->kind == AsyncFunctionDef_kind;
        x.head = a ? "async-function-def" : "function-def";
        F_f(&x, ident(a ? n->v.AsyncFunctionDef.name : n->v.FunctionDef.name));
        F_f(&x, arguments(a ? n->v.AsyncFunctionDef.args : n->v.FunctionDef.args));
        F_f(&x, body(a ? n->v.AsyncFunctionDef.body : n->v.FunctionDef.body));
        asdl_expr_seq *dl = a ? n->v.AsyncFunctionDef.decorator_list : n->v.FunctionDef.decorator_list;
        expr_ty r = a ? n->v.AsyncFunctionDef.returns : n->v.FunctionDef.returns;
        F_o(&x, ":decorators", asdl_seq_LEN(dl) ? exprs(dl) : NULL);
        F_o(&x, ":returns", r ? expr(r) : NULL);
        F_o(&x, ":type-params", tparams(a ? n->v.AsyncFunctionDef.type_params : n->v.FunctionDef.type_params));
        meta_tc(&x, a ? "AsyncFunctionDef" : "FunctionDef", n, elif);
        break;
    }
    case ClassDef_kind:
        x.head = "class-def";
        F_f(&x, ident(n->v.ClassDef.name));
        F_f(&x, body(n->v.ClassDef.body));
        F_o(&x, ":bases", asdl_seq_LEN(n->v.ClassDef.bases) ? exprs(n->v.ClassDef.bases) : NULL);
        F_o(&x, ":keywords", keywords(n->v.ClassDef.keywords));
        F_o(&x, ":decorators", asdl_seq_LEN(n->v.ClassDef.decorator_list) ? exprs(n->v.ClassDef.decorator_list) : NULL);
        F_o(&x, ":type-params", tparams(n->v.ClassDef.type_params));
        meta_x(&x, elif);
        break;
    case Return_kind: x.head = "return"; F_f(&x, n->v.Return.value ? expr(n->v.Return.value) : NULL); meta_x(&x, elif); break;
    case Delete_kind: x.head = "delete"; F_f(&x, exprs(n->v.Delete.targets)); meta_x(&x, elif); break;
    case Assign_kind:
        x.head = "assign";
        F_f(&x, exprs(n->v.Assign.targets));
        F_f(&x, expr(n->v.Assign.value));
        meta_tc(&x, "Assign", n, elif);
        break;
    case TypeAlias_kind:
        x.head = "type-alias";
        F_f(&x, expr(n->v.TypeAlias.name));
        F_f(&x, expr(n->v.TypeAlias.value));
        F_o(&x, ":type-params", tparams(n->v.TypeAlias.type_params));
        meta_x(&x, elif);
        break;
    case AugAssign_kind:
        x.head = "aug-assign";
        F_f(&x, expr(n->v.AugAssign.target));
        F_f(&x, xs(BINOP[n->v.AugAssign.op]));
        F_f(&x, expr(n->v.AugAssign.value));
        meta_x(&x, elif);
        break;
    case AnnAssign_kind: {
        expr_ty t = n->v.AnnAssign.target;
        x.head = "ann-assign";
        F_f(&x, expr(t));
        F_f(&x, expr(n->v.AnnAssign.annotation));
        F_f(&x, n->v.AnnAssign.value ? expr(n->v.AnnAssign.value) : NULL);
        F_o(&x, ":simple", (t->kind == Name_kind && !n->v.AnnAssign.simple) ? xs("false") : NULL);
        meta_x(&x, elif);
        break;
    }
    case For_kind: case AsyncFor_kind: {
        int a = n->kind == AsyncFor_kind;
        x.head = a ? "async-for" : "for";
        F_f(&x, expr(a ? n->v.AsyncFor.target : n->v.For.target));
        F_f(&x, expr(a ? n->v.AsyncFor.iter : n->v.For.iter));
        F_f(&x, body(a ? n->v.AsyncFor.body : n->v.For.body));
        F_o(&x, ":else", orelse(n, a ? n->v.AsyncFor.orelse : n->v.For.orelse));
        meta_tc(&x, a ? "AsyncFor" : "For", n, elif);
        break;
    }
    case While_kind:
        x.head = "while";
        F_f(&x, expr(n->v.While.test));
        F_f(&x, body(n->v.While.body));
        F_o(&x, ":else", orelse(n, n->v.While.orelse));
        meta_x(&x, elif);
        break;
    case If_kind:
        x.head = "if";
        F_f(&x, expr(n->v.If.test));
        F_f(&x, body(n->v.If.body));
        F_o(&x, ":else", orelse(n, n->v.If.orelse));
        meta_x(&x, elif);
        break;
    case With_kind: case AsyncWith_kind: {
        int a = n->kind == AsyncWith_kind;
        x.head = a ? "async-with" : "with";
        F_f(&x, withitems(a ? n->v.AsyncWith.items : n->v.With.items));
        F_f(&x, body(a ? n->v.AsyncWith.body : n->v.With.body));
        meta_tc(&x, a ? "AsyncWith" : "With", n, elif);
        break;
    }
    case Match_kind: {
        x.head = "match";
        F_f(&x, expr(n->v.Match.subject));
        V v = {0};
        for (Py_ssize_t i = 0; i < asdl_seq_LEN(n->v.Match.cases); i++) {
            match_case_ty c = asdl_seq_GET(n->v.Match.cases, i);
            F y = {.head = "match-case"};
            F_f(&y, pattern(c->pattern));
            F_f(&y, body(c->body));
            F_o(&y, ":guard", c->guard ? expr(c->guard) : NULL);
            V_add(&v, form(&y));
        }
        F_f(&x, vec(&v));
        meta_x(&x, elif);
        break;
    }
    case Raise_kind:
        x.head = "raise";
        F_f(&x, n->v.Raise.exc ? expr(n->v.Raise.exc) : NULL);
        F_o(&x, ":from", n->v.Raise.cause ? expr(n->v.Raise.cause) : NULL);
        meta_x(&x, elif);
        break;
    case Try_kind: case TryStar_kind: {
        int s = n->kind == TryStar_kind;
        x.head = s ? "try-star" : "try";
        asdl_stmt_seq *o = s ? n->v.TryStar.orelse : n->v.Try.orelse, *fb = s ? n->v.TryStar.finalbody : n->v.Try.finalbody;
        F_f(&x, body(s ? n->v.TryStar.body : n->v.Try.body));
        F_f(&x, handlers(s ? n->v.TryStar.handlers : n->v.Try.handlers));
        F_o(&x, ":else", asdl_seq_LEN(o) ? body(o) : NULL);
        F_o(&x, ":finally", asdl_seq_LEN(fb) ? body(fb) : NULL);
        meta_x(&x, elif);
        break;
    }
    case Assert_kind:
        x.head = "assert";
        F_f(&x, expr(n->v.Assert.test));
        F_f(&x, n->v.Assert.msg ? expr(n->v.Assert.msg) : NULL);
        meta_x(&x, elif);
        break;
    case Import_kind: x.head = "import"; F_f(&x, aliases(n->v.Import.names)); meta_x(&x, elif); break;
    case ImportFrom_kind:
        x.head = "import-from";
        F_f(&x, n->v.ImportFrom.module ? xs(PyUnicode_AsUTF8(n->v.ImportFrom.module)) : xs("nil"));
        F_f(&x, aliases(n->v.ImportFrom.names));
        F_o(&x, ":level", n->v.ImportFrom.level ? xf("%d", n->v.ImportFrom.level) : NULL);
        meta_x(&x, elif);
        break;
    case Global_kind: x.head = "global"; F_f(&x, idents(n->v.Global.names)); meta_x(&x, elif); break;
    case Nonlocal_kind: x.head = "nonlocal"; F_f(&x, idents(n->v.Nonlocal.names)); meta_x(&x, elif); break;
    case Expr_kind: x.head = "expr"; F_f(&x, expr(n->v.Expr.value)); meta_x(&x, elif); break;
    case Pass_kind: x.head = "pass"; meta_x(&x, elif); break;
    case Break_kind: x.head = "break"; meta_x(&x, elif); break;
    case Continue_kind: x.head = "continue"; meta_x(&x, elif); break;
    default: fail("ValueError: unknown statement"); return xs("?");
    }
    return posit(form(&x), n->lineno, n->col_offset, n->end_lineno, n->end_col_offset, NULL);
}

/* ---- the literals (§3) */

static char *strings(expr_ty n);

static int plain_int(Py_ssize_t a, Py_ssize_t z)
{
    if (z <= a) { return 0; }
    if (T[a] == '0') { return z == a + 1; }
    for (Py_ssize_t i = a; i < z; i++) { if (T[i] < '0' || T[i] > '9') { return 0; } }
    return 1;
}
static char *w_constant(expr_ty n)
{
    PyObject *v = n->v.Constant.value;
    if (v == Py_None) { return xs("None"); }
    if (v == Py_True) { return xs("True"); }
    if (v == Py_False) { return xs("False"); }
    if (v == Py_Ellipsis) { return xs("..."); }
    if (Py_TYPE(v) == &PyUnicode_Type || Py_TYPE(v) == &PyBytes_Type) { return strings(n); }
    Py_ssize_t a = sindex(n->lineno, n->col_offset), z = sindex(n->end_lineno, n->end_col_offset);
    if (z < a) { z = a; }
    F x = {0};
    if (Py_TYPE(v) == &PyLong_Type) {
        if (plain_int(a, z)) { SB b = {0}; for (Py_ssize_t i = a; i < z; i++) { sb_cp(&b, T[i]); } return sb_done(&b); }
        x.head = "int";
    } else if (Py_TYPE(v) == &PyFloat_Type) { x.head = "float"; }
    else if (Py_TYPE(v) == &PyComplex_Type) { x.head = "imag"; }
    else { fail("ValueError: constant of type %s", Py_TYPE(v)->tp_name); return xs("?"); }
    F_f(&x, qstr_t(a, z));
    return form(&x);
}

static int is_prefix_char(Py_UCS4 c) { return c && strchr("rRbBuUfFtT", (int)c) != NULL; }
static int lower_has(Py_ssize_t i, Py_ssize_t j, char c) { for (Py_ssize_t k = i; k < j; k++) { Py_UCS4 d = T[k]; if (d >= 'A' && d <= 'Z') { d += 32; } if (d == (Py_UCS4)c) { return 1; } } return 0; }

typedef struct { expr_ty *v; int n; } Fields;   /* the replacement fields a body's text may open */
static void fields_of(asdl_expr_seq *vals, Fields *fs)
{
    for (Py_ssize_t i = 0; i < asdl_seq_LEN(vals); i++) {
        expr_ty e = asdl_seq_GET(vals, i);
        if (e->kind == FormattedValue_kind || e->kind == Interpolation_kind) { fs->v = realloc(fs->v, (fs->n + 1) * sizeof(expr_ty)); fs->v[fs->n++] = e; }
    }
}
static expr_ty field_at(Fields *fs, Py_ssize_t k)
{
    for (int i = 0; i < fs->n; i++) { if (sindex(fs->v[i]->lineno, fs->v[i]->col_offset) == k) { return fs->v[i]; } }
    return NULL;
}

static char *field(expr_ty f, int raw);

/* fbody: the parts of an f-string's or t-string's body from k to its closing quote (or to spec_end), into ITEMS */
static Py_ssize_t fbody(Py_ssize_t k, const char *quote, Py_ssize_t spec_end, int raw, Fields *fs, V *items)
{
    Py_ssize_t lit = k;
    for (;;) {
        if (wfail) { return k; }
        if (spec_end >= 0) { if (k >= spec_end) { break; } }
        else if (startswith(k, quote)) { break; }
        Py_UCS4 c = at(k);
        if (c == '\\') {
            Py_UCS4 nxt = k + 1 < TN ? T[k + 1] : 0;
            int has = k + 1 < TN;
            if (!raw && startswith(k + 1, "N{")) {
                Py_ssize_t j = k;
                while (j < TN && T[j] != '}') { j++; }
                if (j >= TN) { fail("ValueError: substring not found"); return k; }
                k = j + 1;
            } else if ((has && (nxt == '{' || nxt == '}')) || (raw && !(has && (nxt == '\\' || nxt == '\'' || nxt == '"')))) {
                k += 1;
            } else {
                k += 2;
            }
            continue;
        }
        if (c == '{' && k + 1 < TN && T[k + 1] == '{' && spec_end < 0) { k += 2; continue; }
        if (c == '}' && k + 1 < TN && T[k + 1] == '}' && spec_end < 0) { k += 2; continue; }
        if (c == '{') {
            expr_ty f = field_at(fs, k);
            if (!f) { fail("ValueError: no replacement field at %zd", k); return k; }
            if (lit < k) { V_add(items, qstr_t(lit, k)); }
            V_add(items, field(f, raw));
            k = sindex(f->end_lineno, f->end_col_offset);
            lit = k;
            continue;
        }
        k += 1;
    }
    if (lit < k) { V_add(items, qstr_t(lit, k)); }
    if (spec_end < 0) { k += (Py_ssize_t)strlen(quote); }
    return k;
}

/* skip: k past spaces, line ends, comments, backslash continuations (and the characters of MORE) */
static Py_ssize_t skip(Py_ssize_t k, const char *more)
{
    for (;;) {
        if (wfail) { return k; }
        Py_UCS4 c = at(k);
        if (c == ' ' || c == '\t' || c == '\r' || c == '\n' || c == '\f' || (c && more && strchr(more, (int)c))) { k++; }
        else if (c == '#') { while (!wfail && at(k) != '\r' && at(k) != '\n') { k++; } }
        else if (c == '\\' && k + 1 < TN && (T[k + 1] == '\r' || T[k + 1] == '\n')) { k += 2; }
        else { return k; }
    }
}
/* nocomment over the text from a to z, appended to B */
static void nocomment(SB *b, Py_ssize_t a, Py_ssize_t z)
{
    for (Py_ssize_t i = a; i < z; i++) {
        if (T[i] == '#') { while (i < z && T[i] != '\r' && T[i] != '\n') { i++; } if (i >= z) { break; } }
        sb_cp(b, T[i]);
    }
}

static char *field(expr_ty f, int raw)
{
    int interp = f->kind == Interpolation_kind;
    expr_ty value = interp ? f->v.Interpolation.value : f->v.FormattedValue.value;
    expr_ty spec_n = interp ? f->v.Interpolation.format_spec : f->v.FormattedValue.format_spec;
    int conversion = interp ? f->v.Interpolation.conversion : f->v.FormattedValue.conversion;
    Py_ssize_t fs = sindex(f->lineno, f->col_offset), fe = sindex(f->end_lineno, f->end_col_offset);
    Py_ssize_t vs = sindex(value->lineno, value->col_offset), ve = sindex(value->end_lineno, value->end_col_offset);
    Py_ssize_t k = skip(ve, ")");
    char *debug = NULL;
    if (at(k) == '=' && !(k + 1 < TN && T[k + 1] == '=')) {
        k = skip(k + 1, NULL);
        SB b = {0};
        nocomment(&b, fs + 1, vs);
        for (Py_ssize_t i = vs; i < ve; i++) { sb_cp(&b, T[i]); }
        nocomment(&b, ve, k);
        debug = sb_done(&b);
    }
    Py_UCS4 conv = 0;
    if (at(k) == '!') {
        conv = at(k + 1);
        k += 2;
        while (!wfail && (at(k) == ' ' || at(k) == '\t' || at(k) == '\r' || at(k) == '\n' || at(k) == '\f')) { k++; }
    }
    char *spec = NULL;
    if (at(k) == ':') {
        if (!spec_n) { fail("ValueError: a ':' with no format spec"); free(debug); return xs("?"); }
        Fields sf = {0};
        fields_of(spec_n->v.JoinedStr.values, &sf);
        V items = {0};
        fbody(k + 1, NULL, fe - 1, raw, &sf, &items);
        free(sf.v);
        spec = vec(&items);
    }
    if (conv && conv != 's' && conv != 'r' && conv != 'a') { fail("ValueError: conversion"); }
    int want = conv == 0 ? -1 : (int)conv;
    int implied = want == -1 && debug && !spec_n;
    if (conversion != want && !(implied && conversion == 114)) { fail("ValueError: the conversion written is not the ast's"); }
    F x = {0};
    if (interp) {
        x.head = "interpolation";
        F_f(&x, expr(value));
        F_f(&x, qstr_o(f->v.Interpolation.str));
    } else {
        x.head = "formatted-value";
        F_f(&x, expr(value));
    }
    char cv[8] = {0};
    cv[0] = (char)conv;
    F_o(&x, ":conversion", conv ? xs(cv) : NULL);
    F_o(&x, ":format-spec", spec);
    F_o(&x, ":debug", debug ? qstr_c(debug) : NULL);
    free(debug);
    return posit(form(&x), f->lineno, f->col_offset, f->end_lineno, f->end_col_offset, NULL);
}

/* strings: a Constant str or bytes, a JoinedStr or a TemplateStr, its tokens scanned in the source (§3.2-§3.5) */
static char *strings(expr_ty n)
{
    Py_ssize_t a = sindex(n->lineno, n->col_offset), z = sindex(n->end_lineno, n->end_col_offset);
    Fields fs = {0};
    if (n->kind == JoinedStr_kind) { fields_of(n->v.JoinedStr.values, &fs); }
    else if (n->kind == TemplateStr_kind) { fields_of(n->v.TemplateStr.values, &fs); }
    V parts = {0};
    Py_ssize_t i = a;
    while (i < z && !wfail) {
        Py_UCS4 c = at(i);
        if (c == ' ' || c == '\t' || c == '\f' || c == '\r' || c == '\n') { i++; continue; }
        if (c == '\\') { i++; continue; }
        if (c == '#') { while (i < z && T[i] != '\r' && T[i] != '\n') { i++; } continue; }
        Py_ssize_t j = i;
        while (!wfail && is_prefix_char(at(j))) { j++; }
        char quote[4] = {0};
        if (startswith(j, "\"\"\"")) { strcpy(quote, "\"\"\""); }
        else if (startswith(j, "'''")) { strcpy(quote, "'''"); }
        else if (at(j) == '"' || at(j) == '\'') { quote[0] = (char)T[j]; }
        else { fail("ValueError: no string token at %zd", i); break; }
        Py_ssize_t k = j + (Py_ssize_t)strlen(quote);
        int f = lower_has(i, j, 'f'), t = lower_has(i, j, 't'), b = lower_has(i, j, 'b'), r = lower_has(i, j, 'r');
        char *prefix = qstr_t(i, j);
        int plen = (int)(j - i);
        if (f || t) {
            V items = {0};
            k = fbody(k, quote, -1, r, &fs, &items);
            F x = {.head = f ? "joined-str" : "template-str"};
            F_f(&x, vec(&items));
            int dflt = plen == 1 && (T[i] == (f ? 'f' : 't'));
            F_o(&x, ":prefix", dflt ? NULL : xs(prefix));
            F_o(&x, ":quote", strcmp(quote, "\"") ? qstr_c(quote) : NULL);
            V_add(&parts, form(&x));
        } else {
            Py_ssize_t b0 = k;
            while (!wfail && !startswith(k, quote)) { k += at(k) == '\\' ? 2 : 1; }
            Py_ssize_t b1 = k;
            k += (Py_ssize_t)strlen(quote);
            int hasbs = 0;
            for (Py_ssize_t q = b0; q < b1; q++) { if (T[q] == '\\') { hasbs = 1; } }
            if (b) {
                F x = {.head = "bytes"};
                F_f(&x, qstr_t(b0, b1));
                int dflt = plen == 1 && T[i] == 'b';
                F_o(&x, ":prefix", dflt ? NULL : xs(prefix));
                F_o(&x, ":quote", strcmp(quote, "\"") ? qstr_c(quote) : NULL);
                V_add(&parts, form(&x));
            } else if (plen == 0 && !strcmp(quote, "\"") && !hasbs) {
                V_add(&parts, qstr_t(b0, b1));
            } else {
                F x = {.head = "str"};
                F_f(&x, qstr_t(b0, b1));
                F_o(&x, ":prefix", plen ? xs(prefix) : NULL);
                F_o(&x, ":quote", strcmp(quote, "\"") ? qstr_c(quote) : NULL);
                V_add(&parts, form(&x));
            }
        }
        free(prefix);
        i = k;
    }
    free(fs.v);
    if (!wfail && parts.n == 0) { fail("ValueError: no string token in the node's span"); }
    if (wfail) { for (int q = 0; q < parts.n; q++) { free(parts.v[q]); } free(parts.v); return xs("?"); }
    if (parts.n == 1) { char *s = parts.v[0]; free(parts.v); return s; }
    SB sb = {0};
    sb_s(&sb, "(str-concat");
    for (int q = 0; q < parts.n; q++) { sb_s(&sb, " "); sb_s(&sb, parts.v[q]); free(parts.v[q]); }
    free(parts.v);
    sb_s(&sb, ")");
    return sb_done(&sb);
}

static char *gens(asdl_comprehension_seq *gs)
{
    V v = {0};
    for (Py_ssize_t i = 0; i < asdl_seq_LEN(gs); i++) {
        comprehension_ty g = asdl_seq_GET(gs, i);
        F x = {.head = "comprehension"};
        F_f(&x, expr(g->target));
        F_f(&x, expr(g->iter));
        F_o(&x, ":ifs", asdl_seq_LEN(g->ifs) ? exprs(g->ifs) : NULL);
        F_o(&x, ":async", g->is_async ? xs("true") : NULL);
        V_add(&v, form(&x));
    }
    return vec(&v);
}

static const char *ctxmark(expr_context_ty c) { return c == Store ? "store" : c == Del ? "del" : NULL; }
static void lmeta(F *x, expr_context_ty c) { const char *m = ctxmark(c); if (m) { F_m(x, ":ctx", xf(":%s", m)); } }

static char *expr(expr_ty n)
{
    F x = {0};
    char *t = NULL;
    switch (n->kind) {
    case Name_kind: {
        const char *s = PyUnicode_AsUTF8(n->v.Name.id);
        const char *cm = ctxmark(n->v.Name.ctx);
        if (not_symbol(s)) { x.head = "name"; F_f(&x, qstr_c(s)); lmeta(&x, n->v.Name.ctx); t = form(&x); }
        else { SB b = {0}; if (cm) { sb_s(&b, "^:"); sb_s(&b, cm); sb_s(&b, " "); } sb_s(&b, s); t = sb_done(&b); }
        return posit(t, n->lineno, n->col_offset, n->end_lineno, n->end_col_offset, "name");
    }
    case Constant_kind: t = w_constant(n); break;
    case JoinedStr_kind: case TemplateStr_kind: t = strings(n); break;
    case BoolOp_kind: x.head = "bool-op"; F_f(&x, xs(n->v.BoolOp.op == And ? "and" : "or")); F_f(&x, exprs(n->v.BoolOp.values)); break;
    case NamedExpr_kind: x.head = "named-expr"; F_f(&x, expr(n->v.NamedExpr.target)); F_f(&x, expr(n->v.NamedExpr.value)); break;
    case BinOp_kind: x.head = "bin-op"; F_f(&x, xs(BINOP[n->v.BinOp.op])); F_f(&x, expr(n->v.BinOp.left)); F_f(&x, expr(n->v.BinOp.right)); break;
    case UnaryOp_kind: x.head = "unary-op"; F_f(&x, xs(UNARY[n->v.UnaryOp.op])); F_f(&x, expr(n->v.UnaryOp.operand)); break;
    case Lambda_kind: x.head = "lambda"; F_f(&x, arguments(n->v.Lambda.args)); F_f(&x, expr(n->v.Lambda.body)); break;
    case IfExp_kind: x.head = "if-exp"; F_f(&x, expr(n->v.IfExp.test)); F_f(&x, expr(n->v.IfExp.body)); F_f(&x, expr(n->v.IfExp.orelse)); break;
    case Dict_kind: {
        x.head = "dict";
        V v = {0};
        for (Py_ssize_t i = 0; i < asdl_seq_LEN(n->v.Dict.keys); i++) { expr_ty k = asdl_seq_GET(n->v.Dict.keys, i); V_add(&v, k ? expr(k) : xs("nil")); }
        F_f(&x, vec(&v));
        F_f(&x, exprs(n->v.Dict.values));
        break;
    }
    case Set_kind: x.head = "set"; F_f(&x, exprs(n->v.Set.elts)); break;
    case ListComp_kind: x.head = "list-comp"; F_f(&x, expr(n->v.ListComp.elt)); F_f(&x, gens(n->v.ListComp.generators)); break;
    case SetComp_kind: x.head = "set-comp"; F_f(&x, expr(n->v.SetComp.elt)); F_f(&x, gens(n->v.SetComp.generators)); break;
    case GeneratorExp_kind: x.head = "generator-exp"; F_f(&x, expr(n->v.GeneratorExp.elt)); F_f(&x, gens(n->v.GeneratorExp.generators)); break;
    case DictComp_kind: x.head = "dict-comp"; F_f(&x, expr(n->v.DictComp.key)); F_f(&x, expr(n->v.DictComp.value)); F_f(&x, gens(n->v.DictComp.generators)); break;
    case Await_kind: x.head = "await"; F_f(&x, expr(n->v.Await.value)); break;
    case Yield_kind: x.head = "yield"; F_f(&x, n->v.Yield.value ? expr(n->v.Yield.value) : NULL); break;
    case YieldFrom_kind: x.head = "yield-from"; F_f(&x, expr(n->v.YieldFrom.value)); break;
    case Compare_kind: {
        x.head = "compare";
        F_f(&x, expr(n->v.Compare.left));
        V v = {0};
        for (Py_ssize_t i = 0; i < asdl_seq_LEN(n->v.Compare.ops); i++) { V_add(&v, xs(CMP[asdl_seq_GET(n->v.Compare.ops, i)])); }
        F_f(&x, vec(&v));
        F_f(&x, exprs(n->v.Compare.comparators));
        break;
    }
    case Call_kind:
        x.head = "call";
        F_f(&x, expr(n->v.Call.func));
        F_f(&x, exprs(n->v.Call.args));
        F_o(&x, ":keywords", keywords(n->v.Call.keywords));
        break;
    case Attribute_kind: x.head = "attribute"; F_f(&x, expr(n->v.Attribute.value)); F_f(&x, ident(n->v.Attribute.attr)); lmeta(&x, n->v.Attribute.ctx); break;
    case Subscript_kind: x.head = "subscript"; F_f(&x, expr(n->v.Subscript.value)); F_f(&x, expr(n->v.Subscript.slice)); lmeta(&x, n->v.Subscript.ctx); break;
    case Starred_kind: x.head = "starred"; F_f(&x, expr(n->v.Starred.value)); lmeta(&x, n->v.Starred.ctx); break;
    case List_kind: x.head = "list"; F_f(&x, exprs(n->v.List.elts)); lmeta(&x, n->v.List.ctx); break;
    case Tuple_kind: x.head = "tuple"; F_f(&x, exprs(n->v.Tuple.elts)); lmeta(&x, n->v.Tuple.ctx); break;
    case Slice_kind:
        x.head = "slice";
        F_f(&x, n->v.Slice.lower ? expr(n->v.Slice.lower) : NULL);
        F_f(&x, n->v.Slice.upper ? expr(n->v.Slice.upper) : NULL);
        F_f(&x, n->v.Slice.step ? expr(n->v.Slice.step) : NULL);
        break;
    default: fail("ValueError: unknown expression"); return xs("?");
    }
    if (!t) { t = form(&x); }
    return posit(t, n->lineno, n->col_offset, n->end_lineno, n->end_col_offset, "const");
}

/* ---- repr() of a str and of the values tokenize's TokenError carries (the helper's own error messages) */

static void repr_str(SB *b, const Py_UCS4 *u, Py_ssize_t n)
{
    int sq = 0, dq = 0;
    for (Py_ssize_t i = 0; i < n; i++) { if (u[i] == '\'') { sq = 1; } else if (u[i] == '"') { dq = 1; } }
    char q = (sq && !dq) ? '"' : '\'';
    sb_put(b, &q, 1);
    for (Py_ssize_t i = 0; i < n; i++) {
        Py_UCS4 c = u[i];
        char tmp[16];
        if (c == (Py_UCS4)q || c == '\\') { sb_s(b, "\\"); sb_cp(b, c); }
        else if (c == '\t') { sb_s(b, "\\t"); }
        else if (c == '\n') { sb_s(b, "\\n"); }
        else if (c == '\r') { sb_s(b, "\\r"); }
        else if (c < 0x20 || c == 0x7f) { snprintf(tmp, sizeof tmp, "\\x%02x", c); sb_s(b, tmp); }
        else if (c < 0x7f) { sb_cp(b, c); }
        else if (!_PyUnicode_IsPrintable(c)) {
            if (c <= 0xff) { snprintf(tmp, sizeof tmp, "\\x%02x", c); }
            else if (c <= 0xffff) { snprintf(tmp, sizeof tmp, "\\u%04x", c); }
            else { snprintf(tmp, sizeof tmp, "\\U%08x", c); }
            sb_s(b, tmp);
        }
        else { sb_cp(b, c); }
    }
    sb_put(b, &q, 1);
}
static void repr_obj(SB *b, PyObject *o)
{
    if (!o || o == Py_None) { sb_s(b, "None"); return; }
    if (Py_TYPE(o) == &PyLong_Type) { char tmp[32]; snprintf(tmp, sizeof tmp, "%lld", ((IntObject *)o)->v); sb_s(b, tmp); return; }
    if (Py_TYPE(o) == &PyUnicode_Type) {
        Py_ssize_t n = PyUnicode_GET_LENGTH(o);
        Py_UCS4 *u = malloc((n + 1) * sizeof(Py_UCS4));
        for (Py_ssize_t i = 0; i < n; i++) { u[i] = PyUnicode_READ_CHAR(o, i); }
        repr_str(b, u, n);
        free(u);
        return;
    }
    sb_s(b, "?");
}

static void buf_repr(Buf *b, PyObject *o)
{
    SB s = {0};
    repr_obj(&s, o);
    buf_utf8(b, sb_done(&s), (Py_ssize_t)s.n);
    free(s.p);
}

/* ---- tokenize.tokenize's stream (mode :full's :tokens): _tokenize.TokenizerIter with extra tokens over the lines
   detect_encoding leaves, as queue 489's harness runs it */

PyMODINIT_FUNC PyInit__tokenize(void);
static PyTypeObject *iter_type;

typedef struct { int type; Py_ssize_t sl, sc, el, ec; } Tok;

/* the stream's tokens into *OUT; 0, or 1 with the error's message (the helper's HelperError text) in *ERR */
static int token_stream(char **lines, Py_ssize_t *lens, Py_ssize_t nlines, const char *enc, Tok **out, Py_ssize_t *nout, char **err)
{
    ReadlineObject *rl = (ReadlineObject *)obj_new(&Readline_Type, sizeof(ReadlineObject));
    rl->lines = lines; rl->lens = lens; rl->n = nlines; rl->i = 0;
    Kwargs kw;
    memset(&kw, 0, sizeof kw);
    kw.d.ob_base.ob_refcnt = _Py_IMMORTAL_INITIAL_REFCNT;
    kw.d.ob_base.ob_type = &PyDict_Type;
    kw.d.ma_used = 2;
    kw.extra_tokens = Py_True;
    kw.encoding = PyUnicode_FromString(enc);
    PyObject *args = PyTuple_Pack(1, (PyObject *)rl);
    Py_DECREF(rl);
    harness_refused = 0;
    PyObject *it = iter_type->tp_new(iter_type, args, (PyObject *)&kw);
    Py_DECREF(args);
    Py_DECREF(kw.encoding);
    if (!it) { fatal("TokenizerIter() failed"); }
    Py_ssize_t cap = 256, n = 0;
    Tok *ts = malloc(cap * sizeof(Tok));
    int bad = 0;
    for (;;) {
        PyObject *t = iter_type->tp_iternext(it);
        if (!t) {
            PyObject *e = CUR;
            if (!e || PyErr_ExceptionMatches(PyExc_StopIteration)) { PyErr_Clear(); break; }
            SB b = {0};
            if (harness_refused) { sb_s(&b, "the harness has no decoder for "); sb_s(&b, harness_refused_codec); }
            else if (Py_TYPE(e) == &SyntaxError_T) {
                /* raise TokenError(_transform_msg(e.msg), (e.lineno, e.offset)) */
                ExcObject *x = (ExcObject *)e;
                sb_s(&b, "TokenError: (");
                if (x->msg && Py_TYPE(x->msg) == &PyUnicode_Type
                    && strstr(PyUnicode_AsUTF8(x->msg), "unterminated triple-quoted string literal")) {
                    sb_s(&b, "'EOF in multi-line string'");
                } else {
                    repr_obj(&b, x->msg);
                }
                sb_s(&b, ", ("); repr_obj(&b, x->lineno); sb_s(&b, ", "); repr_obj(&b, x->offset); sb_s(&b, "))");
            } else {
                sb_s(&b, Py_TYPE(e)->tp_name); sb_s(&b, ": the harness's tokenize stream");
            }
            *err = sb_done(&b);
            PyErr_Clear();
            bad = 1;
            break;
        }
        if (n == cap) { cap *= 2; ts = realloc(ts, cap * sizeof(Tok)); }
        PyObject *st = PyTuple_GET_ITEM(t, 2), *en = PyTuple_GET_ITEM(t, 3);
        ts[n].type = (int)((IntObject *)PyTuple_GET_ITEM(t, 0))->v;
        ts[n].sl = ((IntObject *)PyTuple_GET_ITEM(st, 0))->v; ts[n].sc = ((IntObject *)PyTuple_GET_ITEM(st, 1))->v;
        ts[n].el = ((IntObject *)PyTuple_GET_ITEM(en, 0))->v; ts[n].ec = ((IntObject *)PyTuple_GET_ITEM(en, 1))->v;
        n++;
        Py_DECREF(t);
    }
    Py_DECREF(it);
    *out = ts;
    *nout = n;
    return bad;
}

/* GAP: (?:[ \t\f]|\\(?:\r\n|\r|\n))*\Z */
static int is_gap(Py_ssize_t a, Py_ssize_t z)
{
    if (z > TN) { z = TN; }
    Py_ssize_t i = a;
    while (i < z) {
        if (T[i] == ' ' || T[i] == '\t' || T[i] == '\f') { i++; }
        else if (T[i] == '\\' && i + 1 < z && (T[i + 1] == '\r' || T[i + 1] == '\n')) { i += (T[i + 1] == '\r' && i + 2 < z && T[i + 2] == '\n') ? 3 : 2; }
        else { return 0; }
    }
    return 1;
}
static void gap_error(const char *what, Py_ssize_t a, Py_ssize_t z)
{
    SB b = {0};
    sb_s(&b, "ValueError: ");
    sb_s(&b, what);
    repr_str(&b, T + a, (z - a) > 40 ? 40 : z - a);
    if (!wfail) { wfail = sb_done(&b); } else { free(b.p); }
}
static void token_kind(SB *b, int type)
{
    const char *name = _PyParser_TokenNames[type];
    sb_s(b, ":");
    for (const char *p = name; *p; p++) {
        if (*p == '<' || *p == '>') { continue; }
        char c = *p == '_' ? '-' : (*p >= 'A' && *p <= 'Z') ? *p + 32 : *p;
        sb_put(b, &c, 1);
    }
}

/* tokens(data, src): the stream as a flat vector KIND TEXT ..., the text between tokens as :ws (§11) */
static char *tokens(Tok *toks, Py_ssize_t n)
{
    SB b = {0};
    sb_s(&b, "[");
    int first = 1;
    Py_ssize_t prev = 0;
    Py_ssize_t *st = malloc((n + 1) * sizeof(Py_ssize_t));
    for (Py_ssize_t i = 0; i < n; i++) { st[i] = cindex(toks[i].sl, toks[i].sc); }
#define ITEM(s) do { if (!first) { sb_s(&b, " "); } first = 0; char *s_ = (s); sb_s(&b, s_); free(s_); } while (0)
    for (Py_ssize_t i = 0; i < n && !wfail; i++) {
        Py_ssize_t a = st[i] > prev ? st[i] : prev, z;
        if ((toks[i].type == FSTRING_MIDDLE || toks[i].type == TSTRING_MIDDLE) && i + 1 < n) { z = st[i + 1]; }
        else { z = cindex(toks[i].el, toks[i].ec); if (z < a) { z = a; } }
        if (a > prev) {
            if (!is_gap(prev, a)) { gap_error("text between tokens that is not white space: ", prev, a); break; }
            ITEM(xs(":ws")); ITEM(qstr_t(prev, a));
        }
        SB k = {0};
        token_kind(&k, toks[i].type);
        ITEM(sb_done(&k));
        ITEM(qstr_t(a, z));
        prev = z;
    }
    if (!wfail && prev < TN) {
        if (!is_gap(prev, TN)) { gap_error("text after the last token that is not white space: ", prev, TN); }
        else { ITEM(xs(":ws")); ITEM(qstr_t(prev, TN)); }
    }
#undef ITEM
    free(st);
    sb_s(&b, "]");
    return sb_done(&b);
}

/* ---- tokenize.detect_encoding (queue 489's port) */

static int cookie(const char *line, Py_ssize_t n, char *enc, size_t cap)
{
    Py_ssize_t i = 0;
    while (i < n && (line[i] == ' ' || line[i] == '\t' || line[i] == '\f')) { i++; }
    if (i >= n || line[i] != '#') { return 0; }
    for (i++; i + 7 <= n && line[i] != '\n'; i++) {
        if (memcmp(line + i, "coding", 6) || (line[i + 6] != ':' && line[i + 6] != '=')) { continue; }
        Py_ssize_t j = i + 7;
        while (j < n && (line[j] == ' ' || line[j] == '\t')) { j++; }
        Py_ssize_t k = j;
        while (k < n) {
            char c = line[k];
            if ((c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z') || (c >= '0' && c <= '9') || c == '_' || c == '-' || c == '.') { k++; }
            else { break; }
        }
        if (k > j) {
            size_t l = (size_t)(k - j) < cap - 1 ? (size_t)(k - j) : cap - 1;
            memcpy(enc, line + j, l);
            enc[l] = 0;
            return 1;
        }
    }
    return 0;
}
static int blank(const char *line, Py_ssize_t n)
{
    Py_ssize_t i = 0;
    while (i < n && (line[i] == ' ' || line[i] == '\t' || line[i] == '\f')) { i++; }
    return i >= n || line[i] == '#' || line[i] == '\r' || line[i] == '\n';
}
static void normal_name(const char *orig, char *out, size_t cap)
{
    char e[13];
    size_t k = 0;
    for (; orig[k] && k < 12; k++) { char c = orig[k]; e[k] = (char)((c >= 'A' && c <= 'Z') ? c + 32 : (c == '_' ? '-' : c)); }
    e[k] = 0;
    if (!strcmp(e, "utf-8") || !strncmp(e, "utf-8-", 6)) { snprintf(out, cap, "utf-8"); return; }
    if (!strcmp(e, "latin-1") || !strcmp(e, "iso-8859-1") || !strcmp(e, "iso-latin-1")
        || !strncmp(e, "latin-1-", 8) || !strncmp(e, "iso-8859-1-", 11) || !strncmp(e, "iso-latin-1-", 12)) {
        snprintf(out, cap, "iso-8859-1"); return;
    }
    snprintf(out, cap, "%s", orig);
}
static int check_line(const char *line, Py_ssize_t n, const char *enc, char *msg, size_t cap)
{
    if (memchr(line, 0, n)) { snprintf(msg, cap, "source code cannot contain null bytes"); return 1; }
    char mod[256];
    if (!codec_module(enc, mod, sizeof mod)) { snprintf(msg, cap, "unknown encoding: %s", enc); return 1; }
    int refused;
    PyObject *s = decode_with(mod, line, n, NULL, &refused);
    if (refused) { snprintf(msg, cap, "%s", mod); return 2; }
    if (!s) { PyErr_Clear(); snprintf(msg, cap, "invalid or missing encoding declaration"); return 1; }
    Py_DECREF(s);
    return 0;
}
static int find_cookie(const char *line, Py_ssize_t n, int bom, char *enc, size_t cap, char *msg, size_t mcap)
{
    char raw[256], norm[256], mod[256];
    if (!cookie(line, n, raw, sizeof raw)) { return 0; }
    normal_name(raw, norm, sizeof norm);
    if (!codec_module(norm, mod, sizeof mod)) { snprintf(msg, mcap, "unknown encoding: %s", norm); return -1; }
    if (bom) {
        if (strcmp(norm, "utf-8")) { snprintf(msg, mcap, "encoding problem: utf-8"); return -1; }
        snprintf(enc, cap, "utf-8-sig");
        return 1;
    }
    snprintf(enc, cap, "%s", norm);
    return 1;
}
static int detect_encoding(const char *data, Py_ssize_t n, char *enc, size_t cap, char *msg, size_t mcap)
{
    int bom = 0, r;
    const char *dflt = "utf-8";
    const char *first = data, *p;
    Py_ssize_t flen, slen;
    if (n >= 3 && !memcmp(data, "\xef\xbb\xbf", 3)) { bom = 1; first += 3; dflt = "utf-8-sig"; }
    p = memchr(first, '\n', data + n - first);
    flen = p ? p - first + 1 : data + n - first;
    if (flen == 0) { snprintf(enc, cap, "%s", dflt); return 0; }
    r = find_cookie(first, flen, bom, enc, cap, msg, mcap);
    if (r < 0) { return 1; }
    if (r > 0) { return check_line(first, flen, enc, msg, mcap); }
    if (!blank(first, flen)) { snprintf(enc, cap, "%s", dflt); return check_line(first, flen, dflt, msg, mcap); }
    const char *second = first + flen;
    p = memchr(second, '\n', data + n - second);
    slen = p ? p - second + 1 : data + n - second;
    if (slen == 0) { snprintf(enc, cap, "%s", dflt); return check_line(first, flen, dflt, msg, mcap); }
    r = find_cookie(second, slen, bom, enc, cap, msg, mcap);
    if (r < 0) { return 1; }
    if (r > 0) { return check_line(first, flen + slen, enc, msg, mcap); }
    snprintf(enc, cap, "%s", dflt);
    return check_line(first, flen + slen, dflt, msg, mcap);
}

/* ---- ast.parse: compile(source, filename, "exec", PyCF_ONLY_AST[|PyCF_TYPE_COMMENTS]) as builtin compile runs it */

static mod_ty ast_parse(const char *data, Py_ssize_t n, PyObject *fname, int type_comments, PyArena *arena)
{
    if ((Py_ssize_t)strlen(data) != n) {
        PyErr_SetString(PyExc_SyntaxError, "source code string cannot contain null bytes");
        return NULL;
    }
    PyCompilerFlags cf = _PyCompilerFlags_INIT;
    cf.cf_flags = PyCF_ONLY_AST | (type_comments ? PyCF_TYPE_COMMENTS : 0) | PyCF_SOURCE_IS_UTF8;
    mod_ty mod = _PyParser_ASTFromString(data, fname, Py_file_input, &cf, arena);
    if (!mod) { return NULL; }
    /* _PyCompile_AstPreprocess(mod, filename, &cf, -1, arena, syntax_check_only=1) */
    _PyFutureFeatures future;
    if (!_PyFuture_FromAST(mod, fname, &future)) { return NULL; }
    int flags = future.ff_features | cf.cf_flags;
    if (!_PyAST_Preprocess(mod, arena, fname, _Py_GetConfig()->optimization_level, flags, 1, 0)) { return NULL; }
    return mod;
}

/* error_form: (python/error TYPE "MSG" :pos [L O EL EO] :text "TEXT") */
static void opt_int(SB *b, PyObject *o) { if (!o) { sb_s(b, "nil"); } else { char t[32]; snprintf(t, sizeof t, "%lld", ((IntObject *)o)->v); sb_s(b, t); } }
static char *error_form(PyObject *e)
{
    ExcObject *x = (ExcObject *)e;
    SB b = {0};
    sb_s(&b, "(python/error ");
    sb_s(&b, Py_TYPE(e)->tp_name);
    sb_s(&b, " ");
    PyObject *msg = (x->msg && Py_TYPE(x->msg) == &PyUnicode_Type && PyUnicode_GET_LENGTH(x->msg)) ? Py_NewRef(x->msg) : exc_str(e);
    char *q = qstr_o(msg); sb_s(&b, q); free(q);
    Py_DECREF(msg);
    if (x->lineno) {
        sb_s(&b, " :pos [");
        opt_int(&b, x->lineno); sb_s(&b, " "); opt_int(&b, x->offset); sb_s(&b, " ");
        opt_int(&b, x->end_lineno); sb_s(&b, " "); opt_int(&b, x->end_offset); sb_s(&b, "]");
    }
    if (x->text) { sb_s(&b, " :text "); q = qstr_o(x->text); sb_s(&b, q); free(q); }
    sb_s(&b, ")");
    return sb_done(&b);
}
static char *helper_error(const char *msg)
{
    SB b = {0};
    sb_s(&b, "(python/error HelperError ");
    char *q = qstr_c(msg); sb_s(&b, q); free(q);
    sb_s(&b, ")");
    return sb_done(&b);
}
/* "%s: %s" % (type(e).__name__, e), the helper's report of another exception */
static char *helper_exc(PyObject *e)
{
    if (harness_refused) { char m[300]; snprintf(m, sizeof m, "the harness has no decoder for %s", harness_refused_codec); return helper_error(m); }
    if (Py_TYPE(e) == &RecursionError_T) { return helper_error("recursion"); }
    PyObject *s = exc_str(e);
    SB b = {0};
    sb_s(&b, Py_TYPE(e)->tp_name); sb_s(&b, ": "); sb_s(&b, PyUnicode_AsUTF8(s));
    Py_DECREF(s);
    char *r = helper_error(sb_done(&b));
    free(b.p);
    return r;
}

/* the decoded text (decode(): the codec's text, one leading U+FEFF dropped) and its lines' starts */
static int set_source(const char *data, Py_ssize_t n, const char *enc)
{
    char mod[256];
    if (!codec_module(enc, mod, sizeof mod)) { return 0; }
    int refused;
    PyObject *s = decode_with(mod, data, n, NULL, &refused);
    if (!s) { PyErr_Clear(); return 0; }
    Py_ssize_t len = PyUnicode_GET_LENGTH(s), o = 0;
    if (len && PyUnicode_READ_CHAR(s, 0) == 0xFEFF) { o = 1; }
    TN = len - o;
    T = malloc((TN + 1) * sizeof(Py_UCS4));
    for (Py_ssize_t i = 0; i < TN; i++) { T[i] = PyUnicode_READ_CHAR(s, i + o); }
    T[TN] = 0;
    Py_DECREF(s);
    starts = malloc((TN + 2) * sizeof(Py_ssize_t));
    nstarts = 0;
    starts[nstarts++] = 0;
    for (Py_ssize_t i = 0; i < TN; i++) {
        if (T[i] == '\r' && i + 1 < TN && T[i + 1] == '\n') { starts[nstarts++] = i + 2; i++; }
        else if (T[i] == '\r' || T[i] == '\n') { starts[nstarts++] = i + 1; }
    }
    return 1;
}

/* one file: pydump.py's tree_one; the dump's text into *TEXT, its status returned */
static const char *tree_one(const char *path, const char *rel, char **text)
{
    Py_ssize_t n;
    char *data = slurp(path, &n);
    if (!data) { *text = helper_error("FileNotFoundError"); return "helper-error"; }
    PyObject *fname = PyUnicode_FromString(rel);
    harness_refused = 0;
    wfail = NULL;
    PyArena *arena = _PyArena_New();
    mod_ty tree = ast_parse(data, n, fname, 0, arena);
    const char *status = "ok";
    if (!tree) {
        PyObject *e = PyErr_GetRaisedException();
        if (!harness_refused && (PyErr_GivenExceptionMatches(e, PyExc_SyntaxError) || PyErr_GivenExceptionMatches(e, PyExc_ValueError))) {
            *text = error_form(e); status = "error";
        } else {
            *text = helper_exc(e); status = "helper-error";
        }
        Py_DECREF(e);
        goto done;
    }
    /* the type_comments parse */
    PyArena *arena2 = _PyArena_New();
    ntc = 0;
    int tc_rejected = 0;
    mod_ty tc = ast_parse(data, n, fname, 1, arena2);
    if (!tc) {
        PyObject *e = PyErr_GetRaisedException();
        if (!harness_refused && PyErr_GivenExceptionMatches(e, PyExc_SyntaxError)) { tc_rejected = 1; }
        else { *text = helper_exc(e); Py_DECREF(e); _PyArena_Free(arena2); status = "helper-error"; goto done; }
        Py_DECREF(e);
    } else {
        tc_body(tc->v.Module.body);
    }
    char enc[256], msg[512];
    int r = detect_encoding(data, n, enc, sizeof enc, msg, sizeof msg);
    if (r) {
        char m[600];
        if (r == 2) { snprintf(m, sizeof m, "the harness has no decoder for %s", msg); }
        else { snprintf(m, sizeof m, "SyntaxError: %s", msg); }
        *text = helper_error(m); status = "helper-error";
        _PyArena_Free(arena2);
        goto done;
    }
    if (!set_source(data, n, enc)) {
        *text = helper_error("the harness could not decode the file"); status = "helper-error";
        _PyArena_Free(arena2);
        goto done;
    }
    SB meta = {0};
    char *q = qstr_c(rel);
    sb_s(&meta, ":file "); sb_s(&meta, q); free(q);
    sb_s(&meta, full ? " :mode :full" : " :mode :lines");
    if (tc_rejected) { sb_s(&meta, " :type-comments :rejected"); }
    if (tc && asdl_seq_LEN(tc->v.Module.type_ignores)) {
        V v = {0};
        for (Py_ssize_t i = 0; i < asdl_seq_LEN(tc->v.Module.type_ignores); i++) {
            type_ignore_ty ti = asdl_seq_GET(tc->v.Module.type_ignores, i);
            F x = {.head = "type-ignore"};
            F_f(&x, xf("%d", ti->v.TypeIgnore.lineno));
            F_f(&x, qstr_o(ti->v.TypeIgnore.tag));
            V_add(&v, form(&x));
        }
        char *s = vec(&v);
        sb_s(&meta, " :type-ignores "); sb_s(&meta, s); free(s);
    }
    if (full) {
        if (strcmp(enc, "utf-8") && strcmp(enc, "utf-8-sig")) { sb_s(&meta, " :encoding "); q = qstr_c(enc); sb_s(&meta, q); free(q); }
        if (n >= 3 && !memcmp(data, "\xef\xbb\xbf", 3)) { sb_s(&meta, " :bom true"); }
        /* BytesIO's lines, the BOM stripped from the first (a first line left empty dropped) */
        Py_ssize_t cap = 64, nl = 0;
        char **lines = malloc(cap * sizeof(char *));
        Py_ssize_t *lens = malloc(cap * sizeof(Py_ssize_t));
        for (Py_ssize_t i = 0; i < n; ) {
            const char *e = memchr(data + i, '\n', n - i);
            Py_ssize_t l = e ? e - (data + i) + 1 : n - i;
            if (nl == cap) { cap *= 2; lines = realloc(lines, cap * sizeof(char *)); lens = realloc(lens, cap * sizeof(Py_ssize_t)); }
            lines[nl] = data + i; lens[nl] = l; nl++;
            i += l;
        }
        char **ls = lines; Py_ssize_t *lz = lens, k = nl;
        if (n >= 3 && !memcmp(data, "\xef\xbb\xbf", 3) && k) {
            ls[0] += 3; lz[0] -= 3;
            if (lz[0] == 0) { ls++; lz++; k--; }
        }
        char tenc[256];
        snprintf(tenc, sizeof tenc, "%s", strcmp(enc, "utf-8-sig") ? enc : "utf-8");
        Tok *toks; Py_ssize_t ntok; char *err = NULL;
        if (token_stream(ls, lz, k, tenc, &toks, &ntok, &err)) {
            fail("%s", err);
            free(err);
        } else {
            char *s = tokens(toks, ntok);
            sb_s(&meta, " :tokens "); sb_s(&meta, s); free(s);
        }
        free(toks); free(lines); free(lens);
    }
    if (!wfail) {
        char *b = body(tree->v.Module.body);
        SB out = {0};
        sb_s(&out, "(python/module "); sb_s(&out, b); sb_s(&out, " {"); sb_s(&out, sb_done(&meta)); sb_s(&out, "})");
        free(b);
        *text = sb_done(&out);
    }
    free(meta.p);
    if (wfail) { *text = helper_error(wfail); free(wfail); wfail = NULL; status = "helper-error"; }
    free(T); T = NULL; free(starts); starts = NULL;
    _PyArena_Free(arena2);
done:
    _PyArena_Free(arena);
    Py_DECREF(fname);
    PyErr_Clear();
    free(data);
    return status;
}

static void mkdirs(char *path)
{
    for (char *p = path + 1; *p; p++) {
        if (*p == '/') { *p = 0; mkdir(path, 0777); *p = '/'; }
    }
}

/* ---- unicodedata: normalize("NFKC") and \N{...}'s names (the parser's two uses of the module)

   Modules/unicodedata.c's code for them, copied (CPython 3.14.10; PSF license; Copyright (c) Corporation for National
   Research Initiatives; written by Marc-Andre Lemburg, modified by Fredrik Lundh and Martin v. Loewis): _getrecord_ex,
   get_decomp_record, the canonical ordering sorts, nfd_nfkd, find_nfc_index, nfc_nfkc, is_normalized_quickcheck,
   normalize's NFKC path, the DAWG lookup, find_syllable, parse_hex_code, _getcode and capi_getcode, without the
   3.2.0 database's branches (UCD_Check(self) is false for the module and the capsule). Their tables are the tarball's
   Modules/unicodedata_db.h and unicodename_db.h themselves, read at start from the text (the numbers of each array's
   initializer, its #defines), so the node program carries no copy of the data: compiled in, its initializers made
   the node program 33 MB and cf2g's translation of it went past 4 GiB (queue 494). */

static char *ud_dir;               /* the tarball's Modules/ */

/* the integers of NAME's initializer (NAME[] = { ... };) in TEXT */
static unsigned int *ud_array(const char *text, const char *name, Py_ssize_t *count)
{
    char pat[128];
    snprintf(pat, sizeof pat, " %s[] = {", name);
    const char *p = strstr(text, pat);
    if (!p) { fprintf(stderr, "parseharness: no table %s in unicodedata's headers\n", name); exit(70); }
    p += strlen(pat);
    const char *end = strstr(p, "};");
    Py_ssize_t cap = 1024, n = 0;
    unsigned int *v = malloc(cap * sizeof(unsigned int));
    while (p < end) {
        if (*p >= '0' && *p <= '9') {
            char *q;
            unsigned long x = strtoul(p, &q, 0);
            if (n == cap) { cap *= 2; v = realloc(v, cap * sizeof(unsigned int)); }
            v[n++] = (unsigned int)x;
            p = q;
        } else {
            p++;
        }
    }
    if (count) { *count = n; }
    return v;
}
/* #define NAME VALUE, or static const unsigned int NAME = VALUE; */
static unsigned int ud_const(const char *text, const char *name)
{
    char pat[128];
    snprintf(pat, sizeof pat, "#define %s ", name);
    const char *p = strstr(text, pat);
    if (!p) {
        snprintf(pat, sizeof pat, " %s = ", name);
        p = strstr(text, pat);
    }
    if (!p) { fprintf(stderr, "parseharness: no constant %s in unicodedata's headers\n", name); exit(70); }
    return (unsigned int)strtoul(p + strlen(pat), NULL, 0);
}

static unsigned int UD_SHIFT, UD_DECOMP_SHIFT, UD_COMP_SHIFT, UD_TOTAL_LAST;
static unsigned int *ud_records, *ud_index1, *ud_index2, *ud_decomp_data, *ud_decomp_index1, *ud_decomp_index2;
static unsigned int *ud_comp_index, *ud_comp_data, *ud_nfc_first, *ud_nfc_last;
static unsigned int *packed_name_dawg, *dawg_pos_to_codepoint, *name_aliases, *ud_derived;
static unsigned int aliases_start, aliases_end, named_sequences_start, named_sequences_end;
static Py_ssize_t ud_nderived;
static const char *const derived_name_prefixes[] = { "HANGUL SYLLABLE ", "CJK UNIFIED IDEOGRAPH-", "TANGUT IDEOGRAPH-" };

static void ud_load(void)
{
    char path[4096];
    snprintf(path, sizeof path, "%s/unicodedata_db.h", ud_dir);
    char *db = slurp(path, NULL);
    snprintf(path, sizeof path, "%s/unicodename_db.h", ud_dir);
    char *nm = slurp(path, NULL);
    if (!db || !nm) { fprintf(stderr, "parseharness: cannot read unicodedata's headers in %s\n", ud_dir); exit(70); }
    /* _PyUnicode_Database_Records' initializer is "Records[] = {" */
    ud_records = ud_array(db, "_PyUnicode_Database_Records", NULL);
    UD_SHIFT = ud_const(db, "SHIFT");
    UD_DECOMP_SHIFT = ud_const(db, "DECOMP_SHIFT");
    UD_COMP_SHIFT = ud_const(db, "COMP_SHIFT");
    UD_TOTAL_LAST = ud_const(db, "TOTAL_LAST");
    ud_index1 = ud_array(db, "index1", NULL);
    ud_index2 = ud_array(db, "index2", NULL);
    ud_decomp_data = ud_array(db, "decomp_data", NULL);
    ud_decomp_index1 = ud_array(db, "decomp_index1", NULL);
    ud_decomp_index2 = ud_array(db, "decomp_index2", NULL);
    ud_comp_index = ud_array(db, "comp_index", NULL);
    ud_comp_data = ud_array(db, "comp_data", NULL);
    ud_nfc_first = ud_array(db, "nfc_first", NULL);
    ud_nfc_last = ud_array(db, "nfc_last", NULL);
    packed_name_dawg = ud_array(nm, "packed_name_dawg", NULL);
    dawg_pos_to_codepoint = ud_array(nm, "dawg_pos_to_codepoint", NULL);
    name_aliases = ud_array(nm, "name_aliases", NULL);
    ud_derived = ud_array(nm, "derived_name_ranges", &ud_nderived);
    ud_nderived /= 3;
    aliases_start = ud_const(nm, "aliases_start");
    aliases_end = ud_const(nm, "aliases_end");
    named_sequences_start = ud_const(nm, "named_sequences_start");
    named_sequences_end = ud_const(nm, "named_sequences_end");
    free(db);
    free(nm);
}

/* a record: category, combining, bidirectional, mirrored, east_asian_width, normalization_quick_check */
#define REC_COMBINING(r) ((r)[1])
#define REC_QUICKCHECK(r) ((r)[5])

#define SBase   0xAC00
#define LBase   0x1100
#define VBase   0x1161
#define TBase   0x11A7
#define LCount  19
#define VCount  21
#define TCount  28
#define NCount  (VCount*TCount)
#define SCount  (LCount*NCount)

static const unsigned int *
_getrecord_ex(Py_UCS4 code)
{
    int index;
    if (code >= 0x110000)
        index = 0;
    else {
        index = ud_index1[(code>>UD_SHIFT)];
        index = ud_index2[(index<<UD_SHIFT)+(code&((1<<UD_SHIFT)-1))];
    }

    return &ud_records[index * 6];
}

static void
get_decomp_record(Py_UCS4 code, int *index, int *prefix, int *count)
{
    if (code >= 0x110000) {
        *index = 0;
    }
    else {
        *index = ud_decomp_index1[(code>>UD_DECOMP_SHIFT)];
        *index = ud_decomp_index2[(*index<<UD_DECOMP_SHIFT)+
                               (code&((1<<UD_DECOMP_SHIFT)-1))];
    }

    *count = ud_decomp_data[*index] >> 8;
    *prefix = ud_decomp_data[*index] & 255;

    (*index)++;
}

#define CANONICAL_ORDERING_COUNTING_SORT_THRESHOLD 20

static void
canonical_ordering_sort_insertion(int kind, void *data,
                                  Py_ssize_t start, Py_ssize_t end)
{
    for (Py_ssize_t i = start + 1; i < end; i++) {
        Py_UCS4 code = PyUnicode_READ(kind, data, i);
        unsigned char combining = REC_COMBINING(_getrecord_ex(code));
        Py_ssize_t j = i;

        while (j > start) {
            Py_UCS4 previous = PyUnicode_READ(kind, data, j - 1);
            if (REC_COMBINING(_getrecord_ex(previous)) <= combining) {
                break;
            }
            PyUnicode_WRITE(kind, data, j, previous);
            j--;
        }
        if (j != i) {
            PyUnicode_WRITE(kind, data, j, code);
        }
    }
}

static void
canonical_ordering_sort_counting(int kind, void *data,
                                 Py_ssize_t start, Py_ssize_t end,
                                 Py_UCS4 *sortbuf)
{
    Py_ssize_t counts[256] = {0};
    Py_ssize_t run_length = end - start;
    Py_ssize_t total = 0;

    for (Py_ssize_t i = start; i < end; i++) {
        Py_UCS4 code = PyUnicode_READ(kind, data, i);
        unsigned char combining = REC_COMBINING(_getrecord_ex(code));
        counts[combining]++;
    }

    for (size_t i = 0; i < Py_ARRAY_LENGTH(counts); i++) {
        Py_ssize_t count = counts[i];
        counts[i] = total;
        total += count;
    }

    for (Py_ssize_t i = start; i < end; i++) {
        Py_UCS4 code = PyUnicode_READ(kind, data, i);
        unsigned char combining = REC_COMBINING(_getrecord_ex(code));
        sortbuf[counts[combining]++] = code;
    }
    for (Py_ssize_t i = 0; i < run_length; i++) {
        PyUnicode_WRITE(kind, data, start + i, sortbuf[i]);
    }
}

static PyObject*
nfd_nfkd(PyObject *input, int k)
{
    PyObject *result;
    Py_UCS4 *output;
    Py_ssize_t i, o, osize;
    int input_kind, result_kind;
    const void *input_data;
    void *result_data;
    Py_UCS4 stack[20];
    Py_ssize_t space, isize;
    int index, prefix, count, stackptr;
    unsigned char prev, cur;
    Py_UCS4 *sortbuf = NULL;
    Py_ssize_t sortbuflen = 0;

    stackptr = 0;
    isize = PyUnicode_GET_LENGTH(input);
    space = isize;
    if (space > 10) {
        if (space <= PY_SSIZE_T_MAX - 10)
            space += 10;
    }
    else {
        space *= 2;
    }
    osize = space;
    output = PyMem_Malloc(space * sizeof(Py_UCS4));
    i = o = 0;
    input_kind = PyUnicode_KIND(input);
    input_data = PyUnicode_DATA(input);

    while (i < isize) {
        stack[stackptr++] = PyUnicode_READ(input_kind, input_data, i++);
        while(stackptr) {
            Py_UCS4 code = stack[--stackptr];
            if (space < 3) {
                osize += 10;
                space += 10;
                output = PyMem_Realloc(output, osize*sizeof(Py_UCS4));
            }
            if (SBase <= code && code < (SBase+SCount)) {
                int SIndex = code - SBase;
                int L = LBase + SIndex / NCount;
                int V = VBase + (SIndex % NCount) / TCount;
                int T = TBase + SIndex % TCount;
                output[o++] = L;
                output[o++] = V;
                space -= 2;
                if (T != TBase) {
                    output[o++] = T;
                    space --;
                }
                continue;
            }
            get_decomp_record(code, &index, &prefix, &count);
            if (!count || (prefix && !k)) {
                output[o++] = code;
                space--;
                continue;
            }
            while(count) {
                code = ud_decomp_data[index + (--count)];
                stack[stackptr++] = code;
            }
        }
    }

    result = PyUnicode_FromKindAndData(PyUnicode_4BYTE_KIND,
                                       output, o);
    PyMem_Free(output);
    if (!result)
        return NULL;

    result_kind = PyUnicode_KIND(result);
    result_data = PyUnicode_DATA(result);

    i = 0;
    while (i < o) {
        Py_ssize_t run_length, run_start;
        int needs_sort = 0;

        Py_UCS4 ch = PyUnicode_READ(result_kind, result_data, i);
        prev = REC_COMBINING(_getrecord_ex(ch));
        if (prev == 0) {
            i++;
            continue;
        }

        run_start = i++;
        while (i < o) {
            Py_UCS4 ch = PyUnicode_READ(result_kind, result_data, i);
            cur = REC_COMBINING(_getrecord_ex(ch));
            if (cur == 0) {
                break;
            }
            if (prev > cur) {
                needs_sort = 1;
            }
            prev = cur;
            i++;
        }
        if (!needs_sort) {
            continue;
        }

        run_length = i - run_start;
        if (run_length < CANONICAL_ORDERING_COUNTING_SORT_THRESHOLD) {
            canonical_ordering_sort_insertion(result_kind, result_data,
                                              run_start, i);
            continue;
        }

        if (run_length > sortbuflen) {
            sortbuf = PyMem_Realloc(sortbuf, run_length * sizeof(Py_UCS4));
            sortbuflen = run_length;
        }

        canonical_ordering_sort_counting(result_kind, result_data,
                                         run_start, i, sortbuf);
    }
    PyMem_Free(sortbuf);
    return result;
}

/* struct reindex{int start;short count,index;}: the triples of nfc_first and nfc_last */
static int
find_nfc_index(const unsigned int *nfc, Py_UCS4 code)
{
    unsigned int index;
    for (index = 0; nfc[index * 3]; index++) {
        unsigned int start = nfc[index * 3];
        if (code < start)
            return -1;
        if (code <= start + nfc[index * 3 + 1]) {
            unsigned int delta = code - start;
            return nfc[index * 3 + 2] + delta;
        }
    }
    return -1;
}

static PyObject*
nfc_nfkc(PyObject *input, int k)
{
    PyObject *result;
    int kind;
    const void *data;
    Py_UCS4 *output;
    Py_ssize_t i, i1, o, len;
    int f,l,index,index1,comb;
    Py_UCS4 code;
    Py_ssize_t skipped[20];
    int cskipped = 0;

    result = nfd_nfkd(input, k);
    if (!result)
        return NULL;

    kind = PyUnicode_KIND(result);
    data = PyUnicode_DATA(result);
    len = PyUnicode_GET_LENGTH(result);

    output = PyMem_Malloc((len ? len : 1) * sizeof(Py_UCS4));
    i = o = 0;

  again:
    while (i < len) {
      for (index = 0; index < cskipped; index++) {
          if (skipped[index] == i) {
              skipped[index] = skipped[cskipped-1];
              cskipped--;
              i++;
              goto again;
          }
      }
      code = PyUnicode_READ(kind, data, i);
      if (LBase <= code && code < (LBase+LCount) &&
          i + 1 < len &&
          VBase <= PyUnicode_READ(kind, data, i+1) &&
          PyUnicode_READ(kind, data, i+1) < (VBase+VCount)) {
          int LIndex, VIndex;
          LIndex = code - LBase;
          VIndex = PyUnicode_READ(kind, data, i+1) - VBase;
          code = SBase + (LIndex*VCount+VIndex)*TCount;
          i+=2;
          if (i < len &&
              TBase < PyUnicode_READ(kind, data, i) &&
              PyUnicode_READ(kind, data, i) < (TBase+TCount)) {
              code += PyUnicode_READ(kind, data, i)-TBase;
              i++;
          }
          output[o++] = code;
          continue;
      }

      f = find_nfc_index(ud_nfc_first, code);
      if (f == -1) {
          output[o++] = code;
          i++;
          continue;
      }
      i1 = i+1;
      comb = 0;
      output[o] = PyUnicode_READ(kind, data, i);
      while (i1 < len) {
          Py_UCS4 code1 = PyUnicode_READ(kind, data, i1);
          int comb1 = REC_COMBINING(_getrecord_ex(code1));
          if (comb) {
              if (comb1 == 0)
                  break;
              if (comb >= comb1) {
                  i1++;
                  continue;
              }
          }
          l = find_nfc_index(ud_nfc_last, code1);
          if (l == -1) {
            not_combinable:
              if (comb1 == 0)
                  break;
              comb = comb1;
              i1++;
              continue;
          }
          index = f*UD_TOTAL_LAST + l;
          index1 = ud_comp_index[index >> UD_COMP_SHIFT];
          code = ud_comp_data[(index1<<UD_COMP_SHIFT)+
                           (index&((1<<UD_COMP_SHIFT)-1))];
          if (code == 0)
              goto not_combinable;

          output[o] = code;
          skipped[cskipped++] = i1;
          i1++;
          f = find_nfc_index(ud_nfc_first, output[o]);
          if (f == -1)
              break;
      }
      o++; i++;
    }
    if (o == len) {
        PyMem_Free(output);
        return result;
    }
    Py_DECREF(result);
    result = PyUnicode_FromKindAndData(PyUnicode_4BYTE_KIND,
                                       output, o);
    PyMem_Free(output);
    return result;
}

typedef enum {YES = 0, MAYBE = 1, NO = 2} QuickcheckResult;

static QuickcheckResult
is_normalized_quickcheck(PyObject *input, bool nfc, bool k, bool yes_only)
{
    if (PyUnicode_IS_ASCII(input)) {
        return YES;
    }

    Py_ssize_t i, len;
    int kind;
    const void *data;
    unsigned char prev_combining = 0;

    int quickcheck_shift = (nfc ? 4 : 0) + (k ? 2 : 0);

    QuickcheckResult result = YES;

    i = 0;
    kind = PyUnicode_KIND(input);
    data = PyUnicode_DATA(input);
    len = PyUnicode_GET_LENGTH(input);
    while (i < len) {
        Py_UCS4 ch = PyUnicode_READ(kind, data, i++);
        const unsigned int *record = _getrecord_ex(ch);

        unsigned char combining = REC_COMBINING(record);
        if (combining && prev_combining > combining)
            return NO;
        prev_combining = combining;

        unsigned char quickcheck_whole = REC_QUICKCHECK(record);
        if (yes_only) {
            if (quickcheck_whole & (3 << quickcheck_shift))
                return MAYBE;
        } else {
            switch ((quickcheck_whole >> quickcheck_shift) & 3) {
            case NO:
              return NO;
            case MAYBE:
              result = MAYBE;
            }
        }
    }
    return result;
}

/* unicodedata.normalize(form, unistr), METH_FASTCALL as the module's clinic wrapper; pegen.c asks NFKC alone */
static PyObject *ud_normalize(PyObject *self, PyObject *const *args, Py_ssize_t nargs)
{
    (void)self;
    if (nargs != 2 || Py_TYPE(args[0]) != &PyUnicode_Type || Py_TYPE(args[1]) != &PyUnicode_Type) {
        PyErr_SetString(PyExc_TypeError, "normalize() argument");
        return NULL;
    }
    PyObject *form = args[0], *input = args[1];
    if (PyUnicode_GET_LENGTH(input) == 0) {
        return PyUnicode_FromObject(input);
    }
    if (PyUnicode_CompareWithASCIIString(form, "NFKC") == 0) {
        if (is_normalized_quickcheck(input, true, true, true) == YES) {
            return PyUnicode_FromObject(input);
        }
        return nfc_nfkc(input, 1);
    }
    fatal("unicodedata.normalize: a form the harness does not give");
    return NULL;
}

/* the names */

static const char * const hangul_syllables[][3] = {
    { "G",  "A",   ""   },
    { "GG", "AE",  "G"  },
    { "N",  "YA",  "GG" },
    { "D",  "YAE", "GS" },
    { "DD", "EO",  "N", },
    { "R",  "E",   "NJ" },
    { "M",  "YEO", "NH" },
    { "B",  "YE",  "D"  },
    { "BB", "O",   "L"  },
    { "S",  "WA",  "LG" },
    { "SS", "WAE", "LM" },
    { "",   "OE",  "LB" },
    { "J",  "YO",  "LS" },
    { "JJ", "U",   "LT" },
    { "C",  "WEO", "LP" },
    { "K",  "WE",  "LH" },
    { "T",  "WI",  "M"  },
    { "P",  "YU",  "B"  },
    { "H",  "EU",  "BS" },
    { 0,    "YI",  "S"  },
    { 0,    "I",   "SS" },
    { 0,    0,     "NG" },
    { 0,    0,     "J"  },
    { 0,    0,     "C"  },
    { 0,    0,     "K"  },
    { 0,    0,     "T"  },
    { 0,    0,     "P"  },
    { 0,    0,     "H"  }
};

/* derived_name_ranges: {first, last, prefixid} triples */
static int
find_prefix_id(Py_UCS4 code)
{
    for (int i = 0; i < (int)ud_nderived; i++) {
        if (code < ud_derived[i * 3]) {
            return -1;
        }
        if (code <= ud_derived[i * 3 + 1]) {
            return (int)ud_derived[i * 3 + 2];
        }
    }
    return -1;
}

#define IS_ALIAS(cp) ((cp >= aliases_start) && (cp < aliases_end))
#define IS_NAMED_SEQ(cp) ((cp >= named_sequences_start) && \
                          (cp < named_sequences_end))

static unsigned int
_dawg_decode_varint_unsigned(unsigned int index, unsigned int* result)
{
    unsigned int res = 0;
    unsigned int shift = 0;
    for (;;) {
        unsigned char byte = packed_name_dawg[index];
        res |= (byte & 0x7f) << shift;
        index++;
        shift += 7;
        if (!(byte & 0x80)) {
            *result = res;
            return index;
        }
    }
}

static int
_dawg_match_edge(const char* name, unsigned int namelen, unsigned int size,
                 unsigned int label_offset, unsigned int namepos)
{
    if (size > 1 && namepos + size > namelen) {
        return 0;
    }
    for (unsigned int i = 0; i < size; i++) {
        if (packed_name_dawg[label_offset + i] != Py_TOUPPER(name[namepos + i])) {
            if (i > 0) {
                return -1;
            }
            return 0;
        }
    }
    return 1;
}

static unsigned int
_dawg_decode_node(unsigned int node_offset, bool* final)
{
    unsigned int num;
    node_offset = _dawg_decode_varint_unsigned(node_offset, &num);
    *final = num & 1;
    return node_offset;
}

static bool
_dawg_node_is_final(unsigned int node_offset)
{
    unsigned int num;
    _dawg_decode_varint_unsigned(node_offset, &num);
    return num & 1;
}

static unsigned int
_dawg_node_descendant_count(unsigned int node_offset)
{
    unsigned int num;
    _dawg_decode_varint_unsigned(node_offset, &num);
    return num >> 1;
}

static int
_dawg_decode_edge(bool is_first_edge, unsigned int prev_target_node_offset,
                  unsigned int edge_offset, unsigned int* size,
                  unsigned int* label_offset, unsigned int* target_node_offset)
{
    unsigned int num;
    edge_offset = _dawg_decode_varint_unsigned(edge_offset, &num);
    if (num == 0 && is_first_edge) {
        return -1;
    }
    bool last_edge = num & 1;
    num >>= 1;
    bool len_is_one = num & 1;
    num >>= 1;
    *target_node_offset = prev_target_node_offset + num;
    if (len_is_one) {
        *size = 1;
    } else {
        *size = packed_name_dawg[edge_offset++];
    }
    *label_offset = edge_offset;
    return last_edge;
}

static int
_lookup_dawg_packed(const char* name, unsigned int namelen)
{
    unsigned int stringpos = 0;
    unsigned int node_offset = 0;
    unsigned int result = 0;
    while (stringpos < namelen) {
        bool final;
        unsigned int edge_offset = _dawg_decode_node(node_offset, &final);
        unsigned int prev_target_node_offset = edge_offset;
        bool is_first_edge = true;
        for (;;) {
            unsigned int size;
            unsigned int label_offset, target_node_offset;
            int last_edge = _dawg_decode_edge(
                    is_first_edge, prev_target_node_offset, edge_offset,
                    &size, &label_offset, &target_node_offset);
            if (last_edge == -1) {
                return -1;
            }
            is_first_edge = false;
            prev_target_node_offset = target_node_offset;
            int matched = _dawg_match_edge(name, namelen, size, label_offset, stringpos);
            if (matched == -1) {
                return -1;
            }
            if (matched) {
                if (final)
                    result += 1;
                stringpos += size;
                node_offset = target_node_offset;
                break;
            }
            if (last_edge) {
                return -1;
            }
            result += _dawg_node_descendant_count(target_node_offset);
            edge_offset = label_offset + size;
        }
    }
    if (_dawg_node_is_final(node_offset)) {
        return result;
    }
    return -1;
}

static void
find_syllable(const char *str, int *len, int *pos, int count, int column)
{
    int i, len1;
    *len = -1;
    for (i = 0; i < count; i++) {
        const char *s = hangul_syllables[i][column];
        len1 = (int)strlen(s);
        if (len1 <= *len)
            continue;
        if (PyOS_mystrnicmp(str, s, len1) == 0) {
            *len = len1;
            *pos = i;
        }
    }
    if (*len == -1) {
        *len = 0;
    }
}

static int
_check_alias_and_seq(Py_UCS4* code, int with_named_seq)
{
    if (!with_named_seq && IS_NAMED_SEQ(*code))
        return 0;
    if (IS_ALIAS(*code))
        *code = name_aliases[*code-aliases_start];
    return 1;
}

static Py_UCS4
parse_hex_code(const char *name, int namelen)
{
    if (namelen < 4 || namelen > 6) {
        return (Py_UCS4)-1;
    }
    if (*name == '0') {
        return (Py_UCS4)-1;
    }
    int v = 0;
    while (namelen--) {
        v *= 16;
        Py_UCS1 c = Py_TOUPPER(*name);
        if (c >= '0' && c <= '9') {
            v += c - '0';
        }
        else if (c >= 'A' && c <= 'F') {
            v += c - 'A' + 10;
        }
        else {
            return (Py_UCS4)-1;
        }
        name++;
    }
    if (v > 0x10ffff) {
        return (Py_UCS4)-1;
    }
    return v;
}

static int
_getcode(const char* name, int namelen, Py_UCS4* code)
{
    int i = 0;
    size_t prefixlen;
    for (; i < (int)Py_ARRAY_LENGTH(derived_name_prefixes); i++) {
        const char *prefix = derived_name_prefixes[i];
        prefixlen = strlen(derived_name_prefixes[i]);
        if (PyOS_mystrnicmp(name, prefix, prefixlen) == 0) {
            break;
        }
    }

    if (i == 0) {
        int len, L = -1, V = -1, T = -1;
        const char *pos = name + 16;
        find_syllable(pos, &len, &L, LCount, 0);
        pos += len;
        find_syllable(pos, &len, &V, VCount, 1);
        pos += len;
        find_syllable(pos, &len, &T, TCount, 2);
        pos += len;
        if (L != -1 && V != -1 && T != -1 && pos-name == namelen) {
            *code = SBase + (L*VCount+V)*TCount + T;
            return 1;
        }
        return 0;
    }

    if (i < (int)Py_ARRAY_LENGTH(derived_name_prefixes)) {
        Py_UCS4 v = parse_hex_code(name + prefixlen, namelen - prefixlen);
        if (find_prefix_id(v) != i) {
            return 0;
        }
        *code = v;
        return 1;
    }

    int position = _lookup_dawg_packed(name, (unsigned int)namelen);
    if (position < 0) {
        return 0;
    }
    *code = dawg_pos_to_codepoint[position];
    return 1;
}

/* capi_getcode */
static int
ud_getcode(const char* name, int namelen, Py_UCS4* code, int with_named_seq)
{
    if (!_getcode(name, namelen, code)) {
        return 0;
    }
    return _check_alias_and_seq(code, with_named_seq);
}

static void init_runtime(void)
{
    PyASCIIObject *e = (PyASCIIObject *)&_Py_STR(empty);
    e->ob_base.ob_refcnt = _Py_IMMORTAL_INITIAL_REFCNT;
    e->ob_base.ob_type = &PyUnicode_Type;
    e->length = 0;
    e->hash = -1;
    e->state.kind = PyUnicode_1BYTE_KIND;
    e->state.compact = 1;
    e->state.ascii = 1;
    e->state.statically_allocated = 1;
    expr_ty d = &_PyRuntime.parser.dummy_name;
    d->kind = Name_kind;
    d->v.Name.id = &_Py_STR(empty);
    d->v.Name.ctx = Load;
    d->lineno = 1; d->col_offset = 0; d->end_lineno = 1; d->end_col_offset = 0;
    ud_load();
    ModuleObject *tm = module_exec((PyModuleDef *)PyInit__tokenize());
    iter_type = *(PyTypeObject **)tm->state;
    if (!iter_type) { fatal("no TokenizerIter"); }
}

int main(int argc, char **argv)
{
    const char *base = NULL, *list = NULL, *outd = NULL, *tsvp = NULL;
    for (int i = 1; i < argc; i++) {
        if (!strcmp(argv[i], "--full")) { full = 1; continue; }
        if (i + 1 >= argc) { fprintf(stderr, "parseharness: %s needs a value\n", argv[i]); return 2; }
        if (!strcmp(argv[i], "--base")) { base = argv[++i]; }
        else if (!strcmp(argv[i], "--list")) { list = argv[++i]; }
        else if (!strcmp(argv[i], "--out")) { outd = argv[++i]; }
        else if (!strcmp(argv[i], "--tsv")) { tsvp = argv[++i]; }
        else if (!strcmp(argv[i], "--encodings")) { encodings_dir = argv[++i]; }
        else { fprintf(stderr, "parseharness: unknown option %s\n", argv[i]); return 2; }
    }
    if (!base || !list || !outd || !encodings_dir) {
        fprintf(stderr, "usage: parseharness --base DIR --list FILE --out DIR --encodings DIR [--tsv FILE] [--full]\n");
        return 2;
    }
    ud_dir = malloc(strlen(encodings_dir) + 32);
    sprintf(ud_dir, "%s/../../Modules", encodings_dir);
    init_runtime();
    Py_ssize_t ln;
    char *names = slurp(list, &ln);
    if (!names) { fprintf(stderr, "parseharness: cannot read %s\n", list); return 1; }
    FILE *tsv = tsvp ? fopen(tsvp, "w") : NULL;
    for (char *rel = names; *rel; ) {
        char *e = strchr(rel, '\n');
        if (e) { *e = 0; }
        if (*rel) {
            char path[8192], dst[8192];
            snprintf(path, sizeof path, "%s/%s", base, rel);
            char *text = NULL;
            const char *status = tree_one(path, rel, &text);
            snprintf(dst, sizeof dst, "%s/%s.ae", outd, rel);
            mkdirs(dst);
            FILE *fp = fopen(dst, "wb");
            if (!fp) { fprintf(stderr, "parseharness: cannot write %s: %s\n", dst, strerror(errno)); return 1; }
            fputs(text, fp);
            fputs("\n", fp);
            fclose(fp);
            free(text);
            if (tsv) { fprintf(tsv, "%s\t%s\n", rel, status); }
        }
        if (!e) { break; }
        rel = e + 1;
    }
    if (tsv) { fclose(tsv); }
    return 0;
}
