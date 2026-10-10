;; The REPL under rlwrap (rlwrap_linux.clj) is Linux's: on other systems (TamaGo's, B1b) the
;; program runs as it is.
(in-ns 'go.arbace.cmd.arbace)

(go/file "rlwrap_other.go"
  :build ["//go:build !linux"])

(go/func execRlwrap "execRlwrap does nothing here (rlwrap_linux.go).\n" [])
