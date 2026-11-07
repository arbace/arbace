(* polymorphic variants with or-patterns binding, guards, comparisons and hashing, private rows, functors, optional
   arguments' defaults, lazy values, arrays, tuples and string arguments *)
let f = function (`A x | `B x) -> x | `C -> 0
let g x = match x with `A (a, b) when a > b -> a | `A (_, b) -> b | `B r -> r.contents
let h l = List.sort compare l
let tbl : ([ `Key ], int) Hashtbl.t = Hashtbl.create 3
let () = Hashtbl.add tbl `Key 1
let find k = Hashtbl.find tbl k
module type T = sig type t = private [> `A ] val x : t end
module P (X : sig val v : [ `A | `B ] end) = struct let w = match X.v with `A -> 1 | `B -> 2 end
module Q = P (struct let v = `B end)
let rec walk acc = function [] -> acc | `Inc :: r -> walk (acc + 1) r | `Dec :: r -> walk (acc - 1) r
let annotated : ([> `A ] as 'r) -> 'r = fun x -> x
let pr = function `I i -> Printf.sprintf "%d" i | `S s -> s | `L l -> String.concat "," l
let opt_arg ?(mode = `Fast) x = match mode with `Fast -> x | `Slow -> x * 2
let k = opt_arg 3 + opt_arg ~mode:`Slow 3
let lazy_v = lazy (`Computed 3)
let force_it () = match Lazy.force lazy_v with `Computed n -> n
let arr = [| `X; `Y |]
let arr_get i = match arr.(i) with `X -> 'x' | `Y -> 'y'
let tuple_match = function (`A, 1) -> "a1" | (`A, _) -> "a" | (`B, _) -> "b"
let str_tag = function `S "x" -> 1 | `S _ -> 2 | `T -> 3
let () = Printf.printf "%d %d %d %d %d %c %s %d %s\n" (f (`B 2)) (g (`A (3, 1))) (find `Key) Q.w (walk 0 [`Inc; `Inc; `Dec]) (arr_get 1) (tuple_match (`A, 2)) (k + force_it () + str_tag (`S "x")) (pr (`L ["a"; "b"]))
