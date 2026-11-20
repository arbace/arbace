; verdict: valid
; rule: llvm/lib/AsmParser/LLParser.cpp:1747 memory(none) then readonly: the legacy keyword is folded into a memory attribute at the end of the list; no error.
define void @f() memory(none) readonly { ret void }
