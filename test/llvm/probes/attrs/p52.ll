; verdict: invalid expected '{' in function body
; rule: llvm/lib/AsmParser/LLParser.cpp:7360 alloc_size is not a keyword (the lexer returns an error token), the attribute list ends and the body '{' is expected.
define i32 @f() alloc_size { ret i32 0 }
