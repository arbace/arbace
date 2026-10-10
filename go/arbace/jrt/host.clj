;; jrt: the host interface (C2G-SPEC §9.4; B1-PLAN.md, point 4): everything jrt asks of the
;; operating system, in one Go interface, so that B1b (TamaGo in the box) swaps one value.
;; B1a's implementation is over Go's os and time.
(in-ns 'go.arbace.jrt)

(go/file "host.go"
  :imports [[bytes "bytes"] [zlib "compress/zlib"] [rand "crypto/rand"] [errors "errors"] [io "io"] [fs "io/fs"] [os "os"]
            [runtime "runtime"] [strings "strings"] [sync "sync"] [time "time"]])

(go/type Host
  "Host is the operating system as jrt uses it. Translated code never reaches it except
through jrt's classes (System, Runtime, Thread.sleep's clock, the file streams); SetHost
replaces it before the program's first use of those.\n"
  (interface
    (^{:doc "the standard streams\n"} Stdin ^{:tag io/Reader} [])
    (Stdout ^{:tag io/Writer} [])
    (Stderr ^{:tag io/Writer} [])
    (^{:doc "Open opens a file with os.O_* flags (O_RDONLY, O_WRONLY|O_CREATE|O_TRUNC ...).\n"}
      Open [^string name ^int flag ^uint32 perm] :results [HostFile error])
    (Stat [^string name] :results [HostFileInfo error])
    (^{:doc "ReadDir lists a directory's entries' names, sorted.\n"}
      ReadDir [^string name] :results [(slice string) error])
    (Remove ^error [^string name])
    (Mkdir ^error [^string name ^uint32 perm])
    (Rename ^error [^string from ^string to])
    (Getwd [] :results [string error])
    (^{:doc "Now is the wall clock (System.currentTimeMillis, Date).\n"} Now ^{:tag time/Time} [])
    (^{:doc "Nanotime is a monotonic clock in nanoseconds (System.nanoTime).\n"} Nanotime ^int64 [])
    (Getenv [^string name] :results [string bool])
    (Environ ^{:tag (slice string)} [])
    (^{:doc "Args is the command line, the program's name first.\n"} Args ^{:tag (slice string)} [])
    (NumCPU ^int [])
    (^{:doc "RandomBytes fills b with random bytes (SecureRandom's seeds).\n"}
      RandomBytes [^{:tag (slice byte)} b])
    (^{:doc "Resource is an embedded resource by its path (RT.load, getResourceAsStream).\n"}
      Resource [^string name] :results [(slice byte) bool])
    (^{:doc "Exit ends the process with the status code.\n"} Exit [^int code])))

(go/type HostFile
  "HostFile is an open file of the host.\n"
  (interface io/Reader io/Writer io/Closer
    (Seek [^int64 offset ^int whence] :results [int64 error])))

(go/type HostFileInfo
  "HostFileInfo is what Stat tells of a file (java.io.File's queries).\n"
  (struct ^string Name ^int64 Size ^int64 ModTimeMillis ^bool IsDir ^uint32 Mode))

;; ---------------------------------------------------------------------------------------
;; B1a's host: os and time

(go/type OSHost
  "OSHost is B1a's host, over Go's os, time and crypto/rand; Resources (optional) holds the
embedded resources.\n"
  (struct ^{:tag fs/FS} Resources))

(go/var ^{:tag time/Time} hostStart (time/Now))

(go/method Stdin ^{:tag io/Reader} [^OSHost h] os/Stdin)
(go/method Stdout ^{:tag io/Writer} [^OSHost h] os/Stdout)
(go/method Stderr ^{:tag io/Writer} [^OSHost h] os/Stderr)
(go/method Open [^OSHost h ^string name ^int flag ^uint32 perm] :results [HostFile error]
  (let [(values f err) (os/OpenFile name flag (conv fs/FileMode perm))]
    (when (!= err nil)
      (return nil err))
    (return f nil)))
(go/method Stat [^OSHost h ^string name] :results [HostFileInfo error]
  (let [(values fi err) (os/Stat name)]
    (when (!= err nil)
      (return (lit HostFileInfo) err))
    (return (lit HostFileInfo :Name (.Name fi) :Size (.Size fi) :ModTimeMillis (.UnixMilli (.ModTime fi))
                 :IsDir (.IsDir fi) :Mode (conv uint32 (.Mode fi)))
            nil)))
(go/method ReadDir [^OSHost h ^string name] :results [(slice string) error]
  (let [(values es err) (os/ReadDir name)]
    (when (!= err nil)
      (return nil err))
    (let [ns (make (slice string) 0 (len es))]
      (range [_ e es]
        (set! ns (append ns (.Name e))))
      (return ns nil))))
(go/method Remove ^error [^OSHost h ^string name] (os/Remove name))
(go/method Mkdir ^error [^OSHost h ^string name ^uint32 perm] (os/Mkdir name (conv fs/FileMode perm)))
(go/method Rename ^error [^OSHost h ^string from ^string to] (os/Rename from to))
(go/method Getwd [^OSHost h] :results [string error] (os/Getwd))
(go/method Now ^{:tag time/Time} [^OSHost h] (time/Now))
(go/method Nanotime ^int64 [^OSHost h] (conv int64 (time/Since hostStart)))
(go/method Getenv [^OSHost h ^string name] :results [string bool] (os/LookupEnv name))
(go/method Environ ^{:tag (slice string)} [^OSHost h] (os/Environ))
(go/method Args ^{:tag (slice string)} [^OSHost h] os/Args)
(go/method NumCPU ^int [^OSHost h] (runtime/NumCPU))
(go/method RandomBytes [^OSHost h ^{:tag (slice byte)} b] (rand/Read b))
(go/method Resource
  "Resource is an embedded resource: the file name, else name.z inflated (c2g writes the
resources a zlib stream makes a quarter smaller so: amendment SZ2).\n"
  [^OSHost h ^string name] :results [(slice byte) bool]
  (when (== (.-Resources h) nil)
    (return nil false))
  (let [(values b err) (fs/ReadFile (.-Resources h) name)]
    (when (== err nil)
      (return b true)))
  (let [(values z err) (fs/ReadFile (.-Resources h) (+ name ".z"))]
    (when (!= err nil)
      (return nil false))
    (let [(values r zerr) (zlib/NewReader (bytes/NewReader z))]
      (when (!= zerr nil)
        (return nil false))
      (let [(values b rerr) (io/ReadAll r)]
        (return b (== rerr nil))))))
(go/method Exit [^OSHost h ^int code] (StopProfile) (os/Exit code))

(go/func ResourceOrPath
  "ResourceOrPath is a resource as the Go build's class path has it (ClassLoader's
getResourceAsStream; RT.load's order): the program's embedded resource, else the file name in
one of the directories of ARBACE_PATH (separated by :).\n"
  ^{:tag (slice byte)} [^string name] :results [^{:tag (slice byte)} b ^bool ok]
  (let [h (CurrentHost)
        (values r found) (.Resource h name)]
    (when found
      (return r true))
    (let [(values path set) (.Getenv h "ARBACE_PATH")]
      (when (not set)
        (return nil false))
      (range [_ dir (strings/Split path ":")]
        (when (!= dir "")
          (let [(values f err) (.Open h (+ dir "/" name) os/O_RDONLY 0)]
            (when (== err nil)
              (let [(values data rerr) (io/ReadAll f)]
                (.Close f)
                (when (== rerr nil)
                  (return data true))))))))
    (return nil false)))

(go/var ^{:tag Host} theHost (lit OSHost))
(go/var ^{:tag sync/RWMutex} hostMu)

(go/func SetHost "SetHost replaces the host (B1b's monitor calls; tests).\n" [^Host h]
  (.Lock hostMu)
  (set! theHost h)
  (.Unlock hostMu))

(go/func CurrentHost "CurrentHost is the host jrt uses.\n" ^Host []
  (.RLock hostMu)
  (let [h theHost]
    (.RUnlock hostMu)
    h))

;; ---------------------------------------------------------------------------------------
;; The standard streams as byte streams: what System.in, out and err (PrintStream and
;; InputStream, translated) and printStackTrace sit on. Unbuffered: each write goes to the
;; host, under the stream's lock.

(go/type HostStream
  "HostStream is one of the host's standard streams: 0 in, 1 out, 2 err.\n"
  (struct ^{:tag sync/Mutex} mu ^int fd))

(go/var Stdin (addr (lit HostStream :fd 0)))
(go/var Stdout (addr (lit HostStream :fd 1)))
(go/var Stderr (addr (lit HostStream :fd 2)))

(go/method Write [^{:tag (* HostStream)} s ^{:tag (slice byte)} p] :results [^int n ^error err]
  (.Lock (.-mu s))
  (defer (.Unlock (.-mu s)))
  (let [h (CurrentHost)]
    (switch (.-fd s)
      (case [1] (return (.Write (.Stdout h) p)))
      (case [2] (return (.Write (.Stderr h) p))))
    (return 0 (errors/New "stream not open for writing"))))

(go/method WriteString [^{:tag (* HostStream)} s ^string str] :results [^int n ^error err]
  (.Write s (conv (slice byte) str)))

(go/method Read [^{:tag (* HostStream)} s ^{:tag (slice byte)} p] :results [^int n ^error err]
  (when (!= (.-fd s) 0)
    (return 0 (errors/New "stream not open for reading")))
  (.Lock (.-mu s))
  (defer (.Unlock (.-mu s)))
  (.Read (.Stdin (CurrentHost)) p))
