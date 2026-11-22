; verdict: invalid preallocated operand either requires a preallocated bundle or the call to be musttail (but not both)
; rule: llvm/lib/IR/Verifier.cpp:3957 preallocated call argument with no preallocated bundle and a non-musttail call.
declare void @g(ptr, i32)
define void @f(ptr %p) {
 call void @g(ptr preallocated(i32) %p, i32 1)
 ret void
}
