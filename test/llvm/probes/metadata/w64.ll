; verdict: invalid Expected '!' here
; rule: llvm/lib/AsmParser/LLParser.cpp:1052 parseNamedMetadata takes only !N node references (or an inline DIExpression) as operands; the token after '{' is the type i32.
!llvm.ident = !{i32 1}
