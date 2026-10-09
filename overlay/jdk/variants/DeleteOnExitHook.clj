;; Go-build variant of java.io.DeleteOnExitHook (C2G-SPEC §4.6; doc/go/JRT-NOTES.md, "Files"):
;; read by c2g only. The hook registers itself with Runtime.addShutdownHook, jrt's, where the
;; JDK registers it in the VM's own hook slots through SharedSecrets (jrt's JavaLangAccess has
;; no registerShutdownHook); it runs at System.exit and when main and the non-daemon threads
;; have ended, as on the JVM. Copyright (c) the Arbace authors; Eclipse Public License 1.0.
(in-ns 'java.io)

(c2g/variant DeleteOnExitHook
  (static-initializer
    (.addShutdownHook (Runtime/getRuntime)
                      (Thread. (anon Runnable []
                                 (method ^:public run ^void [this] (DeleteOnExitHook/runHooks)))))))
