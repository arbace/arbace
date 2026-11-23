type 'a mig = M : 'a -> 'a mig
module type V = sig type a type types = < a : a > val info : types mig end
type 't ov = (module V with type a = 't)
let im (type a) ((module X) : a ov) = X.info
module A1 = struct type a = int type types = < a : a > let info = M (object method a = 1 end) end
module B1 = struct type a = string type types = < a : a > let info = M (object method a = "b" end) end
let () = let (M o) = im (module A1) in print_int o#a; let (M p) = im (module B1) in print_string p#a; print_newline ()
