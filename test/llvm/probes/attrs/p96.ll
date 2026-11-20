; verdict: valid
; rule: - coro_elide_safe and presplitcoroutine are legal function attributes.
define void @f() coro_elide_safe presplitcoroutine { ret void }
