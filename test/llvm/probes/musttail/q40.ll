; verdict: valid
; rule: llvm/lib/IR/Verifier.cpp:4165 The musttail call in block a is followed by ret.
declare void @g(i32)
define void @f(i32 %x) {
 %c = icmp eq i32 %x, 0
 br i1 %c, label %a, label %b
a:
 musttail call void @g(i32 %x)
 ret void
b:
 ret void
}
