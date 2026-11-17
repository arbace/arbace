// rsi's sample of std (RE3, doc/rust/EVAL.md §8): formatting, collections, strings, env::args, Box<dyn>, i128 and floats through
// std's lowered code; the output is the oracle's.
use std::collections::{BTreeMap, HashMap, HashSet, VecDeque};
use std::fmt;

struct Point { x: i32, y: i32 }
impl fmt::Display for Point {
    fn fmt(&self, f: &mut fmt::Formatter) -> fmt::Result { write!(f, "({}, {})", self.x, self.y) }
}

trait Shape { fn area(&self) -> f64; fn name(&self) -> String { "shape".to_string() } }
struct Sq(f64);
struct Circle(f64);
impl Shape for Sq { fn area(&self) -> f64 { self.0 * self.0 } fn name(&self) -> String { format!("square {}", self.0) } }
impl Shape for Circle { fn area(&self) -> f64 { 3.14159 * self.0 * self.0 } }

fn main() {
    let args: Vec<String> = std::env::args().collect();
    println!("args: {}", args.len());
    println!("{:>8}|{:<8}|{:^8}|{:08.3}|{:+}|{:#x}|{:#b}|{:e}", "r", "l", "c", 3.14159, 7, 255, 5, 1234.5);
    println!("{:?} {:?} {:?}", Some(1), None::<u8>, Ok::<&str, ()>("ok"));
    println!("{:#?}", vec![(1, "a"), (2, "b")]);
    println!("{} {}", Point { x: 3, y: -4 }, i128::MAX);
    println!("{} {} {}", u128::MAX / 3, -170141183460469231731687303715884105728i128, 0.1f64 + 0.2);
    println!("{} {} {} {}", f32::MAX, f64::MIN_POSITIVE, 1e300 * 10.0, (-1.0f64).sqrt());
    let mut m = BTreeMap::new();
    for w in "the quick brown fox jumps over the lazy dog the end".split_whitespace() { *m.entry(w).or_insert(0) += 1; }
    println!("{:?}", m);
    let h: HashMap<u32, u32> = (0..1000).map(|i| (i, i * i)).collect();
    let mut s: u64 = 0;
    for (k, v) in &h { s += (*k as u64) * (*v as u64); }
    println!("hashmap {} {} {:?}", h.len(), s, h.get(&999));
    let hs: HashSet<char> = "mississippi".chars().collect();
    let mut v: Vec<char> = hs.into_iter().collect();
    v.sort();
    println!("{:?}", v);
    let mut d: VecDeque<i32> = (1..=5).collect();
    d.rotate_left(2);
    d.push_front(0);
    println!("{:?}", d);
    let mut words: Vec<String> = "delta alpha charlie bravo".split(' ').map(String::from).collect();
    words.sort_by(|a, b| b.cmp(a));
    println!("{}", words.join(","));
    let text = String::from("Grüße, 世界!");
    println!("{} {} {:?} {}", text.len(), text.chars().count(), text.find('世'), text.to_uppercase());
    println!("{:?}", "a=1;b=2;c=3".split(';').map(|kv| kv.split_once('=').unwrap()).collect::<Vec<_>>());
    println!("{:?} {:?}", "42".parse::<i64>(), "x".parse::<i32>().is_err());
    println!("{:?}", "3.25e2".parse::<f64>());
    let shapes: Vec<Box<dyn Shape>> = vec![Box::new(Sq(2.0)), Box::new(Circle(1.5))];
    for sh in &shapes { println!("{} {:.4}", sh.name(), sh.area()); }
    let total: f64 = shapes.iter().map(|s| s.area()).sum();
    println!("total {:.2}", total);
    let fizz: Vec<String> = (1..=15).map(|i| match (i % 3, i % 5) { (0, 0) => "FizzBuzz".into(), (0, _) => "Fizz".into(), (_, 0) => "Buzz".into(), _ => i.to_string() }).collect();
    println!("{}", fizz.join(" "));
    let r = std::rc::Rc::new(std::cell::RefCell::new(vec![1]));
    r.borrow_mut().push(2);
    println!("{:?} {}", r.borrow(), std::rc::Rc::strong_count(&r));
    eprintln!("to stderr {}", 1 << 20);
}
