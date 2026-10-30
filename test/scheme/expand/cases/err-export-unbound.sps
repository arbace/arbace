(library (lib unbound-export) (export nope) (import (rnrs)) (define yes 1))
(import (rnrs) (lib unbound-export))
1
