; verdict: invalid cannot guarantee tail call due to mismatched ABI impacting function attributes
; rule: llvm/lib/IR/Verifier.cpp:4233 Both swiftcc so the cc matches; the call argument has swiftself but the caller parameter has none.
declare void @g(ptr swiftself)
define swiftcc void @f(ptr %x) {
 musttail call swiftcc void @g(ptr swiftself %x)
 ret void
}
