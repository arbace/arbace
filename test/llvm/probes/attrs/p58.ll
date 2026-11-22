; verdict: invalid invalid value for 'frame-pointer' attribute: bogus
; rule: llvm/lib/IR/Verifier.cpp:2520 frame-pointer must be all, non-leaf, none, reserved or non-leaf-no-reserve.
define void @f() "frame-pointer"="bogus" { ret void }
