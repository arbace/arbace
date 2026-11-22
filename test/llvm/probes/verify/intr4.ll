; verdict: valid
; rule: llvm/lib/IR/AutoUpgrade.cpp:1979 the declaration llvm.smax.i64(i32, i32) has a valid smax signature but the wrong mangling: the parser's UpgradeCallsToIntrinsic (LLParser.cpp:472) remangles it (Intrinsic::remangleIntrinsicFunction) to llvm.smax.i32 and moves the call to it, so the verifier sees a correct module (corrected after the oracle comparison: the first review took the verifier's mangling check, which never sees the old name)
declare i32 @llvm.smax.i64(i32, i32)
define i32 @f() {
  %x = call i32 @llvm.smax.i64(i32 1, i32 2)
  ret i32 %x
}
