; verdict: valid
; rule: llvm/lib/AsmParser/LLParser.cpp:3665 [2 x i32] is a first-class type, a valid argument type.
define void @f([2 x i32] %a) {
 ret void
}
