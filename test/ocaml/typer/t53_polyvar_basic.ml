(* polymorphic variants: tags with and without arguments, open and closed rows inferred, aliases of open rows *)
let red = `Red
let rgb = `Rgb (1, 2, 3)
let colours = [`Red; `Green; `Rgb (0, 0, 0)]

let name = function
  | `Red -> "red"
  | `Green -> "green"
  | `Rgb (r, g, b) -> Printf.sprintf "rgb(%d,%d,%d)" r g b

let name_or_other = function
  | `Red -> "red"
  | _ -> "other"

let keep x = match x with `Red -> x | `Blue -> `Green | y -> y

let rec sum = function
  | `Nil -> 0
  | `Cons (x, rest) -> x + sum rest

let wrap x = `Some x
let unwrap = function `Some x -> x | `None -> 0

let pair = (`A, `B 1)
let apply_tag f = f `Tag

let () =
  List.iter (fun c -> print_endline (name c)) colours;
  print_endline (name_or_other `Blue);
  print_endline (name_or_other red);
  print_endline (name rgb);
  print_int (sum (`Cons (1, `Cons (2, `Cons (3, `Nil))))); print_newline ();
  print_int (unwrap (wrap 42)); print_newline ();
  print_int (unwrap `None); print_newline ();
  (match keep `Blue with `Green -> print_endline "green" | _ -> print_endline "?");
  (match pair with (`A, `B n) -> print_int n | _ -> ()); print_newline ();
  print_endline (apply_tag (function `Tag -> "tag" | `Other -> "other"))
