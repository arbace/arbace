//! The lowered and code levels' sample (doc/rust/SPEC.md §11.1, 3 and 4; RR6): bin/repr-rust
//! test, case 10. Its HIR, MIR and objects, built from it and from its printed files, compared.
#![allow(dead_code)]

/// A closure and an impl, whose names MIR prints with their positions (`{closure@FILE:L:C: L:C}`,
/// `<impl at FILE:L:C: L:C>`), and a cfg'd item, whose attribute's text in the HIR holds spans.
pub struct Counter<T> {
    items: Vec<T>,
}

impl<T: Clone> Counter<T> {
    pub fn new() -> Self { Counter { items: Vec::new() } }

    #[cfg(not(test))]
    pub fn push(&mut self, x: T) -> usize {
        self.items.push(x);
        self.items.len()
    }

    pub fn doubled(&self) -> Vec<(T, T)> {
        self.items.iter().map(|x| (x.clone(), x.clone())).collect()
    }
}

/// Panics' locations
pub fn nth(v: &[u32], i: usize) -> u32 {
    let n = v[i] + 1;
    n
}

/// Boxes a method's receiver moves the inner box out of (autoderef), freed at the block's end,
/// their pointers checked there (debug assertions): the location of the closing brace, which
/// stands alone here and on the last statement's line in the printed file of mode :lines.
pub trait Double {
    fn double(self: Box<Self>) -> usize;
}

impl Double for Box<usize> {
    fn double(self: Box<Box<usize>>) -> usize { **self * 2 }
}

pub fn quadruple() -> usize {
    let x: Box<Box<Box<Box<Box<usize>>>>> = Box::new(Box::new(Box::new(Box::new(Box::new(3)))));
    let n = x.double();
    n
}

/// An expression file of include! (RR6, SPEC RSS6)
pub fn table() -> [u32; 4] {
    include!("lowered_table.rs")
}
