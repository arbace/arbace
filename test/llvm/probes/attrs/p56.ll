; verdict: invalid stack alignment is not a power of two
; rule: llvm/lib/AsmParser/LLParser.cpp:3136 alignstack(3) rejected by parseOptionalStackAlignment.
define void @f() alignstack(3) { ret void }
