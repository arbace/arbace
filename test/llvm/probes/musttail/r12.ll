; verdict: valid
; rule: llvm/lib/IR/Verifier.cpp:3952 preallocated argument on a musttail call without a preallocated bundle is exactly what the check requires; ABI attributes equal.
declare void @g(ptr preallocated(i32))
define void @f(ptr preallocated(i32) %x) {
 musttail call void @g(ptr preallocated(i32) %x)
 ret void
}
