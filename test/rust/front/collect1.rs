// RF7's sample (doc/rust/FRONTEND-SURVEY.md, "As built: RF7"): collection's harder cases, each item judged against
// rustc's generics_of, type_of, fn_sig and explicit_clauses_of (bin/rust-front collect-check collect1)
use std::collections::HashMap;
use std::fmt::{self, Debug, Display};
use std::rc::Rc;

pub trait Shape: Debug + Clone {
    type Unit: Copy + Default;
    const SIDES: usize = 0;
    fn area(&self) -> f64;
    fn scaled(&self, k: Self::Unit) -> Self where Self: Sized { self.clone() }
}

pub trait Store<K, V = String> {
    type Iter<'a>: Iterator<Item = (&'a K, &'a V)> where Self: 'a, K: 'a, V: 'a;
    fn entries<'a>(&'a self) -> Self::Iter<'a>;
}

pub trait Sub: Store<u8> {}

#[derive(Debug, Clone)]
pub struct Square<T = f64, const N: usize = 4> { side: T, tags: [u8; N] }

pub struct Wrapper<'a, T: ?Sized + 'a> { inner: &'a T, next: Option<Box<Wrapper<'a, T>>> }

pub union Bits { i: u32, f: f32 }

pub enum Tree<K: Ord, V> { Leaf, Node(Box<Tree<K, V>>, K, V, Box<Tree<K, V>>) }

pub type Callback<'a> = Box<dyn Fn(&str) -> bool + Send + 'a>;
pub type Table = HashMap<String, Vec<Rc<dyn Display>>>;

pub static NAMES: &[&str] = &["a", "b"];
pub static mut COUNT: usize = 0;
pub const LIMIT: Option<&'static dyn Debug> = None;

impl Shape for Square {
    type Unit = f64;
    const SIDES: usize = 4;
    fn area(&self) -> f64 { self.side * self.side }
}

impl<T: Display, const N: usize> Display for Square<T, N> {
    fn fmt(&self, f: &mut fmt::Formatter<'_>) -> fmt::Result { write!(f, "{}", self.side) }
}

impl<'a, T: ?Sized + Debug> Wrapper<'a, T> {
    pub fn new(inner: &'a T) -> Self { Wrapper { inner, next: None } }
    pub fn get(&self) -> &'a T { self.inner }
    pub fn pair<'b>(&'b self, other: &'b T) -> (&'b T, &'a T) { (other, self.inner) }
}

pub mod geo {
    pub mod deep {
        #[derive(Debug, PartialEq, Eq, Hash, PartialOrd, Ord, Clone, Copy)]
        pub struct Point(pub i32, pub i32);
        impl Point { pub fn origin() -> Self { Point(0, 0) } }
    }
    impl std::fmt::Display for deep::Point {
        fn fmt(&self, f: &mut std::fmt::Formatter) -> std::fmt::Result { write!(f, "({}, {})", self.0, self.1) }
    }
    impl super::Shape for deep::Point {
        type Unit = i32;
        fn area(&self) -> f64 { 0.0 }
    }
}

pub fn largest<'a, T>(xs: &'a [T]) -> Option<&'a T> where T: PartialOrd { xs.iter().fold(None, |m, x| match m { Some(y) if y >= x => Some(y), _ => Some(x) }) }
pub fn outlives<'a, 'b: 'a, T: 'b>(x: &'a T, y: &'b T) -> &'a T { if true { x } else { y } }
pub fn early<'a>() -> &'a str { "" }
pub fn sum_all<I>(it: I) -> i64 where I: IntoIterator, I::Item: Into<i64> { it.into_iter().map(Into::into).sum() }
pub fn first_key<S: Store<u32>>(s: &S) -> Option<&u32> { s.entries().next().map(|(k, _)| k) }
pub fn qualified<T: Iterator>(it: T) -> Option<<T as Iterator>::Item> { let mut it = it; it.next() }
pub fn boxed(n: u8) -> Box<dyn Iterator<Item = u8>> { Box::new(0..n) }
pub fn mixed(f: impl Fn(&u8) -> u8 + Clone, g: &mut dyn FnMut(u8)) -> impl Iterator<Item = u8> + Clone { g(f(&1)); std::iter::once(1) }
pub fn hrtb<F>(f: F) -> u8 where F: for<'x> Fn(&'x u8) -> &'x u8 { *f(&3) }
pub fn ptrs(p: *const u8, q: *mut [u16], r: fn(u8) -> u8, s: unsafe extern "C" fn(i32) -> i32) -> usize { let _ = (p, q, r, s); 0 }
pub unsafe fn danger(x: *mut i32) { unsafe { *x = 1 } }
pub extern "C" fn callback(x: i32) -> i32 { x }
pub fn arrays<const N: usize>(a: [[u8; N]; 2], b: [char; 3]) -> [u8; N] { let _ = b; a[0] }
pub fn tuples(t: (u8,), u: ((), (i8, i16))) -> ! { let _ = (t, u); loop {} }
pub fn rc_dyn(x: Rc<dyn Debug + 'static>, y: &(dyn Debug + Sync)) -> Rc<dyn Debug> { let _ = y; x }

fn main() {
    let s = Square { side: 2.0, tags: [0; 4] };
    println!("{} {:?} {}", s.area(), geo::deep::Point::origin(), Square::<f64, 4>::SIDES);
    let w = Wrapper::new(&5);
    println!("{:?} {:?}", w.get(), largest(&[1, 3, 2]));
    println!("{}", sum_all(vec![1i32, 2]));
}
