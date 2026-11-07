let rec fib n = match n with 0 | 1 -> n | n -> fib (n - 1) + fib (n - 2)
let s = String.concat "," (List.map string_of_int (List.map fib [5; 10; 15]))
let () = print_endline s
let q = if String.length s > 3 then 'y' else 'n'
let () = print_char q; print_newline ()
