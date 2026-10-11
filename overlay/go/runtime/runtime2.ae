;; Copyright 2009 The Go Authors. All rights reserved.
;; Use of this source code is governed by a BSD-style
;; license that can be found in the LICENSE file.

(in-ns 'go.runtime) (go/file "runtime2.go" :imports [


    ^{:line 8} [abi "internal/abi"]
    ^{:line 9} [chacha8rand "internal/chacha8rand"]
    ^{:line 10} [goarch "internal/goarch"]
    ^{:line 11} [atomic "internal/runtime/atomic"]
    ^{:line 12} [sys "internal/runtime/sys"]
    ^{:line 13} [unsafe "unsafe"]])



(go/const "defined constants\n"
  ;; G status
  ;;
  ;; Beyond indicating the general state of a G, the G status
  ;; acts like a lock on the goroutine's stack (and hence its
  ;; ability to execute user code).
  ;;
  ;; If you add to this list, add to the list
  ;; of "okay during garbage collection" status
  ;; in mgcmark.go too.
  ;;
  ;; TODO(austin): The _Gscan bit could be much lighter-weight.
  ;; For example, we could choose not to run _Gscanrunnable
  ;; goroutines found in the run queue, rather than CAS-looping
  ;; until they become _Grunnable. And transitions like
  ;; _Gscanwaiting -> _Gscanrunnable are actually okay because
  ;; they don't affect stack ownership.



  ^{:line 37} [^{:val 0 :doc "_Gidle means this goroutine was just allocated and has not\nyet been initialized.\n"} _Gidle iota] ; 0



  ^{:line 41} [^{:val 1 :doc "_Grunnable means this goroutine is on a run queue. It is\nnot currently executing user code. The stack is not owned.\n"} _Grunnable] ; 1






  ^{:line 48} [^{:val 2 :doc "_Grunning means this goroutine may execute user code. The\nstack is owned by this goroutine. It is not on a run queue.\nIt is assigned an M (g.m is valid) and it usually has a P\n(g.m.p is valid), but there are small windows of time where\nit might not, namely upon entering and exiting _Gsyscall.\n"} _Grunning] ; 2






  ^{:line 55} [^{:val 3 :doc "_Gsyscall means this goroutine is executing a system call.\nIt is not executing user code. The stack is owned by this\ngoroutine. It is not on a run queue. It is assigned an M.\nIt may have a P attached, but it does not own it. Code\nexecuting in this state must not touch g.m.p.\n"} _Gsyscall] ; 3









  ^{:line 65} [^{:val 4 :doc "_Gwaiting means this goroutine is blocked in the runtime.\nIt is not executing user code. It is not on a run queue,\nbut should be recorded somewhere (e.g., a channel wait\nqueue) so it can be ready()d when necessary. The stack is\nnot owned *except* that a channel operation may read or\nwrite parts of the stack under the appropriate channel\nlock. Otherwise, it is not safe to access the stack after a\ngoroutine enters _Gwaiting (e.g., it may get moved).\n"} _Gwaiting] ; 4



  ^{:line 69} [^{:val 5 :doc "_Gmoribund_unused is currently unused, but hardcoded in gdb\nscripts.\n"} _Gmoribund_unused] ; 5







  ^{:line 77} [^{:val 6 :doc "_Gdead means this goroutine is currently unused. It may be\njust exited, on a free list, or just being initialized. It\nis not executing user code. It may or may not have a stack\nallocated. The G and its stack (if any) are owned by the M\nthat is exiting the G or that obtained the G from the free\nlist.\n"} _Gdead] ; 6


  ^{:line 80} [^{:val 7 :doc "_Genqueue_unused is currently unused.\n"} _Genqueue_unused] ; 7




  ^{:line 85} [^{:val 8 :doc "_Gcopystack means this goroutine's stack is being moved. It\nis not executing user code and is not on a run queue. The\nstack is owned by the goroutine that put it in _Gcopystack.\n"} _Gcopystack] ; 8






  ^{:line 92} [^{:val 9 :doc "_Gpreempted means this goroutine stopped itself for a\nsuspendG preemption. It is like _Gwaiting, but nothing is\nyet responsible for ready()ing it. Some suspendG must CAS\nthe status to _Gwaiting to take responsibility for\nready()ing this G.\n"} _Gpreempted] ; 9


  ^{:line 95} [^{:val 10 :doc "_Gleaked represents a leaked goroutine caught by the GC.\n"} _Gleaked] ; 10



  ^{:line 99} [^{:val 11 :doc "_Gdeadextra is a _Gdead goroutine that's attached to an extra M\nused for cgo callbacks.\n"} _Gdeadextra] ; 11












  ^{:line 112} [^{:val 4096 :doc "_Gscan combined with one of the above states other than\n_Grunning indicates that GC is scanning the stack. The\ngoroutine is not executing user code and the stack is owned\nby the goroutine that set the _Gscan bit.\n\n_Gscanrunning is different: it is used to briefly block\nstate transitions while GC signals the G to scan its own\nstack. This is otherwise like _Grunning.\n\natomicstatus&~Gscan gives the state the goroutine will\nreturn to when the scan completes.\n"} _Gscan 0x1000]
  ^{:line 113} [^{:val 4097} _Gscanrunnable (+ _Gscan _Grunnable)] ; 0x1001
  ^{:line 114} [^{:val 4098} _Gscanrunning (+ _Gscan _Grunning)] ; 0x1002
  ^{:line 115} [^{:val 4099} _Gscansyscall (+ _Gscan _Gsyscall)] ; 0x1003
  ^{:line 116} [^{:val 4100} _Gscanwaiting (+ _Gscan _Gwaiting)] ; 0x1004
  ^{:line 117} [^{:val 4105} _Gscanpreempted (+ _Gscan _Gpreempted)] ; 0x1009
  ^{:line 118} [^{:val 4106} _Gscanleaked (+ _Gscan _Gleaked)] ; 0x100a
  ^{:line 119} [^{:val 4107} _Gscandeadextra (+ _Gscan _Gdeadextra)]) ; 0x100b


(go/const
  ;; P status








  ^{:line 132} [^{:val 0 :doc "_Pidle means a P is not being used to run user code or the\nscheduler. Typically, it's on the idle P list and available\nto the scheduler, but it may just be transitioning between\nother states.\n\nThe P is owned by the idle list or by whatever is\ntransitioning its state. Its run queue is empty.\n"} _Pidle iota]








  ^{:line 141} [^{:val 1 :doc "_Prunning means a P is owned by an M and is being used to\nrun user code or the scheduler. Only the M that owns this P\nis allowed to change the P's status from _Prunning. The M\nmay transition the P to _Pidle (if it has no more work to\ndo), or _Pgcstop (to halt for the GC). The M may also hand\nownership of the P off directly to another M (for example,\nto schedule a locked G).\n"} _Prunning]




  ^{:line 146} [^{:val 2 :doc "_Psyscall_unused is a now-defunct state for a P. A P is\nidentified as \"in a system call\" by looking at the goroutine's\nstate.\n"} _Psyscall_unused]









  ^{:line 156} [^{:val 3 :doc "_Pgcstop means a P is halted for STW and owned by the M\nthat stopped the world. The M that stopped the world\ncontinues to use its P, even in _Pgcstop. Transitioning\nfrom _Prunning to _Pgcstop causes an M to release its P and\npark.\n\nThe P retains its run queue and startTheWorld will restart\nthe scheduler on Ps with non-empty run queues.\n"} _Pgcstop]





  ^{:line 162} [^{:val 4 :doc "_Pdead means a P is no longer used (GOMAXPROCS shrank). We\nreuse Ps if GOMAXPROCS increases. A dead P is mostly\nstripped of its resources, though a few things remain\n(e.g., trace buffers).\n"} _Pdead])







(go/type mutex "Mutual exclusion locks.  In the uncontended case,\nas fast as spin locks (just a few user-level instructions),\nbut on the contention path they sleep in the kernel.\nA zeroed Mutex is unlocked (no need to initialize each lock).\nInitialization is helpful for static lock ranking, but not required.\n" (struct

    ^{:line 172 :doc "Empty struct if lock ranking is disabled, otherwise includes the lock rank\n"} lockRankStruct



    ^{:line 176 :tag uintptr :doc "Futex-based impl treats it as uint32 key,\nwhile sema-based impl as M* waitm.\nUsed to be a union, but unions break precise GC.\n"} key))


(go/type funcval (struct
    ^{:line 180 :tag uintptr} fn))
;; variable-size, fn-specific data here


(go/type iface (struct
    ^{:line 185 :tag (* itab)} tab
    ^{:line 186 :tag unsafe/Pointer} data))


(go/type eface (struct
    ^{:line 190 :tag (* _type)} _type
    ^{:line 191 :tag unsafe/Pointer} data))


^{:go/end 196} (go/func efaceOf ^{:tag (* eface)} [^{:tag (* any)} ep]
  (conv (* eface) (conv unsafe/Pointer ep)))


;; The guintptr, muintptr, and puintptr are all used to bypass write barriers.
;; It is particularly important to avoid write barriers when the current P has
;; been released, because the GC thinks the world is stopped, and an
;; unexpected write barrier would not be synchronized with the GC,
;; which can lead to a half-executed write barrier that has marked the object
;; but not queued it. If the GC skips the object and completes before the
;; queuing can occur, it will incorrectly free the object.
;;
;; We tried using special assignment functions invoked only when not
;; holding a running P, but then some updates to a particular memory
;; word went through write barriers and some did not. This breaks the
;; write barrier shadow checking mode, and it is also scary: better to have
;; a word that is completely ignored by the GC than to have one for which
;; only a few updates are ignored.
;;
;; Gs and Ps are always reachable via true pointers in the
;; allgs and allp lists or (during allocation before they reach those lists)
;; from stack variables.
;;
;; Ms are always reachable via true pointers either from allm or
;; freem. Unlike Gs and Ps we do free Ms, so it's important that
;; nothing ever hold an muintptr across a safe point.






















(go/type guintptr "A guintptr holds a goroutine pointer, but typed as a uintptr\nto bypass write barriers. It is used in the Gobuf goroutine state\nand in scheduling lists that are manipulated without a P.\n\nThe Gobuf.g goroutine pointer is almost always updated by assembly code.\nIn one of the few places it is updated by Go code - func save - it must be\ntreated as a uintptr to avoid a write barrier being emitted at a bad time.\nInstead of figuring out how to emit the write barriers missing in the\nassembly manipulation, we change the type of the field to uintptr,\nso that it does not require write barriers at all.\n\nGoroutine structs are published in the allg list and never freed.\nThat will keep the goroutine structs from being collected.\nThere is never a time that Gobuf.g's contain the only references\nto a goroutine: the publishing of the goroutine in allg comes first.\nGoroutine pointers are also kept in non-GC-visible places like TLS,\nso I can't see them ever moving. If we did want to start moving data\nin the GC, we'd need to allocate the goroutine structs from an\nalternate arena. Using guintptr doesn't make that problem any worse.\nNote that pollDesc.rg, pollDesc.wg also store g in uintptr form,\nso they would need to be updated too if g's start moving.\n" uintptr)


^{:go/end 245} (go/method ^:go/nosplit ptr ^{:tag (* g)} [^guintptr gp] (conv (* g) (conv unsafe/Pointer gp)))


^{:go/end 248} (go/method ^:go/nosplit set [^{:tag (* guintptr)} gp ^{:tag (* g)} g] (set! @gp (conv guintptr (conv unsafe/Pointer g))))


^{:go/end 253} (go/method ^:go/nosplit cas ^bool [^{:tag (* guintptr)} gp ^guintptr old ^guintptr new]
  (atomic/Casuintptr (conv (* uintptr) (conv unsafe/Pointer gp)) (conv uintptr old) (conv uintptr new)))



^{:go/end 258} (go/method ^:go/nosplit guintptr ^guintptr [^{:tag (* g)} gp]
  (conv guintptr (conv unsafe/Pointer gp)))







^{:go/end 267} (go/func ^:go/nosplit ^:go/nowritebarrier setGNoWB "setGNoWB performs *gp = new without a write barrier.\nFor times when it's impractical to use a guintptr.\n" [^{:tag (* (* g))} gp ^{:tag (* g)} new]
  (.set (conv (* guintptr) (conv unsafe/Pointer gp)) new))


(go/type puintptr uintptr)


^{:go/end 272} (go/method ^:go/nosplit ptr ^{:tag (* p)} [^puintptr pp] (conv (* p) (conv unsafe/Pointer pp)))


^{:go/end 275} (go/method ^:go/nosplit set [^{:tag (* puintptr)} pp ^{:tag (* p)} p] (set! @pp (conv puintptr (conv unsafe/Pointer p))))










(go/type muintptr "muintptr is a *m that is not tracked by the garbage collector.\n\nBecause we do free Ms, there are some additional constrains on\nmuintptrs:\n\n 1. Never hold an muintptr locally across a safe point.\n\n 2. Any muintptr in the heap must be owned by the M itself so it can\n    ensure it is not in use when the last true *m is released.\n" uintptr)


^{:go/end 289} (go/method ^:go/nosplit ptr ^{:tag (* m)} [^muintptr mp] (conv (* m) (conv unsafe/Pointer mp)))


^{:go/end 292} (go/method ^:go/nosplit set [^{:tag (* muintptr)} mp ^{:tag (* m)} m] (set! @mp (conv muintptr (conv unsafe/Pointer m))))






^{:go/end 301} (go/func ^:go/nosplit ^:go/nowritebarrier setMNoWB "setMNoWB performs *mp = new without a write barrier.\nFor times when it's impractical to use an muintptr.\n" [^{:tag (* (* m))} mp ^{:tag (* m)} new]
  (.set (conv (* muintptr) (conv unsafe/Pointer mp)) new))


(go/type gobuf (struct










    ^{:line 314 :tag uintptr :doc "ctxt is unusual with respect to GC: it may be a\nheap-allocated funcval, so GC needs to track it, but it\nneeds to be set and cleared from assembly, where it's\ndifficult to have write barriers. However, ctxt is really a\nsaved, live register, and we only ever exchange it between\nthe real register and the gobuf. Hence, we treat it as a\nroot during stack scanning, which means assembly that saves\nand restores it doesn't need write barriers. It's still\ntyped as a pointer so that any other writes from Go get\nwrite barriers.\n"} sp
    ^{:line 315 :tag uintptr} pc
    ^{:line 316 :tag guintptr} g
    ^{:line 317 :tag unsafe/Pointer} ctxt
    ^{:line 318 :tag uintptr} lr
    ^{:line 319 :tag uintptr} bp)) ; for framepointer-enabled architectures













(go/type maybeTraceablePtr "maybeTraceablePtr is a special pointer that is conditionally trackable\nby the GC. It consists of an address as a uintptr (vu) and a pointer\nto a data element (vp).\n\nmaybeTraceablePtr values can be in one of three states:\n1. Unset: vu == 0 && vp == nil\n2. Untracked: vu != 0 && vp == nil\n3. Tracked: vu != 0 && vp != nil\n\nDo not set fields manually. Use methods instead.\nExtend this type with additional methods if needed.\n" (struct
    ^{:line 334 :tag unsafe/Pointer} vp ; For liveness only.
    ^{:line 335 :tag uintptr} vu)) ; Source of truth.






^{:go/end 344} (go/method ^:go/nosplit setUntraceable "untrack unsets the pointer but preserves the address.\nThis is used to hide the pointer from the GC.\n" [^{:tag (* maybeTraceablePtr)} p]
  (set! (.-vp p) nil))






^{:go/end 352} (go/method ^:go/nosplit setTraceable "setTraceable resets the pointer to the stored address.\nThis is used to make the pointer visible to the GC.\n" [^{:tag (* maybeTraceablePtr)} p]
  (set! (.-vp p) (conv unsafe/Pointer (.-vu p))))





^{:go/end 360} (go/method ^:go/nosplit set "set sets the pointer to the data element and updates the address.\n" [^{:tag (* maybeTraceablePtr)} p ^unsafe/Pointer v]
  (set! (.-vp p) v)
  (set! (.-vu p) (conv uintptr v)))





^{:go/end 367} (go/method ^:go/nosplit get "get retrieves the pointer to the data element.\n" ^unsafe/Pointer [^{:tag (* maybeTraceablePtr)} p]
  (conv unsafe/Pointer (.-vu p)))





^{:go/end 374} (go/method ^:go/nosplit uintptr "uintptr returns the uintptr address of the pointer.\n" ^uintptr [^{:tag (* maybeTraceablePtr)} p]
  (.-vu p))






(go/type maybeTraceableChan "maybeTraceableChan extends conditionally trackable pointers (maybeTraceablePtr)\nto track hchan pointers.\n\nDo not set fields manually. Use methods instead.\n" (struct
    ^{:line 381} maybeTraceablePtr))



^{:go/end 387} (go/method ^:go/nosplit set [^{:tag (* maybeTraceableChan)} p ^{:tag (* hchan)} c]
  (.set (.-maybeTraceablePtr p) (conv unsafe/Pointer c)))



^{:go/end 392} (go/method ^:go/nosplit get ^{:tag (* hchan)} [^{:tag (* maybeTraceableChan)} p]
  (conv (* hchan) (.get (.-maybeTraceablePtr p))))












(go/type sudog "sudog (pseudo-g) represents a g in a wait list, such as for sending/receiving\non a channel.\n\nsudog is necessary because the g ↔ synchronization object relation\nis many-to-many. A g can be on many wait lists, so there may be\nmany sudogs for one g; and many gs may be waiting on the same\nsynchronization object, so there may be many sudogs for one object.\n\nsudogs are allocated from a special pool. Use acquireSudog and\nreleaseSudog to allocate and free them.\n" (struct
    ;; The following fields are protected by the hchan.lock of the
    ;; channel this sudog is blocking on. shrinkstack depends on
    ;; this for sudogs involved in channel ops.

    ^{:line 409 :tag (* g)} g

    ^{:line 411 :tag (* sudog)} next
    ^{:line 412 :tag (* sudog)} prev

    ^{:line 414 :tag maybeTraceablePtr} elem ; data element (may point to stack)

    ;; The following fields are never accessed concurrently.
    ;; For channels, waitlink is only accessed by g.
    ;; For semaphores, all fields (including the ones above)
    ;; are only accessed when holding a semaRoot lock.

    ^{:line 421 :tag int64} acquiretime
    ^{:line 422 :tag int64} releasetime
    ^{:line 423 :tag uint32} ticket



    ^{:line 427 :tag bool :doc "isSelect indicates g is participating in a select, so\ng.selectDone must be CAS'd to win the wake-up race.\n"} isSelect





    ^{:line 433 :tag bool :doc "success indicates whether communication over channel c\nsucceeded. It is true if the goroutine was awoken because a\nvalue was delivered over channel c, and false if awoken\nbecause c was closed.\n"} success






    ^{:line 440 :tag uint16 :doc "waiters is a count of semaRoot waiting list other than head of list,\nclamped to a uint16 to fit in unused space.\nOnly meaningful at the head of the list.\n(If we wanted to be overly clever, we could store a high 16 bits\nin the second entry in the list.)\n"} waiters

    ^{:line 442 :tag (* sudog)} parent ; semaRoot binary tree
    ^{:line 443 :tag (* sudog)} waitlink ; g.waiting list or semaRoot
    ^{:line 444 :tag (* sudog)} waittail ; semaRoot
    ^{:line 445 :tag maybeTraceableChan} c)) ; channel


(go/type libcall (struct
    ^{:line 449 :tag uintptr} fn
    ^{:line 450 :tag uintptr} n ; number of parameters
    ^{:line 451 :tag uintptr} args ; parameters
    ^{:line 452 :tag uintptr} r1 ; return values
    ^{:line 453 :tag uintptr} r2
    ^{:line 454 :tag uintptr} err)) ; error number





(go/type stack "Stack describes a Go execution stack.\nThe bounds of the stack are exactly [lo, hi),\nwith no implicit data structures on either side.\n" (struct
    ^{:line 461 :tag uintptr} lo
    ^{:line 462 :tag uintptr} hi))



(go/type heldLockInfo "heldLockInfo gives info on a held lock and the rank of that lock\n" (struct
    ^{:line 467 :tag uintptr} lockAddr
    ^{:line 468 :tag lockRank} rank))


(go/type g (struct







    ^{:line 479 :tag stack :doc "Stack parameters.\nstack describes the actual stack memory: [stack.lo, stack.hi).\nstackguard0 is the stack pointer compared in the Go stack growth prologue.\nIt is stack.lo+StackGuard normally, but can be StackPreempt to trigger a preemption.\nstackguard1 is the stack pointer compared in the //go:systemstack stack growth prologue.\nIt is stack.lo+StackGuard on g0 and gsignal stacks.\nIt is ~0 on other goroutine stacks, to trigger a call to morestackc (and crash).\n"} stack ; offset known to runtime/cgo
    ^{:line 480 :tag uintptr} stackguard0 ; offset known to cmd/internal/obj/*
    ^{:line 481 :tag uintptr} stackguard1 ; offset known to cmd/internal/obj/*

    ^{:line 483 :tag (* _panic)} _panic ; innermost panic
    ^{:line 484 :tag (* _defer)} _defer ; innermost defer
    ^{:line 485 :tag (* m)} m ; current m
    ^{:line 486 :tag gobuf} sched
    ^{:line 487 :tag uintptr} syscallsp ; if status==Gsyscall, syscallsp = sched.sp to use during gc
    ^{:line 488 :tag uintptr} syscallpc ; if status==Gsyscall, syscallpc = sched.pc to use during gc
    ^{:line 489 :tag uintptr} syscallbp ; if status==Gsyscall, syscallbp = sched.bp to use in fpTraceback
    ^{:line 490 :tag uintptr} stktopsp ; expected sp at top of stack, to check in traceback













    ^{:line 504 :tag unsafe/Pointer :doc "param is a generic pointer parameter field used to pass\nvalues in particular contexts where other storage for the\nparameter would be difficult to find. It is currently used\nin four ways:\n1. When a channel operation wakes up a blocked goroutine, it sets param to\n   point to the sudog of the completed blocking operation.\n2. By gcAssistAlloc1 to signal back to its caller that the goroutine completed\n   the GC cycle. It is unsafe to do so in any other way, because the goroutine's\n   stack may have moved in the meantime.\n3. By debugCallWrap to pass parameters to a new goroutine because allocating a\n   closure in the runtime is forbidden.\n4. When a panic is recovered and control returns to the respective frame,\n   param may point to a savedOpenDeferState.\n"} param
    ^{:line 505 :tag atomic/Uint32} atomicstatus
    ^{:line 506 :tag uint32} stackLock ; sigprof/scang lock; TODO: fold in to atomicstatus
    ^{:line 507 :tag uint64} goid
    ^{:line 508 :tag guintptr} schedlink
    ^{:line 509 :tag int64} waitsince ; approx time when the g become blocked
    ^{:line 510 :tag waitReason} waitreason ; if status==Gwaiting

    ^{:line 512 :tag bool} preempt ; preemption signal, duplicates stackguard0 = stackpreempt
    ^{:line 513 :tag bool} preemptStop ; transition to _Gpreempted on preemption; otherwise, just deschedule
    ^{:line 514 :tag bool} preemptShrink ; shrink stack at synchronous safe point




    ^{:line 519 :tag bool :doc "asyncSafePoint is set if g is stopped at an asynchronous\nsafe point. This means there are frames on the stack\nwithout precise pointer information.\n"} asyncSafePoint

    ^{:line 521 :tag bool} paniconfault ; panic (instead of crash) on unexpected fault address
    ^{:line 522 :tag bool} gcscandone ; g has scanned stack; protected by _Gscan bit in status
    ^{:line 523 :tag bool} throwsplit ; must not split stack




    ^{:line 528 :tag bool :doc "activeStackChans indicates that there are unlocked channels\npointing into this goroutine's stack. If true, stack\ncopying needs to acquire channel locks to protect these\nareas of the stack.\n"} activeStackChans



    ^{:line 532 :tag atomic/Bool :doc "parkingOnChan indicates that the goroutine is about to\npark on a chansend or chanrecv. Used to signal an unsafe point\nfor stack shrinking.\n"} parkingOnChan


    ^{:line 535 :tag bool :doc "inMarkAssist indicates whether the goroutine is in mark assist.\nUsed by the execution tracer.\n"} inMarkAssist
    ^{:line 536 :tag bool} coroexit ; argument to coroswitch_m

    ^{:line 538 :tag int8} raceignore ; ignore race detection events
    ^{:line 539 :tag bool} nocgocallback ; whether disable callback from C
    ^{:line 540 :tag bool} tracking ; whether we're tracking this G for sched latency statistics
    ^{:line 541 :tag uint8} trackingSeq ; used to decide whether to track this G
    ^{:line 542 :tag int64} trackingStamp ; timestamp of when the G last started being tracked
    ^{:line 543 :tag int64} runnableTime ; the amount of time spent runnable, cleared when running, only used when tracking
    ^{:line 544 :tag muintptr} lockedm
    ^{:line 545 :tag uint8} fipsIndicator
    ^{:line 546 :tag bool} fipsOnlyBypass
    ^{:line 547 :tag bool} ditWanted ; set if g wants to be executed with DIT enabled
    ^{:line 548 :tag bool} syncSafePoint ; set if g is stopped at a synchronous safe point.
    ^{:line 549 :tag atomic/Bool} runningCleanups
    ^{:line 550 :tag uint32} sig
    ^{:line 551 :tag int32} secret ; current nesting of runtime/secret.Do calls.
    ^{:line 552 :tag (slice byte)} writebuf
    ^{:line 553 :tag uintptr} sigcode0
    ^{:line 554 :tag uintptr} sigcode1
    ^{:line 555 :tag uintptr} sigpc
    ^{:line 556 :tag uint64} parentGoid ; goid of goroutine that created this goroutine
    ^{:line 557 :tag uintptr} gopc ; pc of go statement that created this goroutine
    ^{:line 558 :tag (* (slice ancestorInfo))} ancestors ; ancestor information goroutine(s) that created this goroutine (only used if debug.tracebackancestors)
    ^{:line 559 :tag uintptr} startpc ; pc of goroutine function
    ^{:line 560 :tag uintptr} racectx
    ^{:line 561 :tag (* sudog)} waiting ; sudog structures this g is waiting on (that have a valid elem ptr); in lock order
    ^{:line 562 :tag (slice uintptr)} cgoCtxt ; cgo traceback context
    ^{:line 563 :tag unsafe/Pointer} labels ; profiler labels
    ^{:line 564 :tag (* timer)} timer ; cached timer for time.Sleep
    ^{:line 565 :tag int64} sleepWhen ; when to sleep until
    ^{:line 566 :tag atomic/Uint32} selectDone ; are we participating in a select and did someone win the race?



    ^{:line 570 :tag goroutineProfileStateHolder :doc "goroutineProfiled indicates the status of this goroutine's stack for the\ncurrent in-progress goroutine profile\n"} goroutineProfiled

    ^{:line 572 :tag (* coro)} coroarg ; argument during coroutine transfers
    ^{:tag unsafe/Pointer :doc "arbaceLocal is Arbace's goroutine-local slot: jrt's current java.lang.Thread\n(arbace_local.go).\n"} arbaceLocal ; Arbace
    ^{:line 573 :tag (* synctestBubble)} bubble



    ^{:line 577 :tag xRegPerG :doc "xRegs stores the extended register state if this G has been\nasynchronously preempted.\n"} xRegs


    ^{:line 580 :tag gTraceState :doc "Per-G tracer state.\n"} trace

    ;; Per-G GC state








    ^{:line 591 :tag int64 :doc "gcAssistBytes is this G's GC assist credit in terms of\nbytes allocated. If this is positive, then the G has credit\nto allocate gcAssistBytes bytes without assisting. If this\nis negative, then the G must correct this by performing\nscan work. We track this in bytes to make it fast to update\nand check for debt in the malloc hot path. The assist ratio\ndetermines how this corresponds to scan work debt.\n"} gcAssistBytes



    ^{:line 595 :tag uintptr :doc "valgrindStackID is used to track what memory is used for stacks when a program is\nbuilt with the \"valgrind\" build tag, otherwise it is unused.\n"} valgrindStackID))




(go/const ^{:val 8 :doc "gTrackingPeriod is the number of transitions out of _Grunning between\nlatency tracking runs.\n"} gTrackingPeriod 8)

(go/const


  ^{:line 605} [^{:val 6 :doc "tlsSlots is the number of pointer-sized slots reserved for TLS on some platforms,\nlike Windows.\n"} tlsSlots 6]
  ^{:line 606} [^{:val 48} tlsSize (* tlsSlots goarch/PtrSize)])



(go/const "Values for m.freeWait.\n"
  ^{:line 611} [^{:val 0} freeMStack 0] ; M done, free stack and reference.
  ^{:line 612} [^{:val 1} freeMRef 1] ; M done, free reference.
  ^{:line 613} [^{:val 2} freeMWait 2]) ; M still in use.


(go/type m (struct
    ^{:line 617 :tag (* g)} g0 ; goroutine with scheduling stack
    ^{:line 618 :tag gobuf} morebuf ; gobuf arg to morestack
    ^{:line 619 :tag uint32} divmod ; div/mod denominator for arm - known to liblink (cmd/internal/obj/arm/obj5.go)

    ;; Fields whose offsets are not known to debuggers.

    ^{:line 623 :tag uint64} procid ; for debuggers, but offset not hard-coded
    ^{:line 624 :tag (* g)} gsignal ; signal-handling g
    ^{:line 625 :tag gsignalStack} goSigStack ; Go-allocated signal handling stack
    ^{:line 626 :tag sigset} sigmask ; storage for saved signal mask
    ^{:line 627 :tag (array tlsSlots uintptr)} tls ; thread-local storage (for x86 extern register)
    ^{:line 628 :tag (func [])} mstartfn
    ^{:line 629 :tag (* g)} curg ; current running goroutine
    ^{:line 630 :tag guintptr} caughtsig ; goroutine running during fatal signal



    ^{:line 634 :tag bool :doc "Indicates whether we've received a signal while\nrunning in secret mode.\n"} signalSecret







    ^{:line 642 :tag puintptr :doc "p is the currently attached P for executing Go code, nil if not executing user Go code.\n\nA non-nil p implies exclusive ownership of the P, unless curg is in _Gsyscall.\nIn _Gsyscall the scheduler may mutate this instead. The point of synchronization\nis the _Gscan bit on curg's status. The scheduler must arrange to prevent curg\nfrom transitioning out of _Gsyscall if it intends to mutate p.\n"} p

    ^{:line 644 :tag puintptr} nextp ; The next P to install before executing. Implies exclusive ownership of this P.
    ^{:line 645 :tag puintptr} oldp ; The P that was attached before executing a syscall.
    ^{:line 646 :tag int64} id
    ^{:line 647 :tag int32} mallocing
    ^{:line 648 :tag throwType} throwing
    ^{:line 649 :tag string} preemptoff ; if != "", keep curg running on this m
    ^{:line 650 :tag int32} locks
    ^{:line 651 :tag int32} dying
    ^{:line 652 :tag int32} profilehz
    ^{:line 653 :tag bool} spinning ; m is out of work and is actively looking for work
    ^{:line 654 :tag bool} blocked ; m is blocked on a note
    ^{:line 655 :tag bool} newSigstack ; minit on C thread called sigaltstack
    ^{:line 656 :tag int8} printlock
    ^{:line 657 :tag bool} incgo ; m is executing a cgo call
    ^{:line 658 :tag bool} isextra ; m is an extra m
    ^{:line 659 :tag bool} isExtraInC ; m is an extra m that does not have any Go frames
    ^{:line 660 :tag bool} isExtraInSig ; m is an extra m in a signal handler
    ^{:line 661 :tag atomic/Uint32} freeWait ; Whether it is safe to free g0 and delete m (one of freeMRef, freeMStack, freeMWait)
    ^{:line 662 :tag bool} needextram
    ^{:line 663 :tag bool} g0StackAccurate ; whether the g0 stack has accurate bounds
    ^{:line 664 :tag uint8} traceback
    ^{:line 665 :tag (slice (* p))} allpSnapshot ; Snapshot of allp for use after dropping P in findRunnable, nil otherwise.
    ^{:line 666 :tag uint64} ncgocall ; number of cgo calls in total
    ^{:line 667 :tag int32} ncgo ; number of cgo calls currently in progress
    ^{:line 668 :tag atomic/Uint32} cgoCallersUse ; if non-zero, cgoCallers in use temporarily
    ^{:line 669 :tag (* cgoCallers)} cgoCallers ; cgo traceback if crashing in cgo call
    ^{:line 670 :tag note} park
    ^{:line 671 :tag (* m)} alllink ; on allm
    ^{:line 672 :tag muintptr} schedlink
    ^{:line 673 :tag listNodeManual} idleNode
    ^{:line 674 :tag guintptr} lockedg
    ^{:line 675 :tag (array 32 uintptr)} createstack ; stack that created this thread, it's used for StackRecord.Stack0, so it must align with it.
    ^{:line 676 :tag uint32} lockedExt ; tracking for external LockOSThread
    ^{:line 677 :tag uint32} lockedInt ; tracking for internal lockOSThread
    ^{:line 678 :tag mWaitList} mWaitList ; list of runtime lock waiters
    ^{:line 679 :tag bool} ditEnabled ; set if DIT is currently enabled on this M

    ^{:line 681 :tag mLockProfile} mLockProfile ; fields relating to runtime.lock contention
    ^{:line 682 :tag (slice uintptr)} profStack ; used for memory/block/mutex stack traces



    ^{:line 686 :tag (func [(* g) unsafe/Pointer] [bool]) :doc "wait* are used to carry arguments from gopark into park_m, because\nthere's no stack to put them on. That is their sole purpose.\n"} waitunlockf
    ^{:line 687 :tag unsafe/Pointer} waitlock
    ^{:line 688 :tag int} waitTraceSkip
    ^{:line 689 :tag traceBlockReason} waitTraceBlockReason

    ^{:line 691 :tag uint32} syscalltick
    ^{:line 692 :tag (* m)} freelink ; on sched.freem
    ^{:line 693 :tag mTraceState} trace


    ^{:line 696 :tag uintptr :doc "These are here to avoid using the G stack so the stack can move during the call.\n"} libcallpc ; for cpu profiler
    ^{:line 697 :tag uintptr} libcallsp
    ^{:line 698 :tag guintptr} libcallg
    ^{:line 699 :tag winlibcall} winsyscall ; stores syscall parameters on windows

    ^{:line 701 :tag uintptr} vdsoSP ; SP for traceback while in VDSO call (0 if not in call)
    ^{:line 702 :tag uintptr} vdsoPC ; PC for traceback while in VDSO call




    ^{:line 707 :tag atomic/Uint32 :doc "preemptGen counts the number of completed preemption\nsignals. This is used to detect when a preemption is\nrequested, but fails.\n"} preemptGen


    ^{:line 710 :tag atomic/Uint32 :doc "Whether this is a pending preemption signal on this M.\n"} signalPending


    ^{:line 713 :tag pcvalueCache :doc "pcvalue lookup cache\n"} pcvalueCache

    ^{:line 715} dlogPerM

    ^{:line 717} mOS

    ^{:line 719 :tag chacha8rand/State} chacha8
    ^{:line 720 :tag uint32} cheaprand
    ^{:line 721 :tag uint64} cheaprand64


    ^{:line 724 :tag int :doc "Up to 10 locks held by this m, maintained by the lock ranking code.\n"} locksHeldLen
    ^{:line 725 :tag (array 10 heldLockInfo)} locksHeld


    ^{:line 728 :tag mWeakPointer :doc "self points this M until mexit clears it to return nil.\n"} self))


(go/const ^{:val 0} mRedZoneSize (* (<< 16 3) asanenabledBit)) ; redZoneSize(2048)

(go/type mPadded (struct
    ^{:line 734} m





    ^{:line 740 :tag (array (* (- 1 goarch/IsWasm) (- 2048 mallocHeaderSize mRedZoneSize (unsafe/Sizeof (lit m)))) byte) :doc "Size the runtime.m structure so it fits in the 2048-byte size class, and\nnot in the next-smallest (1792-byte) size class. That leaves the 11 low\nbits of muintptr values available for flags, as required by\nlock_spinbit.go.\n"} _))










(go/type mWeakPointer "mWeakPointer is a \"weak\" pointer to an M. A weak pointer for each M is\navailable as m.self. Users may copy mWeakPointer arbitrarily, and get will\nreturn the M if it is still live, or nil after mexit.\n\nThe zero value is treated as a nil pointer.\n\nNote that get may race with M exit. A successful get will keep the m object\nalive, but the M itself may be exited and thus not actually usable.\n" (struct
    ^{:line 752 :tag (* (atomic/Pointer m))} m))


^{:go/end 759} (go/func newMWeakPointer ^mWeakPointer [^{:tag (* m)} mp]
  (let [w (lit mWeakPointer :m (new (inst atomic/Pointer m)))]
    (.Store (.-m w) mp)
    ^{:line 758} w))


^{:go/end 766} (go/method get ^{:tag (* m)} [^mWeakPointer w]
  (when (== (.-m w) nil)
    (return nil))

  (.Load (.-m w)))




^{:go/end 772} (go/method clear "clear sets the weak pointer to nil. It cannot be used on zero value\nmWeakPointers.\n" [^mWeakPointer w]
  (.Store (.-m w) nil))


(go/type p (struct
    ^{:line 775 :tag int32} id
    ^{:line 776 :tag uint32} status ; one of pidle/prunning/...
    ^{:line 777 :tag puintptr} link
    ^{:line 778 :tag uint32} schedtick ; incremented on every scheduler call
    ^{:line 779 :tag uint32} syscalltick ; incremented on every system call
    ^{:line 780 :tag sysmontick} sysmontick ; last tick observed by sysmon
    ^{:line 781 :tag muintptr} m ; back-link to associated m (nil if idle)
    ^{:line 782 :tag (* mcache)} mcache
    ^{:line 783 :tag pageCache} pcache
    ^{:line 784 :tag uintptr} raceprocctx










    ^{:line 795 :tag mWeakPointer :doc "oldm is the previous m this p ran on.\n\nWe are not associated with this m, so we have no control over its\nlifecycle. This value is an m.self object which points to the m\nuntil the m exits.\n\nNote that this m may be idle, running, or exiting. It should only be\nused with mgetSpecific, which will take ownership of the m only if\nit is idle.\n"} oldm

    ^{:line 797 :tag (slice (* _defer))} deferpool ; pool of available defer structs (see panic.go)
    ^{:line 798 :tag (array 32 (* _defer))} deferpoolbuf


    ^{:line 801 :tag uint64 :doc "Cache of goroutine ids, amortizes accesses to runtime·sched.goidgen.\n"} goidcache
    ^{:line 802 :tag uint64} goidcacheend


    ^{:line 805 :tag uint32 :doc "Queue of runnable goroutines. Accessed without lock.\n"} runqhead
    ^{:line 806 :tag uint32} runqtail
    ^{:line 807 :tag (array 256 guintptr)} runq












    ^{:line 820 :tag guintptr :doc "runnext, if non-nil, is a runnable G that was ready'd by\nthe current G and should be run next instead of what's in\nrunq if there's time remaining in the running G's time\nslice. It will inherit the time left in the current time\nslice. If a set of goroutines is locked in a\ncommunicate-and-wait pattern, this schedules that set as a\nunit and eliminates the (potentially large) scheduling\nlatency that otherwise arises from adding the ready'd\ngoroutines to the end of the run queue.\n\nNote that while other P's may atomically CAS this to zero,\nonly the owner P can CAS it to a valid G.\n"} runnext


    ^{:line 823 :tag gList :doc "Available G's (status == Gdead)\n"} gFree

    ^{:line 825 :tag (slice (* sudog))} sudogcache
    ^{:line 826 :tag (array 128 (* sudog))} sudogbuf


    ^{:line 829 :tag (struct ^{:tag int :doc "We need an explicit length here because this field is used\nin allocation codepaths where write barriers are not allowed,\nand eliminating the write barrier/keeping it eliminated from\nslice updates is tricky, more so than just managing the length\nourselves.\n"} len ^{:tag (array 128 (* mspan))} buf) :doc "Cache of mspan objects from the heap.\n"} mspancache











    ^{:line 841 :tag (* pinner) :doc "Cache of a single pinner object to reduce allocations from repeated\npinner creation.\n"} pinnerCache

    ^{:line 843 :tag pTraceState} trace

    ^{:line 845 :tag persistentAlloc} palloc ; per-P to avoid mutex


    ^{:line 848 :tag int64 :doc "Per-P GC state\n"} gcAssistTime ; Nanoseconds in assistAlloc
    ^{:line 849 :tag atomic/Int64} gcFractionalMarkTime ; Nanoseconds in fractional mark worker


    ^{:line 852 :tag limiterEvent :doc "limiterEvent tracks events for the GC CPU limiter.\n"} limiterEvent






    ^{:line 859 :tag gcMarkWorkerMode :doc "gcMarkWorkerMode is the mode for the next mark worker to run in.\nThat is, this is used to communicate with the worker goroutine\nselected for immediate execution by\ngcController.findRunnableGCWorker. When scheduling other goroutines,\nthis field must be set to gcMarkWorkerNotWorker.\n"} gcMarkWorkerMode


    ^{:line 862 :tag int64 :doc "gcMarkWorkerStartTime is the nanotime() at which the most recent\nmark worker started.\n"} gcMarkWorkerStartTime











    ^{:line 874 :tag (* gcBgMarkWorkerNode) :doc "nextGCMarkWorker is the next mark worker to run. This may be set\nduring start-the-world to assign a worker to this P. The P runs this\nworker on the next call to gcController.findRunnableGCWorker. If the\nP runs something else or stops, it must release this worker via\ngcController.releaseNextGCMarkWorker.\n\nSee comment in gcBgMarkWorker about the lifetime of\ngcBgMarkWorkerNode.\n\nOnly accessed by this P or during STW.\n"} nextGCMarkWorker




    ^{:line 879 :tag gcWork :doc "gcw is this P's GC work buffer cache. The work buffer is\nfilled by write barriers, drained by mutator assists, and\ndisposed on certain GC state transitions.\n"} gcw




    ^{:line 884 :tag wbBuf :doc "wbBuf is this P's GC write barrier buffer.\n\nTODO: Consider caching this in the running G.\n"} wbBuf

    ^{:line 886 :tag uint32} runSafePointFn ; if 1, run sched.safePointFn at next safe point



    ^{:line 890 :tag atomic/Uint32 :doc "statsSeq is a counter indicating whether this P is currently\nwriting any stats. Its value is even when not, odd when it is.\n"} statsSeq


    ^{:line 893 :tag timers :doc "Timer heap.\n"} timers


    ^{:line 896 :tag (* cleanupBlock) :doc "Cleanups.\n"} cleanups
    ^{:line 897 :tag uint64} cleanupsQueued ; monotonic count of cleanups queued by this P





    ^{:line 903 :tag int64 :doc "maxStackScanDelta accumulates the amount of stack space held by\nlive goroutines (i.e. those eligible for stack scanning).\nFlushed to gcController.maxStackScan once maxStackScanSlack\nor -maxStackScanSlack is reached.\n"} maxStackScanDelta






    ^{:line 910 :tag uint64 :doc "gc-time statistics about current goroutines\nNote that this differs from maxStackScan in that this\naccumulates the actual stack observed to be used at GC time (hi - sp),\nnot an instantaneous measure of the total stack size that might need\nto be scanned (hi - lo).\n"} scannedStackSize ; stack size of goroutines scanned by this P
    ^{:line 911 :tag uint64} scannedStacks ; number of goroutines scanned by this P



    ^{:line 915 :tag bool :doc "preempt is set to indicate that this P should be enter the\nscheduler ASAP (regardless of what G is running on it).\n"} preempt


    ^{:line 918 :tag int64 :doc "gcStopTime is the nanotime timestamp that this P last entered _Pgcstop.\n"} gcStopTime


    ^{:line 921 :tag uint64 :doc "goroutinesCreated is the total count of goroutines created by this P.\n"} goroutinesCreated




    ^{:line 926 :tag xRegPerP :doc "xRegs is the per-P extended register state used by asynchronous\npreemption. This is an empty struct on platforms that don't use extended\nregister state.\n"} xRegs))

;; Padding is no longer needed. False sharing is now not a worry because p is large enough
;; that its size class is an integer multiple of the cache line size (for any of our architectures).


(go/type schedt (struct
    ^{:line 933 :tag atomic/Uint64} goidgen
    ^{:line 934 :tag atomic/Int64} lastpoll ; time of last network poll, 0 if currently polling
    ^{:line 935 :tag atomic/Int64} pollUntil ; time to which current poll is sleeping
    ^{:line 936 :tag atomic/Int32} pollingNet ; 1 if some P doing non-blocking network poll

    ^{:line 938 :tag mutex} lock

    ;; When increasing nmidle, nmidlelocked, nmsys, or nmfreed, be
    ;; sure to call checkdead().

    ^{:line 943 :tag listHeadManual} midle ; idle m's waiting for work
    ^{:line 944 :tag int32} nmidle ; number of idle m's waiting for work
    ^{:line 945 :tag int32} nmidlelocked ; number of locked m's waiting for work
    ^{:line 946 :tag int64} mnext ; number of m's that have been created and next M ID
    ^{:line 947 :tag int32} maxmcount ; maximum number of m's allowed (or die)
    ^{:line 948 :tag int32} nmsys ; number of system m's not counted for deadlock
    ^{:line 949 :tag int64} nmfreed ; cumulative number of freed m's

    ^{:line 951 :tag atomic/Int32} ngsys ; number of system goroutines
    ^{:line 952 :tag atomic/Int32} nGsyscallNoP ; number of goroutines in syscalls without a P but whose M is not isExtraInC

    ^{:line 954 :tag puintptr} pidle ; idle p's
    ^{:line 955 :tag atomic/Int32} npidle
    ^{:line 956 :tag atomic/Int32} nmspinning ; See "Worker thread parking/unparking" comment in proc.go.
    ^{:line 957 :tag atomic/Uint32} needspinning ; See "Delicate dance" comment in proc.go. Boolean. Must hold sched.lock to set to 1.


    ^{:line 960 :tag gQueue :doc "Global runnable queue.\n"} runq






    ^{:line 967 :tag (struct ^{:tag bool :doc "user disables scheduling of user goroutines.\n"} user ^gQueue runnable) :doc "disable controls selective disabling of the scheduler.\n\nUse schedEnableUser to control this.\n\ndisable is protected by sched.lock.\n"} disable


    ;; pending runnable Gs



    ^{:line 974 :tag (struct ^mutex lock ^gList stack ^gList noStack) :doc "Global cache of dead G's.\n"} gFree

    ;; Gs with stacks
    ;; Gs without stacks



    ^{:line 981 :tag mutex :doc "Central cache of sudog structs.\n"} sudoglock
    ^{:line 982 :tag (* sudog)} sudogcache


    ^{:line 985 :tag mutex :doc "Central pool of available defer structs.\n"} deferlock
    ^{:line 986 :tag (* _defer)} deferpool



    ^{:line 990 :tag (* m) :doc "freem is the list of m's waiting to be freed when their\nm.exited is set. Linked through m.freelink.\n"} freem

    ^{:line 992 :tag atomic/Bool} gcwaiting ; gc is waiting to run
    ^{:line 993 :tag int32} stopwait
    ^{:line 994 :tag note} stopnote
    ^{:line 995 :tag atomic/Bool} sysmonwait
    ^{:line 996 :tag note} sysmonnote



    ^{:line 1000 :tag (func [(* p)]) :doc "safePointFn should be called on each P at the next GC\nsafepoint if p.runSafePointFn is set.\n"} safePointFn
    ^{:line 1001 :tag int32} safePointWait
    ^{:line 1002 :tag note} safePointNote

    ^{:line 1004 :tag int32} profilehz ; cpu profiling rate

    ^{:line 1006 :tag int64} procresizetime ; nanotime() of last change to gomaxprocs
    ^{:line 1007 :tag int64} totaltime ; ∫gomaxprocs dt up to procresizetime

    ^{:line 1009 :tag bool} customGOMAXPROCS ; GOMAXPROCS was manually set from the environment or runtime.GOMAXPROCS





    ^{:line 1015 :tag mutex :doc "sysmonlock protects sysmon's actions on the runtime.\n\nAcquire and hold this mutex to block sysmon from interacting\nwith the rest of the runtime.\n"} sysmonlock




    ^{:line 1020 :tag timeHistogram :doc "timeToRun is a distribution of scheduling latencies, defined\nas the sum of time a G spends in the _Grunnable state before\nit transitions to _Grunning.\n"} timeToRun




    ^{:line 1025 :tag atomic/Int64 :doc "idleTime is the total CPU time Ps have \"spent\" idle.\n\nReset on each GC cycle.\n"} idleTime



    ^{:line 1029 :tag atomic/Int64 :doc "totalMutexWaitTime is the sum of time goroutines have spent in _Gwaiting\nwith a waitreason of the form waitReasonSync{RW,}Mutex{R,}Lock.\n"} totalMutexWaitTime





    ^{:line 1035 :tag timeHistogram :doc "stwStoppingTimeGC/Other are distributions of stop-the-world stopping\nlatencies, defined as the time taken by stopTheWorldWithSema to get\nall Ps to stop. stwStoppingTimeGC covers all GC-related STWs,\nstwStoppingTimeOther covers the others.\n"} stwStoppingTimeGC
    ^{:line 1036 :tag timeHistogram} stwStoppingTimeOther






    ^{:line 1043 :tag timeHistogram :doc "stwTotalTimeGC/Other are distributions of stop-the-world total\nlatencies, defined as the total time from stopTheWorldWithSema to\nstartTheWorldWithSema. This is a superset of\nstwStoppingTimeGC/Other. stwTotalTimeGC covers all GC-related STWs,\nstwTotalTimeOther covers the others.\n"} stwTotalTimeGC
    ^{:line 1044 :tag timeHistogram} stwTotalTimeOther





    ^{:line 1050 :tag atomic/Int64 :doc "totalRuntimeLockWaitTime (plus the value of lockWaitTime on each M in\nallm) is the sum of time goroutines have spent in _Grunnable and with an\nM, but waiting for locks within the runtime. This field stores the value\nfor Ms that have exited.\n"} totalRuntimeLockWaitTime



    ^{:line 1054 :tag atomic/Uint64 :doc "goroutinesCreated (plus the value of goroutinesCreated on each P in allp)\nis the sum of all goroutines created by the program.\n"} goroutinesCreated))



(go/const "Values for the flags field of a sigTabT.\n"
  ^{:line 1059} [^{:val 1} _SigNotify (<< 1 iota)] ; let signal.Notify have signal, even if from kernel
  ^{:line 1060} [^{:val 2} _SigKill] ; if signal.Notify doesn't take it, exit quietly
  ^{:line 1061} [^{:val 4} _SigThrow] ; if signal.Notify doesn't take it, exit loudly
  ^{:line 1062} [^{:val 8} _SigPanic] ; if the signal is from the kernel, panic
  ^{:line 1063} [^{:val 16} _SigDefault] ; if the signal isn't explicitly requested, don't monitor it
  ^{:line 1064} [^{:val 32} _SigGoExit] ; cause all runtime procs to exit (only used on Plan 9).
  ^{:line 1065} [^{:val 64} _SigSetStack] ; Don't explicitly install handler, but add SA_ONSTACK to existing libc handler
  ^{:line 1066} [^{:val 128} _SigUnblock] ; always unblock; see blockableSig
  ^{:line 1067} [^{:val 256} _SigIgn]) ; _SIG_DFL action is to ignore the signal






(go/type _func "Layout of in-memory per-function information prepared by linker\nSee https://golang.org/s/go12symtab.\nKeep in sync with linker (../cmd/link/internal/ld/pcln.go:/pclntab)\nand with package debug/gosym and with symtab.go in package runtime.\n" (struct
    ^{:line 1075} sys/NotInHeap ; Only in static data

    ^{:line 1077 :tag uint32} entryOff ; start pc, as offset from moduledata.text
    ^{:line 1078 :tag int32} nameOff ; function name, as index into moduledata.funcnametab.

    ^{:line 1080 :tag int32} args ; in/out args size
    ^{:line 1081 :tag uint32} deferreturn ; offset of start of a deferreturn call instruction from entry, if any.

    ^{:line 1083 :tag uint32} pcsp
    ^{:line 1084 :tag uint32} pcfile
    ^{:line 1085 :tag uint32} pcln
    ^{:line 1086 :tag uint32} npcdata
    ^{:line 1087 :tag uint32} cuOffset ; runtime.cutab offset of this function's CU
    ^{:line 1088 :tag int32} startLine ; line number of start of function (func keyword/TEXT directive)
    ^{:line 1089 :tag abi/FuncID} funcID ; set for certain special runtime functions
    ^{:line 1090 :tag abi/FuncFlag} flag
    ^{:line 1091 :tag (array 1 byte)} _ ; pad
    ^{:line 1092 :tag uint8} nfuncdata)) ; must be last, must end on a uint32-aligned boundary

;; The end of the struct is followed immediately by two variable-length
;; arrays that reference the pcdata and funcdata locations for this
;; function.

;; pcdata contains the offset into moduledata.pctab for the start of
;; that index's table. e.g.,
;; &moduledata.pctab[_func.pcdata[_PCDATA_UnsafePoint]] is the start of
;; the unsafe point table.
;;
;; An offset of 0 indicates that there is no table.
;;
;; pcdata [npcdata]uint32

;; funcdata contains the offset past moduledata.gofunc which contains a
;; pointer to that index's funcdata. e.g.,
;; *(moduledata.gofunc +  _func.funcdata[_FUNCDATA_ArgsPointerMaps]) is
;; the argument pointer map.
;;
;; An offset of ^uint32(0) indicates that there is no entry.
;;
;; funcdata [nfuncdata]uint32







(go/type funcinl "Pseudo-Func that is returned for PCs that occur in inlined code.\nA *Func can be either a *_func or a *funcinl, and they are distinguished\nby the first uintptr.\n\nTODO(austin): Can we merge this with inlinedCall?\n" (struct
    ^{:line 1123 :tag uint32} ones ; set to ^0 to distinguish from _func
    ^{:line 1124 :tag uintptr} entry ; entry of the real (the "outermost") frame
    ^{:line 1125 :tag string} name
    ^{:line 1126 :tag string} file
    ^{:line 1127 :tag int32} line
    ^{:line 1128 :tag int32} startLine))


(go/type ^:alias itab abi/ITab)



(go/type lfnode "Lock-free stack node.\nAlso known to export_test.go.\n" (struct
    ^{:line 1136 :tag uint64} next
    ^{:line 1137 :tag uintptr} pushcnt))


(go/type forcegcstate (struct
    ^{:line 1141 :tag mutex} lock
    ^{:line 1142 :tag (* g)} g
    ^{:line 1143 :tag atomic/Bool} idle))










(go/type _defer "A _defer holds an entry on the list of deferred calls.\nIf you add a field here, add code to clear it in deferProcStack.\nThis struct must match the code in cmd/compile/internal/ssagen/ssa.go:deferstruct\nand cmd/compile/internal/ssagen/ssa.go:(*state).call.\nSome defers will be allocated on the stack and some on the heap.\nAll defers are logically part of the stack, so write barriers to\ninitialize them are not required. All defers must be manually scanned,\nand for heap defers, marked.\n" (struct
    ^{:line 1155 :tag bool} heap
    ^{:line 1156 :tag bool} rangefunc ; true for rangefunc list
    ^{:line 1157 :tag uintptr} sp ; sp at time of defer
    ^{:line 1158 :tag uintptr} pc ; pc at time of defer
    ^{:line 1159 :tag (func [])} fn ; can be nil for open-coded defers
    ^{:line 1160 :tag (* _defer)} link ; next defer on G; can point to either heap or stack!



    ^{:line 1164 :tag (* (atomic/Pointer _defer)) :doc "If rangefunc is true, *head is the head of the atomic linked list\nduring a range-over-func execution.\n"} head))










(go/type _panic "A _panic holds information about an active panic.\n\nA _panic value must only ever live on the stack.\n\nThe gopanicFP and link fields are stack pointers, but don't need special\nhandling during stack growth: because they are pointer-typed and\n_panic values only live on the stack, regular stack pointer\nadjustment takes care of them.\n" (struct
    ^{:line 1176 :tag any} arg ; argument to panic
    ^{:line 1177 :tag (* _panic)} link ; link to earlier panic



    ^{:line 1181 :tag uintptr :doc "startPC and startSP track where _panic.start was called.\n(These are the SP and PC of the gopanic frame itself.)\n"} startPC
    ^{:line 1182 :tag unsafe/Pointer} startSP


    ^{:line 1185 :tag uintptr :doc "The current stack frame that we're running deferred calls for.\n"} pc
    ^{:line 1186 :tag unsafe/Pointer} sp
    ^{:line 1187 :tag unsafe/Pointer} fp



    ^{:line 1191 :tag uintptr :doc "retpc stores the PC where the panic should jump back to, if the\nfunction last returned by _panic.nextDefer() recovers the panic.\n"} retpc


    ^{:line 1194 :tag (* uint8) :doc "Extra state for handling open-coded defers.\n"} deferBitsPtr
    ^{:line 1195 :tag unsafe/Pointer} slotsPtr

    ^{:line 1197 :tag bool} recovered ; whether this panic has been recovered
    ^{:line 1198 :tag bool} repanicked ; whether this panic repanicked
    ^{:line 1199 :tag bool} goexit
    ^{:line 1200 :tag bool} deferreturn))





(go/type savedOpenDeferState "savedOpenDeferState tracks the extra state from _panic that's\nnecessary for deferreturn to pick up where gopanic left off,\nwithout needing to unwind the stack.\n" (struct
    ^{:line 1207 :tag uintptr} retpc
    ^{:line 1208 :tag uintptr} deferBitsOffset
    ^{:line 1209 :tag uintptr} slotsOffset))



(go/type ancestorInfo "ancestorInfo records details of where a goroutine was started.\n" (struct
    ^{:line 1214 :tag (slice uintptr)} pcs ; pcs from the stack of this goroutine
    ^{:line 1215 :tag uint64} goid ; goroutine id of this goroutine; original goroutine possibly dead
    ^{:line 1216 :tag uintptr} gopc)) ; pc of go statement that created this goroutine




(go/type waitReason "A waitReason explains why a goroutine has been stopped.\nSee gopark. Do not re-use waitReasons, add new ones.\n" uint8)

(go/const
  ^{:line 1224} [^{:tag waitReason :val 0} waitReasonZero iota] ; ""
  ^{:line 1225} [^{:val 1} waitReasonGCAssistMarking] ; "GC assist marking"
  ^{:line 1226} [^{:val 2} waitReasonIOWait] ; "IO wait"
  ^{:line 1227} [^{:val 3} waitReasonDumpingHeap] ; "dumping heap"
  ^{:line 1228} [^{:val 4} waitReasonGarbageCollection] ; "garbage collection"
  ^{:line 1229} [^{:val 5} waitReasonGarbageCollectionScan] ; "garbage collection scan"
  ^{:line 1230} [^{:val 6} waitReasonPanicWait] ; "panicwait"
  ^{:line 1231} [^{:val 7} waitReasonGCAssistWait] ; "GC assist wait"
  ^{:line 1232} [^{:val 8} waitReasonGCSweepWait] ; "GC sweep wait"
  ^{:line 1233} [^{:val 9} waitReasonGCScavengeWait] ; "GC scavenge wait"
  ^{:line 1234} [^{:val 10} waitReasonFinalizerWait] ; "finalizer wait"
  ^{:line 1235} [^{:val 11} waitReasonForceGCIdle] ; "force gc (idle)"
  ^{:line 1236} [^{:val 12} waitReasonUpdateGOMAXPROCSIdle] ; "GOMAXPROCS updater (idle)"
  ^{:line 1237} [^{:val 13} waitReasonSemacquire] ; "semacquire"
  ^{:line 1238} [^{:val 14} waitReasonSleep] ; "sleep"
  ^{:line 1239} [^{:val 15} waitReasonChanReceiveNilChan] ; "chan receive (nil chan)"
  ^{:line 1240} [^{:val 16} waitReasonChanSendNilChan] ; "chan send (nil chan)"
  ^{:line 1241} [^{:val 17} waitReasonSelectNoCases] ; "select (no cases)"
  ^{:line 1242} [^{:val 18} waitReasonSelect] ; "select"
  ^{:line 1243} [^{:val 19} waitReasonChanReceive] ; "chan receive"
  ^{:line 1244} [^{:val 20} waitReasonChanSend] ; "chan send"
  ^{:line 1245} [^{:val 21} waitReasonSyncCondWait] ; "sync.Cond.Wait"
  ^{:line 1246} [^{:val 22} waitReasonSyncMutexLock] ; "sync.Mutex.Lock"
  ^{:line 1247} [^{:val 23} waitReasonSyncRWMutexRLock] ; "sync.RWMutex.RLock"
  ^{:line 1248} [^{:val 24} waitReasonSyncRWMutexLock] ; "sync.RWMutex.Lock"
  ^{:line 1249} [^{:val 25} waitReasonSyncWaitGroupWait] ; "sync.WaitGroup.Wait"
  ^{:line 1250} [^{:val 26} waitReasonTraceReaderBlocked] ; "trace reader (blocked)"
  ^{:line 1251} [^{:val 27} waitReasonWaitForGCCycle] ; "wait for GC cycle"
  ^{:line 1252} [^{:val 28} waitReasonGCWorkerIdle] ; "GC worker (idle)"
  ^{:line 1253} [^{:val 29} waitReasonGCWorkerActive] ; "GC worker (active)"
  ^{:line 1254} [^{:val 30} waitReasonPreempted] ; "preempted"
  ^{:line 1255} [^{:val 31} waitReasonDebugCall] ; "debug call"
  ^{:line 1256} [^{:val 32} waitReasonGCMarkTermination] ; "GC mark termination"
  ^{:line 1257} [^{:val 33} waitReasonStoppingTheWorld] ; "stopping the world"
  ^{:line 1258} [^{:val 34} waitReasonFlushProcCaches] ; "flushing proc caches"
  ^{:line 1259} [^{:val 35} waitReasonTraceGoroutineStatus] ; "trace goroutine status"
  ^{:line 1260} [^{:val 36} waitReasonTraceProcStatus] ; "trace proc status"
  ^{:line 1261} [^{:val 37} waitReasonPageTraceFlush] ; "page trace flush"
  ^{:line 1262} [^{:val 38} waitReasonCoroutine] ; "coroutine"
  ^{:line 1263} [^{:val 39} waitReasonGCWeakToStrongWait] ; "GC weak to strong wait"
  ^{:line 1264} [^{:val 40} waitReasonSynctestRun] ; "synctest.Run"
  ^{:line 1265} [^{:val 41} waitReasonSynctestWait] ; "synctest.Wait"
  ^{:line 1266} [^{:val 42} waitReasonSynctestChanReceive] ; "chan receive (durable)"
  ^{:line 1267} [^{:val 43} waitReasonSynctestChanSend] ; "chan send (durable)"
  ^{:line 1268} [^{:val 44} waitReasonSynctestSelect] ; "select (durable)"
  ^{:line 1269} [^{:val 45} waitReasonSynctestWaitGroupWait] ; "sync.WaitGroup.Wait (durable)"
  ^{:line 1270} [^{:val 46} waitReasonCleanupWait]) ; "cleanup wait"


(go/var waitReasonStrings (lit (array ... string)
    ^{:line 1274} [waitReasonZero ""]
    ^{:line 1275} [waitReasonGCAssistMarking "GC assist marking"]
    ^{:line 1276} [waitReasonIOWait "IO wait"]
    ^{:line 1277} [waitReasonChanReceiveNilChan "chan receive (nil chan)"]
    ^{:line 1278} [waitReasonChanSendNilChan "chan send (nil chan)"]
    ^{:line 1279} [waitReasonDumpingHeap "dumping heap"]
    ^{:line 1280} [waitReasonGarbageCollection "garbage collection"]
    ^{:line 1281} [waitReasonGarbageCollectionScan "garbage collection scan"]
    ^{:line 1282} [waitReasonPanicWait "panicwait"]
    ^{:line 1283} [waitReasonSelect "select"]
    ^{:line 1284} [waitReasonSelectNoCases "select (no cases)"]
    ^{:line 1285} [waitReasonGCAssistWait "GC assist wait"]
    ^{:line 1286} [waitReasonGCSweepWait "GC sweep wait"]
    ^{:line 1287} [waitReasonGCScavengeWait "GC scavenge wait"]
    ^{:line 1288} [waitReasonChanReceive "chan receive"]
    ^{:line 1289} [waitReasonChanSend "chan send"]
    ^{:line 1290} [waitReasonFinalizerWait "finalizer wait"]
    ^{:line 1291} [waitReasonForceGCIdle "force gc (idle)"]
    ^{:line 1292} [waitReasonUpdateGOMAXPROCSIdle "GOMAXPROCS updater (idle)"]
    ^{:line 1293} [waitReasonSemacquire "semacquire"]
    ^{:line 1294} [waitReasonSleep "sleep"]
    ^{:line 1295} [waitReasonSyncCondWait "sync.Cond.Wait"]
    ^{:line 1296} [waitReasonSyncMutexLock "sync.Mutex.Lock"]
    ^{:line 1297} [waitReasonSyncRWMutexRLock "sync.RWMutex.RLock"]
    ^{:line 1298} [waitReasonSyncRWMutexLock "sync.RWMutex.Lock"]
    ^{:line 1299} [waitReasonSyncWaitGroupWait "sync.WaitGroup.Wait"]
    ^{:line 1300} [waitReasonTraceReaderBlocked "trace reader (blocked)"]
    ^{:line 1301} [waitReasonWaitForGCCycle "wait for GC cycle"]
    ^{:line 1302} [waitReasonGCWorkerIdle "GC worker (idle)"]
    ^{:line 1303} [waitReasonGCWorkerActive "GC worker (active)"]
    ^{:line 1304} [waitReasonPreempted "preempted"]
    ^{:line 1305} [waitReasonDebugCall "debug call"]
    ^{:line 1306} [waitReasonGCMarkTermination "GC mark termination"]
    ^{:line 1307} [waitReasonStoppingTheWorld "stopping the world"]
    ^{:line 1308} [waitReasonFlushProcCaches "flushing proc caches"]
    ^{:line 1309} [waitReasonTraceGoroutineStatus "trace goroutine status"]
    ^{:line 1310} [waitReasonTraceProcStatus "trace proc status"]
    ^{:line 1311} [waitReasonPageTraceFlush "page trace flush"]
    ^{:line 1312} [waitReasonCoroutine "coroutine"]
    ^{:line 1313} [waitReasonGCWeakToStrongWait "GC weak to strong wait"]
    ^{:line 1314} [waitReasonSynctestRun "synctest.Run"]
    ^{:line 1315} [waitReasonSynctestWait "synctest.Wait"]
    ^{:line 1316} [waitReasonSynctestChanReceive "chan receive (durable)"]
    ^{:line 1317} [waitReasonSynctestChanSend "chan send (durable)"]
    ^{:line 1318} [waitReasonSynctestSelect "select (durable)"]
    ^{:line 1319} [waitReasonSynctestWaitGroupWait "sync.WaitGroup.Wait (durable)"]
    ^{:line 1320} [waitReasonCleanupWait "cleanup wait"]))


^{:go/end 1328} (go/method String ^string [^waitReason w]
  (when (or (< w 0) (>= w (conv waitReason (len waitReasonStrings))))
    (return "unknown wait reason"))

  (aget waitReasonStrings w))






^{:go/end 1338} (go/method ^:go/nosplit isMutexWait "isMutexWait returns true if the goroutine is blocked because of\nsync.Mutex.Lock or sync.RWMutex.[R]Lock.\n" ^bool [^waitReason w]
  (or (== w waitReasonSyncMutexLock)
    (== w waitReasonSyncRWMutexRLock)
    (== w waitReasonSyncRWMutexLock)))






^{:go/end 1346} (go/method ^:go/nosplit isSyncWait "isSyncWait returns true if the goroutine is blocked because of\nsync library primitive operations.\n" ^bool [^waitReason w]
  (and (<= waitReasonSyncCondWait w) (<= w waitReasonSyncWaitGroupWait)))






^{:go/end 1356} (go/method ^:go/nosplit isChanWait "isChanWait is true if the goroutine is blocked because of non-nil\nchannel operations or a select statement with at least one case.\n" ^bool [^waitReason w]
  (or (== w waitReasonSelect)
    (== w waitReasonChanReceive)
    (== w waitReasonChanSend)))


^{:go/end 1360} (go/method isWaitingForSuspendG ^bool [^waitReason w]
  (aget isWaitingForSuspendG w))








(go/var ^{:doc "isWaitingForSuspendG indicates that a goroutine is only entering _Gwaiting and\nsetting a waitReason because it needs to be able to let the suspendG\n(used by the GC and the execution tracer) take ownership of its stack.\nThe G is always actually executing on the system stack in these cases.\n\nTODO(mknyszek): Consider replacing this with a new dedicated G status.\n"} isWaitingForSuspendG (lit (array (len waitReasonStrings) bool)
    ^{:line 1369} [waitReasonStoppingTheWorld true]
    ^{:line 1370} [waitReasonGCMarkTermination true]
    ^{:line 1371} [waitReasonGarbageCollection true]
    ^{:line 1372} [waitReasonGarbageCollectionScan true]
    ^{:line 1373} [waitReasonTraceGoroutineStatus true]
    ^{:line 1374} [waitReasonTraceProcStatus true]
    ^{:line 1375} [waitReasonPageTraceFlush true]
    ^{:line 1376} [waitReasonGCAssistMarking true]
    ^{:line 1377} [waitReasonGCWorkerActive true]
    ^{:line 1378} [waitReasonFlushProcCaches true]))


^{:go/end 1383} (go/method isIdleInSynctest ^bool [^waitReason w]
  (aget isIdleInSynctest w))



(go/var ^{:doc "isIdleInSynctest indicates that a goroutine is considered idle by synctest.Wait.\n"} isIdleInSynctest (lit (array (len waitReasonStrings) bool)
    ^{:line 1387} [waitReasonChanReceiveNilChan true]
    ^{:line 1388} [waitReasonChanSendNilChan true]
    ^{:line 1389} [waitReasonSelectNoCases true]
    ^{:line 1390} [waitReasonSleep true]
    ^{:line 1391} [waitReasonSyncCondWait true]
    ^{:line 1392} [waitReasonSynctestWaitGroupWait true]
    ^{:line 1393} [waitReasonCoroutine true]
    ^{:line 1394} [waitReasonSynctestRun true]
    ^{:line 1395} [waitReasonSynctestWait true]
    ^{:line 1396} [waitReasonSynctestChanReceive true]
    ^{:line 1397} [waitReasonSynctestChanSend true]
    ^{:line 1398} [waitReasonSynctestSelect true]))


(go/var

  ^{:line 1403} [^{:tag (* m) :doc "Linked-list of all Ms. Written under sched.lock, read atomically.\n"} allm]

  ^{:line 1405} [^int32 gomaxprocs]
  ^{:line 1406} [^int32 numCPUStartup]
  ^{:line 1407} [^forcegcstate forcegc]
  ^{:line 1408} [^schedt sched]
  ^{:line 1409} [^int32 newprocs])


(go/var


  ^{:line 1415} [^{:tag mutex :doc "allpLock protects P-less reads and size changes of allp, idlepMask,\nand timerpMask, and all writes to allp.\n"} allpLock]



  ^{:line 1419} [^{:tag (slice (* p)) :doc "len(allp) == gomaxprocs; may change at safe points, otherwise\nimmutable.\n"} allp]











  ^{:line 1431} [^{:tag pMask :doc "Bitmask of Ps in _Pidle list, one bit per P. Reads and writes must\nbe atomic. Length may change at safe points.\n\nEach P must update only its own bit. In order to maintain\nconsistency, a P going idle must set the idle mask simultaneously with\nupdates to the idle P list under the sched.lock, otherwise a racing\npidleget may clear the mask before pidleput sets the mask,\ncorrupting the bitmap.\n\nN.B., procresize takes ownership of all Ps in stopTheWorldWithSema.\n"} idlepMask]



























  ^{:line 1459} [^{:tag pMask :doc "Bitmask of Ps that may have a timer, one bit per P. Reads and writes\nmust be atomic. Length may change at safe points.\n\nIdeally, the timer mask would be kept immediately consistent on any timer\noperations. Unfortunately, updating a shared global data structure in the\ntimer hot path adds too much overhead in applications frequently switching\nbetween no timers and some timers.\n\nAs a compromise, the timer mask is updated only on pidleget / pidleput. A\nrunning P (returned by pidleget) may add a timer at any time, so its mask\nmust be set. An idle P (passed to pidleput) cannot add new timers while\nidle, so if it has no timers at that time, its mask may be cleared.\n\nThus, we get the following effects on timer-stealing in findRunnable:\n\n  - Idle Ps with no timers when they go idle are never checked in findRunnable\n    (for work- or timer-stealing; this is the ideal case).\n  - Running Ps must always be checked.\n  - Idle Ps whose timers are stolen must continue to be checked until they run\n    again, even after timer expiration.\n\nWhen the P starts running again, the mask should be set, as a timer may be\nadded at any time.\n\nTODO(prattmic): Additional targeted updates may improve the above cases.\ne.g., updating the mask when stealing a timer.\n"} timerpMask])


;; goarmsoftfp is used by runtime/cgo assembly.
;;
(go/directive "//go:linkname goarmsoftfp")

(go/var


  ^{:line 1469} [^{:tag lfstack :doc "Pool of GC parked background workers. Entries are type\n*gcBgMarkWorkerNode.\n"} gcBgMarkWorkerPool]


  ^{:line 1472} [^{:tag int32 :doc "Total number of gcBgMarkWorker goroutines. Protected by worldsema.\n"} gcBgMarkWorkerCount]





  ^{:line 1478} [^{:tag uint32 :doc "Information about what cpu features are available.\nPackages outside the runtime should not use these\nas they are not an external api.\nSet on startup in asm_{386,amd64}.s\n"} processorVersionInfo]
  ^{:line 1479} [^bool isIntel])













(go/directive "//go:linkname goarm")
(go/var "set by cmd/link on arm systems\naccessed using linkname by internal/runtime/atomic.\n\ngoarm should be an internal detail,\nbut widely used packages access it using linkname.\nNotable members of the hall of shame include:\n  - github.com/creativeprojects/go-selfupdate\n\nDo not remove or change the type signature.\nSee go.dev/issue/67401.\n"
  ^{:line 1495} [^uint8 goarm]
  ^{:line 1496} [^uint8 goarmsoftfp])



(go/var "Set by the linker so the runtime can determine the buildmode.\n"
  ^{:line 1501} [^bool islibrary] ; -buildmode=c-shared
  ^{:line 1502} [^bool isarchive]) ; -buildmode=c-archive



(go/const ^{:val true :doc "Must agree with internal/buildcfg.FramePointerEnabled.\n"} framepointer_enabled (or (== GOARCH "amd64") (== GOARCH "arm64")))






^{:go/end 1520} (go/func ^:go/noinline ^:go/nosplit getcallerfp "getcallerfp returns the frame pointer of the caller of the caller\nof this function.\n" ^uintptr []
  (let [fp (getfp)] ; This frame's FP.
    (when (!= fp 0)
      (set! fp @(conv (* uintptr) (conv unsafe/Pointer fp))) ; The caller's FP.
      (set! fp @(conv (* uintptr) (conv unsafe/Pointer fp)))) ; The caller's caller's FP.

    ^{:line 1519} fp))
