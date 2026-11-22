; verdict: invalid immarg attribute only applies to intrinsics
; rule: llvm/lib/IR/Verifier.cpp:2296 immarg on a parameter of a non-intrinsic function; this check precedes verifyParameterAttrs in the loop.
define void @f(i32 immarg %p) { ret void }
