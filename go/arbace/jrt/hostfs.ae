;; jrt: the host's file system beyond Host's basic file calls (doc/go/JRT-NOTES.md, "Files"):
;; what java.io.File asks of the operating system that Host (host.clj) does not offer, as an
;; optional second interface, so that Host stays as it is. A host implements HostFS as well when
;; it can (B1a's OSHost does on Linux, hostfs_linux.clj); without it, java.io.File's permission,
;; time-setting, canonical-path and space queries answer as the JDK does when the system call
;; fails.
(in-ns 'go.arbace.jrt)

(go/file "hostfs.go")

(go/type HostFS
  "HostFS is the file system calls of java.io.File beyond Host's: a Host may implement it.\n"
  (interface
    (^{:doc "Access is access(2): mode 4 read, 2 write, 1 execute (0: exists).\n"}
      Access ^error [^string name ^uint32 mode])
    (^{:doc "Chmod sets the Unix permission bits (07777) of name.\n"}
      Chmod ^error [^string name ^uint32 mode])
    (^{:doc "Chtimes sets name's modification time, in milliseconds since the epoch; the access
time is kept.\n"}
      Chtimes ^error [^string name ^int64 mtimeMillis])
    (^{:doc "Realpath is realpath(3): name with every symbolic link, . and .. resolved; an error
that is fs.ErrNotExist or fs.ErrPermission when a name of the path does not exist, is not a
directory, or cannot be searched.\n"}
      Realpath [^string name] :results [string error])
    (^{:doc "Statfs tells the space of the file system holding name, and its longest name.\n"}
      Statfs [^string name] :results [HostFSInfo error])))

(go/type HostFSInfo
  "HostFSInfo is what Statfs tells: bytes in all, free, and free to the user; the longest name.\n"
  (struct ^int64 Total ^int64 Free ^int64 Usable ^int64 NameMax))

(go/func CurrentHostFS
  "CurrentHostFS is the current host as a HostFS, or nil when it is not one.\n"
  ^HostFS []
  (let [(values fs ok) (assert HostFS (CurrentHost))]
    (when (not ok)
      (return nil))
    fs))
