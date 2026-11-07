(* recursive values (Value_rec_check, Value_rec_compiler): blocks pre-allocated and backpatched, lazy values,
   constants, dynamic bindings and functions that are not syntactic functions *)
type t = { hd : int; tl : t }
let rec ones = 1 :: ones
let rec cyc = { hd = 3; tl = cyc }
let rec l = lazy (fst (42, l))
let rec n = (let _ = p in 7) and p x = x + n and m = String.length "abc"
let rec f = let k = 10 in fun x -> if x = 0 then k else f (x - 1) + 1
let rec g = let h x = if x > 3 then x else g (x + 1) in h
let rec a = Some b and b = [| 1; 2 |]
let () =
  print_int (List.hd (List.tl ones)); print_newline ();
  print_int cyc.tl.tl.hd; print_newline ();
  print_int (Lazy.force l); print_newline ();
  print_int (n + m + p 1); print_newline ();
  print_int (f 5 + g 0); print_newline ();
  (match a with Some b -> print_int b.(1) | None -> ()); print_newline ()

let local x =
  let rec xs = x :: ys and ys = x + 1 :: xs in
  List.hd (List.tl (List.tl xs))
let () = print_int (local 4); print_newline ()
