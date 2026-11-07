(* self types ('self, object ('a)), functional update, coercions to object types and to a parent class *)
class point x = object (self : 'self)
  val x = x
  method x = x
  method moved d : 'self = {< x = x + d >}
  method eq (p : point) = p#x = x
end
let p = new point 1
let q = p#moved 2
let () = print_int q#x; print_string (string_of_bool (p#eq q)); print_newline ()
let f (o : < x : int; .. >) = o#x
let g = f (p :> < x : int >)
class colored x (c : string) = object
  inherit point x
  method color = c
end
let cp = new colored 3 "red"
let h = (cp :> point)#x
let k (o : #point) = o#x + 1
let () = print_int (k cp + h + g); print_newline ()
