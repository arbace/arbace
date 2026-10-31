// CP76 (b): HotSpot read by clang under the JDK build's own flags on musl (doc/cpp/CPP-SURVEY.md, "As built: CP76 (b), CP77 (b)").
// Given by `-include` to clang only (the corpus entry's :clang-args; g++ is not given it).
//
// musl's headers declare the C library without exception specifications. HotSpot (share/utilities/forbiddenFunctions.hpp and
// os/posix/forbiddenFunctions_posix.hpp) redeclares some of those functions `noexcept`: g++ accepts a redeclaration that
// differs in the exception specification, clang rejects it either way ("exception specification in declaration does not match
// previous declaration", "'wcsdup' is missing exception specification 'noexcept'"). While musl's headers are read, each of the
// functions HotSpot redeclares, followed by its parameter list, gets `noexcept`: a function-like macro does not expand itself
// again, so `char *strdup (const char *);` reads `char *strdup (const char *) noexcept;`. The macros end with the headers
// (libstdc++'s <cstdlib> undefines some of them itself), so no use of a function sees them. The headers are read here, first,
// as the includes of the TU would otherwise reach them after HotSpot's declarations.
//
// Nothing else is changed: HotSpot's own sources, the build's flags and the platform's headers are the build's.
#if defined(__clang__) && defined(__cplusplus)
#define strndup(...) strndup(__VA_ARGS__) noexcept
#define strdup(...) strdup(__VA_ARGS__) noexcept
#define strerror(...) strerror(__VA_ARGS__) noexcept
#define strtok(...) strtok(__VA_ARGS__) noexcept
#define wcsdup(...) wcsdup(__VA_ARGS__) noexcept
#define posix_memalign(...) posix_memalign(__VA_ARGS__) noexcept
#define aligned_alloc(...) aligned_alloc(__VA_ARGS__) noexcept
#define realpath(...) realpath(__VA_ARGS__) noexcept
#define get_current_dir_name(...) get_current_dir_name(__VA_ARGS__) noexcept
#define getwd(...) getwd(__VA_ARGS__) noexcept
#define exit(...) exit(__VA_ARGS__) noexcept
#define _Exit(...) _Exit(__VA_ARGS__) noexcept
#define malloc(...) malloc(__VA_ARGS__) noexcept
#define free(...) free(__VA_ARGS__) noexcept
#define calloc(...) calloc(__VA_ARGS__) noexcept
#define realloc(...) realloc(__VA_ARGS__) noexcept
#define sprintf(...) sprintf(__VA_ARGS__) noexcept
#define snprintf(...) snprintf(__VA_ARGS__) noexcept
#define vsprintf(...) vsprintf(__VA_ARGS__) noexcept
#define vsnprintf(...) vsnprintf(__VA_ARGS__) noexcept
#include <string.h>
#include <stdlib.h>
#include <stdio.h>
#include <unistd.h>
#include <wchar.h>
#undef strndup
#undef strdup
#undef strerror
#undef strtok
#undef wcsdup
#undef posix_memalign
#undef aligned_alloc
#undef realpath
#undef get_current_dir_name
#undef getwd
#undef exit
#undef _Exit
#undef malloc
#undef free
#undef calloc
#undef realloc
#undef sprintf
#undef snprintf
#undef vsprintf
#undef vsnprintf
#endif
