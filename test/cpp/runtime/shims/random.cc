// CX7 (test/cpp/runtime/units.edn, :shims): libstdc++-v3/src/c++11/random.cc as the oracle compiles it. The installed
// <bits/c++config.h> says g++ found rdrand and rdseed (_GLIBCXX_X86_RDRAND, _GLIBCXX_X86_RDSEED); random.cc then calls g++'s
// builtins __builtin_ia32_rdseed_si_step and its cpuid, which clang does not have and which the lowering would refuse as
// target intrinsics and inline assembly. Without them std::random_device has its other sources (getentropy, /dev/urandom,
// the mt19937 fallback): the tokens "rdrand", "rdseed" and "rdrnd" are not available. The file itself is unchanged.
#include <bits/c++config.h>
#undef _GLIBCXX_X86_RDRAND
#undef _GLIBCXX_X86_RDSEED
#include <random.cc>
