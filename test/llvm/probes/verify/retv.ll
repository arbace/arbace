; verdict: invalid value doesn't match function result type 'i32'
; rule: llvm/lib/AsmParser/LLParser.cpp:7927 ret void in an i32 function.
define i32 @f() {
  ret void
}
