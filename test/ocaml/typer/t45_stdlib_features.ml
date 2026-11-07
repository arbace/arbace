(* what the stdlib's units need: inline records, a re-exported variant, lazy values, [@tail_mod_cons], aliases *)
type 'a tree = Leaf | Node of { left : 'a tree; value : 'a; right : 'a tree; mutable size : int }

let rec insert x = function
  | Leaf -> Node { left = Leaf; value = x; right = Leaf; size = 1 }
  | Node ({ value; left; right; _ } as n) ->
      n.size <- n.size + 1;
      if x < value then Node { n with left = insert x left }
      else Node { n with right = insert x right }

let rec to_list = function
  | Leaf -> []
  | Node { left; value; right; _ } -> to_list left @ (value :: to_list right)

type 'a opt = 'a option = None | Some of 'a

let equal eq o0 o1 = match o0, o1 with
  | Some v0, Some v1 -> eq v0 v1
  | None, None -> true
  | _ -> false

let rec map f = function [] -> [] | x :: r -> f x :: map f r [@@tail_mod_cons]
let[@tail_mod_cons] rec append l1 l2 = match l1 with [] -> l2 | x :: r -> x :: append r l2

let expensive = lazy (print_string "forced "; 42)
let cheap = lazy 7

let map_left f = function Either.Left v -> Either.Left (f v) | Either.Right _ as e -> e

let () =
  let t = List.fold_left (fun t x -> insert x t) Leaf [5; 3; 8; 1] in
  List.iter print_int (to_list t); print_newline ();
  print_string (if equal ( = ) (Some 1) (Some 1) then "eq" else "ne"); print_newline ();
  List.iter print_int (append (map succ [1; 2]) [9]); print_newline ();
  print_int (Lazy.force expensive + Lazy.force expensive + Lazy.force cheap); print_newline ();
  (match map_left succ (Either.Left 1) with Either.Left v -> print_int v | Either.Right () -> ());
  print_newline ()
