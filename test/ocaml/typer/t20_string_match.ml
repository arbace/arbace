let num = function "one" -> 1 | "two" -> 2 | "three" -> 3 | _ -> 0
let () = print_int (num "two" + num "three" + num "four"); print_newline ()
