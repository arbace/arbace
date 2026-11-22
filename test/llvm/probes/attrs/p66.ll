; verdict: valid
; rule: - frame-pointer all is valid; stackrealign and zero-call-used-regs are not validated.
define void @f() nounwind "frame-pointer"="all" "stackrealign" "zero-call-used-regs"="bogus" { ret void }
