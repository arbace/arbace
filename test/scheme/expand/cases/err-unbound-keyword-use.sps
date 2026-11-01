(import (rnrs))
(let-syntax ((m (syntax-rules () ((_) (undefined-macro-or-proc)))))
  (set! m 1))
