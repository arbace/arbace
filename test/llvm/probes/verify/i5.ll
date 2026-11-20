; verdict: valid
; rule: llvm/lib/IR/AutoUpgrade.cpp:1588 Old 2-argument llvm.lifetime.start.p0(i64, ptr) is auto-upgraded in UpgradeCallsToIntrinsic: renamed .old, new declaration lifetime.start.p0(ptr) inserted, old one erased.
declare void @llvm.lifetime.start.p0(i64, ptr)
