let greet ?(greeting = "hello") name = greeting ^ ", " ^ name
let () = print_string (greet "bob"); print_newline ()
let () = print_string (greet ~greeting:"hi" "ann"); print_newline ()
let h ?x () = match x with None -> 0 | Some v -> v
let () = print_int (h () + h ~x:5 ()); print_newline ()
