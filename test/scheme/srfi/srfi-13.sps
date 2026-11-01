#!r6rs
;;; SRFI 13 tests. The SRFI repository has no tests: the cases are the SRFI document's examples (srfi-13.txt at
;;; https://github.com/scheme-requests-for-implementation/srfi-13 e608d1280b73944cfc96a05d8da6003cf296f09f, marked "spec") and our own
;;; (each procedure's documented behaviour, boundary cases, the start/end arguments), run on Chez Scheme 10.3.0 and on si.
;;; The names SRFI 13 shares with (rnrs) come from (srfi :13) here.
(import (except (rnrs) string-copy string-for-each string-hash string-upcase string-downcase string-titlecase string->list)
        (srfi :13) (srfi :14))
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
(define-syntax raises
  (syntax-rules ()
    ((_ expr) (test #t (guard (c (#t #t)) expr #f)))))
(define (mut s) (let ((m (make-string (string-length s)))) (do ((i 0 (+ i 1))) ((= i (string-length s)) m) (string-set! m i (string-ref s i)))))

;; basic
(test #t (string-null? ""))
(test #f (string-null? "a"))
(test "cdefab" (xsubstring "abcdef" 2))             ; spec
(test "efabcd" (xsubstring "abcdef" -2))            ; spec
(test "abcabca" (xsubstring "abc" 0 7))             ; spec
(test "bcab" (xsubstring "abc" 1 5))
(test "" (xsubstring "abc" 2 2))
(test "Beta substitution" (string-copy "Beta substitution"))        ; spec
(test "eta subst" (string-copy "Beta substitution" 1 10))          ; spec
(test "substitution" (string-copy "Beta substitution" 5))          ; spec
(raises (string-copy "Beta substitution" 10 5))
(raises (string-copy "abc" 0 4))
(test "abc" (substring/shared "abc" 0))
(test "bc" (substring/shared "abc" 1))
(test "b" (substring/shared "abc" 1 2))
(test '(#\a #\b #\c) (string->list "abc"))
(test '(#\b #\c) (string->list "abc" 1))
(test '(#\b) (string->list "abc" 1 2))
(let ((s (mut "abcde")))
  (test #t (begin (string-fill! s #\x 1 3) #t))
  (test "axxde" s)
  (string-fill! s #\z)
  (test "zzzzz" s))
(let ((s (mut "abcde")))
  (string-copy! s 1 "XYZ")
  (test "aXYZe" s)
  (string-copy! s 0 "12345" 3)
  (test "45YZe" s)
  (string-copy! s 2 "abcdef" 1 3)
  (test "45bce" s))
(test "abcabc" (string-concatenate '("abc" "abc")))
(test "" (string-concatenate '()))
(test "abcdef" (string-concatenate/shared '("abc" "def")))
(test "abcdef" (string-append/shared "abc" "def"))
(test "Hello, I must be going." (string-concatenate-reverse '(" must be" "Hello, I") " going.XXXX" 7))   ; spec
(test "abc" (string-concatenate-reverse '("c" "b" "a")))  ; the list reversed, final ""
(test "abc" (string-concatenate-reverse '("b" "a") "c" 1) )
(test "cba" (reverse-list->string '(#\a #\b #\c)))
(test "Able" (string-reverse "elbA"))
(test ".able was I ere I saw elbA" (string-reverse "Able was I ere I saw elba."))   ; spec
(test "cb" (string-reverse "abcd" 1 3))
(let ((s (mut "abcd"))) (string-reverse! s 1 3) (test "acbd" s))
(let ((s (mut "abcd"))) (string-reverse! s) (test "dcba" s))
;; mapping and iteration
(test "ABC" (string-map char-upcase "abc"))
(test "BC" (string-map char-upcase "abc" 1 3))
(let ((s (mut "abc"))) (string-map! char-upcase s 1) (test "aBC" s))
(test '(#\c #\b #\a) (string-fold cons '() "abc"))
(test '(#\a #\b #\c) (string-fold-right cons '() "abc"))
(test 3 (string-fold (lambda (c n) (+ n 1)) 0 "abc"))
(test '(#\b #\a) (string-fold cons '() "abc" 0 2 ))
(test "abc" (string-unfold null? car cdr '(#\a #\b #\c)))
(test "xabc!" (string-unfold null? car cdr '(#\a #\b #\c) "x" (lambda (x) "!")))
(test "abc" (string-unfold-right null? car cdr '(#\c #\b #\a)))
(test "cba!" (string-unfold-right null? car cdr '(#\a #\b #\c) "!"))
(test "abcde" (string-tabulate (lambda (i) (integer->char (+ 97 i))) 5))
(test "" (string-tabulate (lambda (i) #\a) 0))
(test '(#\a #\b) (let ((l '())) (string-for-each (lambda (c) (set! l (cons c l))) "ab") (reverse l)))
(test '(0 1 2) (let ((l '())) (string-for-each-index (lambda (i) (set! l (cons i l))) "abc") (reverse l)))
(test '(1) (let ((l '())) (string-for-each-index (lambda (i) (set! l (cons i l))) "abc" 1 2) l))
(test #t (string-every char-alphabetic? "abc"))
(test #f (string-every char-alphabetic? "ab1"))
(test #t (string-every #\a "aaa"))
(test #t (string-every char-set:digit ""))
(test #t (string-any char-numeric? "ab1"))
(test #f (string-any char-numeric? "abc"))
(test #\b (string-any (lambda (c) (and (char=? c #\b) c)) "abc"))
(test #t (string-any #\c "abc"))
(test #t (string-any (string->char-set "xyz") "abz"))
;; comparison
(test 5 (string-compare "The cat in the hat" "abcdefgh" values values values 4 6 2 4))   ; spec
(test 'lt (string-compare "abc" "abd" (lambda (i) 'lt) (lambda (i) 'eq) (lambda (i) 'gt)))
(test 'eq (string-compare "abc" "abc" (lambda (i) 'lt) (lambda (i) 'eq) (lambda (i) 'gt)))
(test 'gt (string-compare "abd" "abc" (lambda (i) 'lt) (lambda (i) 'eq) (lambda (i) 'gt)))
(test 'eq (string-compare-ci "ABC" "abc" (lambda (i) 'lt) (lambda (i) 'eq) (lambda (i) 'gt)))
(test 3 (string-compare "abcd" "abce" values values values))
(test #t (and (string= "abc" "abc") #t))   ; the reference returns the end index, true
(test #f (string= "abc" "abd"))
(test #t (and (string= "xabc" "yabc" 1 4 1 4) #t))   ; the reference returns the end index, true
(test 2 (string<> "abc" "abd"))
(test #f (string<> "abc" "abc"))
(test 2 (string< "abc" "abd"))
(test #f (string< "abd" "abc"))
(test 1 (string< "a" "ab"))
(test #f (string> "a" "ab"))
(test 1 (string> "ab" "a"))
(test 3 (string<= "abc" "abc"))
(test 3 (string>= "abc" "abc"))
(test #f (string<= "abd" "abc"))
(test #t (and (string-ci= "ABC" "abc") #t))   ; the reference returns the end index, true
(test 2 (string-ci< "abC" "ABd"))
(test 2 (string-ci> "abE" "ABd"))
(test #f (string-ci<> "ABC" "abc"))
(test 3 (string-ci<= "ABC" "abc"))
(test 3 (string-ci>= "ABC" "abc"))
(test 2 (string-prefix-length "abcd" "abxy"))
(test 2 (string-suffix-length "xxcd" "abcd" ))
(test 2 (string-prefix-length-ci "ABcd" "abxy"))
(test 3 (string-suffix-length-ci "xBCD" "abcd"))
(test #t (string-prefix? "ab" "abcd"))
(test #f (string-prefix? "abd" "abcd"))
(test #t (string-prefix? "" "abcd"))
(test #t (string-suffix? "cd" "abcd"))
(test #f (string-suffix? "bd" "abcd"))
(test #t (string-prefix-ci? "AB" "abcd"))
(test #t (string-suffix-ci? "CD" "abcd"))
(test #t (string-prefix? "bc" "abcd" 0 2 1 3))
;; hash
(test #t (= (string-hash "abc" 1000) (string-hash (mut "abc") 1000)))
(test #t (< -1 (string-hash "hello world" 100) 100))
(test #t (= (string-hash-ci "ABC" 1000) (string-hash-ci "abc" 1000)))
(test #t (exact? (string-hash "abc")))
(test #t (= (string-hash "xabcx" 1000 1 4) (string-hash "abc" 1000)))
;; case
(test "ABC1" (string-upcase "abc1"))
(test "abc1" (string-downcase "ABC1"))
(test "BC" (string-upcase "abc" 1))
(test "--Capitalize This Sentence." (string-titlecase "--capitalize tHIS sentence."))   ; spec
(test "See Spot Run. See Nix Run." (string-titlecase "see Spot run. see Nix run."))        ; spec
(test "3Com Makes Routers." (string-titlecase "3com makes routers."))                    ; spec
(test "Easy Fried Chicken" (string-titlecase "greasy fried chicken" 2))                  ; spec
(let ((s (mut "abc def"))) (string-upcase! s) (test "ABC DEF" s) (string-downcase! s 0 3) (test "abc DEF" s) (string-titlecase! s) (test "Abc Def" s))
;; take, drop, pad, trim
(test "Pete S" (string-take "Pete Szilagyi" 6))          ; spec
(test "zilagyi" (string-drop "Pete Szilagyi" 6))         ; spec
(test "rules" (string-take-right "Beta rules" 5))        ; spec
(test "Beta " (string-drop-right "Beta rules" 5))        ; spec
(raises (string-take "foo" 37))                          ; spec
(raises (string-drop "foo" 4))
(test "" (string-take "foo" 0))
(test "  325" (string-pad "325" 5))                      ; spec
(test "71325" (string-pad "71325" 5))                    ; spec
(test "71325" (string-pad "8871325" 5))                  ; spec
(test "325  " (string-pad-right "325" 5))
(test "88713" (string-pad-right "8871325" 5))
(test "00325" (string-pad "325" 5 #\0))
(test "xxabc" (string-pad "abc" 5 #\x))
(test "" (string-pad "abc" 0))
(test "The outlook wasn't brilliant," (string-trim-both "  The outlook wasn't brilliant,  \n\r"))   ; spec
(test "abc  " (string-trim "  abc  "))
(test "  abc" (string-trim-right "  abc  "))
(test "abc" (string-trim-both "  abc  "))
(test "" (string-trim-both "    "))
(test "abc" (string-trim-both "xxabcxx" #\x))
(test "abcxx" (string-trim "xxabcxx" #\x))
(test "xxabc" (string-trim-right "xxabcxx" #\x))
(test "" (string-trim "abc" char-lower-case? 0 3))   ; the criterion says what to trim
(test "bc" (string-trim "abc" #\a))
(test "abc" (string-trim-both "12abc34" char-numeric?))
(test "abc" (string-trim-both "12abc34" char-set:digit))
;; delete, filter, index, skip, count
(test "abc" (string-delete char-numeric? "a1b2c3"))
(test "123" (string-filter char-numeric? "a1b2c3"))
(test "bc" (string-delete #\a "abca" ))
(test "ac" (string-delete (string->char-set "b") "abc"))
(test "a1b" (string-filter (lambda (c) (not (char=? c #\2))) "a1b2" ))
(test "b" (string-filter char-alphabetic? "a1b2" 2))
(test 1 (string-index "abc" #\b))
(test #f (string-index "abc" #\z))
(test 2 (string-index "ab1" char-numeric?))
(test 2 (string-index "ab1" char-set:digit))
(test 3 (string-index "abab" #\b 2))
(test #f (string-index "abab" #\b 4))
(test 3 (string-index-right "abab" #\b))
(test 1 (string-index-right "abab" #\b 0 3))
(test 0 (string-skip "abc" char-numeric?))
(test 3 (string-skip "123abc" char-numeric?))
(test #f (string-skip "123" char-numeric?))
(test 2 (string-skip-right "abc12" char-numeric?))
(test #f (string-skip-right "123" char-numeric?))
(test 2 (string-count "a1b2c" char-numeric?))
(test 3 (string-count "abab ba" #\b))
(test 1 (string-count "abab" #\b 0 3))
(test 3 (string-count "a1b2c3" char-set:digit))
;; searching
(test 15 (string-contains "eek -- what a geek." "ee" 12 18))   ; spec
(test 0 (string-contains "eek -- what a geek." "ee"))
(test 0 (string-contains "abc" ""))
(test #f (string-contains "abc" "abcd"))
(test 2 (string-contains "ababcab" "abc"))
(test 0 (string-contains "aaab" "aa"))
(test #f (string-contains "abc" "bc" 0 2))
(test 1 (string-contains-ci "ABC" "bc"))
(test #f (string-contains-ci "ABC" "bd"))
(test '#(-1 0 0 -1 1 2) (make-kmp-restart-vector "abdabc"))   ; spec
(test -3 (string-kmp-partial-search "bc" (make-kmp-restart-vector "bc") "abcd" 0))
(test 1 (string-kmp-partial-search "cd" (make-kmp-restart-vector "cd") "abc" 0))
;; replace, join, tokenize
(test "The miserable perl programmer endured daily ridicule."
      (string-replace "The TCL programmer endured daily ridicule." "another miserable perl drone" 4 7 8 22))   ; spec
(test "It's lots of fun to code it up in Scheme." (string-replace "It's easy to code it up in Scheme." "lots of fun" 5 9))   ; spec
(test "It's really easy to code it up in Scheme." (string-replace "It's easy to code it up in Scheme." "really " 5 5))   ; spec
(test "foo:bar:baz" (string-join '("foo" "bar" "baz") ":"))          ; spec
(test "foo:bar:baz:" (string-join '("foo" "bar" "baz") ":" 'suffix)) ; spec
(test "" (string-join '() ":"))                                      ; spec
(test "" (string-join '("") ":"))                                    ; spec
(test "" (string-join '() ":" 'suffix))                              ; spec
(test ":" (string-join '("") ":" 'suffix))                           ; spec
(test ":foo:bar" (string-join '("foo" "bar") ":" 'prefix))
(test "foo bar" (string-join '("foo" "bar")))
(test "foo:bar" (string-join '("foo" "bar") ":" 'strict-infix))
(raises (string-join '() ":" 'strict-infix))
(raises (string-join '("a") ":" 'bogus))
(test '("Help" "make" "programs" "run," "run," "RUN!") (string-tokenize "Help make programs run, run, RUN!"))   ; spec
(test '() (string-tokenize "   "))
(test '("a" "b") (string-tokenize "a,b" (char-set-complement (char-set #\,))))
(test '("b") (string-tokenize "a b c" char-set:graphic 2 3))
;; re-exported R5RS names
(test #t (string? "a"))
(test 3 (string-length (make-string 3 #\a)))
(test #\b (string-ref "abc" 1))
(test "abc" (string #\a #\b #\c))
(test "abcd" (string-append "ab" "cd"))
(test "ab" (list->string '(#\a #\b)))
(let ((s (make-string 2 #\a))) (string-set! s 1 #\z) (test "az" s))
;; argument errors
(raises (string-index 5 #\a))
(raises (string-pad "abc" -1))
(raises (string-join 5))
(raises (string-upcase 5))
(raises (string-copy "abc" 'x))
(raises (string-contains "abc" 5))

(display "srfi-13: ") (display passed) (display " passed, ") (display failed) (display " failed") (newline)
