// RF4's sample (doc/rust/FRONTEND-SURVEY.md, "As built: RF4-RF5 start"): macro expansion's cases beyond the synthetic
// corpus: macro_rules! with nested repetitions, separators, every fragment kind, recursion, $crate, hygiene, macros in
// item, statement, expression, pattern and type position; std's macros and the builtins; the derives on enums and
// generic types.
use std::collections::HashMap;
use std::fmt::Write as _;

macro_rules! square {
    ($e:expr) => { $e * $e };
}
macro_rules! maxm {
    ($x:expr) => { $x };
    ($x:expr, $($rest:expr),+) => {{ let a = $x; let b = maxm!($($rest),+); if a > b { a } else { b } }};
}
macro_rules! make_fns {
    ($($name:ident => $val:literal),* $(,)?) => { $(fn $name() -> i32 { $val })* };
}
macro_rules! point {
    ($name:ident { $($f:ident : $t:ty),* }) => {
        #[derive(Debug, Clone, PartialEq, Default)]
        struct $name { $($f: $t),* }
    };
}
macro_rules! table {
    ($($row:ident: [$($v:expr),*]);* $(;)?) => {{
        let mut m = HashMap::new();
        $( m.insert(stringify!($row), vec![$($v),*]); )*
        m
    }};
}
macro_rules! hyg {
    ($e:expr) => {{ let x = 10; $e + x }};
}
macro_rules! pat_or {
    ($p:pat) => { matches!(3, $p) };
}
macro_rules! ty_of {
    ($t:ty) => { std::mem::size_of::<$t>() };
}
macro_rules! blk {
    ($b:block) => { (|| $b)() };
}
macro_rules! count {
    () => { 0usize };
    ($h:tt $($t:tt)*) => { 1usize + count!($($t)*) };
}
macro_rules! lifetimes {
    ($l:lifetime) => { fn pick<$l>(a: &$l str, _b: &$l str) -> &$l str { a } };
}
macro_rules! vis_struct {
    ($v:vis $n:ident) => { $v struct $n(pub u8); };
}

make_fns!(one => 1, two => 2, three => 3,);
point!(P3 { x: i32, y: i32, z: i32 });
lifetimes!('a);
vis_struct!(pub Wrapped);

#[derive(Debug, Clone, Copy, PartialEq, Eq, Hash, PartialOrd, Ord)]
enum Level { Low, Mid(u8), High { v: u16 } }

#[derive(Debug, Clone, PartialEq, Default)]
struct Pair<A, B>(A, B);

#[derive(Debug, Hash, PartialEq, Eq)]
enum Tree<T> { Leaf(T), Node(Box<Tree<T>>, Box<Tree<T>>) }

fn main() {
    let n = 7;
    println!("{} {}", square!(n + 1), maxm!(3, n, 5, 2));
    println!("{} {} {}", one(), two(), three());
    let p = P3 { x: 1, ..Default::default() };
    println!("{:?} {}", p.clone(), p == P3::default());
    let t = table!(a: [1, 2]; b: [3]; c: []);
    let mut keys: Vec<_> = t.keys().collect();
    keys.sort();
    println!("{:?}", keys);
    let x = 1;
    println!("{}", hyg!(x * 2));
    println!("{} {}", pat_or!(1 | 3), pat_or!(4..=9));
    println!("{} {}", ty_of!(u64), ty_of!((u8, u32)));
    println!("{}", blk!({ let q = 2; q * 21 }));
    println!("{}", count!(a b (c d) [e] {f}));
    println!("{}", pick("l", "r"));
    let w = Wrapped(9);
    println!("{}", w.0);
    let v = vec![0u8; 4];
    let e: Vec<i32> = vec![];
    assert_eq!(v.len(), 4);
    assert_ne!(e.len(), 1, "empty has {} elements", e.len());
    assert!(e.is_empty(), "{:?}", e);
    let mut s = String::new();
    write!(s, "{:>8.3}|{0:?}|{name}|{{}}", 3.14159, name = "nm").unwrap();
    writeln!(s, "!").unwrap();
    print!("{}", s);
    eprintln!("{}", format!("{:#x} {:08b} {:+e}", 255, 5u8, 1500.0));
    println!("{} {} {}", concat!("a", 1, 'b', true), stringify!(x + y), line!() > 0);
    println!("{} {}", file!().ends_with(".rs"), cfg!(unix));
    println!("{:?} {:?}", Level::Mid(2) < Level::High { v: 1 }, Level::Low.max(Level::Mid(0)));
    println!("{:?}", Pair(1, "x").clone() == Pair::default());
    let tr = Tree::Node(Box::new(Tree::Leaf(1)), Box::new(Tree::Leaf(2)));
    println!("{:?} {}", tr, tr == Tree::Leaf(3));
    let r: Result<u8, String> = Ok(1);
    match r {
        Ok(v) if v > 5 => unreachable!("big {}", v),
        Ok(_) => {}
        Err(e) => panic!("{}", e),
    }
    if n > 100 { todo!() }
    if n > 200 { unimplemented!("never") }
}
