type 'a mig = M : 'a -> 'a mig
module type V = sig type a type types = < a : a > val info : types mig end
type 't ov = (module V with type a = 't)
let im (type a) ((module X) : a ov) : < a : a > mig = X.info
module A1 = struct type a = int type types = < a : a > let info = M (object method a = 1 end) end
let () = let (M o) = im (module A1) in print_int o#a; print_newline ()
