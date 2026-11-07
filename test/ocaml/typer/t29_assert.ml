let safe_div a b = assert (b <> 0); a / b
let () = print_int (safe_div 10 2); print_newline ()
let () = try ignore (safe_div 1 0) with Assert_failure _ -> print_endline "assert"
