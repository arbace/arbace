; verdict: invalid functions with 'sret' argument must return void
; rule: llvm/lib/AsmParser/LLParser.cpp:7193 Parameter 0 is sret but the return type is i32.
define i32 @f(ptr sret(i32)) { ret i32 0 }
