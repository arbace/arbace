;; The interactive REPL under rlwrap (doc/go/EXEC-NOTES.md, "Line editing: rlwrap"): when the
;; command line starts arbace.main's REPL, standard input and output are terminals, TERM names
;; a terminal, rlwrap is on PATH and the program does not already run under it, the executable
;; replaces itself (execve) with rlwrap running the executable with the same arguments, with
;; the flags of Clojure's clj script. Part of the program's main package (copied by c2g
;; --program); main calls execRlwrap first, before jrt is set up and the image replayed.
;; Other systems (TamaGo's, B1b) get rlwrap_other.clj's empty execRlwrap.
(in-ns 'go.arbace.cmd.arbace)

(go/file "rlwrap_linux.go"
  :build ["//go:build linux"]
  :imports [[os "os"] [strconv "strconv"] [strings "strings"] [syscall "syscall"] [unsafe "unsafe"]])

(go/var ^{:tag (slice string)
          :doc "rlwrapFlags are clj's (Clojure's Alpine package): multi-line editing, completion on the words seen, \\ and \"\nas quote characters (clj's '\\\"'), Clojure's delimiters as word breaks.\n"}
  rlwrapFlags
  (lit (slice string) "-m" "-r" "-q" "\\\"" "-b" "(){}[],^%#@\";:'"))

(go/func startsREPL
  "startsREPL tells whether arbace.main runs its REPL with these arguments (arbace.main/main):\nno arguments, or init options (-i, -e and --report, each with its argument) followed by -r\nor --repl. Init options alone, -m, -h and a script (- included) do not start it.\n"
  ^bool [^{:tag (slice string)} args]
  (when (== (len args) 0)
    (return true))
  (for [i 0] (< i (len args)) _
    (switch (aget args i)
      (case ["--report" "-i" "--init" "-e" "--eval"]
        (set! i + 2))
      (case ["-r" "--repl"]
        (return true))
      (default
        (return false))))
  (return false))

(go/func terminal "terminal tells whether fd is a terminal (isatty: TCGETS succeeds).\n" ^bool [^uintptr fd]
  (let [^{:tag syscall/Termios} t (zero syscall/Termios)
        (values _ _ e) (syscall/Syscall syscall/SYS_IOCTL fd (conv uintptr syscall/TCGETS) (conv uintptr (conv unsafe/Pointer (addr t))))]
    (== e 0)))

(go/func columns "columns is the width of terminal fd (TIOCGWINSZ), 0 when unknown: rlwrap refuses a width of 0.\n" ^int [^uintptr fd]
  (let [^{:tag (array 4 uint16)} ws (zero (array 4 uint16))
        (values _ _ e) (syscall/Syscall syscall/SYS_IOCTL fd (conv uintptr syscall/TIOCGWINSZ) (conv uintptr (conv unsafe/Pointer (addr ws))))]
    (when (!= e 0)
      (return 0))
    (conv int (aget ws 1))))

(go/func parentIsRlwrap "parentIsRlwrap tells whether the parent process is rlwrap (rlwrap arbace, by hand).\n" ^bool []
  (let [(values b err) (os/ReadFile (+ "/proc/" (strconv/Itoa (os/Getppid)) "/comm"))]
    (and (== err nil) (== (strings/TrimSpace (conv string b)) "rlwrap"))))

(go/func lookPath "lookPath is the executable file name in one of PATH's absolute directories, or \"\".\n" ^string [^string name]
  (range [_ dir (strings/Split (os/Getenv "PATH") ":")]
    (when (strings/HasPrefix dir "/")
      (let [p (+ dir "/" name)
            (values fi err) (os/Stat p)]
        (when (and (== err nil) (.IsRegular (.Mode fi)) (!= (bit-and (.Mode fi) 0111) 0))
          (return p)))))
  (return ""))

(go/func execRlwrap
  "execRlwrap replaces the process with rlwrap running the executable, when the REPL is to run\ninteractively (EXEC-NOTES.md, \"Line editing: rlwrap\"); otherwise, or when the exec fails, it\nreturns and the program runs as it is. ARBACE_RLWRAP=off disables it; the executable sets\nARBACE_RLWRAP=wrapped for its child, which therefore does not exec again.\n"
  []
  (let [mode (os/Getenv "ARBACE_RLWRAP")]
    (when (or (== mode "off") (== mode "wrapped"))
      (return)))
  (let [term (os/Getenv "TERM")]
    (when (or (== term "") (== term "dumb"))
      (return)))
  (when (or (not (startsREPL (subslice os/Args 1))) (not (terminal 0)) (not (terminal 1)) (== (columns 0) 0) (parentIsRlwrap))
    (return))
  (let [path (lookPath "rlwrap")]
    (when (== path "")
      (return))
    (let [(values exe err) (os/Executable)]
      (when (!= err nil)
        (return))
      (let [argv (append (lit (slice string) "rlwrap") (spread rlwrapFlags))]
        (set! argv (append argv exe))
        (set! argv (append argv (spread (subslice os/Args 1))))
        (let [env (lit (slice string))]
          (range [_ kv (os/Environ)]
            (when (not (strings/HasPrefix kv "ARBACE_RLWRAP="))
              (set! env (append env kv))))
          (set! env (append env "ARBACE_RLWRAP=wrapped"))
          ;; on success execve does not return
          (syscall/Exec path argv env))))))
