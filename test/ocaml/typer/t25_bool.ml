let xor a b = (a || b) && not (a && b)
let show b = print_string (string_of_bool b)
let () = show (xor true false); show (xor true true); print_newline ()
let () = if 3 > 2 && "a" < "b" then print_endline "ordered"
