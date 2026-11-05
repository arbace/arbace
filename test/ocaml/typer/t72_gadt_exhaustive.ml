(* GADT-aware exhaustiveness: matches total only because the impossible counter-examples do not type
   (tuples of GADTs related by their indices, nested GADT patterns): no Match_failure handler *)
type _ ty = Int : int ty | Bool : bool ty | Pair : 'a ty * 'b ty -> ('a * 'b) ty

let rec eq : type a. a ty -> a ty -> a -> a -> bool = fun t1 t2 x y ->
  match t1, t2 with
  | Int, Int -> x = y
  | Bool, Bool -> x = y
  | Pair (a1, b1), Pair (a2, b2) -> eq a1 a2 (fst x) (fst y) && eq b1 b2 (snd x) (snd y)

let first : type a b. (a * b) ty -> a ty = function Pair (a, _) -> a

type (_, _) rel = RInt : (int, int) rel | RBool : (bool, bool) rel

let both : type a b. (a, b) rel -> (b, a) rel -> int = fun r s ->
  match r, s with
  | RInt, RInt -> 1
  | RBool, RBool -> 2

let inner : type a. a ty option -> a -> int = fun o v ->
  match o with
  | None -> 0
  | Some Int -> v
  | Some Bool -> if v then 1 else 0
  | Some (Pair _) -> 2

let () =
  print_endline (string_of_bool (eq (Pair (Int, Bool)) (Pair (Int, Bool)) (1, true) (1, true)));
  print_endline (string_of_bool (eq Int Int 1 2));
  (match first (Pair (Bool, Int)) with Bool -> print_endline "bool");
  print_int (both RInt RInt + both RBool RBool); print_newline ();
  print_int (inner (Some Int) 5 + inner (Some Bool) true + inner None 3); print_newline ()
