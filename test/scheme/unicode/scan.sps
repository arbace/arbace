;; SCS16 (b) (the user 2026-11-09; doc/scheme/SCHEME-SURVEY.md, "As built: SCS15-SCS18"): (rnrs unicode)'s answers for every
;; Unicode scalar value, one line a scalar value whose general category is not Cn or which any answer singles out, as
;;   CP CAT UP DOWN TITLE FOLD FLAGS SUP SDOWN STITLE SFOLD
;; (hex; CAT char-general-category; UP ... FOLD the char- mappings; FLAGS the letters of char-alphabetic? char-numeric?
;; char-whitespace? char-upper-case? char-lower-case? char-title-case? that hold, a, n, w, u, l, t, or -; SUP ... SFOLD the
;; string- mappings of the one-character string, its scalar values joined by .), then the contextual cases (the final sigma).
;; A portable R6RS top-level program: run by Chez 10.3.0 (chez --program) and by si (bin/si run), each output to a file, the two
;; compared by `bin/si unicode-scan` (test/scheme/unicode/scan.ae). Read by nothing but those two.
(import (rnrs))

(define (hex n) (string-upcase (number->string n 16)))
(define (cps s) (let loop ([i (- (string-length s) 1)] [acc '()])
                  (if (< i 0) acc (loop (- i 1) (cons (hex (char->integer (string-ref s i))) acc)))))
(define (joined xs) (if (null? xs) "" (fold-left (lambda (a x) (string-append a "." x)) (car xs) (cdr xs))))
(define (flags c)
  (let ([s (string-append (if (char-alphabetic? c) "a" "") (if (char-numeric? c) "n" "") (if (char-whitespace? c) "w" "")
                          (if (char-upper-case? c) "u" "") (if (char-lower-case? c) "l" "") (if (char-title-case? c) "t" ""))])
    (if (string=? s "") "-" s)))
(define out (current-output-port))
(define (line . xs) (put-string out (joined* xs)) (put-string out "\n"))
(define (joined* xs) (if (null? (cdr xs)) (car xs) (string-append (car xs) " " (joined* (cdr xs)))))

(let loop ([n 0])
  (when (< n #x110000)
    (if (and (>= n #xD800) (<= n #xDFFF))
        (loop #xE000)
        (let* ([c (integer->char n)] [s (string c)] [cat (symbol->string (char-general-category c))]
               [up (char-upcase c)] [down (char-downcase c)] [title (char-titlecase c)] [fold (char-foldcase c)]
               [f (flags c)] [su (cps (string-upcase s))] [sd (cps (string-downcase s))] [st (cps (string-titlecase s))]
               [sf (cps (string-foldcase s))] [me (list (hex n))])
          (when (or (not (string=? cat "Cn")) (not (string=? f "-")) (not (char=? up c)) (not (char=? down c))
                    (not (char=? title c)) (not (char=? fold c)) (not (equal? su me)) (not (equal? sd me)) (not (equal? st me))
                    (not (equal? sf me)))
            (line (hex n) cat (hex (char->integer up)) (hex (char->integer down)) (hex (char->integer title))
                  (hex (char->integer fold)) f (joined su) (joined sd) (joined st) (joined sf)))
          (loop (+ n 1))))))

;; the contextual mappings (R6RS 1.2's string-downcase: the final sigma; string-titlecase's words)
(for-each (lambda (s)
            (line "ctx" (joined (cps s)) (joined (cps (string-upcase s))) (joined (cps (string-downcase s)))
                  (joined (cps (string-titlecase s))) (joined (cps (string-foldcase s)))))
          (list "ΑΣ" "ΑΣ Α" "ΑΣΑ" "Σ" "ΑΣ." "Α.Σ" "ΑΣ\x301;" "ΑΣ\x301;Α" "Α'Σ" "ΑΣ'" "hello world" "kNock KNoCK" "ǆǅǄ" "ßtraße"
                "ﬁne ﬂow" "o'neil can't" "3rd 1st" "ǈubica" "ŉ" "ΐ" "ᾳ ᾼ" "İstanbul" "i̇" "aBc-dEf_gHi" "x.y:z" "Ⅻ ⓐ"))
