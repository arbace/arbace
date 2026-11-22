; verdict: invalid Declaration may not be in a Comdat!
; rule: llvm/lib/IR/Verifier.cpp:656 Parser accepts comdat($c) on a declare; visitFunction -> visitGlobalValue rejects a declaration (isDeclarationForLinker) with a comdat. Functions are verified before module-level comdat checks.
$c = comdat any
declare void @f() comdat($c)
