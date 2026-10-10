;; jrt: the system properties of JAVA_TOOL_OPTIONS (doc/go/JRT-NOTES.md, "Sockets"). The JVM reads
;; this environment variable at its start, as options before the command line's: its -Dname=value
;; options are system properties, so that one setting configures both builds (the socket server's
;; arbace.server.* properties, which RT.doInit starts). The text is split as HotSpot's
;; Arguments::parse_options_buffer splits it: at white space outside quotes, a quote (' or ")
;; grouping up to the same quote, the quotes removed. Options other than -D are ignored (the JVM's
;; are the JVM's).
(in-ns 'go.arbace.jrt)

(go/file "tooloptions.go"
  :imports [[strings "strings"]])

(go/func splitToolOptions
  "splitToolOptions splits s into options as HotSpot splits JAVA_TOOL_OPTIONS.\n"
  ^{:tag (slice string)} [^string s]
  (let [^{:tag (slice string)} opts nil
        b (lit strings/Builder)
        in false
        ^byte quote 0]
    (for [i 0] (< i (len s)) (inc! i)
      (let [c (aget s i)]
        (cond
          (!= quote 0) (if (== c quote) (set! quote 0) (.WriteByte b c))
          (or (== c \') (== c \")) (do (set! quote c) (set! in true))
          (or (== c \space) (== c \tab) (== c \newline) (== c \return))
            (when in
              (set! opts (append opts (.String b)))
              (.Reset b)
              (set! in false))
          :else (do (.WriteByte b c) (set! in true)))))
    (when in
      (set! opts (append opts (.String b))))
    opts))

(go/func toolOptionProps
  "toolOptionProps sets, with set, the -Dname=value options of the host's JAVA_TOOL_OPTIONS
(-Dname alone: the empty value).\n"
  [^Host h ^{:tag (func [string string])} set]
  (let [(values v ok) (.Getenv h "JAVA_TOOL_OPTIONS")]
    (when (not ok)
      (return))
    (range [_ o (splitToolOptions v)]
      (when (and (strings/HasPrefix o "-D") (> (len o) 2))
        (let [kv (subslice o 2)
              i (strings/IndexByte kv \=)]
          (if (< i 0)
            (set kv "")
            (when (> i 0)
              (set (subslice kv 0 i) (subslice kv (+ i 1))))))))))
