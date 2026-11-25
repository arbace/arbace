module type S = sig
  type 'k key
  type 'k t
  module Make (A : sig type 'a t val return : 'a -> 'a t end) : sig
    val get : 'k t -> f:('k key -> 'v A.t) -> 'v A.t
  end
end
module type T = sig
  include S with type 'k key := 'k with type 'k t := 'k list
end
module M : T = struct
  module Make (A : sig type 'a t val return : 'a -> 'a t end) = struct
    let get t ~f = f (List.hd t)
  end
end
module O = M.Make (struct type 'a t = 'a option let return x = Some x end)
let () = match O.get [41] ~f:(fun k -> Some (k + 1)) with Some n -> print_int n; print_newline () | None -> ()
