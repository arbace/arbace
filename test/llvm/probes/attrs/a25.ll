; verdict: valid
; rule: llvm/lib/AsmParser/LLParser.cpp:1747 readonly among function attributes is upgraded to memory(read) by upgradeMemoryAttr; nothing to reject.
define void @f() readonly { ret void }
