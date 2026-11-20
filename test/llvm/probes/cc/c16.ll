; verdict: valid
; rule: llvm/lib/IR/Verifier.cpp:3853 x86_intrcc is callable (isCallableCC true) and the byval call argument matches; the verifier has no 'interrupt cannot be called' check.
define x86_intrcc void @f(ptr byval(i32) %p) { ret void }
define void @g(ptr %p) { call x86_intrcc void @f(ptr byval(i32) %p)
 ret void}
