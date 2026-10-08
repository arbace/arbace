(in-ns 'go.multi)

(go/file "b.go" :imports [^:alias [strs "strings"]])

(go/var b (strs/Repeat "b" 2))
