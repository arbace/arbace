; verdict: invalid Cannot have multiple 'swiftself' parameters!
; rule: llvm/lib/IR/Verifier.cpp:2328 Two swiftself params; verifyFunctionAttrs rejects the second.
define swiftcc void @f(ptr swiftself %e, ptr swiftself %e2) { ret void }
