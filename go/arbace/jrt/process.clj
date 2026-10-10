;; jrt: the natives of java.lang's processes (doc/go/JRT-NOTES.md, "Processes"): ProcessImpl's
;; fork and exec (the variant's hostForkAndExec, in place of jdk26u's forkAndExec over
;; jspawnhelper), ProcessHandleImpl's natives, ProcessHandleImpl.Info's (the variant's hostInfo)
;; and ProcessEnvironment's environ, over the host's optional HostProc (hostproc.clj). The
;; children's pipes are entries of jrt's file table (files.clj), so that the JDK's process
;; streams are FileInputStreams and FileOutputStreams over them, as on the JVM. What jdk26u's
;; C does is followed: the program's search on the PATH as this machine's JVM does it (musl's
;; execvp, or JDK_execvpe of childproc.c when the environment is given), the IOException texts
;; of ProcessImpl_md.c (IOE_FORMAT, with musl's strerror texts), the exit status encoding and
;; the start-time checks of ProcessHandleImpl_unix.c.
(in-ns 'go.arbace.jrt)

(go/file "process.go"
  :imports [[errors "errors"] [fs "io/fs"] [strconv "strconv"] [strings "strings"] [sync "sync"]
            [syscall "syscall"]])

;; ---------------------------------------------------------------------------------------
;; The C library's texts (strerror): this machine's JVM is built on musl

(go/var ^{:tag (map syscall/Errno string)} muslErrnoText
  (lit (map syscall/Errno string)
       [syscall/EPERM "Operation not permitted"]
       [syscall/ENOENT "No such file or directory"]
       [syscall/ESRCH "No such process"]
       [syscall/EINTR "Interrupted system call"]
       [syscall/EIO "I/O error"]
       [syscall/ENXIO "No such device or address"]
       [syscall/E2BIG "Argument list too long"]
       [syscall/ENOEXEC "Exec format error"]
       [syscall/EBADF "Bad file descriptor"]
       [syscall/ECHILD "No child process"]
       [syscall/EAGAIN "Resource temporarily unavailable"]
       [syscall/ENOMEM "Out of memory"]
       [syscall/EACCES "Permission denied"]
       [syscall/EFAULT "Bad address"]
       [syscall/EBUSY "Resource busy"]
       [syscall/EEXIST "File exists"]
       [syscall/ENODEV "No such device"]
       [syscall/ENOTDIR "Not a directory"]
       [syscall/EISDIR "Is a directory"]
       [syscall/EINVAL "Invalid argument"]
       [syscall/ENFILE "Too many open files in system"]
       [syscall/EMFILE "No file descriptors available"]
       [syscall/ETXTBSY "Text file busy"]
       [syscall/EFBIG "File too large"]
       [syscall/ENOSPC "No space left on device"]
       [syscall/ESPIPE "Invalid seek"]
       [syscall/EROFS "Read-only file system"]
       [syscall/EPIPE "Broken pipe"]
       [syscall/ENAMETOOLONG "Filename too long"]
       [syscall/ENOSYS "Function not implemented"]
       [syscall/ELOOP "Symbolic link loop"]
       [syscall/ESTALE "Stale file handle"]
       [syscall/ETIMEDOUT "Operation timed out"]))

(go/func ErrnoText
  "ErrnoText is strerror(e) as this machine's C library (musl) gives it.\n"
  ^string [^{:tag syscall/Errno} e]
  (let [(values t ok) (aget muslErrnoText e)]
    (when ok
      (return t)))
  (capitalized (.Error e)))

(go/func processIOException
  "processIOException is ProcessImpl_md.c's throwIOException: IOE_FORMAT, \"%s, error: %d (%s) %s\"
with an empty internal detail.\n"
  ^Throwable_I [^string detail ^{:tag syscall/Errno} e]
  (IOException_New_String
    (Str (+ detail ", error: " (strconv/Itoa (conv int e)) " (" (ErrnoText e) ") "))))

;; ---------------------------------------------------------------------------------------
;; The children the program started

(go/var ^{:tag sync/Mutex} childrenMu)
(go/var ^{:tag (map int HostChild)} children (make (map int HostChild)))

(go/func child "child is the child of pid the program started and has not reaped, or nil.\n"
  ^HostChild [^int pid]
  (.Lock childrenMu)
  (defer (.Unlock childrenMu))
  (aget children pid))

(go/func addHostFile "addHostFile enters f into jrt's file table (files.clj): its handle.\n"
  ^int32 [^HostFile f]
  (.Lock filesMu)
  (defer (.Unlock filesMu))
  (let [h nextFile]
    (inc! nextFile)
    (aset openFiles h f)
    h))

(go/func cString "cString is the bytes of b before its NUL (ProcessImpl's toCString), or \"\" for null.\n"
  ^string [^{:tag (* ByteArray)} b]
  (when (== b nil)
    (return ""))
  (let [n (len (.-A b))]
    (when (and (> n 0) (== (aget (.-A b) (- n 1)) 0))
      (set! n (- n 1)))
    (conv string (byteView b 0 (conv int32 n)))))

(go/func cStrings
  "cStrings is the count NUL-terminated strings of block (an argument or environment block).\n"
  ^{:tag (slice string)} [^{:tag (* ByteArray)} block ^int32 count]
  (let [r (make (slice string) 0 count)]
    (when (or (== block nil) (== count 0))
      (return r))
    (let [b (conv string (byteView block 0 (conv int32 (len (.-A block)))))
          i 0]
      (while (< (len r) (conv int count))
        (let [j (strings/IndexByte (subslice b i) 0)]
          (when (< j 0)
            (set! r (append r (subslice b i)))
            (break))
          (set! r (append r (subslice b i (+ i j))))
          (set! i (+ i j 1))))
      r)))

(go/var ^{:tag sync/Once} parentPathOnce)
(go/var ^{:tag (slice string)} parentPathv nil)

(go/func searchDirs
  "searchDirs is where a program name without a slash is looked for: with the environment given,
JDK_execvpe's parentPathv (the program's PATH when ProcessImpl was first used, else
\":/bin:/usr/bin\", an empty component \".\"); else musl's execvp's (PATH, else
\"/usr/local/bin:/bin:/usr/bin\", an empty component the name alone).\n"
  ^{:tag (slice string)} [^Host h ^bool envGiven]
  (when envGiven
    (.Do parentPathOnce
         (fn []
           (let [(values p ok) (.Getenv h "PATH")]
             (when (not ok)
               (set! p ":/bin:/usr/bin"))
             (range [_ d (strings/Split p ":")]
               (when (== d "")
                 (set! d "."))
               (set! parentPathv (append parentPathv d))))))
    (return parentPathv))
  (let [(values p ok) (.Getenv h "PATH")]
    (when (not ok)
      (set! p "/usr/local/bin:/bin:/usr/bin"))
    (strings/Split p ":")))

(go/func candidate "candidate is d/file (file alone for an empty d, as musl's execvp joins them).\n"
  ^string [^string d ^string file]
  (cond
    (== d "") (return file)
    (strings/HasSuffix d "/") (return (+ d file)))
  (+ d "/" file))

(go/func execSkippable
  "skippable tells whether an exec error lets the search go on to the next directory: musl's
execvp goes on after ENOENT, ENOTDIR and EACCES; JDK_execvpe also after ELOOP, ESTALE, ENODEV
and ETIMEDOUT.\n"
  ^bool [^{:tag syscall/Errno} e ^bool envGiven]
  (switch e
    (case [syscall/ENOENT syscall/ENOTDIR syscall/EACCES] (return true))
    (case [syscall/ELOOP syscall/ESTALE syscall/ENODEV syscall/ETIMEDOUT] (return envGiven)))
  false)

(go/func startProgram
  "startProgram starts file as the child's exec would find it (directly when it has a slash, else
on the search path), in dir: the child, the program's ends of its pipes, or the error
(*HostProcError) the JDK reports. A candidate that does not exist (as the host's Stat tells, from
dir for a relative one) is passed over without a fork; the change of directory is checked first,
as the child does it before the exec.\n"
  [^HostProc hp ^string file ^{:tag (slice string)} argv ^{:tag (slice string)} env ^bool envGiven
   ^string dir ^{:tag (slice HostStdio)} stdio ^bool errToOut]
  :results [HostChild (slice HostFile) error]
  (let [^{:tag (slice string)} cands nil]
    (if (strings/ContainsRune file \/)
      (set! cands (lit (slice string) file))
      (when (!= file "")
        (range [_ d (searchDirs (CurrentHost) envGiven)]
          (set! cands (append cands (candidate d file))))))
    (let [^{:tag syscall/Errno} last syscall/ENOENT
          sticky false
          tried false]
      (range [_ c cands]
        (let [p c]
          (when (and (!= dir "") (not (strings/HasPrefix c "/")))
            (set! p (+ dir "/" c)))
          (let [(values _ serr) (.Stat (CurrentHost) p)]
            (when (!= serr nil)
              (let [^{:tag syscall/Errno} e 0]
                (when (and (not (errors/As serr (addr e))) (errors/Is serr fs/ErrNotExist))
                  (set! e syscall/ENOENT))
                (when (or (== e syscall/ENOENT) (== e syscall/ENOTDIR))
                  (set! last e)
                  (continue))))))
        (set! tried true)
        (let [(values ch pipes err) (.Start hp c argv env dir stdio errToOut)]
          (when (== err nil)
            (return ch pipes nil))
          (let [pe (assert (* HostProcError) err)]
            (when (or (.-Chdir pe) (not (execSkippable (.-Errno pe) envGiven)))
              (return nil nil err))
            (when (== (.-Errno pe) syscall/EACCES)
              (set! sticky true))
            (set! last (.-Errno pe)))))
      (when (and (not tried) (!= dir ""))
        ;; nothing to exec: the child would still have changed directory first
        (let [(values ch pipes err) (.Start hp (+ dir "/\u0000") argv env dir stdio errToOut)]
          (when (== err nil)
            (return ch pipes nil))
          (when (.-Chdir (assert (* HostProcError) err))
            (return nil nil err))))
      (when sticky
        (set! last syscall/EACCES))
      (return nil nil (addr (lit HostProcError :Errno last))))))

(go/func ProcessImpl_HostForkAndExec_B1_B1_I_B1_I_B1_I1_Z__I_native
  "ProcessImpl_HostForkAndExec_B1_B1_I_B1_I_B1_I1_Z__I_native is ProcessImpl.forkAndExec
(ProcessImpl_md.c) over the host: prog, the argc arguments of argBlock, the envc variables of
envBlock (null: the program's environment), dir (null: the program's), and fds, the child's
standard streams (-1 a pipe, 0, 1 and 2 the program's own, else a handle of jrt's file table),
on return the handles of the program's ends of the pipes (-1 for the others); error output to
standard output when redirectErrorStream. It returns the child's pid, or throws the JDK's
IOException.\n"
  ^int32 [^{:tag (* ByteArray)} prog ^{:tag (* ByteArray)} argBlock ^int32 argc ^{:tag (* ByteArray)} envBlock
          ^int32 envc ^{:tag (* ByteArray)} dir ^{:tag (* IntArray)} fds ^bool redirectErrorStream]
  (let [hp (CurrentHostProc)]
    (when (== hp nil)
      (panic (processIOException "Exec failed" syscall/ENOSYS)))
    (let [file (cString prog)
          argv (append (lit (slice string) file) (spread (cStrings argBlock argc)))
          ^{:tag (slice string)} env nil
          stdio (make (slice HostStdio) 3)]
      (when (!= envBlock nil)
        (set! env (cStrings envBlock envc)))
      (range [i _ stdio]
        (let [h (aget (.-A fds) i)]
          (cond
            (== h -1) (aset stdio i (lit HostStdio :Kind StdioPipe))
            (<= h 2) (aset stdio i (lit HostStdio :Kind StdioInherit))
            :else (do (.Lock filesMu)
                      (let [f (aget openFiles h)]
                        (.Unlock filesMu)
                        (when (== f nil)
                          (panic (processIOException "Exec failed" syscall/EBADF)))
                        (aset stdio i (lit HostStdio :Kind StdioFile :File f)))))))
      (let [(values ch pipes err) (startProgram hp file argv env (!= envBlock nil) (cString dir) stdio
                                                redirectErrorStream)]
        (when (!= err nil)
          (let [pe (assert (* HostProcError) err)]
            (if (.-Chdir pe)
              (panic (processIOException "Failed to access working directory" (.-Errno pe)))
              (panic (processIOException "Exec failed" (.-Errno pe))))))
        (range [i p pipes]
          (if (!= p nil)
            (aset (.-A fds) i (addHostFile p))
            (aset (.-A fds) i -1)))
        (let [pid (.Pid ch)]
          (.Lock childrenMu)
          (aset children pid ch)
          (.Unlock childrenMu)
          (conv int32 pid))))))

;; ---------------------------------------------------------------------------------------
;; ProcessEnvironment

(go/func ProcessEnvironment_Environ__B2_native
  "ProcessEnvironment_Environ__B2_native is ProcessEnvironment.environ (ProcessEnvironment_md.c):
the program's environment as names and values, alternately, in bytes; a variable without = is
left out.\n"
  ^{:tag (* RefArray)} []
  (let [^{:tag (slice any)} r nil]
    (range [_ kv (.Environ (CurrentHost))]
      (let [i (strings/IndexByte kv \=)]
        (when (>= i 0)
          (set! r (append r (goBytes (subslice kv 0 i)) (goBytes (subslice kv (+ i 1))))))))
    (let [a (NewRefArray (.ArrayClass Prim_byte) (conv int32 (len r)))]
      (copy (.-A a) r)
      a)))

(go/func goBytes "goBytes is s's bytes as a new byte[].\n" ^{:tag (* ByteArray)} [^string s]
  (let [a (NewByteArray (conv int32 (len s)))]
    (range [i _ (.-A a)]
      (aset (.-A a) i (conv int8 (aget s i))))
    a))

;; ---------------------------------------------------------------------------------------
;; ProcessHandleImpl

(go/const
  [^{:tag int32 :val -2 :doc "notAChild is ProcessHandleImpl.NOT_A_CHILD.\n"} notAChild -2])

(go/func ProcessHandleImpl_GetCurrentPid0__J_native
  "ProcessHandleImpl_GetCurrentPid0__J_native is getpid(): the host's, 0 without processes.\n"
  ^int64 []
  (let [hp (CurrentHostProc)]
    (when (== hp nil)
      (return 0))
    (conv int64 (.Pid hp))))

(go/func ProcessHandleImpl_IsAlive0_J__J_native
  "ProcessHandleImpl_IsAlive0_J__J_native is isAlive0: process pid's start time in milliseconds
(0 when unknown), -1 when there is no such process.\n"
  ^int64 [^int64 pid]
  (let [hp (CurrentHostProc)]
    (when (== hp nil)
      (return -1))
    (let [(values st ok) (.ProcStat hp (conv int pid))]
      (when (not ok)
        (return -1))
      (.-StartMillis st))))

(go/func ProcessHandleImpl_WaitForProcessExit0_J_Z__I_native
  "ProcessHandleImpl_WaitForProcessExit0_J_Z__I_native is waitForProcessExit0: the exit status of
the program's child pid when it has ended (reaped, its entry gone, when reap), or NOT_A_CHILD for
another process. The host has reaped the child already (no zombie); this waits for that.\n"
  ^int32 [^int64 pid ^bool reap]
  (let [c (child (conv int pid))]
    (when (== c nil)
      (return notAChild))
    (let [status (.Wait c)]
      (when reap
        (.Lock childrenMu)
        (delete children (conv int pid))
        (.Unlock childrenMu))
      (conv int32 status))))

(go/func ProcessHandleImpl_Destroy0_J_J_Z__Z_native
  "ProcessHandleImpl_Destroy0_J_J_Z__Z_native is destroy0: SIGTERM, or SIGKILL when forcibly, to
pid if it is still the process of startTime; a child of the program through the host's handle
of it, which never signals a reaped child's reused pid.\n"
  ^bool [^int64 pid ^int64 startTime ^bool forcibly]
  (let [hp (CurrentHostProc)]
    (when (== hp nil)
      (return false))
    (let [c (child (conv int pid))]
      (when (!= c nil)
        (return (== (.Signal c forcibly) nil))))
    (let [start (ProcessHandleImpl_IsAlive0_J__J_native pid)]
      (when (or (== start startTime) (== start 0) (== startTime 0))
        (return (== (.Kill hp (conv int pid) forcibly) nil)))
      false)))

(go/func ProcessHandleImpl_Parent0_J_J__J_native
  "ProcessHandleImpl_Parent0_J_J__J_native is parent0: the parent of pid, -1 when unknown or
when pid is no longer the process of startTime.\n"
  ^int64 [^int64 pid ^int64 startTime]
  (let [hp (CurrentHostProc)]
    (when (== hp nil)
      (return -1))
    (let [(values st ok) (.ProcStat hp (conv int pid))]
      (when (not ok)
        (return -1))
      (when (and (!= (conv int pid) (.Pid hp))
                 (!= (.-StartMillis st) startTime) (!= (.-StartMillis st) 0) (!= startTime 0))
        (return -1))
      (conv int64 (.-Ppid st)))))

(go/func ProcessHandleImpl_GetProcessPids0_J_J1_J1_J1__I_native
  "ProcessHandleImpl_GetProcessPids0_J_J1_J1_J1__I_native is getProcessPids0 (os_getChildren):
the processes whose parent is pid (all for 0), their parents and start times, as many as the
arrays hold; the count of them all.\n"
  ^int32 [^int64 pid ^{:tag (* LongArray)} pids ^{:tag (* LongArray)} ppids ^{:tag (* LongArray)} stimes]
  (when (== pids nil)
    (panic (NPE)))
  (let [n (len (.-A pids))]
    (when (or (and (!= ppids nil) (!= (len (.-A ppids)) n)) (and (!= stimes nil) (!= (len (.-A stimes)) n)))
      (panic (IllegalArgumentException_New_String (Str "array sizes not equal"))))
    (let [hp (CurrentHostProc)
          ^int32 count 0]
      (when (== hp nil)
        (return 0))
      (range [_ p (.Pids hp)]
        (let [(values st ok) (.ProcStat hp p)]
          (when (and ok (or (== pid 0) (== (conv int64 (.-Ppid st)) pid)))
            (when (< (conv int count) n)
              (aset (.-A pids) count (conv int64 p))
              (when (!= ppids nil)
                (aset (.-A ppids) count (conv int64 (.-Ppid st))))
              (when (!= stimes nil)
                (aset (.-A stimes) count (.-StartMillis st))))
            (inc! count))))
      count)))

(go/func ProcessHandleImpl_Info_HostInfo_J_J1__String1_native
  "ProcessHandleImpl_Info_HostInfo_J_J1__String1_native is ProcessHandleImpl.Info's info0 for the
variant: times gets the CPU time and the start (when the process exists); the result is the
command, the command line, the user, a marker (\"\" when the arguments are known, null
otherwise) and the arguments, each null when unknown.\n"
  ^{:tag (* RefArray)} [^int64 pid ^{:tag (* LongArray)} times]
  (let [hp (CurrentHostProc)
        opt (fn ^any [^string s] (when (== s "") (return nil)) (return (Str s)))]
    (when (== hp nil)
      (return (RefArrayOf String_class nil nil nil nil)))
    (let [(values st ok) (.ProcStat hp (conv int pid))]
      (when ok
        (aset (.-A times) 0 (.-TotalNanos st))
        (aset (.-A times) 1 (.-StartMillis st))))
    (let [info (.ProcInfo hp (conv int pid))
          r (RefArrayOf String_class (opt (.-Command info)) (opt (.-CommandLine info)) (opt (.-User info)) nil)]
      (when (.-HasArgs info)
        (aset (.-A r) 3 (Str ""))
        (range [_ a (.-Args info)]
          (set! (.-A r) (append (.-A r) (Str a)))))
      r)))
