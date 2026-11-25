module A = struct
  module type S = sig type t val hash : t -> int end
  type 'a t = (module S with type t = 'a)
end
module B = struct
  module type Key = sig type t val hash : t -> int end
  let of_key (type a) (module K : Key with type t = a) = K.hash
end
module type T = sig val create : 'a A.t -> 'a -> int end
module M : T = struct let create m x = B.of_key m x end
let () = print_int (M.create (module struct type t = int let hash x = x + 1 end) 41); print_newline ()
