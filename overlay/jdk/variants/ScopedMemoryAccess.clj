;; Go-build variant of jdk.internal.misc.ScopedMemoryAccess (C2G-SPEC §4.6; JRT-NOTES.md, "The
;; JDK's resource data"): read by c2g only. The class's static initializer registers its VM
;; natives, of which closeScope0 closes a shared memory session (a handshake with the threads
;; accessing it). The Go build has no memory sessions to close (the heap buffers' session is
;; null), so the variant removes the static initializer and registerNatives, and closeScope0
;; throws UnsupportedOperationException instead of being a native. Copyright (c) the Arbace authors;
;; Eclipse Public License 1.0.
(in-ns 'jdk.internal.misc)

(c2g/variant ScopedMemoryAccess
  (c2g/cut (static-initializer 0))
  (c2g/cut ^:private ^:static registerNatives ^void [])
  (method closeScope0 ^void [this ^MemorySessionImpl session ^ScopedAccessError error]
    (throw (UnsupportedOperationException. "closeScope0: no memory sessions in the Go build"))))
