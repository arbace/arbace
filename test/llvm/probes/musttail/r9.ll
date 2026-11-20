; verdict: valid
; rule: - returned is not in the ABI attribute list of getParameterABIAttributes, so the differing returned attribute is accepted.
declare i32 @g(i32 returned)
define i32 @f(i32 %x) {
 %r = musttail call i32 @g(i32 returned %x)
 ret i32 %r
}
