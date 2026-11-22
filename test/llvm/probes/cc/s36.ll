; verdict: invalid invalid type for function argument
; rule: llvm/lib/AsmParser/LLParser.cpp:3666 label is not a valid argument type (isValidArgumentType).
define void @f(label %x) {
 ret void
}
