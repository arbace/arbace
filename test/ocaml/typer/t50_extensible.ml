(* extensible variants: type extensions, GADT extension constructors, matches with a catch-all *)
type t = ..
type t += A of int | B
type _ id = ..
type _ id += Id : int id
let f = function A n -> n | B -> 0 | _ -> -1
let g : type a. a id -> a -> int = fun i x -> match i with Id -> x | _ -> 0
let () = print_int (f (A 3) + f B + g Id 4); print_newline ()
