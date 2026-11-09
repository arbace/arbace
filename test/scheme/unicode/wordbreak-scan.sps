;; SCS21 (b) (the user 2026-11-09; doc/scheme/SCHEME-SURVEY.md, "As built: SCS19-SCS21"): string-titlecase's word breaks, every pair
;; a<c>b: for every Unicode scalar value c whose general category is not Cn, one line
;;   CP R1 R2 R3 R4 R5 R6 R7 R8 R9
;; (hex; Rn the string-titlecase of frame n with c in its place, its scalar values joined by .; R7 and R8 string-downcase's) of six frames that make a word
;; break before a cased letter, the one thing titlecasing shows, depend on c and its neighbours (UAX #29's rules WB3 to WB16 as far
;; as titlecasing sees them):
;;   1  a c b       c between two letters (WB5 to WB7, WB4's Extend and Format)
;;   2  1 c 2 b     c between two digits, a letter after them (WB8 to WB12)
;;   3  a c U+0301 b   c followed by an Extend (WB4: the Extend belongs to c)
;;   4  c a b       c at the start (WB1, WB2)
;;   5  a c 1 b     c between a letter and a digit (WB9, WB10, WB6's and WB7's contexts)
;;   6  1 c b       c between a digit and a letter
;; and, since SCS22 (b) and SCS23 (b) (the user 2026-11-09; "As built: SCS22-SCS24"):
;;   7  string-downcase of U+0391 U+03A3 c   the final sigma unless c is cased (Final_Sigma's after context; c ignorable: looked past)
;;   8  string-downcase of U+0391 c U+03A3   the final sigma when c is cased, or ignorable (its before context)
;;   9  a U+200D c b   c after a ZWJ (WB3c: no break before an Extended_Pictographic; else WB4 gives the ZWJ to a)
;; A portable R6RS top-level program: run by Chez 10.3.0 (chez --program) and by si (bin/si run), each output to a file, the two
;; compared by `bin/si unicode-scan` against Chez's recorded answers (test/scheme/wordbreak-scan.edn).
(import (rnrs))

(define (hex n) (string-upcase (number->string n 16)))
(define (cps s) (let loop ([i (- (string-length s) 1)] [acc '()])
                  (if (< i 0) acc (loop (- i 1) (cons (hex (char->integer (string-ref s i))) acc)))))
(define (joined xs) (if (null? xs) "" (fold-left (lambda (a x) (string-append a "." x)) (car xs) (cdr xs))))
(define out (current-output-port))
(define (title s) (joined (cps (string-titlecase s))))
(define (down s) (joined (cps (string-downcase s))))

(let loop ([n 0])
  (when (< n #x110000)
    (if (and (>= n #xD800) (<= n #xDFFF))
        (loop #xE000)
        (let ([c (integer->char n)])
          (unless (eq? (char-general-category c) 'Cn)
            (put-string out
              (string-append (hex n) " " (title (string #\a c #\b)) " " (title (string #\1 c #\2 #\b)) " "
                             (title (string #\a c #\x301 #\b)) " " (title (string c #\a #\b)) " "
                             (title (string #\a c #\1 #\b)) " " (title (string #\1 c #\b)) " "
                             (down (string #\x391 #\x3A3 c)) " " (down (string #\x391 c #\x3A3)) " "
                             (title (string #\a #\x200D c #\b)) "\n")))
          (loop (+ n 1))))))
