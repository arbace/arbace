; verdict: invalid a call with operand bundle "clang.arc.attachedcall" must call a function returning a pointer or a non-returning function that has a void return type
; rule: llvm/lib/IR/Verifier.cpp:7552 verifyAttachedCallBundle: the callee returns void and the call is not noreturn.
declare void @g(i32)
define void @f(i32 %x) {
 call void @g(i32 %x) [ "clang.arc.attachedcall"() ]
 ret void
}
