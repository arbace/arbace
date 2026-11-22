; verdict: valid
; rule: - Both patchable-function attributes are valid integers and there is no cross check.
define void @f() "patchable-function-prefix"="4" "patchable-function-entry"="2" { ret void }
