; report 043's F4: volatile on the generic builtins (float load/store/xchg, the compare-exchange loop of fadd and umax)
target datalayout = "e-m:e-p270:32:32-p271:32:32-p272:64:64-i64:64-i128:128-f80:128-n8:16:32:64-S128"
define float @f(ptr %p, float %v, i32 %u) {
  %a = load atomic volatile float, ptr %p seq_cst, align 4
  store atomic volatile float %v, ptr %p seq_cst, align 4
  %b = atomicrmw volatile xchg ptr %p, float %v seq_cst, align 4
  %c = atomicrmw volatile fadd ptr %p, float %v seq_cst, align 4
  %d = atomicrmw volatile umax ptr %p, i32 %u seq_cst, align 4
  ret float %a
}
