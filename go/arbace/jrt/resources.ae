;; jrt: the natives of jdk.internal.jrt.ClassPathResources (overlay/jdk/java.base/jdk/internal/jrt/
;; ClassPathResources.java, doc/go/JRT-NOTES.md "Resources by URL"): the embedded resources by name, and
;; the executable's path, which their jar: URLs name; VM.isSystemDomainLoader, which URL asks of
;; their handler.
(in-ns 'go.arbace.jrt)

(go/file "resources.go")

(go/func ClassPathResources_Embedded0_String__Z_native
  "ClassPathResources_Embedded0_String__Z_native tells whether name is a resource embedded in the
executable (Host.Resource).\n"
  ^bool [^{:tag (* String)} name]
  (let [(values _ ok) (.Resource (CurrentHost) (.String name))]
    ok))

(go/func ClassPathResources_Bytes0_String__B1_native
  "ClassPathResources_Bytes0_String__B1_native is the embedded resource's bytes, or null.\n"
  ^{:tag (* ByteArray)} [^{:tag (* String)} name]
  (let [(values b ok) (.Resource (CurrentHost) (.String name))]
    (when (not ok)
      (return nil))
    (let [a (NewByteArray (conv int32 (len b)))]
      (range [i x b] (aset (.-A a) i (conv int8 x)))
      a)))

(go/type HostExecutable
  "HostExecutable is a host that can tell the executable's path (the embedded resources' jar:
URLs name it): a Host may implement it (B1a's OSHost does on Linux, resources_linux.clj).\n"
  (interface
    (^{:doc "Executable is the executable's absolute path, symbolic links resolved.\n"}
      Executable [] :results [string error])))

(go/func ClassPathResources_Executable0__String_native
  "ClassPathResources_Executable0__String_native is the executable's absolute path, or \"\" when
the host cannot tell (a host without HostExecutable: B1b's box).\n"
  ^{:tag (* String)} []
  (let [(values he ok) (assert HostExecutable (CurrentHost))]
    (when (not ok)
      (return (Str "")))
    (let [(values p err) (.Executable he)]
      (when (!= err nil)
        (return (Str "")))
      (Str p))))

(go/func VM_IsSystemDomainLoader_ClassLoader__Z
  "VM_IsSystemDomainLoader_ClassLoader__Z is jdk.internal.misc.VM.isSystemDomainLoader, as the
JDK's: the bootstrap loader (null) or the platform loader. URL asks it of a URL's handler that is
not one of the JDK's (the embedded resources' jar: handler, ClassPathResources').\n"
  ^bool [^ClassLoader_I loader]
  (or (== loader nil) (== loader (ClassLoader_GetPlatformClassLoader__ClassLoader))))
