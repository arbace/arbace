module P = struct type 'a t = 'a end
type ('k, 'v) m = ('k * 'v) list
module M : sig val empty : ('k, 'v) m end = struct
  let empty : ('a P.t, 'b) m = []
end
let () = print_int (List.length (M.empty : (int, int) m)); print_newline ()
