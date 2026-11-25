module Make (C : sig type 'a t end) = struct
  type ('a, 'b) pp = P : ('a * 'b) list * 'b C.t -> ('a, int) pp
  let mk l c = P (l, c)
end
module M = Make (struct type 'a t = arg:int -> 'a end)
let run (p : (string, int) M.pp) = match p with M.P (l, f) -> List.length l + (match l with (s, b) :: _ -> String.length s + (if f ~arg:1 = b then 1 else 0) | [] -> 0)
let () = print_int (run (M.mk [("ab", 3)] (fun ~arg -> arg + 2))); print_newline ()
