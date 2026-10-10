;; jrt: B1a's OSHost as a HostFS (hostfs.clj; doc/go/JRT-NOTES.md, "Files"), over Go's os and
;; syscall on Linux: the calls jdk26u's UnixFileSystem_md.c makes (access, chmod, utimes,
;; realpath, statvfs, pathconf). Other systems (TamaGo's, B1b) get their own host.
(in-ns 'go.arbace.jrt)

(go/file "hostfs_linux.go"
  :build ["//go:build linux"]
  :imports [[errors "errors"] [fs "io/fs"] [os "os"] [filepath "path/filepath"] [syscall "syscall"]
            [time "time"]])

(go/method Access ^error [^OSHost h ^string name ^uint32 mode]
  (syscall/Access name mode))

(go/method Chmod ^error [^OSHost h ^string name ^uint32 mode]
  (syscall/Chmod name mode))

(go/method Chtimes ^error [^OSHost h ^string name ^int64 mtimeMillis]
  ;; a zero time.Time leaves the access time as it is (UTIME_OMIT)
  (os/Chtimes name (lit time/Time) (time/UnixMilli mtimeMillis)))

(go/method Realpath [^OSHost h ^string name] :results [string error]
  (let [(values abs err) (filepath/Abs name)]
    (when (!= err nil)
      (return "" err))
    (let [(values r err2) (filepath/EvalSymlinks abs)]
      (when (!= err2 nil)
        (when (or (errors/Is err2 syscall/ENOTDIR) (errors/Is err2 syscall/ENOENT))
          (return "" fs/ErrNotExist))
        (when (errors/Is err2 syscall/EACCES)
          (return "" fs/ErrPermission))
        (return "" err2))
      (return r nil))))

(go/method Statfs [^OSHost h ^string name] :results [HostFSInfo error]
  (let [^{:tag syscall/Statfs_t} st (lit syscall/Statfs_t)
        err (syscall/Statfs name (addr st))]
    (when (!= err nil)
      (return (lit HostFSInfo) err))
    (let [bs (conv int64 (.-Frsize st))]
      (when (== bs 0)
        (set! bs (conv int64 (.-Bsize st))))
      (return (lit HostFSInfo :Total (* bs (conv int64 (.-Blocks st)))
                   :Free (* bs (conv int64 (.-Bfree st)))
                   :Usable (* bs (conv int64 (.-Bavail st)))
                   :NameMax (conv int64 (.-Namelen st)))
              nil))))
