; verdict: invalid invalid cast opcode for cast from 'i64' to 'i32'
; rule: llvm/lib/AsmParser/LLParser.cpp:8572 zext i64 to i32 narrows, castIsValid is false in parseCast.
define i32 @f(i64 %c) {
  %x = zext i64 %c to i32
  ret i32 %x
}
