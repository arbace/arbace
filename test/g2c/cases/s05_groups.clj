;; SPEC §5.3, ROUNDTRIP.md known gaps: field groups of generic declarations as the source has
;; them, with the proposed :go/grouped marker (PRINTER-NOTES, amendment A1).
(ns go.groups (:require [arbace.go :as go]))
(go/package groups :path "groups" :files ["s05_groups.go"])
(go/file "s05_groups.go" :imports [])

(go/func F :type-params [^any T ^any U] [^T a ^T b ^U c ^{:tag U :go/grouped true} d]
  :results [^T x ^{:tag T :go/grouped true} y]
  (return a b))

(go/func G :type-params [T ^:go/grouped U] [^T a ^{:tag T :go/grouped true} b] :results [T U]
  (let [^U u (zero U)]
    (return a u)))

(go/type S :type-params [T]
  (struct ^T a ^{:tag T :go/grouped true} b ^T c ^{:tag (func [^T x ^{:tag T :go/grouped true} y])} f))
