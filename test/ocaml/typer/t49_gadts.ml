(* GADTs: type equality, an evaluator with polymorphic recursion, existentials, locally abstract types *)
type (_, _) eq = Equal : ('a, 'a) eq
type _ value = Int : int -> int value | Bool : bool -> bool value | Pair : 'a value * 'b value -> ('a * 'b) value
type any = Any : 'a value -> any

let cast : type a b. (a, b) eq -> a -> b = fun Equal x -> x

let rec eval : type a. a value -> a = function
  | Int n -> n
  | Bool b -> b
  | Pair (x, y) -> (eval x, eval y)

let rec show : type a. a value -> string = fun v ->
  match v with
  | Int n -> string_of_int n
  | Bool b -> string_of_bool b
  | Pair (x, y) -> "(" ^ show x ^ ", " ^ show y ^ ")"

let count (Any v) = String.length (show v)

let () =
  print_int (cast Equal 3); print_newline ();
  let (n, b) = eval (Pair (Int 1, Bool true)) in
  print_int n; print_string (string_of_bool b); print_newline ();
  print_string (show (Pair (Int 1, Pair (Bool false, Int 2)))); print_newline ();
  print_int (count (Any (Pair (Int 10, Int 20)))); print_newline ()
