// RF5's sample (doc/rust/FRONTEND-SURVEY.md, "As built: RF4-RF5 start"): name resolution's cases beyond the
// synthetic corpus: nested modules, crate/self/super paths, globs of local enums and modules, renames, shadowing,
// block items and imports, generics and Self, let-else, while let, or-patterns, consts in patterns.
use std::collections::HashMap as Map;
use std::fmt::{self, Debug};

mod geom {
    pub mod shapes {
        #[derive(Debug, Clone, Copy, PartialEq)]
        pub enum Kind { Dot, Line(u32), Box { w: u32, h: u32 } }
        pub const ORIGIN: (i32, i32) = (0, 0);
        pub fn area(k: Kind) -> u32 {
            use self::Kind::*;
            match k { Dot => 0, Line(_) => 0, Box { w, h } => w * h }
        }
        pub fn unit() -> super::Unit { super::Unit }
    }
    #[derive(Debug, Default)]
    pub struct Unit;
    pub use self::shapes::Kind;
    pub fn describe(k: Kind) -> String { format!("{:?}/{}", k, crate::geom::shapes::area(k)) }
}

use geom::shapes::{self as sh, Kind::*};

const LIMIT: u32 = 3;
static mut COUNTER: u32 = 0;

trait Named {
    const TAG: &'static str;
    fn name(&self) -> String { Self::TAG.to_string() }
    fn twice(&self) -> String where Self: Sized { let n = self.name(); n.clone() + &n }
}

struct Pair<T, const N: usize> { items: [T; N] }

impl<T: Copy + Debug, const N: usize> Pair<T, N> {
    fn new(x: T) -> Self { Pair { items: [x; N] } }
    fn first(&self) -> T { self.items[0] }
    fn len() -> usize { N }
}

impl<T, const N: usize> Named for Pair<T, N> { const TAG: &'static str = "pair"; }

impl fmt::Display for geom::Unit {
    fn fmt(&self, f: &mut fmt::Formatter<'_>) -> fmt::Result { write!(f, "unit") }
}

fn classify(n: u32) -> &'static str {
    const SMALL: u32 = 1;
    match n {
        0 => "zero",
        SMALL => "one",
        LIMIT => "limit",
        2 | 4 => "even",
        _ => "other",
    }
}

fn parse(s: &str) -> Option<u32> {
    let Some(first) = s.chars().next() else { return None };
    let d = first.to_digit(10)?;
    Some(d)
}

fn main() {
    let mut m: Map<&str, sh::Kind> = Map::new();
    m.insert("dot", Dot);
    m.insert("line", Line(4));
    m.insert("box", Box { w: 2, h: 3 });
    let x = 1;
    let x = x + 1;
    {
        fn helper(v: u32) -> u32 { v + LIMIT }
        use geom::describe as d;
        println!("{} {}", helper(x), d(Dot));
    }
    let p: Pair<u8, 4> = Pair::new(7);
    println!("{} {} {}", p.first(), Pair::<u8, 4>::len(), p.twice());
    let mut it = vec![1, 2, 3].into_iter();
    while let Some(v) = it.next() {
        if let 1 | 2 = v { println!("{}", classify(v)); }
    }
    let total: u32 = m.values().map(|k| sh::area(*k)).sum();
    unsafe { COUNTER += total; }
    let u = geom::shapes::unit();
    println!("{} {:?} {:?} {}", u, geom::Unit::default(), parse("7x"), geom::shapes::ORIGIN.0);
    let sh::Kind::Line(n) = geom::Kind::Line(9) else { panic!() };
    println!("{}", n + geom::describe(geom::Kind::Dot).len() as u32);
}
