module type S = sig type t val x : t module N : sig end end
module F (X : S) (Y : S with type t = int) = struct include X let y = X.x open Y end
module A = struct type t = int let x = 1 module N = struct end end
module G = F (A) (struct type t = int let x = 2 module N = struct end end)
module H : S = G
let f () = let open List in let module M = struct let z = 1 end in M.z
let g = List.(length [])
module I = struct include G let w = 2 end
open I
let h = w + y
let () = print_int h; print_newline (); print_int (f ()); print_int g; print_newline ()
