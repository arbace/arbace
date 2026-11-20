; verdict: valid
; rule: - allocalign on an i32 parameter is legal; no rule demands allocptr or allockind.
define ptr @f(i32 allocalign %x) { ret ptr null }
