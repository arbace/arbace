(* polymorphic variant types: abbreviations named in printing, inheritance, bounds, #t patterns *)
type base = [ `A | `B of int ]
type ext = [ base | `C of string ]
type 'a tree = [ `Leaf | `Node of 'a tree * 'a * 'a tree ]

let describe (x : ext) = match x with
  | `A -> "A"
  | `B n -> "B" ^ string_of_int n
  | `C s -> "C" ^ s

let classify x = match x with
  | #base -> "base"
  | `C _ -> "C"
  | `D -> "D"

let upper : [< `A | `B of int ] -> int = function `A -> 0 | `B n -> n
let lower (x : [> `A ]) = x
let both (x : [< `A | `B > `A ]) = x

let rec size : 'a tree -> int = function
  | `Leaf -> 0
  | `Node (l, _, r) -> size l + 1 + size r

let t : int tree = `Node (`Node (`Leaf, 1, `Leaf), 2, `Leaf)

let only_base = function #base as b -> Some b | _ -> None

let () =
  print_endline (describe `A);
  print_endline (describe (`B 3));
  print_endline (describe (`C "x"));
  print_endline (classify (`B 1));
  print_endline (classify `D);
  print_int (upper (`B 7)); print_newline ();
  (match lower `A with `A -> print_endline "lower" | _ -> ());
  (match both `A with `A -> print_endline "both A" | `B -> print_endline "both B");
  print_int (size t); print_newline ();
  (match only_base (`C "z") with None -> print_endline "none" | Some _ -> print_endline "some");
  (match only_base `A with None -> print_endline "none" | Some _ -> print_endline "some")
