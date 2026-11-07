let s = "hello"
let n = String.length s
let t = String.sub s 1 3
let () = print_string (t ^ string_of_int n); print_newline ()
let () = print_endline (String.concat "-" ["a"; "b"; "c"])
let c = s.[0]
let () = print_char c; print_newline ()
