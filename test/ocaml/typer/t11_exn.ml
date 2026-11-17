exception Oops of string
let risky n = if n > 3 then raise (Oops "big") else n
let safe n = try risky n with Oops m -> print_string m; print_newline (); 0
let () = print_int (safe 2 + safe 5); print_newline ()
let () = try failwith "boom" with Failure s -> print_string s; print_newline ()
