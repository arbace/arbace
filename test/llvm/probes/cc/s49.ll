; verdict: invalid swifterror argument for call has mismatched alloca
; rule: llvm/lib/IR/Verifier.cpp:3912 swifterror call argument is an alloca not marked swifterror.
declare void @g(ptr swifterror)
define void @f() {
 %p = alloca ptr
 call void @g(ptr swifterror %p)
 ret void
}
