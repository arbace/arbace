exception Empty
exception Bad of int * string
let check = function
  | [] -> raise Empty
  | x :: _ when x < 0 -> raise (Bad (x, "negative"))
  | x :: _ -> x
let try_it l = match check l with
  | v -> print_int v; print_newline ()
  | exception Empty -> print_endline "empty"
  | exception Bad (n, s) -> print_int n; print_endline s
let () = try_it [3]; try_it []; try_it [-1]
