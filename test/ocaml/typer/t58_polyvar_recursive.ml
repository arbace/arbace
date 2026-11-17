(* recursive variant types through abbreviations (open recursion, as the mixin test does), declarations with a
   constraint, comparisons with constant tags, variants in exceptions *)
type var = [ `Var of string ]
type 'a lambda = [ `Var of string | `Abs of string * 'a | `App of 'a * 'a ]
type 'a closed = 'a constraint 'a = [< `A of int | `B ]

let free_lambda ~free_rec : _ lambda -> _ = function
  | #var as x -> (match x with `Var s -> [s])
  | `Abs (s, t) -> List.filter (fun v -> v <> s) (free_rec t)
  | `App (t1, t2) -> free_rec t1 @ free_rec t2

let rec free1 x = free_lambda ~free_rec:free1 x

type lexpr = [ `Var of string | `Abs of string * lexpr | `App of lexpr * lexpr | `Num of int ]

let rec free : lexpr -> string list = function
  | #lambda as x -> free_lambda ~free_rec:free x
  | `Num _ -> []

let rec map f = function `Nil -> `Nil | `Cons (x, l) -> `Cons (f x, map f l)
let rec length = function `Nil -> 0 | `Cons (_, l) -> 1 + length l

let only_a (x : [ `A of int ] closed) = match x with `A n -> n

let is_a x = x = `A
let not_b x = x <> `B

exception Tagged of [ `Code of int | `Unknown ]
let code () = try raise (Tagged (`Code 7)) with Tagged (`Code n) -> n | Tagged `Unknown -> 0

let () =
  print_endline (String.concat "," (free1 (`Abs ("x", `App (`Var "x", `Var "y")))));
  print_endline (String.concat "," (free (`App (`Num 1, `Abs ("z", `Var "w")))));
  print_int (length (map succ (`Cons (1, `Cons (2, `Nil))))); print_newline ();
  print_int (only_a (`A 5)); print_newline ();
  Printf.printf "%b %b %b\n" (is_a `A) (is_a `C) (not_b `B);
  print_int (code ()); print_newline ()
