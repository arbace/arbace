; verdict: invalid '%x' defined with type 'i32' but expected 'ptr'
; rule: llvm/lib/AsmParser/LLParser.cpp:1861 Callee %x is an i32 argument but the call needs a ptr callee.
define i32 @f(i32 %x) {
 %r = call i32 %x(i32 1)
 ret i32 %r
}
