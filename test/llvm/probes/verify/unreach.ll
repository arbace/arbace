; verdict: valid
; rule: llvm/lib/IR/Dominators.cpp:180 Block x is unreachable: any use there is dominated, so the use-before-def of %z within it is accepted; forward ref %z is same type i32.
define i32 @f() {
  ret i32 0
x:
  %y = add i32 %z, 1
  %z = add i32 %y, 1
  ret i32 %z
}
