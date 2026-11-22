/* tokharness.c: Arbace's own harness around CPython 3.14.10's C tokenizer (queue 489; py-own-judge-1 (a), the user,
   2026-11-20): the tokenizer's sources from the pinned tarball, unmodified (Parser/lexer/, Parser/tokenizer/helpers.c and
   readline_tokenizer.c, Parser/token.c, Python/Python-tokenize.c, Objects/unicodectype.c, Python/pyctype.c), built by ci's
   builder (bin/pyi-clib build pytokenizer) into one node program with this file, which gives them the C API they call
   (PyMem, the exceptions and SyntaxError's arguments, str and bytes in CPython's own layouts, PyUnicode's UTF-8 decoding,
   the module and type machinery Python-tokenize.c's module asks, Py_BuildValue's formats) and runs _tokenize.TokenizerIter
   over each file as tools/pydump/tokdump.py does, writing tokdump's format: lex-check's reference without CPython
   (bin/repr-python lex-check, REPR_PYTHON_LEX_REF=own).

   usage: tokharness --base DIR --list FILE --out DIR [--tsv FILE] [--encodings DIR]
     every file of the list (paths relative to DIR): its encoding found as tokenize.detect_encoding finds it (the BOM,
     PEP 263's cookie; the codec's existence asked of DIR, the tarball's Lib/encodings, as codecs.lookup imports
     encodings.NAME), then TokenizerIter's two streams (extra_tokens True, then False) over its lines decoded with that
     encoding, written as OUT/REL.tok in tokdump.py's format. Decoders: utf-8, latin-1, ascii and the charmap codecs of
     Lib/encodings (their decoding_table read from DIR); another codec is the harness's refusal, a !HelperError line.

   Except where it is copied from CPython (marked; PSF license, LICENSE.md) this file is Arbace's. */
#include "Python.h"
#include "pycore_runtime.h"
#include "pycore_token.h"           // _PyParser_TokenNames
#include "pycore_unicodeobject.h"   // _PyUnicode_IsXidStart
#include "pegen.h"                  // the byte-offset conversions' declarations
#include "errcode.h"
#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <stdarg.h>
#include <sys/stat.h>
#include <errno.h>

/* ---------------------------------------------------------------- the runtime's data */

_PyRuntimeState _PyRuntime;     /* helpers.c's &_Py_ID(filename) takes an address in it; nothing reads it */

static void obj_free(PyObject *op) { free(op); }

PyTypeObject PyType_Type = { PyVarObject_HEAD_INIT(&PyType_Type, 0) .tp_name = "type",
                             .tp_flags = Py_TPFLAGS_TYPE_SUBCLASS };
PyTypeObject PyBaseObject_Type = { PyVarObject_HEAD_INIT(&PyType_Type, 0) .tp_name = "object" };
PyTypeObject PyUnicode_Type = { PyVarObject_HEAD_INIT(&PyType_Type, 0) .tp_name = "str",
                                .tp_flags = Py_TPFLAGS_UNICODE_SUBCLASS, .tp_dealloc = obj_free };
PyTypeObject PyBytes_Type = { PyVarObject_HEAD_INIT(&PyType_Type, 0) .tp_name = "bytes",
                              .tp_flags = Py_TPFLAGS_BYTES_SUBCLASS, .tp_dealloc = obj_free };
static void tuple_dealloc(PyObject *op);
PyTypeObject PyTuple_Type = { PyVarObject_HEAD_INIT(&PyType_Type, 0) .tp_name = "tuple",
                              .tp_flags = Py_TPFLAGS_TUPLE_SUBCLASS, .tp_dealloc = tuple_dealloc };
PyTypeObject PyLong_Type = { PyVarObject_HEAD_INIT(&PyType_Type, 0) .tp_name = "int",
                             .tp_flags = Py_TPFLAGS_LONG_SUBCLASS, .tp_dealloc = obj_free };
PyTypeObject PyBool_Type = { PyVarObject_HEAD_INIT(&PyType_Type, 0) .tp_name = "bool" };
PyTypeObject PyDict_Type = { PyVarObject_HEAD_INIT(&PyType_Type, 0) .tp_name = "dict",
                             .tp_flags = Py_TPFLAGS_DICT_SUBCLASS };
PyTypeObject PyModule_Type = { PyVarObject_HEAD_INIT(&PyType_Type, 0) .tp_name = "module" };
static PyTypeObject Readline_Type = { PyVarObject_HEAD_INIT(&PyType_Type, 0) .tp_name = "readline" };

PyObject _Py_NoneStruct = { .ob_refcnt = _Py_STATIC_IMMORTAL_INITIAL_REFCNT, .ob_type = &PyBaseObject_Type };
PyLongObject _Py_TrueStruct = { PyObject_HEAD_INIT(&PyBool_Type) };
PyLongObject _Py_FalseStruct = { PyObject_HEAD_INIT(&PyBool_Type) };

/* data the headers' inline functions name (no tokenizer source reads them; the program must not leave them at address 0):
   PyByteArray_AS_STRING's empty buffer, and Py_UNICODE_ISSPACE's table, Objects/unicodeobject.c's _Py_ascii_whitespace
   (CPython 3.14.10; PSF license): 1 for 0x09-0x0D and 0x1C-0x20 */
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

/* the exceptions: a type each, its base as CPython's hierarchy has it (the part the tokenizer asks) */
#define EXC(var, name, base) \
    static PyTypeObject var##_T = { PyVarObject_HEAD_INIT(&PyType_Type, 0) .tp_name = name, \
                                    .tp_flags = Py_TPFLAGS_BASE_EXC_SUBCLASS | Py_TPFLAGS_TYPE_SUBCLASS, .tp_base = base }; \
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
EXC(Warning, "Warning", &Exception_T)
EXC(SyntaxWarning, "SyntaxWarning", &Warning_T)
EXC(DeprecationWarning, "DeprecationWarning", &Warning_T)

/* ---------------------------------------------------------------- memory and objects */

void *PyMem_Malloc(size_t n) { return malloc(n ? n : 1); }
void *PyMem_Calloc(size_t n, size_t s) { return calloc(n ? n : 1, s ? s : 1); }
void *PyMem_Realloc(void *p, size_t n) { return realloc(p, n ? n : 1); }
void PyMem_Free(void *p) { free(p); }

static void fatal(const char *msg) { fprintf(stderr, "tokharness: %s\n", msg); exit(70); }
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

/* PyLong: the value after the header (the tokenizer only boxes and passes them) */
typedef struct { PyObject_HEAD long long v; } IntObject;
static PyObject *int_new(long long v) { IntObject *o = (IntObject *)obj_new(&PyLong_Type, sizeof(IntObject)); o->v = v; return (PyObject *)o; }
PyObject *PyLong_FromLong(long v) { return int_new(v); }
PyObject *PyLong_FromSsize_t(Py_ssize_t v) { return int_new(v); }

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

/* tuples: CPython's layout */
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

/* UTF-8's decoder: errors "strict" (NULL) or "replace", one U+FFFD a maximal subpart as CPython's decoder gives it;
   strict: UnicodeDecodeError set, NULL */
static Py_ssize_t utf8_seq(const unsigned char *s, Py_ssize_t n, Py_UCS4 *out)
{
    /* the length of a valid sequence at s (out its code point), or minus the length of the maximal subpart to replace */
    unsigned c = s[0];
    if (c < 0x80) { *out = c; return 1; }
    int need; Py_UCS4 cp; unsigned lo = 0x80, hi = 0xBF;
    if (c >= 0xC2 && c <= 0xDF) { need = 1; cp = c & 0x1F; }
    else if (c >= 0xE0 && c <= 0xEF) { need = 2; cp = c & 0x0F; if (c == 0xE0) { lo = 0xA0; } else if (c == 0xED) { hi = 0x9F; } }
    else if (c >= 0xF0 && c <= 0xF4) { need = 3; cp = c & 0x07; if (c == 0xF0) { lo = 0x90; } else if (c == 0xF4) { hi = 0x8F; } }
    else { return -1; }
    for (int i = 1; i <= need; i++) {
        if (i >= n) { return -i; }
        unsigned d = s[i];
        if (d < lo || d > hi) { return -i; }
        lo = 0x80; hi = 0xBF;
        cp = (cp << 6) | (d & 0x3F);
    }
    *out = cp;
    return need + 1;
}

static PyObject *decode_utf8(const char *s, Py_ssize_t n, const char *errors)
{
    Py_UCS4 *u = malloc((n + 1) * sizeof(Py_UCS4));
    Py_ssize_t k = 0, i = 0;
    while (i < n) {
        Py_UCS4 cp;
        Py_ssize_t l = utf8_seq((const unsigned char *)s + i, n - i, &cp);
        if (l > 0) { u[k++] = cp; i += l; continue; }
        if (errors == NULL || strcmp(errors, "replace") != 0) {
            free(u);
            PyErr_Format(PyExc_UnicodeDecodeError, "'utf-8' codec can't decode byte 0x%02x in position %zd", (unsigned char)s[i], i);
            return NULL;
        }
        u[k++] = 0xFFFD; i += -l;
    }
    PyObject *r = str_from_ucs4(u, k);
    free(u);
    return r;
}

PyObject *PyUnicode_DecodeUTF8(const char *s, Py_ssize_t n, const char *errors) { return decode_utf8(s, n, errors); }
PyObject *PyUnicode_FromStringAndSize(const char *s, Py_ssize_t n) { return decode_utf8(s, n, NULL); }
PyObject *PyUnicode_FromString(const char *s) { return decode_utf8(s, (Py_ssize_t)strlen(s), NULL); }

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

/* Parser/pegen.c's three byte-offset conversions Python-tokenize.c calls, copied (CPython 3.14.10; PSF license): pegen.c
   is the generated parser's support, not built */
Py_ssize_t
_PyPegen_byte_offset_to_character_offset_line(PyObject *line, Py_ssize_t col_offset, Py_ssize_t end_col_offset)
{
    const unsigned char *data = (const unsigned char*)PyUnicode_AsUTF8(line);

    Py_ssize_t len = 0;
    while (col_offset < end_col_offset) {
        Py_UCS4 ch = data[col_offset];
        if (ch < 0x80) {
            col_offset += 1;
        } else if ((ch & 0xe0) == 0xc0) {
            col_offset += 2;
        } else if ((ch & 0xf0) == 0xe0) {
            col_offset += 3;
        } else if ((ch & 0xf8) == 0xf0) {
            col_offset += 4;
        } else {
            PyErr_SetString(PyExc_ValueError, "Invalid UTF-8 sequence");
            return -1;
        }
        len++;
    }
    return len;
}

Py_ssize_t
_PyPegen_byte_offset_to_character_offset_raw(const char* str, Py_ssize_t col_offset)
{
    Py_ssize_t len = (Py_ssize_t)strlen(str);
    if (col_offset > len + 1) {
        col_offset = len + 1;
    }
    assert(col_offset >= 0);
    PyObject *text = PyUnicode_DecodeUTF8(str, col_offset, "replace");
    if (!text) {
        return -1;
    }
    Py_ssize_t size = PyUnicode_GET_LENGTH(text);
    Py_DECREF(text);
    return size;
}

Py_ssize_t
_PyPegen_byte_offset_to_character_offset(PyObject *line, Py_ssize_t col_offset)
{
    const char *str = PyUnicode_AsUTF8(line);
    if (!str) {
        return -1;
    }
    return _PyPegen_byte_offset_to_character_offset_raw(str, col_offset);
}

/* ---------------------------------------------------------------- PyUnicode_FromFormat (the tokenizer's formats) */

typedef struct { Py_UCS4 *u; Py_ssize_t n, cap; } Buf;
static void buf_put(Buf *b, Py_UCS4 c)
{
    if (b->n == b->cap) { b->cap = b->cap ? b->cap * 2 : 64; b->u = realloc(b->u, b->cap * sizeof(Py_UCS4)); }
    b->u[b->n++] = c;
}
static void buf_utf8(Buf *b, const char *s, Py_ssize_t n)
{
    PyObject *t = decode_utf8(s, n, "replace");
    int kind = PyUnicode_KIND(t);
    const void *data = PyUnicode_DATA(t);
    for (Py_ssize_t i = 0; i < PyUnicode_GET_LENGTH(t); i++) { buf_put(b, PyUnicode_READ(kind, data, i)); }
    Py_DECREF(t);
}
static void buf_str(Buf *b, PyObject *t)
{
    int kind = PyUnicode_KIND(t);
    const void *data = PyUnicode_DATA(t);
    for (Py_ssize_t i = 0; i < PyUnicode_GET_LENGTH(t); i++) { buf_put(b, PyUnicode_READ(kind, data, i)); }
}

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
        case 'd': case 'i': case 'u': case 'x': case 'X': {
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
        case 'U': case 'S': buf_str(&b, va_arg(ap, PyObject *)); break;
        case 'V': {
            PyObject *o = va_arg(ap, PyObject *);
            const char *s = va_arg(ap, const char *);
            if (o) { buf_str(&b, o); } else { buf_utf8(&b, s, (Py_ssize_t)strlen(s)); }
            break;
        }
        default: fatal("PyUnicode_FromFormat: a format the harness does not give");
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

/* ---------------------------------------------------------------- the codecs */

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
                Py_ssize_t l = utf8_seq((const unsigned char *)q, 4, &c);
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
    if (!strcmp(mod, "utf_8")) { return decode_utf8(s, n, errors); }
    if (!strcmp(mod, "utf_8_sig")) {
        if (n >= 3 && !memcmp(s, "\xef\xbb\xbf", 3)) { s += 3; n -= 3; }
        return decode_utf8(s, n, errors);
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
                PyErr_Format(PyExc_UnicodeDecodeError, "'%s' codec can't decode byte 0x%02x in position %zd", mod, (unsigned char)s[i], i);
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

/* ---------------------------------------------------------------- the error state */

static PyObject *err_type, *err_value;
/* a SyntaxError's attributes, set by PyErr_SyntaxLocationObject on the raised one (NULL: not set) */
static PyObject *se_lineno, *se_offset, *se_end_lineno, *se_end_offset, *se_text;
static int se_located;

static void clear_located(void)
{
    Py_CLEAR(se_lineno); Py_CLEAR(se_offset); Py_CLEAR(se_end_lineno); Py_CLEAR(se_end_offset); Py_CLEAR(se_text);
    se_located = 0;
}

void PyErr_Restore(PyObject *t, PyObject *v, PyObject *tb)
{
    Py_XDECREF(err_type); Py_XDECREF(err_value); Py_XDECREF(tb);
    err_type = t; err_value = v;
    clear_located();
}
void PyErr_SetObject(PyObject *t, PyObject *v) { Py_XINCREF(t); Py_XINCREF(v); PyErr_Restore(t, v, NULL); }
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
PyObject *PyErr_Occurred(void) { return err_type; }
void PyErr_Clear(void) { PyErr_Restore(NULL, NULL, NULL); }
void PyErr_Fetch(PyObject **t, PyObject **v, PyObject **tb)
{
    *t = err_type; *v = err_value; *tb = NULL;
    err_type = err_value = NULL;
    clear_located();
}
static int subclass(PyTypeObject *a, PyTypeObject *b) { for (; a; a = a->tp_base) { if (a == b) { return 1; } } return 0; }
int PyErr_GivenExceptionMatches(PyObject *given, PyObject *exc)
{
    if (!given || !exc) { return 0; }
    PyTypeObject *g = (Py_TYPE(given) == &PyType_Type) ? (PyTypeObject *)given : Py_TYPE(given);
    return subclass(g, (PyTypeObject *)exc);
}
int PyErr_ExceptionMatches(PyObject *exc) { return PyErr_GivenExceptionMatches(err_type, exc); }

/* warnings: tokdump.py runs with warnings.simplefilter("ignore") */
int PyErr_WarnExplicitObject(PyObject *category, PyObject *message, PyObject *filename, int lineno, PyObject *module, PyObject *registry)
{
    (void)category; (void)message; (void)filename; (void)lineno; (void)module; (void)registry;
    return 0;
}

/* PyErr_SyntaxLocationObjectEx's attributes (Python/errors.c): lineno, offset (None when negative), end_lineno,
   end_offset (None), filename and the text PyErr_ProgramTextObject reads of the file "<string>": none */
void PyErr_SyntaxLocationObject(PyObject *filename, int lineno, int col_offset)
{
    (void)filename;
    clear_located();
    se_located = 1;
    se_lineno = int_new(lineno);
    se_offset = col_offset >= 0 ? int_new(col_offset) : NULL;
    se_end_lineno = int_new(lineno);
    se_end_offset = NULL;
    se_text = NULL;
}

/* the module and type machinery (Python-tokenize.c's module, its one type) */
typedef struct { PyObject_HEAD PyModuleDef *def; void *state; } ModuleObject;

PyObject *PyModuleDef_Init(PyModuleDef *def) { return (PyObject *)def; }
void *PyModule_GetState(PyObject *m) { return ((ModuleObject *)m)->state; }
int PyModule_AddType(PyObject *m, PyTypeObject *t) { (void)m; (void)t; return 0; }
PyObject *PyType_GetModuleByDef(PyTypeObject *t, PyModuleDef *def) { (void)t; (void)def; return NULL; }

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

PyObject *PyObject_GenericGetAttr(PyObject *o, PyObject *name) { (void)o; (void)name; fatal("getattr"); return NULL; }
PyObject *PyObject_SelfIter(PyObject *o) { Py_INCREF(o); return o; }
int PyObject_SetAttr(PyObject *o, PyObject *name, PyObject *v) { (void)o; (void)name; (void)v; return 0; }
PyObject *PyObject_Str(PyObject *o) { Py_INCREF(o); return o; }
int PyObject_IsTrue(PyObject *o) { return o == Py_True; }

PyObject *Py_GetConstant(unsigned int id)
{
    static PyObject *empty;
    switch (id) {
    case Py_CONSTANT_NONE: return Py_None;
    case Py_CONSTANT_EMPTY_STR:
        if (!empty) { empty = str_new(0, 0); empty->ob_refcnt = _Py_IMMORTAL_INITIAL_REFCNT; }
        return empty;
    default: fatal("Py_GetConstant"); return NULL;
    }
}
PyObject *Py_GetConstantBorrowed(unsigned int id) { return Py_GetConstant(id); }

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
void _PyArg_BadArgument(const char *fname, const char *displayname, const char *expected, PyObject *arg)
{
    (void)fname; (void)displayname; (void)expected; (void)arg;
    PyErr_SetString(PyExc_TypeError, "bad argument");
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
    case 'O': o = va_arg(*ap, PyObject *); Py_INCREF(o); return o;
    case 'N': return va_arg(*ap, PyObject *);
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

/* ---------------------------------------------------------------- tokdump.py's format */

typedef struct { char *b; size_t n, cap; } Out;
static void out_bytes(Out *o, const char *s, size_t n)
{
    if (o->n + n + 1 > o->cap) { while (o->n + n + 1 > o->cap) { o->cap = o->cap ? o->cap * 2 : 65536; } o->b = realloc(o->b, o->cap); }
    memcpy(o->b + o->n, s, n);
    o->n += n;
}
static void out_s(Out *o, const char *s) { out_bytes(o, s, strlen(s)); }
static void out_cp(Out *o, Py_UCS4 ch)
{
    char b[4]; int k = 0;
    if (ch < 0x80) { b[k++] = (char)ch; }
    else if (ch < 0x800) { b[k++] = (char)(0xC0 | (ch >> 6)); b[k++] = (char)(0x80 | (ch & 0x3F)); }
    else if (ch < 0x10000) { b[k++] = (char)(0xE0 | (ch >> 12)); b[k++] = (char)(0x80 | ((ch >> 6) & 0x3F)); b[k++] = (char)(0x80 | (ch & 0x3F)); }
    else { b[k++] = (char)(0xF0 | (ch >> 18)); b[k++] = (char)(0x80 | ((ch >> 12) & 0x3F)); b[k++] = (char)(0x80 | ((ch >> 6) & 0x3F)); b[k++] = (char)(0x80 | (ch & 0x3F)); }
    out_bytes(o, b, k);
}
/* tokdump.py's esc */
static void out_esc(Out *o, PyObject *s)
{
    int kind = PyUnicode_KIND(s);
    const void *data = PyUnicode_DATA(s);
    for (Py_ssize_t i = 0; i < PyUnicode_GET_LENGTH(s); i++) {
        Py_UCS4 ch = PyUnicode_READ(kind, data, i);
        char tmp[16];
        if (ch == '\\') { out_s(o, "\\\\"); }
        else if (ch == '\n') { out_s(o, "\\n"); }
        else if (ch == '\r') { out_s(o, "\\r"); }
        else if (ch == '\t') { out_s(o, "\\t"); }
        else if (ch < 0x20 || ch == 0x7F) { snprintf(tmp, sizeof tmp, "\\u%04X", (unsigned)ch); out_s(o, tmp); }
        else { out_cp(o, ch); }
    }
}
static void out_esc_c(Out *o, const char *s) { PyObject *t = PyUnicode_FromString(s); out_esc(o, t); Py_DECREF(t); }
static void out_opt(Out *o, PyObject *v)
{
    char tmp[32];
    if (!v || v == Py_None) { out_s(o, "-"); return; }
    if (Py_TYPE(v) == &PyLong_Type) { snprintf(tmp, sizeof tmp, "%lld", ((IntObject *)v)->v); out_s(o, tmp); return; }
    fatal("an attribute neither an int nor None");
}

/* the SyntaxError's line: !CLASS L O EL EO MSG TEXT, as SyntaxError.__init__ sets its attributes from its arguments
   (msg, or (msg, (filename, lineno, offset, text[, end_lineno, end_offset]))) and PyErr_SyntaxLocationObject after */
static void out_error(Out *o, const char *cls, PyObject *value)
{
    PyObject *msg = NULL, *lineno = NULL, *offset = NULL, *end_lineno = NULL, *end_offset = NULL, *text = NULL;
    if (value && Py_TYPE(value) == &PyTuple_Type && PyTuple_GET_SIZE(value) == 2
        && Py_TYPE(PyTuple_GET_ITEM(value, 1)) == &PyTuple_Type) {
        PyObject *info = PyTuple_GET_ITEM(value, 1);
        Py_ssize_t n = PyTuple_GET_SIZE(info);
        msg = PyTuple_GET_ITEM(value, 0);
        if (n >= 4) { lineno = PyTuple_GET_ITEM(info, 1); offset = PyTuple_GET_ITEM(info, 2); text = PyTuple_GET_ITEM(info, 3); }
        if (n >= 6) { end_lineno = PyTuple_GET_ITEM(info, 4); end_offset = PyTuple_GET_ITEM(info, 5); }
    } else if (value && Py_TYPE(value) == &PyTuple_Type && PyTuple_GET_SIZE(value) >= 1) {
        msg = PyTuple_GET_ITEM(value, 0);
    } else {
        msg = value;
    }
    if (se_located) { lineno = se_lineno; offset = se_offset; end_lineno = se_end_lineno; end_offset = se_end_offset; text = se_text; }
    out_s(o, "!"); out_s(o, cls); out_s(o, "\t");
    out_opt(o, lineno); out_s(o, "\t"); out_opt(o, offset); out_s(o, "\t");
    out_opt(o, end_lineno); out_s(o, "\t"); out_opt(o, end_offset); out_s(o, "\t");
    if (msg && Py_TYPE(msg) == &PyUnicode_Type) { out_esc(o, msg); } else { out_s(o, "None"); }
    out_s(o, "\t");
    if (!text || text == Py_None) { out_s(o, "-"); } else { out_esc(o, text); }
}

/* detect_encoding's SyntaxError (a message alone: the attributes None) */
static void out_detect_error(Out *o, const char *msg)
{
    out_s(o, "!SyntaxError\t-\t-\t-\t-\t"); out_esc_c(o, msg); out_s(o, "\t-");
}

PyMODINIT_FUNC PyInit__tokenize(void);
static PyTypeObject *iter_type;

/* one stream: TokenizerIter(readline, extra_tokens=EXTRA, encoding=ENC) over LINES; 1 when it ended in an error,
   -1 when the harness refused (a codec it has no decoder for) */
static int stream(char **lines, Py_ssize_t *lens, Py_ssize_t nlines, const char *enc, int extra, Out *o, long *ntok)
{
    ReadlineObject *rl = (ReadlineObject *)obj_new(&Readline_Type, sizeof(ReadlineObject));
    rl->lines = lines; rl->lens = lens; rl->n = nlines; rl->i = 0;
    Kwargs kw;
    memset(&kw, 0, sizeof kw);
    kw.d.ob_base.ob_refcnt = _Py_IMMORTAL_INITIAL_REFCNT;
    kw.d.ob_base.ob_type = &PyDict_Type;
    kw.d.ma_used = 2;
    kw.extra_tokens = extra ? Py_True : Py_False;
    kw.encoding = PyUnicode_FromString(enc);
    PyObject *args = PyTuple_Pack(1, (PyObject *)rl);
    Py_DECREF(rl);
    harness_refused = 0;
    PyObject *it = iter_type->tp_new(iter_type, args, (PyObject *)&kw);
    Py_DECREF(args);
    Py_DECREF(kw.encoding);
    if (!it) { fatal("TokenizerIter() failed"); }
    int bad = 0;
    for (;;) {
        PyObject *t = iter_type->tp_iternext(it);
        if (!t) {
            if (harness_refused) { bad = -1; PyErr_Clear(); break; }
            /* NULL without an exception is the iterator's end, as StopIteration is (tp_iternext's protocol) */
            if (!err_type || PyErr_ExceptionMatches(PyExc_StopIteration)) { PyErr_Clear(); break; }
            if (PyErr_ExceptionMatches(PyExc_SyntaxError)) {
                out_error(o, ((PyTypeObject *)err_type)->tp_name, err_value);
                out_s(o, "\n");
                PyErr_Clear();
                bad = 1;
                break;
            }
            /* another exception: tokdump.py's helper error */
            out_s(o, "!HelperError\t-\t-\t-\t-\t");
            out_esc_c(o, err_type ? ((PyTypeObject *)err_type)->tp_name : "no exception");
            out_s(o, ": ");
            if (err_value && Py_TYPE(err_value) == &PyUnicode_Type) { out_esc(o, err_value); }
            out_s(o, "\t-\n");
            PyErr_Clear();
            bad = 2;
            break;
        }
        char tmp[128];
        long type = ((IntObject *)PyTuple_GET_ITEM(t, 0))->v;
        PyObject *s = PyTuple_GET_ITEM(t, 1), *st = PyTuple_GET_ITEM(t, 2), *en = PyTuple_GET_ITEM(t, 3);
        const char *name = _PyParser_TokenNames[type];
        if (name[0] == '<') {   /* <ERRORTOKEN>, <ENCODING>: token.tok_name's spelling without the brackets */
            snprintf(tmp, sizeof tmp, "%.*s", (int)strlen(name) - 2, name + 1);
            out_s(o, tmp);
        } else {
            out_s(o, name);
        }
        snprintf(tmp, sizeof tmp, "\t%lld\t%lld\t%lld\t%lld\t",
                 ((IntObject *)PyTuple_GET_ITEM(st, 0))->v, ((IntObject *)PyTuple_GET_ITEM(st, 1))->v,
                 ((IntObject *)PyTuple_GET_ITEM(en, 0))->v, ((IntObject *)PyTuple_GET_ITEM(en, 1))->v);
        out_s(o, tmp);
        out_esc(o, s);
        out_s(o, "\n");
        (*ntok)++;
        Py_DECREF(t);
    }
    Py_DECREF(it);
    return bad;
}

/* ---------------------------------------------------------------- tokenize.detect_encoding */

/* cookie_re: ^[ \t\f]*#.*?coding[:=][ \t]*([-\w.]+) (ASCII), the group into ENC; 1 when it matches */
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

/* blank_re: ^[ \t\f]*(?:[#\r\n]|$) */
static int blank(const char *line, Py_ssize_t n)
{
    Py_ssize_t i = 0;
    while (i < n && (line[i] == ' ' || line[i] == '\t' || line[i] == '\f')) { i++; }
    return i >= n || line[i] == '#' || line[i] == '\r' || line[i] == '\n';
}

/* _get_normal_name */
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

/* check(line, encoding): 0 ok, else the message into MSG (2: the harness has no decoder) */
static int check(const char *line, Py_ssize_t n, const char *enc, char *msg, size_t cap)
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

/* find_cookie: 0 none, 1 found (ENC), -1 its SyntaxError's message in MSG */
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

/* the encoding of DATA as detect_encoding finds it into ENC; 0 ok, 1 its SyntaxError's message in MSG, 2 the harness
   refused (MSG the codec) */
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
    if (r > 0) { return check(first, flen, enc, msg, mcap); }
    if (!blank(first, flen)) { snprintf(enc, cap, "%s", dflt); return check(first, flen, dflt, msg, mcap); }
    const char *second = first + flen;
    p = memchr(second, '\n', data + n - second);
    slen = p ? p - second + 1 : data + n - second;
    if (slen == 0) { snprintf(enc, cap, "%s", dflt); return check(first, flen, dflt, msg, mcap); }
    r = find_cookie(second, slen, bom, enc, cap, msg, mcap);
    if (r < 0) { return 1; }
    if (r > 0) { return check(first, flen + slen, enc, msg, mcap); }
    snprintf(enc, cap, "%s", dflt);
    return check(first, flen + slen, dflt, msg, mcap);
}

/* ---------------------------------------------------------------- one file */

static void mkdirs(char *path)
{
    for (char *p = path + 1; *p; p++) {
        if (*p == '/') { *p = 0; mkdir(path, 0777); *p = '/'; }
    }
}

static const char *one(const char *path, Out *o, long *ntok)
{
    Py_ssize_t n;
    char *data = slurp(path, &n);
    char enc[256], msg[512];
    if (!data) {
        out_s(o, "!HelperError\t-\t-\t-\t-\tFileNotFoundError: ");
        out_esc_c(o, path);
        out_s(o, "\t-\n");
        return "helper-error";
    }
    int r = detect_encoding(data, n, enc, sizeof enc, msg, sizeof msg);
    if (r == 1) { out_detect_error(o, msg); out_s(o, "\n"); free(data); return "error"; }
    if (r == 2) {
        out_s(o, "!HelperError\t-\t-\t-\t-\tthe harness has no decoder for "); out_esc_c(o, msg); out_s(o, "\t-\n");
        free(data);
        return "helper-error";
    }
    if (!strcmp(enc, "utf-8-sig")) { snprintf(enc, sizeof enc, "utf-8"); }
    out_s(o, "@"); out_s(o, enc); out_s(o, "\n");
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
    const char *status = "ok";
    for (int extra = 1; extra >= 0; extra--) {
        out_s(o, extra ? "#extra\n" : "#parser\n");
        int b = stream(ls, lz, k, enc, extra, o, ntok);
        if (b < 0) {
            o->n = 0;
            out_s(o, "!HelperError\t-\t-\t-\t-\tthe harness has no decoder for "); out_esc_c(o, harness_refused_codec); out_s(o, "\t-\n");
            status = "helper-error";
            break;
        }
        if (b) { status = "error"; }
    }
    free(lines); free(lens); free(data);
    return status;
}

int main(int argc, char **argv)
{
    const char *base = NULL, *list = NULL, *outd = NULL, *tsvp = NULL;
    for (int i = 1; i + 1 < argc; i += 2) {
        if (!strcmp(argv[i], "--base")) { base = argv[i + 1]; }
        else if (!strcmp(argv[i], "--list")) { list = argv[i + 1]; }
        else if (!strcmp(argv[i], "--out")) { outd = argv[i + 1]; }
        else if (!strcmp(argv[i], "--tsv")) { tsvp = argv[i + 1]; }
        else if (!strcmp(argv[i], "--encodings")) { encodings_dir = argv[i + 1]; }
        else { fprintf(stderr, "tokharness: unknown option %s\n", argv[i]); return 2; }
    }
    if (!base || !list || !outd || !encodings_dir) {
        fprintf(stderr, "usage: tokharness --base DIR --list FILE --out DIR --encodings DIR [--tsv FILE]\n");
        return 2;
    }
    /* _tokenize's module: its exec slot makes TokenizerIter's type */
    PyModuleDef *def = (PyModuleDef *)PyInit__tokenize();
    ModuleObject *m = (ModuleObject *)obj_new(&PyModule_Type, sizeof(ModuleObject));
    m->def = def;
    m->state = calloc(1, def->m_size);
    for (PyModuleDef_Slot *s = def->m_slots; s && s->slot; s++) {
        if (s->slot == Py_mod_exec && ((int (*)(PyObject *))s->value)((PyObject *)m) < 0) { fatal("_tokenize's exec failed"); }
    }
    iter_type = *(PyTypeObject **)m->state;
    if (!iter_type) { fatal("no TokenizerIter"); }

    Py_ssize_t ln;
    char *names = slurp(list, &ln);
    if (!names) { fprintf(stderr, "tokharness: cannot read %s\n", list); return 1; }
    FILE *tsv = tsvp ? fopen(tsvp, "w") : NULL;
    Out o = {0};
    for (char *rel = names; *rel; ) {
        char *e = strchr(rel, '\n');
        if (e) { *e = 0; }
        if (*rel) {
            char path[8192], dst[8192];
            long ntok = 0;
            snprintf(path, sizeof path, "%s/%s", base, rel);
            o.n = 0;
            const char *status = one(path, &o, &ntok);
            snprintf(dst, sizeof dst, "%s/%s.tok", outd, rel);
            mkdirs(dst);
            FILE *fp = fopen(dst, "wb");
            if (!fp) { fprintf(stderr, "tokharness: cannot write %s: %s\n", dst, strerror(errno)); return 1; }
            fwrite(o.b, 1, o.n, fp);
            fclose(fp);
            if (tsv) { fprintf(tsv, "%s\t%s\t%ld\t0.0000\n", rel, status, ntok); }
        }
        if (!e) { break; }
        rel = e + 1;
    }
    if (tsv) { fclose(tsv); }
    return 0;
}
