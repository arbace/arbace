; verdict: valid
; rule: llvm/lib/IR/Verifier.cpp:5494 Scope !1 = {self, domain, "n"} (3 operands, string third) with domain !2 = {self, "d"}; the uniqued self-references are resolved at the end of parsing.
define void @f(ptr %p) {
 %v = load i32, ptr %p, !alias.scope !0
 ret void
}
!0 = !{!1}
!1 = !{!1, !2, !"n"}
!2 = !{!2, !"d"}
