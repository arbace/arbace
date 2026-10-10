;; Go-build variant of sun.net.www.protocol.file.Handler (C2G-SPEC §4.6; doc/go/JRT-NOTES.md,
;; "Files"): read by c2g only. The handler is translated from jdk26u
;; (src/java.base/unix/classes); its openConnection(URL) calls openConnection(URL, Proxy), whose
;; Proxy is outside the closed world: the variant does what that call does with no proxy (a
;; local file's connection, else the JDK's refusal of non-local file URLs, the FTP fallback being
;; outside the world). Copyright (c) the Arbace authors; Eclipse Public License 1.0.
(in-ns 'sun.net.www.protocol.file)

(c2g/variant Handler
  (method ^:public openConnection :throws [IOException] ^URLConnection [this ^URL u]
    (when-not (ParseUtil/isLocalFileURL u)
      (FileURLConnection/requireFtpFallbackEnabled)
      (throw (IOException. (java-str "Unable to connect to: " (.toExternalForm u)))))
    (.createFileURLConnection this u (File. (ParseUtil/decode (.getPath u))))))
