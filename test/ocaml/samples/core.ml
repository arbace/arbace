(* Arbace's own sample: the core language (SPEC §3-§5). *)
let x = 1 + 2 * 3
let rec fact n = if n <= 1 then 1 else n * fact (n - 1)
and even n = n = 0 || odd (n - 1)
and odd n = n <> 0 && even (n - 1)
let f ?(x = 1) ?y ~z (a, b) = x + z + a + b + (match y with Some y -> y | None -> 0)
let g = function [] -> 0 | [ x ] -> x | x :: _ :: _ when x > 0 -> x | _ -> -1
let h (type a) (x : a) : a = x
let t = (1, "two", 3.0, 'c', 4l, 5L, 6n, 0x1F, 1_000)
let labelled = (~a:1, ~b:2)
let { contents = c } = ref 3
let arr = [| 1; 2; 3 |]
let () = arr.(0) <- 10; Array.iter print_int arr
let s = {id|a "raw" string|id} ^ "esc\n\t\"\\" ^ "\u{1F600}"
let lz = lazy (1 + 1)
let () = assert (Lazy.force lz = 2)
let r = { contents = 1 }
let r2 = { r with contents = 2 }
let () = r.contents <- 3
let v = [ `A; `B 1; `C (2, 3) ]
let w = function `A -> 0 | `B n | `C (n, _) -> n
let () = for i = 1 to 3 do print_int i done; for i = 3 downto 1 do print_int i done
let () = let i = ref 0 in while !i < 3 do incr i done
let c1 = (1 : int)
let c2 = (`A :> [> `A | `B ])
let ch = match 'x' with 'a' .. 'z' -> 1 | _ -> 0
let ex = try failwith "x" with Failure _ | Not_found -> ()
let tup (a, b) = a
let lst = [ 1; 2; 3 ] @ [ 4 ]
let opt = Some (Some 1)
let neg = -1 + (-.2.0 |> int_of_float)
let (+++) a b = a + b
let ( let* ) o f = match o with None -> None | Some x -> f x
let ( and* ) a b = match a, b with Some x, Some y -> Some (x, y) | _ -> None
let sum = let* a = Some 1 and* b = Some 2 in Some (a + b)
let local_open = List.(length [ 1 ])
let local_open2 = let open List in length [ 1 ]
let rec_val : int -> int = fun x -> x
let poly : 'a. 'a -> 'a = fun x -> x
let coerced = (fun x -> x : int -> int :> int -> int)
