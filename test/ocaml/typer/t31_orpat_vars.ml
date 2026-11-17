type t = A of int | B of int * int | C
let f = function A x | B (x, _) -> x | C -> 0
let g = function (Some (a, b), _) when a > b -> a | (_, Some c) -> c | _ -> -1
let () = print_int (f (B (3, 4)) + f C + g (Some (5, 1), None) + g (None, Some 7)); print_newline ()
