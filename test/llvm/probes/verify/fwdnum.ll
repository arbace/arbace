; verdict: invalid use of undefined value '%4'
; rule: llvm/lib/AsmParser/LLParser.cpp:3972 Label 3 is accepted, %4 is a forward reference never defined; finishFunction reports it at the closing brace.
define i32 @f() {
  br label %3
3:
  ret i32 %4
}
