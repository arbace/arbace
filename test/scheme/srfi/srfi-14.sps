#!r6rs
;;; SRFI 14 tests. Part 1: Olin Shivers' official regression suite (srfi-14-tests.scm of
;;; https://github.com/scheme-requests-for-implementation/srfi-14 at d6e98129d1e075c03bfe4dcb30e1b5847727e26e), each clause of
;;; its (test ...) form one check, the test macro replaced by the harness below; one clause differs: char-set-unfold's base is the ASCII digits
;;; (char-set:digit is every Nd character here, Latin-1 in the original). Part 2: cases from the SRFI document's
;;; specification and our own, checked against Chez Scheme 10.3.0 (counts of the Unicode sets are not tested: si and Chez
;;; carry different Unicode versions).
(import (rnrs) (srfi :14))
(define passed 0)
(define failed 0)
(define (report expr got expected ok)
  (if ok (set! passed (+ passed 1))
      (begin (set! failed (+ failed 1))
             (display "FAIL: ") (write expr) (display " => ") (write got)
             (display ", expected ") (write expected) (newline))))
(define-syntax test
  (syntax-rules ()
    ((_ expected actual)
     (let ((got (guard (c (#t (list 'raised (if (message-condition? c) (condition-message c) c)))) actual)))
       (report 'actual got expected (equal? got expected))))))
(define-syntax check
  (syntax-rules ()
    ((_ form ...) (begin (test #t (and form #t)) ...))))
(define-syntax raises
  (syntax-rules ()
    ((_ expr) (test #t (guard (c (#t #t)) expr #f)))))

(let ((vowel? (lambda (c) (member c '(#\a #\e #\i #\o #\u)))))
(check
 (not (char-set? 5))

 (char-set? (char-set #\a #\e #\i #\o #\u))

 (char-set=)
 (char-set= (char-set))

 (char-set= (char-set #\a #\e #\i #\o #\u)
	    (string->char-set "ioeauaiii"))

 (not (char-set= (char-set #\e #\i #\o #\u)
		 (string->char-set "ioeauaiii")))

 (char-set<=)
 (char-set<= (char-set))

 (char-set<= (char-set #\a #\e #\i #\o #\u)
	     (string->char-set "ioeauaiii"))

 (char-set<= (char-set #\e #\i #\o #\u)
	     (string->char-set "ioeauaiii"))

 (<= 0 (char-set-hash char-set:graphic 100) 99)

 (= 4 (char-set-fold (lambda (c i) (+ i 1)) 0
		     (char-set #\e #\i #\o #\u #\e #\e)))

 (char-set= (string->char-set "eiaou2468013579999")
	    (char-set-unfold null? car cdr '(#\a #\e #\i #\o #\u #\u #\u)
			     (char-set-intersection char-set:ascii char-set:digit)))

 (char-set= (string->char-set "eiaou246801357999")
	    (char-set-unfold! null? car cdr '(#\a #\e #\i #\o #\u)
			      (string->char-set "0123456789")))

 (not (char-set= (string->char-set "eiaou246801357")
		 (char-set-unfold! null? car cdr '(#\a #\e #\i #\o #\u)
				   (string->char-set "0123456789"))))

 (let ((cs (string->char-set "0123456789")))
   (char-set-for-each (lambda (c) (set! cs (char-set-delete cs c)))
		      (string->char-set "02468000"))
   (char-set= cs (string->char-set "97531")))

 (not (let ((cs (string->char-set "0123456789")))
	(char-set-for-each (lambda (c) (set! cs (char-set-delete cs c)))
			   (string->char-set "02468"))
	(char-set= cs (string->char-set "7531"))))

 (char-set= (char-set-map char-upcase (string->char-set "aeiou"))
	    (string->char-set "IOUAEEEE"))

 (not (char-set= (char-set-map char-upcase (string->char-set "aeiou"))
		 (string->char-set "OUAEEEE")))

 (char-set= (char-set-copy (string->char-set "aeiou"))
	    (string->char-set "aeiou"))

 (char-set= (char-set #\x #\y) (string->char-set "xy"))
 (not (char-set= (char-set #\x #\y #\z) (string->char-set "xy")))

 (char-set= (string->char-set "xy") (list->char-set '(#\x #\y)))
 (not (char-set= (string->char-set "axy") (list->char-set '(#\x #\y))))

 (char-set= (string->char-set "xy12345")
	    (list->char-set '(#\x #\y) (string->char-set "12345")))
 (not (char-set= (string->char-set "y12345")
		 (list->char-set '(#\x #\y) (string->char-set "12345"))))

 (char-set= (string->char-set "xy12345")
	    (list->char-set! '(#\x #\y) (string->char-set "12345")))
 (not (char-set= (string->char-set "y12345")
		 (list->char-set! '(#\x #\y) (string->char-set "12345"))))

 (char-set= (string->char-set "aeiou12345")
	    (char-set-filter vowel? char-set:ascii (string->char-set "12345")))
 (not (char-set= (string->char-set "aeou12345")
		 (char-set-filter vowel? char-set:ascii (string->char-set "12345"))))

 (char-set= (string->char-set "aeiou12345")
	    (char-set-filter! vowel? char-set:ascii (string->char-set "12345")))
 (not (char-set= (string->char-set "aeou12345")
		 (char-set-filter! vowel? char-set:ascii (string->char-set "12345"))))


 (char-set= (string->char-set "abcdef12345")
	    (ucs-range->char-set 97 103 #t (string->char-set "12345")))
 (not (char-set= (string->char-set "abcef12345")
		 (ucs-range->char-set 97 103 #t (string->char-set "12345"))))

 (char-set= (string->char-set "abcdef12345")
	    (ucs-range->char-set! 97 103 #t (string->char-set "12345")))
 (not (char-set= (string->char-set "abcef12345")
		 (ucs-range->char-set! 97 103 #t (string->char-set "12345"))))


 (char-set= (->char-set #\x)
	    (->char-set "x")
	    (->char-set (char-set #\x)))

 (not (char-set= (->char-set #\x)
		 (->char-set "y")
		 (->char-set (char-set #\x))))

 (= 10 (char-set-size (char-set-intersection char-set:ascii char-set:digit)))

 (= 5 (char-set-count vowel? char-set:ascii))

 (equal? '(#\x) (char-set->list (char-set #\x)))
 (not (equal? '(#\X) (char-set->list (char-set #\x))))

 (equal? "x" (char-set->string (char-set #\x)))
 (not (equal? "X" (char-set->string (char-set #\x))))

 (char-set-contains? (->char-set "xyz") #\x)
 (not (char-set-contains? (->char-set "xyz") #\a))

 (char-set-every char-lower-case? (->char-set "abcd"))
 (not (char-set-every char-lower-case? (->char-set "abcD")))
 (char-set-any char-lower-case? (->char-set "abcd"))
 (not (char-set-any char-lower-case? (->char-set "ABCD")))

 (char-set= (->char-set "ABCD")
	    (let ((cs (->char-set "abcd")))
	      (let lp ((cur (char-set-cursor cs)) (ans '()))
		(if (end-of-char-set? cur) (list->char-set ans)
		    (lp (char-set-cursor-next cs cur)
			(cons (char-upcase (char-set-ref cs cur)) ans))))))


 (char-set= (char-set-adjoin (->char-set "123") #\x #\a)
	    (->char-set "123xa"))
 (not (char-set= (char-set-adjoin (->char-set "123") #\x #\a)
		 (->char-set "123x")))
 (char-set= (char-set-adjoin! (->char-set "123") #\x #\a)
	    (->char-set "123xa"))
 (not (char-set= (char-set-adjoin! (->char-set "123") #\x #\a)
		 (->char-set "123x")))

 (char-set= (char-set-delete (->char-set "123") #\2 #\a #\2)
	    (->char-set "13"))
 (not (char-set= (char-set-delete (->char-set "123") #\2 #\a #\2)
		 (->char-set "13a")))
 (char-set= (char-set-delete! (->char-set "123") #\2 #\a #\2)
	    (->char-set "13"))
 (not (char-set= (char-set-delete! (->char-set "123") #\2 #\a #\2)
		 (->char-set "13a")))

 (char-set= (char-set-intersection char-set:hex-digit (char-set-complement char-set:digit))
	    (->char-set "abcdefABCDEF"))
 (char-set= (char-set-intersection! (char-set-complement! (->char-set "0123456789"))
				    char-set:hex-digit)
	    (->char-set "abcdefABCDEF"))

 (char-set= (char-set-union char-set:hex-digit
			    (->char-set "abcdefghijkl"))
	    (->char-set "abcdefABCDEFghijkl0123456789"))
 (char-set= (char-set-union! (->char-set "abcdefghijkl")
			     char-set:hex-digit)
	    (->char-set "abcdefABCDEFghijkl0123456789"))

 (char-set= (char-set-difference (->char-set "abcdefghijklmn")
				 char-set:hex-digit)
	    (->char-set "ghijklmn"))
 (char-set= (char-set-difference! (->char-set "abcdefghijklmn")
				  char-set:hex-digit)
	    (->char-set "ghijklmn"))

 (char-set= (char-set-xor (->char-set "0123456789")
			  char-set:hex-digit)
	    (->char-set "abcdefABCDEF"))
 (char-set= (char-set-xor! (->char-set "0123456789")
			   char-set:hex-digit)
	    (->char-set "abcdefABCDEF"))

 (call-with-values (lambda ()
		     (char-set-diff+intersection char-set:hex-digit
						 char-set:letter))
   (lambda (d i)
     (and (char-set= d (->char-set "0123456789"))
	  (char-set= i (->char-set "abcdefABCDEF")))))

 (call-with-values (lambda ()
		     (char-set-diff+intersection! (char-set-copy char-set:hex-digit)
						  (char-set-copy char-set:letter)))
   (lambda (d i)
     (and (char-set= d (->char-set "0123456789"))
	  (char-set= i (->char-set "abcdefABCDEF"))))))
)

;;; Part 2: the SRFI's specification and our own cases.
(define (ch . ns) (map integer->char ns))
(define u (char-set #\a #\b #\c))
(test '(#\a #\b #\c) (char-set->list u))
(test 3 (char-set-size u))
(test "abc" (char-set->string u))
(test #t (char-set-contains? u #\b))
(test #f (char-set-contains? u #\d))
(test '() (char-set->list char-set:empty))
(test #t (char-set= char-set:empty (char-set)))
(test #t (char-set= (char-set-complement char-set:empty) char-set:full))
(test 1112064 (char-set-size char-set:full))
(test 128 (char-set-size char-set:ascii))
(test 22 (char-set-size char-set:hex-digit))
(test #t (char-set-contains? char-set:digit #\7))
(test #t (char-set-contains? char-set:digit (integer->char #x0663)))   ; ARABIC-INDIC DIGIT THREE
(test #f (char-set-contains? char-set:digit #\a))
(test #t (char-set-contains? char-set:letter (integer->char #x3b1)))
(test #t (char-set-contains? char-set:letter (integer->char #x4e2d)))
(test #f (char-set-contains? char-set:letter #\1))
(test #t (char-set-contains? char-set:lower-case #\a))
(test #f (char-set-contains? char-set:lower-case #\A))
(test #t (char-set-contains? char-set:upper-case (integer->char #x391)))
(test #t (char-set-contains? char-set:title-case (integer->char #x1c5)))
(test #t (char-set<= char-set:lower-case char-set:letter))
(test #t (char-set<= char-set:letter char-set:letter+digit char-set:graphic char-set:printing))
(test #t (char-set<= char-set:hex-digit char-set:ascii char-set:full))
(test #t (char-set-contains? char-set:whitespace #\space))
(test #t (char-set-contains? char-set:whitespace #\newline))
(test #t (char-set-contains? char-set:whitespace (integer->char #xa0)))
(test #t (char-set-contains? char-set:whitespace (integer->char #x2028)))
(test #f (char-set-contains? char-set:whitespace #\a))
(test #t (char-set-contains? char-set:blank #\tab))
(test #f (char-set-contains? char-set:blank #\newline))
(test #t (char-set= char-set:iso-control (char-set-union (ucs-range->char-set 0 32) (ucs-range->char-set 127 160))))
(test #t (char-set= (char-set-intersection char-set:ascii char-set:punctuation) (string->char-set "!\"#%&'()*,-./:;?@[\\]_{}")))
(test #t (char-set= (char-set-intersection char-set:ascii char-set:symbol) (string->char-set "$+<=>^`|~")))
(test 52 (char-set-size (char-set-intersection char-set:ascii char-set:letter)))
(test #f (char-set-contains? char-set:graphic #\space))
(test #t (char-set-contains? char-set:printing #\space))
(test #f (char-set-contains? char-set:graphic #\x0))
;; surrogates are not characters; the sets never hold them
(test #t (char-set-contains? char-set:full #\x0))
;; supplementary planes
(test '(#\x10000 #\x10FFFF) (char-set->list (char-set #\x10FFFF #\x10000)))
(test #t (char-set-contains? (char-set-complement (char-set #\a)) #\x10FFFF))
(test #f (char-set-contains? (char-set-complement (char-set #\a)) #\a))
(test 1112063 (char-set-size (char-set-complement (char-set #\a))))
;; ucs-range->char-set: [lower, upper)
(test '(#\a #\b #\c) (char-set->list (ucs-range->char-set 97 100)))
(test '() (char-set->list (ucs-range->char-set 100 100)))
(test 1112064 (char-set-size (ucs-range->char-set 0 #x200000)))
(test 1112064 (char-set-size (ucs-range->char-set #xD000 #x200000 #f (ucs-range->char-set 0 #xD000))))
(raises (ucs-range->char-set #xD7F0 #xE010 #t))
(test 0 (char-set-size (ucs-range->char-set #xD800 #xE000)))
(test #t (char-set= (ucs-range->char-set #xD700 #xE100) (ucs-range->char-set #xD700 #xD800 #f (ucs-range->char-set #xE000 #xE100))))
;; algebra
(let ((a (string->char-set "abcdef")) (b (string->char-set "defghi")))
  (check (char-set= (char-set-union a b) (string->char-set "abcdefghi"))
         (char-set= (char-set-intersection a b) (string->char-set "def"))
         (char-set= (char-set-difference a b) (string->char-set "abc"))
         (char-set= (char-set-difference a) a)
         (char-set= (char-set-xor a b) (string->char-set "abcghi"))
         (char-set= (char-set-union) char-set:empty)
         (char-set= (char-set-intersection) char-set:full)
         (char-set= (char-set-xor) char-set:empty)
         (char-set= (char-set-union a) a)
         (char-set= (char-set-difference (string->char-set "abcdef") (string->char-set "ab") (string->char-set "ef"))
                    (string->char-set "cd"))
         (char-set= (char-set-xor a b (string->char-set "adz")) (string->char-set "bcdghiz"))
         (char-set<= a (char-set-union a b))
         (not (char-set<= (char-set-union a b) a))
         (char-set<= a a a)
         (char-set= a (string->char-set "fedcba") (list->char-set (string->list "abcdef")))))
(call-with-values (lambda () (char-set-diff+intersection (string->char-set "abcdef") (string->char-set "def") (string->char-set "xa")))
  (lambda (d i)
    (check (char-set= d (string->char-set "bc")) (char-set= i (string->char-set "adef")))))
;; the pure operations do not touch their arguments
(let* ((a (string->char-set "abc")) (b (char-set-adjoin a #\d)) (c (char-set-delete a #\a)) (d (char-set-copy a)))
  (check (char-set= a (string->char-set "abc")) (char-set= b (string->char-set "abcd"))
         (char-set= c (string->char-set "bc")) (char-set= d a) (not (eq? a d))))
;; linear update: use the result
(let* ((a (string->char-set "abc")) (a (char-set-adjoin! a #\z #\y)) (a (char-set-delete! a #\a)))
  (check (char-set= a (string->char-set "bcyz"))))
(check (char-set= (char-set-complement! (string->char-set "a")) (char-set-complement (string->char-set "a"))))
(check (char-set= (char-set-union! (string->char-set "ab") (string->char-set "bc") (string->char-set "d")) (string->char-set "abcd")))
(check (char-set= (char-set-xor! (string->char-set "ab") (string->char-set "bc")) (string->char-set "ac")))
(check (char-set= (char-set-difference! (string->char-set "abc") (string->char-set "b")) (string->char-set "ac")))
;; fold, for-each, map, count, any, every, cursors
(test '(#\a #\b #\c) (char-set-fold cons '() (string->char-set "abc")))   ; descending visit order, as in the reference
(test "cba" (let ((l '())) (char-set-for-each (lambda (c) (set! l (cons c l))) (string->char-set "abc")) (list->string (reverse l))))
(test #t (char-set= (char-set-map char-upcase (string->char-set "abc")) (string->char-set "ABC")))
(test #t (char-set= (char-set-map (lambda (c) #\x) (string->char-set "abc")) (string->char-set "x")))
(test 3 (char-set-count char-lower-case? (string->char-set "abcDE1")))
(test #\b (char-set-any (lambda (c) (and (char=? c #\b) c)) (string->char-set "abc")))
(test #f (char-set-any (lambda (c) (char=? c #\z)) (string->char-set "abc")))
(test #t (char-set-every char-alphabetic? (string->char-set "abc")))
(test #t (char-set-every char-alphabetic? char-set:empty))
(test #f (char-set-any char-alphabetic? char-set:empty))
(test '(#\a #\b #\c #\d)
      (let ((cs (string->char-set "dcba")))
        (let lp ((cur (char-set-cursor cs)) (acc '()))
          (if (end-of-char-set? cur) acc
              (lp (char-set-cursor-next cs cur) (cons (char-set-ref cs cur) acc))))))
(test #t (end-of-char-set? (char-set-cursor char-set:empty)))
(test '(#\a #\x3b1 #\x10000)
      (let ((cs (char-set #\a #\x3b1 #\x10000)))
        (let lp ((cur (char-set-cursor cs)) (acc '()))
          (if (end-of-char-set? cur) acc
              (lp (char-set-cursor-next cs cur) (cons (char-set-ref cs cur) acc))))))
(test 5 (let ((n 0)) (let lp ((cur (char-set-cursor (ucs-range->char-set #xfffe #x10003)))) (if (end-of-char-set? cur) n (begin (set! n (+ n 1)) (lp (char-set-cursor-next (ucs-range->char-set #xfffe #x10003) cur)))))))
;; unfold, filter, list/string conversions with bases, ->char-set
(test #t (char-set= (char-set-unfold (lambda (n) (> n 3)) (lambda (n) (integer->char (+ 96 n))) (lambda (n) (+ n 1)) 1) (string->char-set "abc")))
(test #t (char-set= (char-set-unfold null? car cdr '(#\a #\b) (string->char-set "z")) (string->char-set "abz")))
(test #t (char-set= (char-set-filter char-upper-case? (string->char-set "aBcD")) (string->char-set "BD")))
(test #t (char-set= (char-set-filter char-upper-case? (string->char-set "aBcD") (string->char-set "xy")) (string->char-set "BDxy")))
(test #t (char-set= (list->char-set '(#\a #\b) (string->char-set "xy")) (string->char-set "abxy")))
(test #t (char-set= (string->char-set "ab" (string->char-set "xy")) (string->char-set "abxy")))
(test #t (char-set= (->char-set #\a) (->char-set "a") (char-set #\a)))
(raises (->char-set 5))
(raises (char-set-contains? 5 #\a))
(raises (char-set-contains? u 5))
(raises (list->char-set '(#\a) 5))
(raises (list->char-set '(#\a) u u))
;; hash: equal sets hash alike, within the bound
(test #t (= (char-set-hash (string->char-set "abc") 1000) (char-set-hash (list->char-set '(#\c #\b #\a)) 1000)))
(test #t (<= 0 (char-set-hash char-set:letter 97) 96))
(test #t (= (char-set-hash (char-set-union char-set:letter char-set:digit))
            (char-set-hash char-set:letter+digit)))
(test #t (exact? (char-set-hash u)))
(test #t (char-set? u))
(test #f (char-set? "abc"))
(test #t (char-set=))
(test #t (char-set= u))
(test #t (char-set<=))
(test #t (char-set<= u))

(display "srfi-14: ") (display passed) (display " passed, ") (display failed) (display " failed") (newline)
