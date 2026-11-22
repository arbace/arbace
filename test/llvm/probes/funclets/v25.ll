; verdict: invalid ResumeInst needs to be in a function with a personality.
; rule: llvm/lib/IR/Verifier.cpp:4904 h has no personality; f is valid.
declare i32 @__CxxFrameHandler3(...)
declare void @g()
define void @f() personality ptr @__CxxFrameHandler3 {
 ret void
}
define void @h() {
 resume {ptr, i32} undef
}
