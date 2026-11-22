; verdict: invalid musttail call must precede a ret
; rule: llvm/lib/IR/Verifier.cpp:4186 Next is unreachable; ret check precedes parameter counts.
declare void @g(ptr sret(i32))
define void @f(ptr %x, i32 %y) {
 musttail call void @g(ptr sret(i32) %x)
 unreachable
}
