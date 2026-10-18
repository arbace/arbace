// repr/rust's mode :full on what std's five crates lack (bin/repr-rust test, step 6): tabs,
// trailing blanks, a form feed, raw identifiers that are no keywords, an identifier not in
// NFC, a leading | in an arm, trailing commas, comments everywhere (beyond the BMP too: 😀),
// no final newline; the test makes a copy with a byte order mark and CRLF line ends.

pub fn r#foo(r#x: u8,) -> u8 {   
	let v: Vec<Vec<u8> > = vec![vec![r#x,],]; /* a /* nested */ block */
	match v.len() {
		| 0 => 1,   // tab, then spaces
		n if n > 1 => { 2 }, 
		_ => 3
	}
}



struct Café { a: u8, /* ✓ */ b: u16 , }
/// doc
fn main() { let _ = r#foo(1 /* arg */ ,); let _ = (1, 2,); }
// the end, with no newline