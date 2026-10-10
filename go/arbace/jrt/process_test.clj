;; jrt's tests: the processes under ProcessImpl, ProcessHandleImpl and ProcessEnvironment
;; (process.clj, hostproc_linux.clj; doc/go/JRT-NOTES.md, "Processes"), through the natives as the
;; translated classes call them, on the host's /bin/sh.
(in-ns 'go.arbace.jrt)

(go/file "process_test.go"
  :imports [[os "os"] [strconv "strconv"] [strings "strings"] [testing "testing"] [time "time"]])

(go/func cBytes "cBytes is s as ProcessImpl.toCString makes it: its bytes and a NUL.\n"
  ^{:tag (* ByteArray)} [^string s]
  (javaBytes (+ s "\u0000")))

(go/func cBlock "cBlock is the strings, each NUL-terminated, as one block.\n"
  ^{:tag (* ByteArray)} [& ^{:tag (slice string)} ss]
  (let [b ""]
    (range [_ s ss]
      (set! b (+ b s "\u0000")))
    (javaBytes b)))

(go/func thrown
  "thrown is the message of the Java exception f throws, or \"\" when it returns.\n"
  [^{:tag (func [])} f]
  :results [^string msg]
  (defer ((fn []
            (let [r (recover)]
              (when (!= r nil)
                (let [(values th ok) (assert Throwable_I r)]
                  (if ok
                    (set! msg (.String (.GetMessage__String th)))
                    (panic r))))))))
  (f)
  (return ""))

(go/func spawn
  "spawn starts /bin/sh -c script (a pipe for each stream unless fds says otherwise; env nil: the
program's), as ProcessImpl.start passes it: the pid and the handles.\n"
  [^{:tag (* testing/T)} t ^string script ^{:tag (* ByteArray)} env ^int32 envc ^string dir ^bool errToOut]
  :results [int32 (* IntArray)]
  (let [fds (NewIntArray 3)
        ^{:tag (* ByteArray)} d nil]
    (range [i _ (.-A fds)]
      (aset (.-A fds) i -1))
    (when (!= dir "")
      (set! d (cBytes dir)))
    (let [pid (ProcessImpl_HostForkAndExec_B1_B1_I_B1_I_B1_I1_Z__I_native
                (cBytes "/bin/sh") (cBlock "-c" script) 2 env envc d fds errToOut)]
      (when (<= pid 0)
        (.Fatalf t "pid %d" pid))
      (return pid fds))))

(go/func readAll "readAll reads handle h to its end and closes it.\n" ^string [^int32 h]
  (let [buf (NewByteArray 64)
        out ""]
    (while true
      (let [n (HostFiles_Read0_I_B1_I_I__I_native h buf 0 64)]
        (when (< n 0)
          (break))
        (set! out (+ out (stringOf buf n)))))
    (HostFiles_Close0_I__V_native h)
    out))

(go/func TestProcessStreams
  "A child's three pipes, its exit status, its entry gone once reaped, and no zombie left.\n"
  [^{:tag (* testing/T)} t]
  (let [(values pid fds) (spawn t "read x; echo out:$x; echo err >&2; exit 3" nil 0 "" false)
        in (aget (.-A fds) 0)]
    (when (or (<= in 2) (<= (aget (.-A fds) 1) 2) (<= (aget (.-A fds) 2) 2))
      (.Fatalf t "handles %v" (.-A fds)))
    (HostFiles_Write0_I_B1_I_I__V_native in (javaBytes "abc\n") 0 4)
    (HostFiles_Close0_I__V_native in)
    (when (!= (readAll (aget (.-A fds) 1)) "out:abc\n")
      (.Error t "stdout"))
    (when (!= (readAll (aget (.-A fds) 2)) "err\n")
      (.Error t "stderr"))
    (when (!= (ProcessHandleImpl_WaitForProcessExit0_J_Z__I_native (conv int64 pid) true) 3)
      (.Error t "exit status"))
    (when (!= (child (conv int pid)) nil)
      (.Error t "the reaped child is still in the table"))
    (when (!= (ProcessHandleImpl_WaitForProcessExit0_J_Z__I_native (conv int64 pid) true) notAChild)
      (.Error t "a reaped child is not a child"))
    (let [(values _ err) (os/Stat (+ "/proc/" (strconv/Itoa (conv int pid))))]
      (when (== err nil)
        (.Error t "a zombie is left")))
    (when (!= (ProcessHandleImpl_IsAlive0_J__J_native (conv int64 pid)) -1)
      (.Error t "isAlive0 of an ended child"))))

(go/func TestProcessReapedUnwaited
  "A child that ends while nobody waits for it is reaped by the host at once.\n"
  [^{:tag (* testing/T)} t]
  (let [(values pid fds) (spawn t "exit 0" nil 0 "" false)
        p (+ "/proc/" (strconv/Itoa (conv int pid)))
        t0 (time/Now)]
    (range [_ h (.-A fds)]
      (HostFiles_Close0_I__V_native h))
    (while (< (time/Since t0) (* 5 time/Second))
      (let [(values _ err) (os/Stat p)]
        (when (!= err nil)
          (break)))
      (time/Sleep (* 5 time/Millisecond)))
    (let [(values _ err) (os/Stat p)]
      (when (== err nil)
        (.Error t "not reaped")))
    (when (!= (ProcessHandleImpl_WaitForProcessExit0_J_Z__I_native (conv int64 pid) true) 0)
      (.Error t "status"))))

(go/func TestProcessAvailable
  "What an ended child left in its pipe is available (ProcessPipeInputStream.processExited drains
it so).\n"
  [^{:tag (* testing/T)} t]
  (let [(values pid fds) (spawn t "printf abcde" nil 0 "" false)]
    (ProcessHandleImpl_WaitForProcessExit0_J_Z__I_native (conv int64 pid) true)
    (when (!= (HostFiles_Available0_I__I_native (aget (.-A fds) 1)) 5)
      (.Errorf t "available %d" (HostFiles_Available0_I__I_native (aget (.-A fds) 1))))
    (range [_ h (.-A fds)]
      (HostFiles_Close0_I__V_native h))))

(go/func TestProcessOptions
  "The environment, the directory, error output to standard output, inherited streams.\n"
  [^{:tag (* testing/T)} t]
  (let [(values pid fds) (spawn t "echo $A-$B; pwd" (cBlock "A=1" "B=two") 2 "/" false)]
    (HostFiles_Close0_I__V_native (aget (.-A fds) 0))
    (when (!= (readAll (aget (.-A fds) 1)) "1-two\n/\n")
      (.Error t "environment and directory"))
    (HostFiles_Close0_I__V_native (aget (.-A fds) 2))
    (ProcessHandleImpl_WaitForProcessExit0_J_Z__I_native (conv int64 pid) true))
  (let [(values pid fds) (spawn t "echo a; echo b >&2" nil 0 "" true)]
    (when (!= (readAll (aget (.-A fds) 2)) "")
      (.Error t "the error pipe of a redirected error output is not empty"))
    (HostFiles_Close0_I__V_native (aget (.-A fds) 0))
    (when (!= (readAll (aget (.-A fds) 1)) "a\nb\n")
      (.Error t "error output to standard output"))
    (ProcessHandleImpl_WaitForProcessExit0_J_Z__I_native (conv int64 pid) true))
  ;; an empty environment: the shell's own variables alone (busybox's sh sets SHLVL and PWD)
  (let [(values pid fds) (spawn t "env | grep -v -e ^SHLVL= -e ^PWD= | wc -l" (cBlock) 0 "" false)]
    (when (!= (strings/TrimSpace (readAll (aget (.-A fds) 1))) "0")
      (.Error t "empty environment"))
    (HostFiles_Close0_I__V_native (aget (.-A fds) 0))
    (HostFiles_Close0_I__V_native (aget (.-A fds) 2))
    (ProcessHandleImpl_WaitForProcessExit0_J_Z__I_native (conv int64 pid) true)))

(go/func TestProcessErrors
  "The JVM's texts of a program not found, not executable, a missing directory.\n"
  [^{:tag (* testing/T)} t]
  (let [start (fn ^string [^string prog ^string dir ^bool env]
                (let [fds (NewIntArray 3)
                      ^{:tag (* ByteArray)} d nil
                      ^{:tag (* ByteArray)} e nil]
                  (range [i _ (.-A fds)]
                    (aset (.-A fds) i -1))
                  (when (!= dir "")
                    (set! d (cBytes dir)))
                  (when env
                    (set! e (cBlock "X=1")))
                  (thrown (fn []
                            (ProcessImpl_HostForkAndExec_B1_B1_I_B1_I_B1_I1_Z__I_native
                              (cBytes prog) (cBlock) 0 e 1 d fds false)))))]
    (let [cases (lit (slice string)
                     "no-such-program-xyz" "" "false" "Exec failed, error: 2 (No such file or directory) "
                     "no-such-program-xyz" "" "true" "Exec failed, error: 2 (No such file or directory) "
                     "/etc/passwd" "" "false" "Exec failed, error: 13 (Permission denied) "
                     "/" "" "false" "Exec failed, error: 13 (Permission denied) "
                     "" "" "false" "Exec failed, error: 2 (No such file or directory) "
                     "ls" "/no/such/dir" "false" "Failed to access working directory, error: 2 (No such file or directory) "
                     "no-such-program-xyz" "/etc/passwd" "false" "Failed to access working directory, error: 20 (Not a directory) ")]
      (for [i 0] (< i (len cases)) (set! i (+ i 4))
        (let [c (subslice cases i (+ i 4))
              got (start (aget c 0) (aget c 1) (== (aget c 2) "true"))]
          (when (!= got (aget c 3))
            (.Errorf t "%q in %q: %q" (aget c 0) (aget c 1) got)))))))

(go/func TestProcessDestroy
  "destroy0: SIGTERM and SIGKILL, the JDK's statuses 0x80 + signal; an ended child is not
signalled.\n"
  [^{:tag (* testing/T)} t]
  (range [_ force (lit (slice bool) false true)]
    (let [(values pid fds) (spawn t "exec sleep 10" nil 0 "" false)
          start (ProcessHandleImpl_IsAlive0_J__J_native (conv int64 pid))]
      (when (< start 0)
        (.Error t "a running child not alive"))
      (when (not (ProcessHandleImpl_Destroy0_J_J_Z__Z_native (conv int64 pid) start force))
        (.Error t "destroy0"))
      (let [want 143]
        (when force
          (set! want 137))
        (when (!= (ProcessHandleImpl_WaitForProcessExit0_J_Z__I_native (conv int64 pid) true) (conv int32 want))
          (.Errorf t "status after destroy (force %v)" force)))
      (when (ProcessHandleImpl_Destroy0_J_J_Z__Z_native (conv int64 pid) start force)
        (.Error t "destroy0 of an ended child"))
      (range [_ h (.-A fds)]
        (HostFiles_Close0_I__V_native h)))))

(go/func TestProcessHandles
  "The current process, its children, parent and info; the environment's bytes.\n"
  [^{:tag (* testing/T)} t]
  (let [self (ProcessHandleImpl_GetCurrentPid0__J_native)]
    (when (!= self (conv int64 (os/Getpid)))
      (.Error t "current pid"))
    (when (!= (ProcessHandleImpl_Parent0_J_J__J_native self 0) (conv int64 (os/Getppid)))
      (.Error t "parent"))
    (when (<= (ProcessHandleImpl_IsAlive0_J__J_native self) 0)
      (.Error t "start time"))
    (let [(values pid fds) (spawn t "exec sleep 10" nil 0 "" false)
          pids (NewLongArray 10)
          n (ProcessHandleImpl_GetProcessPids0_J_J1_J1_J1__I_native self pids nil nil)]
      (when (or (< n 1) (not (contains (subslice (.-A pids) 0 n) (conv int64 pid))))
        (.Errorf t "children %v" (subslice (.-A pids) 0 n)))
      (let [times (NewLongArray 2)
            info (ProcessHandleImpl_Info_HostInfo_J_J1__String1_native (conv int64 pid) times)
            str (fn ^string [^int i] (let [s (aget (.-A info) i)] (when (== s nil) (return "")) (return (.String (assert (* String) s)))))]
        (when (or (== (str 0) "") (!= (len (.-A info)) 5) (!= (str 4) "10") (== (str 2) ""))
          (.Errorf t "info %q %d" (str 0) (len (.-A info))))
        (when (or (< (aget (.-A times) 0) 0) (<= (aget (.-A times) 1) 0))
          (.Errorf t "times %v" (.-A times))))
      (ProcessHandleImpl_Destroy0_J_J_Z__Z_native (conv int64 pid) 0 true)
      (ProcessHandleImpl_WaitForProcessExit0_J_Z__I_native (conv int64 pid) true)
      (range [_ h (.-A fds)]
        (HostFiles_Close0_I__V_native h))))
  (let [env (ProcessEnvironment_Environ__B2_native)
        found false]
    (for [i 0] (< i (len (.-A env))) (set! i (+ i 2))
      (when (== (stringOf (assert (* ByteArray) (aget (.-A env) i)) (conv int32 (len (.-A (assert (* ByteArray) (aget (.-A env) i)))))) "PATH")
        (set! found true)))
    (when (or (!= (% (len (.-A env)) 2) 0) (not found))
      (.Error t "environ"))))

(go/func contains ^bool [^{:tag (slice int64)} s ^int64 x]
  (range [_ y s]
    (when (== y x)
      (return true)))
  false)

(go/type noProcHost
  "noProcHost is a host without processes (Host alone).\n"
  (struct Host))

(go/func TestNoProcesses "A host that does not implement HostProc starts no program.\n"
  [^{:tag (* testing/T)} t]
  (let [old (CurrentHost)]
    (SetHost (lit noProcHost :Host old))
    (defer (SetHost old))
    (let [fds (NewIntArray 3)
          got (thrown (fn []
                        (ProcessImpl_HostForkAndExec_B1_B1_I_B1_I_B1_I1_Z__I_native
                          (cBytes "/bin/sh") (cBlock) 0 nil 0 nil fds false)))]
      (when (!= got "Exec failed, error: 38 (Function not implemented) ")
        (.Errorf t "start: %q" got)))
    (when (!= (ProcessHandleImpl_IsAlive0_J__J_native 1) -1)
      (.Error t "isAlive0"))
    (when (not (strings/HasPrefix (ErrnoText 2) "No such"))
      (.Error t "errno text"))))
