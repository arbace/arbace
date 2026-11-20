; verdict: invalid Calling convention parameter requires byval
; rule: llvm/lib/IR/Verifier.cpp:3081 x86_intrcc function with a first parameter lacking byval.
define x86_intrcc void @f(ptr %p) { ret void }
