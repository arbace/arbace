(in-ns 'go.multi)

(go/file "a.go" :doc "Package multi has two files.\n" :imports [[strings "strings"]])

(go/func A ^string [] (strings/ToUpper b))
