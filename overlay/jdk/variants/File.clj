;; Go-build variant of java.io.File (C2G-SPEC §4.6; doc/go/JRT-NOTES.md, "Files"): read by c2g
;; only. File is translated from jdk26u. Its temporary files' directory is java.io.tmpdir read
;; through System.getProperty (StaticProperty is not in the closed world), and their random
;; names come from java.util.Random (SecureRandom is not in the world either; createTempFile
;; creates the file exclusively, O_EXCL, so the names need not be unpredictable to be safe
;; from races). toPath makes the host's Path (jrt's jdk.internal.jrt.HostPath, the Go build's
;; UnixPath) where the JDK asks FileSystems.getDefault(). Copyright (c) the Arbace authors; Eclipse Public License 1.0.
(in-ns 'java.io)

(c2g/variant File$TempDirectory
  (field ^:private ^:static ^:final ^File TMPDIR (File. (System/getProperty "java.io.tmpdir")))
  (field ^:private ^:static ^:final ^java.util.Random RANDOM (java.util.Random.)))

(c2g/variant File
  (method ^:public toPath ^java.nio.file.Path [this]
    (let [^:mutable result filePath]
      (when (nil? result)
        (locking this
          (set! result filePath)
          (when (nil? result)
            (set! result (jdk.internal.jrt.HostPath/ofFile path))
            (set! filePath result))))
      result)))
