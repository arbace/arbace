;; jrt: B1a's OSHost as a HostProc (hostproc.clj; doc/go/JRT-NOTES.md, "Processes"), over Go's
;; os and syscall on Linux. A child is started by os.StartProcess (fork and exec from a
;; multi-threaded program, as Go does it: clone with CLONE_VFORK, the exec's errno reported back
;; through a close-on-exec pipe, the failed child reaped), with pipes of os.Pipe (close-on-exec:
;; only the child's three streams reach it). A goroutine waits for each child (os.Process.Wait,
;; over a pidfd), so a child that ends is reaped at once and leaves no zombie, whether or not the
;; program waits for it. The processes are read from /proc as jdk26u's
;; src/java.base/linux/native/libjava/ProcessHandleImpl_linux.c reads them (os_getChildren,
;; os_getParentPidAndTimings, os_getCmdlineAndUserInfo, getBoottime) and
;; src/java.base/unix/native/libjava/ProcessHandleImpl_unix.c (unix_getUserInfo: the user's name
;; by getpwuid, here /etc/passwd's line of the uid, as musl's getpwuid reads it).
(in-ns 'go.arbace.jrt)

(go/file "hostproc_linux.go"
  :build ["//go:build linux"]
  :imports [[bytes "bytes"] [errors "errors"] [io "io"] [os "os"] [strconv "strconv"] [strings "strings"]
            [sync "sync"] [syscall "syscall"] [unsafe "unsafe"]])

;; ---------------------------------------------------------------------------------------
;; Starting a child

(go/type osChild
  "osChild is a child OSHost.Start started; done is closed when the waiting goroutine has
reaped it, status then its JDK-encoded status.\n"
  (struct ^{:tag (* os/Process)} p ^{:tag (chan (struct))} done ^int status))

(go/method Pid ^int [^{:tag (* osChild)} c] (.-Pid (.-p c)))

(go/method Wait ^int [^{:tag (* osChild)} c]
  (<! (.-done c))
  (.-status c))

(go/method Signal ^error [^{:tag (* osChild)} c ^bool force]
  (let [^{:tag syscall/Signal} sig syscall/SIGTERM]
    (when force
      (set! sig syscall/SIGKILL))
    (.Signal (.-p c) sig)))

(go/func waitStatus
  "waitStatus is the JDK's encoding of a wait status (ProcessHandleImpl_unix.c's
waitForProcessExit0): the exit status, or 0x80 plus the terminating signal.\n"
  ^int [^{:tag syscall/WaitStatus} ws]
  (cond
    (.Exited ws) (return (.ExitStatus ws))
    (.Signaled ws) (return (+ 0x80 (conv int (.Signal ws)))))
  (conv int ws))

(go/func errnoOf "errnoOf is the system error number of err (EIO when it has none).\n"
  ^{:tag syscall/Errno} [^error err]
  (let [^{:tag syscall/Errno} e 0]
    (when (errors/As err (addr e))
      (return e))
    syscall/EIO))

(go/func checkDir
  "checkDir is the error the child's chdir(dir) would meet (ENOENT, ENOTDIR, EACCES ...), or 0.\n"
  ^{:tag syscall/Errno} [^string dir]
  (let [^{:tag syscall/Stat_t} st (lit syscall/Stat_t)]
    (let [err (syscall/Stat dir (addr st))]
      (when (!= err nil)
        (return (errnoOf err))))
    (when (!= (bit-and (.-Mode st) syscall/S_IFMT) syscall/S_IFDIR)
      (return syscall/ENOTDIR))
    (let [err (syscall/Access dir 1)]
      (when (!= err nil)
        (return (errnoOf err))))
    0))

(go/method Start [^OSHost h ^string path ^{:tag (slice string)} argv ^{:tag (slice string)} env ^string dir
                  ^{:tag (slice HostStdio)} stdio ^bool errToOut]
  :results [HostChild (slice HostFile) error]
  (when (!= dir "")
    (let [e (checkDir dir)]
      (when (!= e 0)
        (return nil nil (addr (lit HostProcError :Chdir true :Errno e))))))
  (let [files (make (slice (* os/File)) 3)
        own (lit (slice (* os/File)) os/Stdin os/Stdout os/Stderr)
        pipes (make (slice HostFile) 3)
        ^{:tag (slice (* os/File))} childEnds nil
        ^{:tag (slice (* os/File))} parentEnds nil
        fail (fn [^{:tag syscall/Errno} e] :results [HostChild (slice HostFile) error]
               (range [_ f childEnds] (.Close f))
               (range [_ f parentEnds] (.Close f))
               (return nil nil (addr (lit HostProcError :Errno e))))]
    (range [i s stdio]
      (when (and (== i 2) errToOut)
        ;; the child's error output is its standard output; a pipe is still made, its end in
        ;; the child closed (ProcessImpl_md.c makes the err pipe whether or not
        ;; redirectErrorStream; the stream reads its end at once)
        (aset files 2 (aget files 1))
        (when (== (.-Kind s) StdioPipe)
          (let [(values r w err) (os/Pipe)]
            (when (!= err nil)
              (return (fail (errnoOf err))))
            (set! childEnds (append childEnds w))
            (set! parentEnds (append parentEnds r))
            (aset pipes 2 (addr (lit procPipe :f r)))))
        (continue))
      (switch (.-Kind s)
        (case [StdioPipe]
          (let [(values r w err) (os/Pipe)]
            (when (!= err nil)
              (return (fail (errnoOf err))))
            (if (== i 0)
              (do (aset files 0 r)
                  (set! childEnds (append childEnds r))
                  (set! parentEnds (append parentEnds w))
                  (aset pipes 0 (addr (lit procPipe :f w))))
              (do (aset files i w)
                  (set! childEnds (append childEnds w))
                  (set! parentEnds (append parentEnds r))
                  (aset pipes i (addr (lit procPipe :f r)))))))
        (case [StdioInherit]
          (aset files i (aget own i)))
        (default
          (let [f (osFileOf (.-File s))]
            (when (== f nil)
              (return (fail syscall/EBADF)))
            (aset files i f)))))
    (let [(values p err) (os/StartProcess path argv (addr (lit os/ProcAttr :Dir dir :Env env :Files files)))]
      (range [_ f childEnds] (.Close f))
      (when (!= err nil)
        (range [_ f parentEnds] (.Close f))
        (return nil nil (addr (lit HostProcError :Errno (errnoOf err)))))
      (let [c (addr (lit osChild :p p :done (make (chan (struct)))))]
        (go ((fn []
               (let [(values st werr) (.Wait p)]
                 (if (!= werr nil)
                   (set! (.-status c) -1)
                   (set! (.-status c) (waitStatus (assert syscall/WaitStatus (.Sys st))))))
               (close (.-done c)))))
        (return c pipes nil)))))

(go/func osFileOf
  "osFileOf is the *os.File of an open file of the host (Host.Open's, or a pipe end), or nil.\n"
  ^{:tag (* os/File)} [^HostFile f]
  (let [(values of ok) (assert (* os/File) f)]
    (when ok
      (return of)))
  (let [(values pp ok) (assert (* procPipe) f)]
    (when ok
      (return (.-f pp))))
  nil)

;; ---------------------------------------------------------------------------------------
;; The program's ends of a child's pipes

(go/type procPipe
  "procPipe is the program's end of a pipe to or from a child, a HostFile of jrt's file table
(HostFiles): reads and writes fail with the C library's texts, as the JVM's do (Broken pipe); a
read on an end another thread closed ends the stream; Available is FIONREAD's count.\n"
  (struct ^{:tag (* os/File)} f))

(go/method Read [^{:tag (* procPipe)} p ^{:tag (slice byte)} b] :results [int error]
  (let [(values n err) (.Read (.-f p) b)]
    (when (and (!= err nil) (errors/Is err os/ErrClosed))
      (return n io/EOF))
    (when (and (!= err nil) (not (errors/Is err io/EOF)))
      (return n (errors/New (ErrnoText (errnoOf err)))))
    (return n err)))

(go/method Write [^{:tag (* procPipe)} p ^{:tag (slice byte)} b] :results [int error]
  (let [(values n err) (.Write (.-f p) b)]
    (when (!= err nil)
      (when (errors/Is err os/ErrClosed)
        (return n (errors/New "Stream Closed")))
      (return n (errors/New (ErrnoText (errnoOf err)))))
    (return n nil)))

(go/method Close ^error [^{:tag (* procPipe)} p] (.Close (.-f p)))

(go/method Seek [^{:tag (* procPipe)} p ^int64 offset ^int whence] :results [int64 error]
  (return 0 (errors/New (ErrnoText syscall/ESPIPE))))

(go/method Available [^{:tag (* procPipe)} p] :results [int error]
  (let [(values rc err) (.SyscallConn (.-f p))]
    (when (!= err nil)
      (return 0 err))
    (let [^int32 n 0]
      (.Control rc (fn [^uintptr fd]
                     (syscall/Syscall syscall/SYS_IOCTL fd (conv uintptr syscall/TIOCINQ)
                                      (conv uintptr (conv unsafe/Pointer (addr n))))))
      (return (conv int n) nil))))

;; ---------------------------------------------------------------------------------------
;; The processes, from /proc

(go/method Pid ^int [^OSHost h] (os/Getpid))

(go/method Kill ^error [^OSHost h ^int pid ^bool force]
  (let [^{:tag syscall/Signal} sig syscall/SIGTERM]
    (when force
      (set! sig syscall/SIGKILL))
    (syscall/Kill pid sig)))

(go/const
  [^{:tag int64 :val 100 :doc "clockTicks is sysconf(_SC_CLK_TCK): Linux's USER_HZ, 100 on every
architecture.\n"}
   clockTicks 100])

(go/var ^{:tag sync/Once} bootOnce)
(go/var ^int64 bootMillis 0)

(go/func bootTimeMillis
  "bootTimeMillis is getBoottime: /proc/stat's btime, in milliseconds since the epoch (0 when
unknown).\n"
  ^int64 []
  (.Do bootOnce
       (fn []
         (let [(values b err) (os/ReadFile "/proc/stat")]
           (when (!= err nil)
             (return))
           (range [_ line (strings/Split (conv string b) "\n")]
             (when (strings/HasPrefix line "btime ")
               (let [(values v perr) (strconv/ParseInt (strings/TrimSpace (subslice line 6)) 10 64)]
                 (when (== perr nil)
                   (set! bootMillis (* v 1000))))
               (return))))))
  bootMillis)

(go/method ProcStat [^OSHost h ^int pid] :results [HostProcStat bool]
  ;; os_getParentPidAndTimings: pid (command) state ppid ... utime(14) stime(15) ... starttime(22)
  (let [(values b err) (os/ReadFile (+ "/proc/" (strconv/Itoa pid) "/stat"))]
    (when (!= err nil)
      (return (lit HostProcStat) false))
    (let [s (conv string b)
          i (strings/LastIndexByte s \))]
      (when (or (< (strings/IndexByte s \() 0) (< i 0))
        (return (lit HostProcStat) false))
      (let [fs (strings/Fields (subslice s (+ i 1)))]
        ;; fs[0] is the state (field 3): ppid is fs[1], utime fs[11], stime fs[12], starttime fs[19]
        (when (< (len fs) 20)
          (return (lit HostProcStat :Ppid 0) true))
        (let [(values ppid e1) (strconv/Atoi (aget fs 1))
              (values ut e2) (strconv/ParseUint (aget fs 11) 10 64)
              (values st e3) (strconv/ParseUint (aget fs 12) 10 64)
              (values start e4) (strconv/ParseUint (aget fs 19) 10 64)]
          (when (or (!= e1 nil) (!= e2 nil) (!= e3 nil) (!= e4 nil))
            (return (lit HostProcStat :Ppid 0) true))
          (return (lit HostProcStat
                       :Ppid ppid
                       :TotalNanos (* (conv int64 (+ ut st)) (/ 1000000000 clockTicks))
                       :StartMillis (+ (bootTimeMillis) (/ (* (conv int64 start) 1000) clockTicks)))
                  true))))))

(go/method Pids ^{:tag (slice int)} [^OSHost h]
  (let [(values f err) (os/Open "/proc")]
    (when (!= err nil)
      (return nil))
    (defer (.Close f))
    (let [(values names _) (.Readdirnames f -1)
          ^{:tag (slice int)} pids nil]
      (range [_ n names]
        (let [(values pid aerr) (strconv/Atoi n)]
          (when (and (== aerr nil) (> pid 0))
            (set! pids (append pids pid)))))
      pids)))

(go/func userName
  "userName is the name of user uid, as getpwuid finds it in /etc/passwd; \"\" when none.\n"
  ^string [^uint32 uid]
  (let [(values b err) (os/ReadFile "/etc/passwd")
        u (strconv/FormatUint (conv uint64 uid) 10)]
    (when (!= err nil)
      (return ""))
    (range [_ line (strings/Split (conv string b) "\n")]
      (let [fs (strings/Split line ":")]
        (when (and (>= (len fs) 3) (== (aget fs 2) u))
          (return (aget fs 0)))))
    ""))

(go/method ProcInfo ^HostProcInfo [^OSHost h ^int pid]
  ;; os_getCmdlineAndUserInfo
  (let [info (lit HostProcInfo)
        dir (+ "/proc/" (strconv/Itoa pid))
        ^{:tag syscall/Stat_t} st (lit syscall/Stat_t)]
    (when (== (syscall/Stat dir (addr st)) nil)
      (set! (.-User info) (userName (.-Uid st))))
    (let [(values f err) (os/Open (+ dir "/cmdline"))]
      (when (!= err nil)
        (return info))
      (defer (.Close f))
      ;; the executable's path: /proc/pid/exe, readable for the user's own processes
      (let [(values exe lerr) (os/Readlink (+ dir "/exe"))]
        (when (and (== lerr nil) (!= exe ""))
          (set! (.-Command info) exe)))
      ;; the arguments, NUL-separated, at most a page (PAGE_SIZE characters)
      (let [page (os/Getpagesize)
            buf (make (slice byte) page)
            n 0]
        (while (< n page)
          (let [(values k rerr) (.Read f (subslice buf n))]
            (set! n (+ n k))
            (when (!= rerr nil)
              (break))))
        (let [cmdline (subslice buf 0 n)
              truncated (and (== n page) (!= (aget buf (- page 1)) 0))]
          (when (and (> n 0) (or (== (.-Command info) "") truncated))
            ;; no exact command, or the arguments are cut: the command line, every NUL a space
            (set! (.-CommandLine info)
                  (conv string (bytes/ReplaceAll cmdline (lit (slice byte) 0) (lit (slice byte) 32)))))
          (when (not truncated)
            ;; unix_fillArgArray: the strings before the first empty one; the arguments are
            ;; those after the first
            (let [parts (strings/Split (conv string cmdline) "\u0000")
                  k 0]
              (while (and (< k (len parts)) (!= (aget parts k) ""))
                (set! k (+ k 1)))
              (when (>= k 1)
                (set! (.-HasArgs info) true)
                (set! (.-Args info) (append (lit (slice string)) (spread (subslice parts 1 k)))))))))
      info)))
