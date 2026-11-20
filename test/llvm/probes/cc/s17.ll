; verdict: invalid cannot use argument of naked function
; rule: llvm/lib/IR/Verifier.cpp:3071 naked function whose argument %x is used by the load.
define void @f(ptr %x) naked {
 %y = load i32, ptr %x
 ret void
}
