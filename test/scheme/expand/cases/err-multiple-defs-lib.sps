(library (lib dup) (export x) (import (rnrs)) (define x 1) (define x 2))
(import (rnrs) (lib dup))
x
