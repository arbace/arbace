; verdict: invalid musttail call must precede a ret
; rule: llvm/lib/IR/Verifier.cpp:4186 An add follows the call instead of ret.
declare i32 @g()
define i32 @f() {
 %r = musttail call i32 @g()
 %q = add i32 %r, 0
 ret i32 %q
}
