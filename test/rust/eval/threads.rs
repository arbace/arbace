// rsi's sample of threads (doc/rust/EVAL.md §10, RE6): std's threads on ci's scheduler (crt's sched.go), each output
// deterministic natively too: results returned by join, a Mutex's counter, a channel from one producer, scoped threads,
// a Condvar, a Barrier, a named thread's panic caught by join (its message on stderr), a thread-local's destructor run
// as its thread exits, park and unpark, a spin wait on an atomic (ci's preemption), sleeps.
use std::cell::RefCell;
use std::sync::atomic::{AtomicBool, AtomicUsize, Ordering};
use std::sync::mpsc;
use std::sync::{Arc, Barrier, Condvar, Mutex};
use std::thread;
use std::time::{Duration, Instant};

struct Noisy(u32);
impl Drop for Noisy {
    fn drop(&mut self) { println!("tls drop {}", self.0); }
}
thread_local! {
    static LOCAL: RefCell<u32> = RefCell::new(1);
    static NOISY: RefCell<Option<Noisy>> = RefCell::new(None);
}

fn main() {
    // join's results, each thread's own thread-local
    let hs: Vec<_> = (0..4u32).map(|i| thread::spawn(move || {
        LOCAL.with(|l| *l.borrow_mut() += i);
        LOCAL.with(|l| *l.borrow() * 10)
    })).collect();
    let rs: Vec<u32> = hs.into_iter().map(|h| h.join().unwrap()).collect();
    println!("results {:?}, main's local {}", rs, LOCAL.with(|l| *l.borrow()));

    // a Mutex's counter
    let c = Arc::new(Mutex::new(0u64));
    let hs: Vec<_> = (0..8).map(|_| { let c = c.clone(); thread::spawn(move || for _ in 0..500 { *c.lock().unwrap() += 1; }) }).collect();
    for h in hs { h.join().unwrap(); }
    println!("counter {}", *c.lock().unwrap());

    // a channel
    let (tx, rx) = mpsc::channel();
    let p = thread::spawn(move || { for i in 0..5 { tx.send(i * i).unwrap(); } });
    let got: Vec<i32> = rx.iter().collect();
    p.join().unwrap();
    println!("channel {:?}", got);

    // a bounded channel, the producer waits for the consumer
    let (tx, rx) = mpsc::sync_channel(1);
    let p = thread::spawn(move || { for i in 0..4 { tx.send(i).unwrap(); } });
    let mut sum = 0; for v in rx { sum += v; }
    p.join().unwrap();
    println!("sync channel sum {}", sum);

    // scoped threads borrowing the stack
    let data = vec![1, 2, 3, 4, 5, 6];
    let total = AtomicUsize::new(0);
    thread::scope(|s| {
        for ch in data.chunks(2) { let total = &total; s.spawn(move || { total.fetch_add(ch.iter().sum::<usize>(), Ordering::SeqCst); }); }
    });
    println!("scoped total {}", total.load(Ordering::SeqCst));

    // a Condvar
    let pair = Arc::new((Mutex::new(false), Condvar::new()));
    let p2 = pair.clone();
    let h = thread::spawn(move || { let (m, cv) = &*p2; *m.lock().unwrap() = true; cv.notify_one(); });
    { let (m, cv) = &*pair; let mut g = m.lock().unwrap(); while !*g { g = cv.wait(g).unwrap(); } }
    h.join().unwrap();
    println!("condvar ok");

    // a Barrier
    let b = Arc::new(Barrier::new(3));
    let hs: Vec<_> = (0..3).map(|_| { let b = b.clone(); thread::spawn(move || b.wait().is_leader()) }).collect();
    let leaders = hs.into_iter().map(|h| h.join().unwrap()).filter(|&l| l).count();
    println!("barrier leaders {}", leaders);

    // a named thread's panic, caught by join
    let h = thread::Builder::new().name("worker".into()).spawn(|| -> u32 { panic!("boom {}", 7) }).unwrap();
    let r = h.join();
    let e = r.unwrap_err();
    println!("panicked thread: {:?}", e.downcast_ref::<String>().cloned().or(e.downcast_ref::<&str>().map(|s| s.to_string())));
    println!("names: {:?} {:?}", thread::current().name(), thread::Builder::new().name("n2".into()).spawn(|| thread::current().name().map(|s| s.to_string())).unwrap().join().unwrap());

    // a thread-local's destructor at its thread's exit
    thread::spawn(|| { NOISY.with(|n| *n.borrow_mut() = Some(Noisy(42))); println!("set noisy"); }).join().unwrap();
    println!("after noisy");

    // park and unpark
    let flag = Arc::new(AtomicBool::new(false));
    let f2 = flag.clone();
    let h = thread::spawn(move || { while !f2.load(Ordering::Acquire) { thread::park(); } "unparked" });
    flag.store(true, Ordering::Release);
    h.thread().unpark();
    println!("park: {}", h.join().unwrap());

    // a spin wait on an atomic, no system call in the loop
    let go = Arc::new(AtomicBool::new(false));
    let g2 = go.clone();
    let h = thread::spawn(move || { let mut n = 0u64; while !g2.load(Ordering::SeqCst) { n += 1; std::hint::spin_loop(); } n > 0 || true });
    thread::sleep(Duration::from_millis(5));
    go.store(true, Ordering::SeqCst);
    println!("spin: {}", h.join().unwrap());

    // sleeps
    let t0 = Instant::now();
    let hs: Vec<_> = (1..=3u64).map(|i| thread::spawn(move || { thread::sleep(Duration::from_millis(10 * i)); i })).collect();
    let s: u64 = hs.into_iter().map(|h| h.join().unwrap()).sum();
    println!("slept {} at least 30 ms: {}", s, t0.elapsed() >= Duration::from_millis(30));
}
