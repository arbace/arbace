; verdict: valid
; rule: llvm/lib/AsmParser/LLParser.cpp:1747 readnone readonly on a function are both upgraded into memory effects and intersected into memory(none).
define void @f() readnone readonly { ret void }
