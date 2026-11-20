; verdict: invalid unknown allockind bogus
; rule: llvm/lib/AsmParser/LLParser.cpp:2659 Unknown allockind keyword.
define void @f() allockind("bogus") { ret void }
