(* let module, local opens, let exception, functors applied in expressions, functor results constrained *)
module type ORD = sig type t val compare : t -> t -> int end

module MaxOf (O : ORD) : sig val max : O.t -> O.t -> O.t val name : string end = struct
  let name = "max"
  let max a b = if O.compare a b >= 0 then a else b
end

module IntOrd = struct type t = int let compare (a : int) b = compare a b end

let biggest l =
  let module M = MaxOf (IntOrd) in
  List.fold_left M.max min_int l

let words () =
  let module S = MaxOf (struct type t = string let compare = String.compare end) in
  S.max "apple" "pear" ^ " " ^ S.name

let sum l = List.(fold_left ( + ) 0 (rev l))

let lengths l = let open List in map length l

let find_first p l =
  let exception Found of int in
  try List.iter (fun x -> if p x then raise (Found x)) l; None
  with Found x -> Some x

let () =
  print_int (biggest [3; 9; 2]); print_newline ();
  print_string (words ()); print_newline ();
  print_int (sum [1; 2; 3]); print_newline ();
  List.iter print_int (lengths [[1]; []; [1; 2]]); print_newline ();
  (match find_first (fun x -> x > 4) [1; 5; 7] with Some x -> print_int x | None -> ());
  print_newline ()
