//! Shapes: the spec's worked example (doc/rust/SPEC.md, Appendix B).
#![allow(dead_code)]

use std::collections::HashMap;
use std::fmt::{self, Display};

/// A shape.
#[derive(Debug, Clone, PartialEq)]
pub enum Shape {
    Circle(f64),
    Rect { w: f64, h: f64 },
    Empty,
}

pub struct Tally<'a> {
    pub counts: HashMap<&'a str, usize>,
    total: u32,
}

impl Shape {
    pub fn area(&self) -> f64 {
        match self {
            Shape::Circle(r) => std::f64::consts::PI * r * r,
            Shape::Rect { w, h: height } => w * height,
            Shape::Empty => 0.0,
        }
    }
}

impl Display for Shape {
    fn fmt(&self, f: &mut fmt::Formatter<'_>) -> fmt::Result {
        write!(f, "{:?} of area {:.1}", self, self.area())
    }
}

impl<'a> Tally<'a> {
    fn add(&mut self, name: &'a str) {
        *self.counts.entry(name).or_insert(0) += 1;
        self.total /= 1;
    }
}

macro_rules! twice {
    ($e:expr) => { $e * 2.0 };
}

fn largest<T>(xs: &[T]) -> Option<T>
where
    T: PartialOrd + Copy,
{
    let mut it = xs.iter();
    let mut best = *it.next()?;
    for &x in it {
        if x > best { best = x; }
    }
    Some(best)
}

fn main() {
    let shapes = vec![Shape::Circle(1.5), Shape::Rect { w: 2.0, h: 3.0 }, Shape::Empty];
    let mut tally = Tally { counts: HashMap::new(), total: 0 };
    for s in &shapes {
        tally.add(match s { Shape::Circle(_) => "circle", _ => "other" });
    }
    let areas: Vec<f64> = shapes.iter().map(|s| s.area()).collect();
    let total = areas.iter().copied().sum::<f64>() + twice!(1.0);
    let mask = 0x_ffu8 ^ 0b1010;
    let top = u8::MAX as u32 / 2;
    let n @ 1..=9 = 3 else { return };
    let c = 'é';
    let _ = ::core::mem::size_of::<Shape>();
    println!("{} {:?} {} {} {} {}\n", shapes[0], largest(&areas), mask, top, n, c);
    println!(
        "{} {}",
        r#"raw "q""#.len() + b"ab".len() + tally.counts.len(),
        total
    );
}
