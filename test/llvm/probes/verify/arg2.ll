; verdict: valid
; rule: llvm/lib/AsmParser/LLParser.cpp:3597 Unnamed argument numbered %1 with next ID 0 is accepted (checkValueID only rejects ID < next); body is a plain ret void.
define void @f(i32 %1) { ret void }
