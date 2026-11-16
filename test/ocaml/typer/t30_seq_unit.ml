let log msg = print_string "["; print_string msg; print_string "]"
let () = log "a"; log "b"; print_newline ()
let unit_fun () = 42
let () = print_int (unit_fun ()); print_newline ()
let ignore_it = ignore (unit_fun ())
