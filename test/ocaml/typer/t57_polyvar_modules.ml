(* polymorphic variants with modules and records: signatures, recursive rows, open recursion, refs of weak rows *)
module Shape : sig
  type t = [ `Circle of float | `Square of float ]
  val area : [< t ] -> float
  val unit_square : t
end = struct
  type t = [ `Circle of float | `Square of float ]
  let area = function `Circle r -> 3. *. r *. r | `Square s -> s *. s
  let unit_square = `Square 1.
end

type 'a expr = [ `Num of int | `Add of 'a * 'a ]

let eval_expr eval : 'a expr -> int = function
  | `Num n -> n
  | `Add (a, b) -> eval a + eval b

let rec eval e = eval_expr eval e

type labelled = { tag : [ `On | `Off ]; count : int }
let toggle r = { r with tag = (match r.tag with `On -> `Off | `Off -> `On) }

let last = ref `Start
let set v = last := v

let rec to_list = function `Nil -> [] | `Cons (x, r) -> x :: to_list r

let () =
  Printf.printf "%.1f %.1f\n" (Shape.area (`Circle 1.)) (Shape.area Shape.unit_square);
  print_int (eval (`Add (`Num 1, `Add (`Num 2, `Num 3)))); print_newline ();
  let r = toggle { tag = `On; count = 1 } in
  print_endline (match r.tag with `On -> "on" | `Off -> "off");
  set `Stop;
  print_endline (match !last with `Start -> "start" | `Stop -> "stop");
  print_int (List.length (to_list (`Cons (1, `Cons (2, `Nil))))); print_newline ()
