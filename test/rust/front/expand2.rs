// RF4's second sample (doc/rust/FRONTEND-SURVEY.md, "As built: RF4-RF5 start"): cfg on fields, variants, statements and
// expressions, #[default] variants, derives with bounds and where clauses, macros defined in blocks and by macros, $crate in
// a user's macro, a macro shadowed, dbg!, matches! with a guard, nested format arguments.
#![allow(dead_code, unused)]

#[macro_export]
macro_rules! here {
    () => { $crate::helper() };
}
fn helper() -> u32 { 41 }

macro_rules! define_getter {
    ($name:ident, $v:expr) => {
        macro_rules! $name { () => { $v } }
    };
}
define_getter!(seven, 7);

macro_rules! shadowed { () => { 1 } }
macro_rules! shadowed { () => { 2 } }

#[derive(Debug, Clone, PartialEq)]
struct Config {
    name: String,
    #[cfg(unix)]
    unix_only: u8,
    #[cfg(windows)]
    windows_only: u8,
}

#[derive(Debug, Default, Clone, Copy, PartialEq, Eq, Hash)]
enum Mode { Fast, #[default] Normal, Slow }

#[derive(Debug, Clone, PartialEq)]
struct Bounded<T: Clone + Default, U>(T, U) where U: Copy;

#[derive(Debug, PartialEq, PartialOrd)]
enum Num { I(i64), F(f64) }

enum Platform {
    #[cfg(unix)] Unix,
    #[cfg(windows)] Windows,
    Other,
}

fn main() {
    macro_rules! local { ($x:expr) => { $x + 100 } }
    println!("{} {} {} {}", here!(), seven!(), shadowed!(), local!(1));
    let c = Config { name: "n".into(), unix_only: 1 };
    println!("{:?}", c.clone() == c);
    #[cfg(unix)]
    let os = "unix";
    #[cfg(not(unix))]
    let os = "other";
    println!("{} {:?} {:?}", os, Mode::default(), Bounded(1u8, 'c'));
    let v = dbg!(2 * 3);
    let w = dbg!(v + 1, "two");
    println!("{} {}", matches!(Some(3), Some(n) if n > 2), matches!(Num::I(1), Num::I(_) | Num::F(_)));
    println!("{}", format!("{} {}", format!("{:>3}", v), format_args!("{}", w.0)));
    println!("{:?}", Num::F(1.5).partial_cmp(&Num::I(2)));
    let p = Platform::Other;
    let k = match p { #[cfg(unix)] Platform::Unix => 1, Platform::Other => 2 };
    println!("{}", k);
}
