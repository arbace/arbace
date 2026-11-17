(* class declarations, parameters, new, self calls *)
class c = object method m = 1 end
let x = (new c)#m
class point (x0 : int) = object (self)
  val mutable x = x0
  method get = x
  method move d = x <- x + d
  method print = print_int self#get
end
let p = new point 3
let () = p#move 2; p#print; print_newline ()
