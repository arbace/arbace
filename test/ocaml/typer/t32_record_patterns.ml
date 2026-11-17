type r = { mutable n : int; tag : string }
let describe = function { n = 0; tag } -> tag | { n; tag = "x" } -> string_of_int n | _ -> "other"
let r = { n = 3; tag = "x" }
let () = print_endline (describe r); r.n <- 0; print_endline (describe r)
