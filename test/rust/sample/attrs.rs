//! Attributes that rustdoc's JSON writes with their spans (bin/repr-rust test, case 5: the
//! interface level). Indented by two spaces, so that the printed file's columns differ.
#![allow(dead_code)]

pub mod inner {
  /// Kept on every target this corpus documents.
  #[cfg(not(target_os = "none"))]
  pub fn kept(x: u32) -> u32 {
    x + 1
  }

  #[cfg_attr(all(), doc = "Documented through `cfg_attr`.")]
  #[cfg(any(unix, windows))]
  pub struct Pair<T> {
    pub first: T,
    pub second: T,
  }

  #[cfg(target_pointer_width = "64")]
  pub const WIDE: bool = true;
}
