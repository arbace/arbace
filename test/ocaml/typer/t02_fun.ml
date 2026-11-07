let add a b = a + b
let inc = add 1
let twice f x = f (f x)
let () = print_int (twice inc 5); print_newline ()
let compose f g x = f (g x)
let () = print_int (compose inc (add 10) 0); print_newline ()
