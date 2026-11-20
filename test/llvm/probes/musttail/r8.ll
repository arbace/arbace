; verdict: invalid Incompatible argument and return types for 'returned' attribute
; rule: llvm/lib/IR/Verifier.cpp:2315 Declaration @g (visited first) has returned on an i32 param of a void-returning function; i32 cannot be bitcast to void.
declare void @g(i32 returned)
define void @f(i32 %x) {
 musttail call void @g(i32 returned %x)
 ret void
}
