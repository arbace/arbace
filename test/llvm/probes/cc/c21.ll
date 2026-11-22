; verdict: invalid Cannot have multiple 'swiftasync' parameters!
; rule: llvm/lib/IR/Verifier.cpp:2333 Two swiftasync params; verifyFunctionAttrs rejects the second.
define swiftcc void @f(ptr swiftasync %e, ptr swiftasync %e2) { ret void }
