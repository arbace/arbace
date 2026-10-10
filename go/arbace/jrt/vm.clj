;; jrt: jdk.internal.misc.VM and CDS, the two VM services the translated JDK classes call
;; (JRT-SOURCES.md, the shim edge: VM.getSavedProperty and isBooted, CDS.initializeFromArchive
;; and getRandomSeedForDumping). Added with c2g (doc/go/C2G-NOTES.md): Integer$IntegerCache's
;; and the other caches' static initializers call them.
(in-ns 'go.arbace.jrt)

(go/file "vm.go")

(go/type VM "VM is jdk.internal.misc.VM: the VM's state, as the translated classes ask for it.\n"
  (struct Object))

(go/var VM_class
  (Define (addr (lit ClassInfo :Name "jdk.internal.misc.VM" :Kind KindClass
                     :Modifiers (bit-or AccPublic AccFinal) :Super Object_class :Go "arbace/jrt.VM"))))

(go/func VM_GetSavedProperty_String__String
  "VM_GetSavedProperty_String__String is VM.getSavedProperty: the system properties saved at the
VM's start for the JDK's internal use. There are none: the caches it configures
(java.lang.Integer.IntegerCache.high) keep their defaults, as on a JVM started without -D.\n"
  ^{:tag (* String)} [^{:tag (* String)} key]
  nil)

(go/func VM_GetNanoTimeAdjustment_J__J
  "VM_GetNanoTimeAdjustment_J__J is VM.getNanoTimeAdjustment (java.time.Clock's system clock;
JRT-NOTES.md, \"Time\"): the host's wall clock as nanoseconds from the second offset, or -1 when
that is more than 2^32 seconds away (the JDK's sentinel, after which Clock takes a new offset).
The host's clock has Go's precision, nanoseconds on Linux, as the JDK's clock_gettime.\n"
  ^int64 [^int64 offset]
  (let [now (.Now (CurrentHost))
        diff (- (.Unix now) offset)]
    (when (or (> diff 4294967296) (< diff -4294967296))
      (return -1))
    (+ (* diff 1000000000) (conv int64 (.Nanosecond now)))))

(go/func VM_IsBooted__Z "VM_IsBooted__Z is VM.isBooted: the program runs after initialization.\n"
  ^bool []
  true)

(go/type CDS "CDS is jdk.internal.misc.CDS: class data sharing, which jrt has not.\n"
  (struct Object))

(go/var CDS_class
  (Define (addr (lit ClassInfo :Name "jdk.internal.misc.CDS" :Kind KindClass
                     :Modifiers (bit-or AccPublic AccFinal) :Super Object_class :Go "arbace/jrt.CDS"))))

(go/func CDS_InitializeFromArchive_Class__V
  "CDS_InitializeFromArchive_Class__V is CDS.initializeFromArchive: nothing archived, so the
class initializes its statics itself, as on a JVM without an archive.\n"
  [^{:tag (* Class)} c])

(go/func CDS_GetRandomSeedForDumping__J "CDS_GetRandomSeedForDumping__J is CDS.getRandomSeedForDumping: 0, not dumping.\n"
  ^int64 []
  0)
