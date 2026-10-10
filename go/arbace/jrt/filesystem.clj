;; jrt: the natives of java.io.UnixFileSystem's Go-build variant (overlay/jdk/variants/
;; UnixFileSystem.clj; doc/go/JRT-NOTES.md, "Files"): java.io.File's queries and changes over
;; the host (Host, and HostFS when the host has it), with the results of jdk26u's
;; src/java.base/unix/native/libjava/UnixFileSystem_md.c, canonicalize_md.c and path_util.c
;; (whose canonicalization and collapse of . and .. are transcribed here: LICENSE.md). Paths are
;; the File's path strings, in UTF-8 for the host as the JVM's sun.jnu.encoding.
(in-ns 'go.arbace.jrt)

(go/file "filesystem.go"
  :imports [[errors "errors"] [fs "io/fs"] [os "os"] [strings "strings"]])

;; java.io.FileSystem's constants
(go/const
  [^{:tag int32 :val 1} baExists 0x01]
  [^{:tag int32 :val 2} baRegular 0x02]
  [^{:tag int32 :val 4} baDirectory 0x04]
  [^{:tag int32 :val 0} spaceTotal 0]
  [^{:tag int32 :val 1} spaceFree 1]
  [^{:tag int32 :val 2} spaceUsable 2])

(go/func unixPerm
  "unixPerm is the Unix permission bits (07777) of a HostFileInfo's Mode (an fs.FileMode).\n"
  ^uint32 [^uint32 mode]
  (let [m (conv fs/FileMode mode)
        p (conv uint32 (bit-and m fs/ModePerm))]
    (when (!= (bit-and m fs/ModeSetuid) 0) (set! p (bit-or p 04000)))
    (when (!= (bit-and m fs/ModeSetgid) 0) (set! p (bit-or p 02000)))
    (when (!= (bit-and m fs/ModeSticky) 0) (set! p (bit-or p 01000)))
    p))

(go/func hostChmod "hostChmod is chmod(2) through the host's HostFS (false without one).\n"
  ^bool [^string name ^uint32 mode]
  (let [hfs (CurrentHostFS)]
    (and (!= hfs nil) (== (.Chmod hfs name mode) nil))))

;; ---------------------------------------------------------------------------------------
;; canonicalize_md.c and path_util.c

(go/func collapsePath
  "collapsePath is path_util.c's collapse: the names . removed, and each .. with the name
before it (a leading .. of an absolute path removed, of a relative one kept).\n"
  ^string [^string path]
  (let [abs (strings/HasPrefix path "/")
        names path]
    (when abs
      (set! names (subslice path 1)))
    (when (== names "")
      (return path))
    (let [ix (strings/Split names "/")
          dots false]
      (range [_ n ix]
        (when (or (== n ".") (== n ".."))
          (set! dots true)))
      (when (or (not dots) (< (len ix) 2))
        (return path))
      (let [keep (make (slice bool) (len ix))]
        (range [i _ ix]
          (aset keep i true))
        (range [i n ix]
          (cond
            (== n ".") (aset keep i false)
            (== n "..")
            (let [j (- i 1)]
              (while (and (>= j 0) (not (aget keep j)))
                (dec! j))
              (if (< j 0)
                (when abs
                  (aset keep i false))
                (do (aset keep j false)
                    (aset keep i false))))))
        (let [out (make (slice string) 0 (len ix))]
          (range [i n ix]
            (when (aget keep i)
              (set! out (append out n))))
          (let [r (strings/Join out "/")]
            (when abs
              (return (+ "/" r)))
            r))))))

(go/func skippable
  "skippable tells a realpath error that canonicalize_md.c passes over (ENOENT, ENOTDIR,
EACCES: HostFS.Realpath's fs.ErrNotExist and fs.ErrPermission).\n"
  ^bool [^error err]
  (or (errors/Is err fs/ErrNotExist) (errors/Is err fs/ErrPermission)))

(go/func canonicalizePath
  "canonicalizePath is JDK_Canonicalize: realpath of the whole path, else of its longest
prefix that resolves, the rest appended; . and .. collapsed. Without a HostFS, the path
collapsed.\n"
  [^string orig] :results [string error]
  (let [hfs (CurrentHostFS)]
    (when (== hfs nil)
      (return (collapsePath orig) nil))
    (let [(values r err) (.Realpath hfs orig)]
      (when (== err nil)
        (return (collapsePath r) nil))
      (let [p (len orig)]
        (while (> p 0)
          ;; skip the last name
          (dec! p)
          (while (and (> p 0) (!= (aget orig p) \/))
            (dec! p))
          (when (== p 0)
            (break))
          (let [(values r2 err2) (.Realpath hfs (subslice orig 0 p))]
            (when (== err2 nil)
              (let [rest (subslice orig p)]
                (when (and (strings/HasSuffix r2 "/") (strings/HasPrefix rest "/"))
                  (set! rest (subslice rest 1)))
                (return (collapsePath (+ r2 rest)) nil)))
            (when (not (skippable err2))
              (return "" err2))))
        (return (collapsePath orig) nil)))))

;; ---------------------------------------------------------------------------------------
;; UnixFileSystem's natives (the variant's host* methods)

(go/func UnixFileSystem_HostCanonicalize_String__String_native
  "UnixFileSystem_HostCanonicalize_String__String_native is canonicalize0: the canonical form
of path, or an IOException (the system's reason, else \"Bad pathname\").\n"
  ^{:tag (* String)} [^{:tag (* String)} path]
  (let [(values r err) (canonicalizePath (.String (NN path)))]
    (when (!= err nil)
      (let [msg (fileErrorText err)]
        (when (== msg "")
          (set! msg "Bad pathname"))
        (panic (IOException_New_String (Str msg)))))
    (Str r)))

(go/func UnixFileSystem_HostBooleanAttributes_String__I_native
  "UnixFileSystem_HostBooleanAttributes_String__I_native is getBooleanAttributes0: exists,
regular, directory (stat(2), following links).\n"
  ^int32 [^{:tag (* String)} path]
  (let [(values fi err) (.Stat (CurrentHost) (.String (NN path)))]
    (when (!= err nil)
      (return 0))
    (let [^int32 rv baExists]
      (cond (.-IsDir fi) (set! rv (bit-or rv baDirectory))
            (== (bit-and (conv fs/FileMode (.-Mode fi)) fs/ModeType) 0) (set! rv (bit-or rv baRegular)))
      rv)))

(go/func UnixFileSystem_HostCheckAccess_String_I__Z_native
  "UnixFileSystem_HostCheckAccess_String_I__Z_native is checkAccess0: access(2) with
FileSystem's ACCESS_READ (4), ACCESS_WRITE (2) or ACCESS_EXECUTE (1), which are R_OK, W_OK and
X_OK. Without a HostFS, whether the file exists.\n"
  ^bool [^{:tag (* String)} path ^int32 access]
  (let [name (.String (NN path))
        hfs (CurrentHostFS)]
    (when (== hfs nil)
      (let [(values _ err) (.Stat (CurrentHost) name)]
        (return (== err nil))))
    (== (.Access hfs name (conv uint32 access)) nil)))

(go/func UnixFileSystem_HostLastModifiedTime_String__J_native
  "UnixFileSystem_HostLastModifiedTime_String__J_native is getLastModifiedTime0: milliseconds
since the epoch, 0 when the file cannot be read.\n"
  ^int64 [^{:tag (* String)} path]
  (let [(values fi err) (.Stat (CurrentHost) (.String (NN path)))]
    (when (!= err nil)
      (return 0))
    (.-ModTimeMillis fi)))

(go/func UnixFileSystem_HostLength_String__J_native
  "UnixFileSystem_HostLength_String__J_native is getLength0: the size, 0 when the file cannot
be read.\n"
  ^int64 [^{:tag (* String)} path]
  (let [(values fi err) (.Stat (CurrentHost) (.String (NN path)))]
    (when (!= err nil)
      (return 0))
    (.-Size fi)))

(go/func UnixFileSystem_HostSetPermission_String_I_Z_Z__Z_native
  "UnixFileSystem_HostSetPermission_String_I_Z_Z__Z_native is setPermission0: the owner's
bit of access, or everyone's, set or cleared with chmod(2).\n"
  ^bool [^{:tag (* String)} path ^int32 access ^bool enable ^bool owneronly]
  (let [name (.String (NN path))
        ;; the owner's bit of the access: 0400, 0200 or 0100
        ^uint32 amode (<< (conv uint32 access) 6)]
    (when (not owneronly)
      (set! amode (bit-or amode (>> amode 3) (>> amode 6))))
    (let [(values fi err) (.Stat (CurrentHost) name)]
      (when (!= err nil)
        (return false))
      (let [mode (unixPerm (.-Mode fi))]
        (if enable
          (set! mode (bit-or mode amode))
          (set! mode (bit-and-not mode amode)))
        (hostChmod name mode)))))

(go/func UnixFileSystem_HostCreateFileExclusively_String__Z_native
  "UnixFileSystem_HostCreateFileExclusively_String__Z_native is createFileExclusively0: the
file created (O_CREAT|O_EXCL, 0666), false when it exists, an IOException otherwise.\n"
  ^bool [^{:tag (* String)} path]
  (let [name (.String (NN path))]
    ;; the root directory always exists
    (when (== name "/")
      (return false))
    (let [(values f err) (.Open (CurrentHost) name (bit-or os/O_RDWR os/O_CREATE os/O_EXCL) 0666)]
      (when (!= err nil)
        (when (errors/Is err fs/ErrExist)
          (return false))
        (panic (IOException_New_String (Str (fileErrorText err)))))
      (let [cerr (.Close f)]
        (when (!= cerr nil)
          (panic (IOException_New_String (Str (fileErrorText cerr))))))
      true)))

(go/func UnixFileSystem_HostDelete_String__Z_native
  "UnixFileSystem_HostDelete_String__Z_native is delete0: remove(3), a file or an empty
directory.\n"
  ^bool [^{:tag (* String)} path]
  (== (.Remove (CurrentHost) (.String (NN path))) nil))

(go/func UnixFileSystem_HostList_String__String1_native
  "UnixFileSystem_HostList_String__String1_native is list0: the names of the directory's
entries but . and .. (sorted: the host's ReadDir), null when it cannot be read.\n"
  ^{:tag (* RefArray)} [^{:tag (* String)} path]
  (let [(values names err) (.ReadDir (CurrentHost) (.String (NN path)))]
    (when (!= err nil)
      (return nil))
    (let [a (NewRefArray String_class (conv int32 (len names)))]
      (range [i n names]
        (aset (.-A a) i (Str n)))
      a)))

(go/func UnixFileSystem_HostCreateDirectory_String__Z_native
  "UnixFileSystem_HostCreateDirectory_String__Z_native is createDirectory0: mkdir(2), 0777.\n"
  ^bool [^{:tag (* String)} path]
  (== (.Mkdir (CurrentHost) (.String (NN path)) 0777) nil))

(go/func UnixFileSystem_HostRename_String_String__Z_native
  "UnixFileSystem_HostRename_String_String__Z_native is rename0: rename(2).\n"
  ^bool [^{:tag (* String)} from ^{:tag (* String)} to]
  (== (.Rename (CurrentHost) (.String (NN from)) (.String (NN to))) nil))

(go/func UnixFileSystem_HostSetLastModifiedTime_String_J__Z_native
  "UnixFileSystem_HostSetLastModifiedTime_String_J__Z_native is setLastModifiedTime0: the
modification time set, the access time kept.\n"
  ^bool [^{:tag (* String)} path ^int64 time]
  (let [name (.String (NN path))
        (values _ err) (.Stat (CurrentHost) name)
        hfs (CurrentHostFS)]
    (and (== err nil) (!= hfs nil) (== (.Chtimes hfs name time) nil))))

(go/func UnixFileSystem_HostSetReadOnly_String__Z_native
  "UnixFileSystem_HostSetReadOnly_String__Z_native is setReadOnly0: the write bits cleared.\n"
  ^bool [^{:tag (* String)} path]
  (let [name (.String (NN path))
        (values fi err) (.Stat (CurrentHost) name)]
    (when (!= err nil)
      (return false))
    (hostChmod name (bit-and-not (unixPerm (.-Mode fi)) 0222))))

(go/func UnixFileSystem_HostSpace_String_I__J_native
  "UnixFileSystem_HostSpace_String_I__J_native is getSpace0: statvfs(3)'s total, free or
usable bytes (SPACE_TOTAL, SPACE_FREE, SPACE_USABLE), 0 when it fails.\n"
  ^int64 [^{:tag (* String)} path ^int32 t]
  (let [name (.String (NN path))
        hfs (CurrentHostFS)]
    (when (== hfs nil)
      (return 0))
    (let [(values st err) (.Statfs hfs name)]
      (when (!= err nil)
        (return 0))
      (switch t
        (case [spaceTotal] (return (.-Total st)))
        (case [spaceFree] (return (.-Free st)))
        (case [spaceUsable] (return (.-Usable st))))
      0)))

(go/func UnixFileSystem_HostNameMax_String__J_native
  "UnixFileSystem_HostNameMax_String__J_native is getNameMax0: pathconf(3)'s _PC_NAME_MAX,
else NAME_MAX (255).\n"
  ^int64 [^{:tag (* String)} path]
  (let [hfs (CurrentHostFS)]
    (when (!= hfs nil)
      (let [(values st err) (.Statfs hfs (.String (NN path)))]
        (when (and (== err nil) (> (.-NameMax st) 0))
          (return (.-NameMax st)))))
    255))
