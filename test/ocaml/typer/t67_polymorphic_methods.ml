(* polymorphic methods ('a. t) in classes, object types and immediate objects *)
class idc = object
  method id : 'a. 'a -> 'a = fun x -> x
  method pair : 'a 'b. 'a -> 'b -> 'a * 'b = fun a b -> (a, b)
end
let o = new idc
let () = print_int (o#id 3); print_string (o#id "x"); print_int (fst (o#pair 1 "y")); print_newline ()
let apply (f : < id : 'a. 'a -> 'a >) = (f#id 1, f#id true)
let r = apply (o :> < id : 'a. 'a -> 'a >)
let q = object method m : 'a. 'a list -> int = List.length end
let n = q#m [1;2] + q#m ["a"]
