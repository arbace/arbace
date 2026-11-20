; verdict: invalid expected integer
; rule: llvm/lib/AsmParser/LLParser.cpp:1973 The '!' is eaten (LexExclaim gives a bare '!' before '"'), then parseMDNodeID's parseUInt32 needs a node number but finds the string "x".
!llvm.module.flags = !{!"x"}
