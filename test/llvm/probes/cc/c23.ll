; verdict: invalid Attribute 'swifterror' applied to incompatible type!
; rule: llvm/lib/IR/Verifier.cpp:2155 swifterror on i32: typeIncompatible lists SwiftError for non-pointer types, checked in verifyParameterAttrs.
define swiftcc void @f(i32 swifterror %e) { ret void }
