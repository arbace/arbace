(* Operators, indexing operators, user-defined binding operators. *)
let ( let+ ) = Option.map
let ( and+ ) a b = match a, b with Some x, Some y -> Some (x, y) | _ -> None
let ( .%() ) = Array.get
let ( .%()<- ) = Array.set
let ( .%[] ) = String.get
let ( .%{} ) = Bigarray.Array1.get
let a = [| 1 |]
let b = a.%(0)
let () = a.%(0) <- 2
let c = "x".%[0]
let d = let+ a = Some 1 and+ b = Some 2 in a + b
let e = 1 + 2 - 3 * 4 / 5 mod 6 land 7 lor 8 lxor 9 lsl 1 lsr 2 asr 3
let f = 1 :: 2 :: []
let g = not true && true || false
let h = a.(0) + (Array.get [|1|] 0) + !(ref 1)
let i = (:=) (ref 1) 2
let j = (::) (1, [])
let k = ( ** ) 2. 3.
let l = x#m
let m' = (!)
let n = 1 |> succ |> ( + ) 1
let o = (+)
let p = Stdlib.( + ) 1 2
let q = List.( @ ) [] []
let r = (-) 1 2
let s = - 1
let t = -. 1.
let u = a.(0).(1)
let v = s.[0]
let w = r.f.g
let x = 1; 2
