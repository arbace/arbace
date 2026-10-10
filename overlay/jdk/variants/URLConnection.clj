;; Go-build variant of java.net.URLConnection (C2G-SPEC §4.6; doc/go/JRT-NOTES.md, "Files"):
;; read by c2g only. URLConnection is translated from jdk26u; its file name map (the MIME types
;; by file extension) is sun.net.www.MimeTable's, read from the JDK's content-types.properties,
;; outside the Go build: the variant's map knows no type (guessContentTypeFromName answers
;; null). Copyright (c) the Arbace authors; Eclipse Public License 1.0.
(in-ns 'java.net)

(c2g/variant URLConnection
  (method ^:public ^:static getFileNameMap ^FileNameMap []
    (let [^:mutable map fileNameMap]
      (when (nil? map)
        (set! fileNameMap
              (set! map
                    (anon FileNameMap []
                      (method ^:public getContentTypeFor ^String [this ^String fileName] nil)))))
      map)))
