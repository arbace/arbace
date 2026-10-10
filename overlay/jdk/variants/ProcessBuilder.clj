;; Go-build variant of java.lang.ProcessBuilder (C2G-SPEC §4.6; doc/go/JRT-NOTES.md,
;; "Processes"): read by c2g only. start records no JFR event (jdk.internal.event is not in the
;; Go build) and logs nothing (System.Logger is not in the Go build; the JDK logs a start only
;; when the logger java.lang.ProcessBuilder is configured for DEBUG); the null file of
;; Redirect.DISCARD is /dev/null (jdk.internal.util.OperatingSystem is not in the Go build: the
;; platform is Linux). The replacing methods keep jdk26u's code where they keep it (baf63fb;
;; Copyright (c) Oracle and/or its affiliates, GPL 2 with the Classpath Exception: LICENSE.md);
;; the rest Copyright (c) the Arbace authors; Eclipse Public License 1.0.
(in-ns 'java.lang)

(c2g/variant ProcessBuilder
  (c2g/cut (field ^:private ^:static ^System$Logger LOGGER))

  (method ^:private start :throws [java.io.IOException] ^Process [this ^ProcessBuilder$Redirect/1 redirects]
    (let [^:mutable cmdarray (cast String/1 (.toArray command (new String/1 (.size command))))]
      (set! cmdarray (.clone cmdarray))
      (for-each [^String arg cmdarray] (when (nil? arg) (throw (NullPointerException.))))
      (let [prog (aget cmdarray 0)
            ^String dir (when (some? directory) (.toString directory))]
        (for-each [^String s cmdarray]
          (when (>= (.indexOf s \u0000) 0)
            (throw (java.io.IOException. "invalid null character in command"))))
        (try
          (ProcessImpl/start cmdarray environment dir redirects redirectErrorStream)
          (catch [java.io.IOException IllegalArgumentException] e
            (throw (java.io.IOException. (java-str "Cannot run program \""
                                                   prog
                                                   "\""
                                                   (if (nil? dir)
                                                       ""
                                                       (java-str " (in directory \"" dir "\")"))
                                                   ": "
                                                   (.getMessage e))
                                         e))))))))

(c2g/variant ProcessBuilder$Redirect
  (c2g/cut (field ^:private ^:static ^:final ^java.io.File NULL_FILE))
  (c2g/add (field ^:private ^:static ^:final ^java.io.File NULL_FILE (java.io.File. "/dev/null"))))
