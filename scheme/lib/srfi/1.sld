;;;; (srfi 1): SRFI 1 as R7RS spells its name (SC116 (c), doc/scheme/SCHEME-SURVEY.md): (srfi :1) with (scheme base)'s
;;;; map, member, assoc, make-list and list-copy, so that (import (scheme base) (srfi 1)) is not a conflict. The five
;;;; agree in meaning (SRFI 1's map stops at the shortest list as R7RS's does); (srfi :1), R6RS's spelling, is unchanged.
(define-library (srfi 1)
  (export
    xcons list-tabulate cons* iota proper-list? circular-list? dotted-list? not-pair? null-list? list= circular-list
    length+ first second third fourth fifth sixth seventh eighth ninth tenth car+cdr take drop take-right drop-right
    take! drop-right! split-at split-at! last last-pair zip unzip1 unzip2 unzip3 unzip4 unzip5 count append!
    append-reverse append-reverse! concatenate concatenate! unfold fold pair-fold reduce unfold-right fold-right
    pair-fold-right reduce-right append-map append-map! map! pair-for-each filter-map map-in-order filter partition
    remove filter! partition! remove! find find-tail any every list-index take-while drop-while take-while! span break
    span! break! delete delete! alist-cons alist-copy delete-duplicates delete-duplicates! alist-delete alist-delete!
    reverse! lset<= lset= lset-adjoin lset-union lset-intersection lset-difference lset-xor lset-diff+intersection
    lset-union! lset-intersection! lset-difference! lset-xor! lset-diff+intersection! map member assoc make-list
    list-copy)
  (import (except (srfi :1) map member assoc make-list list-copy)
          (only (scheme base) map member assoc make-list list-copy)))
