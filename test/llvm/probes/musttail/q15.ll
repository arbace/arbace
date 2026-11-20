; verdict: valid
; rule: llvm/lib/IR/Verifier.cpp:4143 sret(i32) on both caller param and call argument: ABI attribute sets equal; sret on the first param of a void function is legal.
declare void @g(ptr sret(i32))
define void @f(ptr sret(i32) %x) {
 musttail call void @g(ptr sret(i32) %x)
 ret void
}
