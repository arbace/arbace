type counter = { name : string; mutable count : int }
let c = { name = "c"; count = 0 }
let bump c = c.count <- c.count + 1
let () = bump c; bump c; print_string c.name; print_int c.count; print_newline ()
