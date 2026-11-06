// RF7's second sample: impls' paths (colocated or not, in fn bodies), Self in impls, defaults naming other parameters,
// supertraits' associated types, trait objects with arguments, auto traits, enum discriminants, statics with closures
use std::borrow::Cow;
use std::error::Error;
use std::fmt;
use std::ops::{Add, Deref, Index};

pub trait Container<Idx = usize> {
    type Output: ?Sized;
    fn at(&self, i: Idx) -> Option<&Self::Output>;
}

pub trait Named: Container { fn name(&self) -> Cow<'static, str>; }

pub struct Grid<T, S = Vec<T>> { cells: S, width: usize, _t: std::marker::PhantomData<T> }

impl<T> Container for Grid<T> {
    type Output = T;
    fn at(&self, i: usize) -> Option<&Self::Output> { self.cells.get(i) }
}

impl<T: Clone> Named for Grid<T> { fn name(&self) -> Cow<'static, str> { Cow::Borrowed("grid") } }

impl<T> Index<(usize, usize)> for Grid<T> {
    type Output = T;
    fn index(&self, (r, c): (usize, usize)) -> &T { &self.cells[r * self.width + c] }
}

impl<T: Add<Output = T> + Copy> Grid<T> {
    pub const ORIGIN: (usize, usize) = (0, 0);
    pub fn sum(&self) -> Option<T> { self.cells.iter().copied().reduce(|a, b| a + b) }
    pub fn rows(&self) -> impl Iterator<Item = &[T]> + '_ { self.cells.chunks(self.width) }
}

pub struct Meters(pub f64);
impl Deref for Meters { type Target = f64; fn deref(&self) -> &Self::Target { &self.0 } }
impl From<f64> for Meters { fn from(x: f64) -> Self { Meters(x) } }
impl<'a> From<&'a Meters> for f64 { fn from(m: &'a Meters) -> f64 { m.0 } }

pub mod units {
    pub struct Feet(pub f64);
    impl From<super::Meters> for Feet { fn from(m: super::Meters) -> Feet { Feet(m.0 * 3.28) } }
    impl std::fmt::Debug for super::Meters {
        fn fmt(&self, f: &mut std::fmt::Formatter<'_>) -> std::fmt::Result { write!(f, "{}m", self.0) }
    }
}

#[repr(u8)]
pub enum Level { Low = 1, Mid = 5, High = 9 }

#[derive(Debug)]
pub enum AppError { Io(std::io::Error), Parse { line: usize, msg: String } }
impl fmt::Display for AppError { fn fmt(&self, f: &mut fmt::Formatter) -> fmt::Result { write!(f, "app") } }
impl Error for AppError { fn source(&self) -> Option<&(dyn Error + 'static)> { None } }

pub fn objects(a: &dyn Container<u8, Output = str>, b: Box<dyn Error + Send + Sync>, c: &(dyn Named<Output = u8> + Send)) -> usize {
    let _ = (a, b, c); 0
}
pub fn bounded<C>(c: &C) -> Option<&C::Output> where C: Named + ?Sized { c.at(0) }
pub fn sup<N: Named<Output = i32>>(n: &N) -> i32 { *n.at(0).unwrap() }
pub fn with_self<T: Iterator<Item = u8> + Clone>(t: T) -> (T, Option<T::Item>) { (t.clone(), t.clone().next()) }
pub fn cow<'a>(s: &'a str, owned: bool) -> Cow<'a, str> { if owned { Cow::Owned(s.to_uppercase()) } else { Cow::Borrowed(s) } }
pub fn results() -> Result<(), Box<dyn Error>> { Ok(()) }

pub static HOOK: fn(u8) -> u8 = |x| x + 1;
pub const TABLE: [fn(i32) -> i32; 2] = [|x| x, |x| -x];

fn main() {
    struct Local<'s> { s: &'s str }
    impl<'s> Local<'s> { fn get(&self) -> &'s str { self.s } }
    impl fmt::Display for Local<'_> { fn fmt(&self, f: &mut fmt::Formatter<'_>) -> fmt::Result { f.write_str(self.s) } }
    let l = Local { s: "x" };
    let g = Grid { cells: vec![1, 2, 3, 4], width: 2, _t: std::marker::PhantomData };
    println!("{} {} {:?} {:?} {}", l.get(), l, g.sum(), g.at(1), g[(1, 1)]);
    println!("{} {:?} {}", *Meters(2.0), Meters(1.0), HOOK(TABLE[1](3) as u8 + 4));
    println!("{} {}", Level::Mid as u8, cow("a", true));
}
