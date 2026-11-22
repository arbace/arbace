; verdict: valid
; rule: llvm/lib/IR/Verifier.cpp:4177 musttail call with identical caller/callee signature, cc and attributes, followed by ret void.
declare void @f(ptr)
define void @g(ptr %p) { musttail call void @f(ptr %p)
 ret void }
