(* binding operators: let*, and*, let+ (Typecore's Pexp_letop, Translcore.transl_letop) *)
let ( let* ) x f = match x with Some v -> f v | None -> None
let ( and* ) a b = match a, b with Some x, Some y -> Some (x, y) | _ -> None
let ( let+ ) x f = Option.map f x
let ( and+ ) = ( and* )

let sum3 a b c = let* x = a and* y = b and* z = c in Some (x + y + z)
let pair = let+ (a, b) = Some (1, 2) and+ c = Some 3 in a + b + c
let none = let* x = Some 1 and* _ = None in Some x

let show = function Some n -> string_of_int n | None -> "none"
let () = print_endline (show (sum3 (Some 1) (Some 2) (Some 3)));
  print_endline (show pair); print_endline (show none)
