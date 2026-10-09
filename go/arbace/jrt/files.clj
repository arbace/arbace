;; jrt: the natives of jdk.internal.jrt.HostFiles (overlay/jdk/java.base/jdk/internal/jrt/
;; HostFiles.java, doc/go/JRT-NOTES.md "Phase 2C"), the file table under the FileDescriptor,
;; FileInputStream and FileOutputStream written for jrt: handles 0, 1 and 2 are the host's
;; standard streams (Stdin, Stdout, Stderr), the others the files the host opened (Host.Open).
(in-ns 'go.arbace.jrt)

(go/file "files.go"
  :imports [[errors "errors"] [io "io"] [fs "io/fs"] [os "os"] [sync "sync"] [unsafe "unsafe"]])

(go/func byteView "byteView is n bytes of b from off as Go's []byte (shared, not copied).\n"
  ^{:tag (slice byte)} [^{:tag (* ByteArray)} b ^int32 off ^int32 n]
  (when (== n 0)
    (return nil))
  (unsafe/Slice (conv (* byte) (conv unsafe/Pointer (addr (aget (.-A b) off)))) n))

(go/var ^{:tag sync/Mutex} filesMu)
(go/var ^{:tag (map int32 HostFile)} openFiles (make (map int32 HostFile)))
(go/var ^int32 nextFile 3)

(go/func hostFile "hostFile is the open file of handle h (> 2), or an IOException.\n"
  ^HostFile [^int32 h]
  (.Lock filesMu)
  (let [f (aget openFiles h)]
    (.Unlock filesMu)
    (when (== f nil)
      (panic (IOException_New_String (Str "Stream Closed"))))
    f))

(go/func ioError "ioError is the IOException of a host error.\n"
  ^Throwable_I [^error err]
  (IOException_New_String (Str (.Error err))))

(go/func fileErrorText
  "fileErrorText is the reason the JVM's FileNotFoundException gives in parentheses.\n"
  ^string [^error err]
  (cond
    (errors/Is err fs/ErrNotExist) (return "No such file or directory")
    (errors/Is err fs/ErrPermission) (return "Permission denied")
    (errors/Is err os/ErrExist) (return "File exists"))
  (let [(values pe ok) (assert (* fs/PathError) err)]
    (when ok
      (return (.Error (.-Err pe))))
    (.Error err)))

(go/func HostFiles_Open0_String_I_String1__I_native
  "HostFiles_Open0_String_I_String1__I_native opens path for reading (mode 0), writing (1:
created or truncated) or appending (2): its handle, or -1 with the reason in error[0].\n"
  ^int32 [^{:tag (* String)} path ^int32 mode ^{:tag (* RefArray)} error]
  (let [flag os/O_RDONLY]
    (switch mode
      (case [1] (set! flag (bit-or os/O_WRONLY os/O_CREATE os/O_TRUNC)))
      (case [2] (set! flag (bit-or os/O_WRONLY os/O_CREATE os/O_APPEND))))
    (let [name (.String path)]
      (when (== mode 0)
        ;; the JVM refuses to open a directory for reading
        (let [(values fi err) (.Stat (CurrentHost) name)]
          (when (and (== err nil) (.-IsDir fi))
            (aset (.-A error) 0 (Str "Is a directory"))
            (return -1))))
      (let [(values f err) (.Open (CurrentHost) name flag 0666)]
        (when (!= err nil)
          (aset (.-A error) 0 (Str (fileErrorText err)))
          (return -1))
        (.Lock filesMu)
        (defer (.Unlock filesMu))
        (let [h nextFile]
          (inc! nextFile)
          (aset openFiles h f)
          (return h))))))

(go/func HostFiles_Close0_I__V_native
  "HostFiles_Close0_I__V_native closes handle h (> 2).\n"
  [^int32 h]
  (let [f (hostFile h)]
    (.Lock filesMu)
    (delete openFiles h)
    (.Unlock filesMu)
    (let [err (.Close f)]
      (when (!= err nil)
        (panic (ioError err))))))

(go/func HostFiles_Write0_I_B1_I_I__V_native
  "HostFiles_Write0_I_B1_I_I__V_native writes len bytes of b from off to handle h.\n"
  [^int32 h ^{:tag (* ByteArray)} b ^int32 off ^int32 n]
  (let [p (byteView b off n)
        ^{:tag io/Writer} w nil]
    (cond (== h 1) (set! w Stdout)
          (== h 2) (set! w Stderr)
          (== h 0) (panic (IOException_New_String (Str "Bad file descriptor")))
          :else (set! w (hostFile h)))
    (let [(values _ err) (.Write w p)]
      (when (!= err nil)
        (panic (ioError err))))))

(go/func HostFiles_Read0_I_B1_I_I__I_native
  "HostFiles_Read0_I_B1_I_I__I_native reads up to len (> 0) bytes of handle h into b at off:
the count, or -1 at the end.\n"
  ^int32 [^int32 h ^{:tag (* ByteArray)} b ^int32 off ^int32 n]
  (let [p (byteView b off n)
        ^{:tag io/Reader} r nil]
    (cond (== h 0) (set! r Stdin)
          (or (== h 1) (== h 2)) (panic (IOException_New_String (Str "Bad file descriptor")))
          :else (set! r (hostFile h)))
    (while true
      (let [(values k err) (.Read r p)]
        (when (> k 0)
          (return (conv int32 k)))
        (when (errors/Is err io/EOF)
          (return -1))
        (when (!= err nil)
          (panic (ioError err)))))))

(go/func HostFiles_Available0_I__I_native
  "HostFiles_Available0_I__I_native is what handle h gives without blocking: a file's bytes
after its position, 0 for the standard input.\n"
  ^int32 [^int32 h]
  (when (<= h 2)
    (return 0))
  (let [f (hostFile h)
        (values cur err) (.Seek f 0 io/SeekCurrent)]
    (when (!= err nil)
      (return 0))
    (let [(values end err2) (.Seek f 0 io/SeekEnd)]
      (when (!= err2 nil)
        (return 0))
      (.Seek f cur io/SeekStart)
      (let [k (- end cur)]
        (cond (< k 0) (return 0)
              (> k 0x7fffffff) (return 0x7fffffff))
        (conv int32 k)))))

(go/func HostFiles_Skip0_I_J__J_native
  "HostFiles_Skip0_I_J__J_native skips up to n (> 0) bytes of handle h: a file seeks (past its
end, as the JVM's lseek), the standard input reads them.\n"
  ^int64 [^int32 h ^int64 n]
  (when (> h 2)
    (let [f (hostFile h)
          (values cur err) (.Seek f 0 io/SeekCurrent)]
      (when (!= err nil)
        (panic (ioError err)))
      (let [(values pos err2) (.Seek f n io/SeekCurrent)]
        (when (!= err2 nil)
          (panic (ioError err2)))
        (return (- pos cur)))))
  (let [buf (make (slice byte) 8192)
        ^int64 done 0]
    (while (< done n)
      (let [k (conv int64 (len buf))]
        (when (> k (- n done))
          (set! k (- n done)))
        (let [(values r err) (.Read Stdin (subslice buf 0 k))]
          (set! done + (conv int64 r))
          (when (!= err nil)
            (break)))))
    done))
