; verdict: invalid More than one parameter has attribute nest!
; rule: llvm/lib/IR/Verifier.cpp:2308 Two nest parameters; the second one fails SawNest.
define void @f(ptr nest, ptr nest) { ret void }
