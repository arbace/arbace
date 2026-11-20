; verdict: invalid musttail call must precede a ret
; rule: llvm/lib/IR/Verifier.cpp:4186 The musttail call is followed by br, not ret.
declare void @g(i32)
define void @f(i32 %x) {
 musttail call void @g(i32 %x)
 br label %b
b:
 ret void
}
