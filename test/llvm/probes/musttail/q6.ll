; verdict: invalid musttail call must precede a ret
; rule: llvm/lib/IR/Verifier.cpp:4186 A bitcast sits between the call and the ret, so Next is not a ret.
declare i32 @g(i32)
define i32 @f(i32 %x) {
 %r = musttail call i32 @g(i32 %x)
 %c = bitcast i32 %r to i32
 ret i32 %c
}
