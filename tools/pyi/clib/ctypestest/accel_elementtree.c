/* CPython 3.14.10's Modules/_elementtree.c (the PSF's license, LICENSE.md) as a release build compiles it (py-accel, after
   queue 521's _datetime and _decimal): with NDEBUG, the source included unchanged. Its parser reaches Expat through the
   capsule pyexpat.expat_CAPI, which CPython's Modules/pyexpat.c makes; pyi's pyexpat is a Go port with no C functions, so
   this glue makes that capsule over the world's own Expat (CPython's Modules/expat, built by lib.conf) and the module
   pyctypes makes from PyInit__elementtree finds it there: pyi_expat_install puts it on pyexpat before the PyInit runs.
   template_buffer and PyUnknownEncodingHandler, and the capsule's table in pyi_expat_install, are pyexpat.c's own
   (lines 1459-1520 and 2422-2496 of the same tarball), copied unchanged but for the capsule's attribute set on the module
   pyi already has. */
#define NDEBUG 1
#include "Modules/_elementtree.c"

static const unsigned char template_buffer[256] =
    {0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19,
     20, 21, 22, 23, 24, 25, 26, 27, 28, 29, 30, 31, 32, 33, 34, 35, 36, 37,
     38, 39, 40, 41, 42, 43, 44, 45, 46, 47, 48, 49, 50, 51, 52, 53, 54, 55,
     56, 57, 58, 59, 60, 61, 62, 63, 64, 65, 66, 67, 68, 69, 70, 71, 72, 73,
     74, 75, 76, 77, 78, 79, 80, 81, 82, 83, 84, 85, 86, 87, 88, 89, 90, 91,
     92, 93, 94, 95, 96, 97, 98, 99, 100, 101, 102, 103, 104, 105, 106, 107,
     108, 109, 110, 111, 112, 113, 114, 115, 116, 117, 118, 119, 120, 121, 122,
     123, 124, 125, 126, 127, 128, 129, 130, 131, 132, 133, 134, 135, 136, 137,
     138, 139, 140, 141, 142, 143, 144, 145, 146, 147, 148, 149, 150, 151, 152,
     153, 154, 155, 156, 157, 158, 159, 160, 161, 162, 163, 164, 165, 166, 167,
     168, 169, 170, 171, 172, 173, 174, 175, 176, 177, 178, 179, 180, 181, 182,
     183, 184, 185, 186, 187, 188, 189, 190, 191, 192, 193, 194, 195, 196, 197,
     198, 199, 200, 201, 202, 203, 204, 205, 206, 207, 208, 209, 210, 211, 212,
     213, 214, 215, 216, 217, 218, 219, 220, 221, 222, 223, 224, 225, 226, 227,
     228, 229, 230, 231, 232, 233, 234, 235, 236, 237, 238, 239, 240, 241, 242,
     243, 244, 245, 246, 247, 248, 249, 250, 251, 252, 253, 254, 255};


static int
PyUnknownEncodingHandler(void *encodingHandlerData,
                         const XML_Char *name,
                         XML_Encoding *info)
{
    PyObject *u;
    int i;
    const void *data;
    int kind;

    if (PyErr_Occurred())
        return XML_STATUS_ERROR;

    u = PyUnicode_Decode((const char*) template_buffer, 256, name, "replace");
    if (u == NULL) {
        Py_XDECREF(u);
        return XML_STATUS_ERROR;
    }

    if (PyUnicode_GET_LENGTH(u) != 256) {
        Py_DECREF(u);
        PyErr_SetString(PyExc_ValueError,
                        "multi-byte encodings are not supported");
        return XML_STATUS_ERROR;
    }

    kind = PyUnicode_KIND(u);
    data = PyUnicode_DATA(u);
    for (i = 0; i < 256; i++) {
        Py_UCS4 ch = PyUnicode_READ(kind, data, i);
        if (ch != Py_UNICODE_REPLACEMENT_CHARACTER)
            info->map[i] = ch;
        else
            info->map[i] = -1;
    }

    info->data = NULL;
    info->convert = NULL;
    info->release = NULL;
    Py_DECREF(u);

    return XML_STATUS_OK;
}

static void
pyi_expat_capsule_destructor(PyObject *capsule)
{
    void *p = PyCapsule_GetPointer(capsule, PyExpat_CAPSULE_NAME);
    PyMem_Free(p);
}

/* pyi_expat_install: pyexpat.expat_CAPI over the world's Expat; 0, else -1 with the error set. */
int
pyi_expat_install(void)
{
    PyObject *mod = PyImport_ImportModule("pyexpat");
    if (mod == NULL) {
        return -1;
    }
    struct PyExpat_CAPI *capi = PyMem_Malloc(sizeof(*capi));
    if (capi == NULL) {
        Py_DECREF(mod);
        PyErr_NoMemory();
        return -1;
    }
    /* initialize pyexpat dispatch table */
    capi->size = sizeof(*capi);
    capi->magic = PyExpat_CAPI_MAGIC;
    capi->MAJOR_VERSION = XML_MAJOR_VERSION;
    capi->MINOR_VERSION = XML_MINOR_VERSION;
    capi->MICRO_VERSION = XML_MICRO_VERSION;
    capi->ErrorString = XML_ErrorString;
    capi->GetErrorCode = XML_GetErrorCode;
_Py_COMP_DIAG_PUSH
_Py_COMP_DIAG_IGNORE_DEPR_DECLS
    capi->GetErrorColumnNumber = XML_GetErrorColumnNumber;
    capi->GetErrorLineNumber = XML_GetErrorLineNumber;
_Py_COMP_DIAG_POP
    capi->Parse = XML_Parse;
    capi->ParserCreate_MM = XML_ParserCreate_MM;
    capi->ParserFree = XML_ParserFree;
    capi->SetCharacterDataHandler = XML_SetCharacterDataHandler;
    capi->SetCommentHandler = XML_SetCommentHandler;
    capi->SetDefaultHandlerExpand = XML_SetDefaultHandlerExpand;
    capi->SetElementHandler = XML_SetElementHandler;
    capi->SetNamespaceDeclHandler = XML_SetNamespaceDeclHandler;
    capi->SetProcessingInstructionHandler = XML_SetProcessingInstructionHandler;
    capi->SetUnknownEncodingHandler = XML_SetUnknownEncodingHandler;
    capi->SetUserData = XML_SetUserData;
    capi->SetStartDoctypeDeclHandler = XML_SetStartDoctypeDeclHandler;
    capi->SetEncoding = XML_SetEncoding;
    capi->DefaultUnknownEncodingHandler = PyUnknownEncodingHandler;
#if XML_COMBINED_VERSION >= 20100
_Py_COMP_DIAG_PUSH
_Py_COMP_DIAG_IGNORE_DEPR_DECLS
    capi->SetHashSalt = XML_SetHashSalt;
_Py_COMP_DIAG_POP
#else
    capi->SetHashSalt = NULL;
#endif
#if XML_COMBINED_VERSION >= 20800
    capi->SetHashSalt16Bytes = XML_SetHashSalt16Bytes;
#else
    capi->SetHashSalt16Bytes = NULL;
#endif
#if XML_COMBINED_VERSION >= 20600
    capi->SetReparseDeferralEnabled = XML_SetReparseDeferralEnabled;
#else
    capi->SetReparseDeferralEnabled = NULL;
#endif
#if XML_COMBINED_VERSION >= 20702
    capi->SetAllocTrackerActivationThreshold = XML_SetAllocTrackerActivationThreshold;
    capi->SetAllocTrackerMaximumAmplification = XML_SetAllocTrackerMaximumAmplification;
#else
    capi->SetAllocTrackerActivationThreshold = NULL;
    capi->SetAllocTrackerMaximumAmplification = NULL;
#endif
#if XML_COMBINED_VERSION >= 20400
    capi->SetBillionLaughsAttackProtectionActivationThreshold = XML_SetBillionLaughsAttackProtectionActivationThreshold;
    capi->SetBillionLaughsAttackProtectionMaximumAmplification = XML_SetBillionLaughsAttackProtectionMaximumAmplification;
#else
    capi->SetBillionLaughsAttackProtectionActivationThreshold = NULL;
    capi->SetBillionLaughsAttackProtectionMaximumAmplification = NULL;
#endif


    PyObject *capi_object = PyCapsule_New(capi, PyExpat_CAPSULE_NAME,
                                          pyi_expat_capsule_destructor);
    if (capi_object == NULL) {
        PyMem_Free(capi);
        Py_DECREF(mod);
        return -1;
    }
    int r = PyObject_SetAttrString(mod, "expat_CAPI", capi_object);
    Py_DECREF(capi_object);
    Py_DECREF(mod);
    return r;
}

/* Python/bootstrap_hash.c's _Py_HashSecret (the PSF's license), which _elementtree reads for Expat's hash salt
   (_Py_HashSecret.expat.hashsalt): pyi's hashing is Go's and fills no C secret, so it stays zero, and a zero salt leaves
   Expat to pick its own (expat_config.h's XML_POOR_ENTROPY). */
_Py_HashSecret_t _Py_HashSecret;
