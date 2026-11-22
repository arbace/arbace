; verdict: invalid intrinsic argument 3 type expected i1, but got i32
; rule: llvm/lib/IR/Verifier.cpp:3299 Declaration of llvm.memcpy with i32 as 4th param (not upgraded: only 5-arg form is); Intrinsic::isSignatureValid message from Intrinsics.cpp:976.
declare void @llvm.memcpy.p0.p0.i64(ptr, ptr, i64, i32)
