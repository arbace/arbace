#!r6rs
(library (scheme complex) (export angle imag-part magnitude make-polar make-rectangular real-part) (import (except (rnrs) angle) (rename (only (psyntax system $all) $chez-angle) ($chez-angle angle))))
