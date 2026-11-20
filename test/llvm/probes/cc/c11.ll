; verdict: valid
; rule: llvm/lib/IR/Verifier.cpp:3077 x86_stdcallcc is not in visitFunction's varargs-restricted list, so varargs is accepted.
define x86_stdcallcc void @f(...) { ret void }
