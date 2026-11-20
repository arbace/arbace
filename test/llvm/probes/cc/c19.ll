; verdict: invalid Cannot have multiple 'swifterror' parameters!
; rule: llvm/lib/IR/Verifier.cpp:2338 Two swifterror params; verifyFunctionAttrs (before the CC switch) rejects the second.
define swiftcc void @f(ptr swifterror %e, ptr swifterror %e2) { ret void }
