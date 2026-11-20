; verdict: invalid Attribute 'allocalign' applied to incompatible type!
; rule: llvm/lib/IR/Verifier.cpp:2156 allocalign on a float parameter: only integers allowed.
define ptr @f(float allocalign %x) { ret ptr null }
