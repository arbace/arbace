(* parameterized variant abbreviations in #t patterns, variants in records and constructors, exception cases,
   explicitly polymorphic annotations, conjunctions, many constant tags *)
type 'a box = [ `Box of 'a | `Empty ]
let unbox = function #box as b -> (match b with `Box x -> Some x | `Empty -> None)
let get_or d = function `Box x -> x | `Empty -> d
type r = { kind : [ `Int | `Float ]; v : float }
let show r = match r with { kind = `Int; v } -> string_of_int (int_of_float v) | { kind = `Float; v } -> string_of_float v
type c = C of [ `On | `Off ] | D
let cv = function C `On -> 1 | C `Off -> 2 | D -> 3
let ex x = match x with `A -> 1 | exception Not_found -> 2 | `B -> 3
let poly : 'a. ([< `A | `B ] as 'a) -> int = function `A -> 1 | `B -> 2
let conj (x : [< `A of int & int | `B ]) = match x with `B -> 0
let first = function `F1 | `F2 | `F3 | `F4 | `F5 | `F6 | `F7 | `F8 | `F9 | `F10 -> 1 | `G -> 2
let seq = [ `F1; `F5; `G ]
let () = List.iter (fun x -> print_int (first x)) seq; print_newline ();
  Printf.printf "%d %s %s %d %d %d\n" (get_or 0 (`Box 4)) (show { kind = `Float; v = 1.5 }) (show { kind = `Int; v = 2. }) (cv (C `Off)) (poly `B) (ex `B);
  (match unbox (`Box "s") with Some s -> print_endline s | None -> ())
