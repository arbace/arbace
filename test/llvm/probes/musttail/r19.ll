; verdict: invalid musttail call must precede a ret
; rule: llvm/lib/IR/Verifier.cpp:4186 In h the first musttail call is followed by another call, not a ret.
declare void @g(ptr)
define void @f(ptr %x) {
 musttail call void @g(ptr %x)
 ret void
}
define void @h(ptr %x) {
 musttail call void @f(ptr %x)
 musttail call void @g(ptr %x)
 ret void
}
