; verdict: valid
; rule: llvm/lib/IR/Verifier.cpp:3077 x86_vectorcallcc is not in the varargs-restricted list.
define x86_vectorcallcc void @f(...) { ret void }
