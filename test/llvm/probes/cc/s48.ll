; verdict: invalid swifterror argument for call has mismatched parameter
; rule: llvm/lib/IR/Verifier.cpp:3919 swifterror call argument is a parameter of f that lacks swifterror.
declare void @g(ptr swifterror)
define void @f(ptr %p) {
 call void @g(ptr swifterror %p)
 ret void
}
