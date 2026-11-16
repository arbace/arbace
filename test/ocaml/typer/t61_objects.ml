(* immediate objects at the top: instance variables, methods, method calls on inferred object types, < .. > *)
let p = object
  val mutable x = 1
  method get = x
  method incr = x <- x + 1
end
let () = p#incr; print_int p#get; print_newline ()
let f o = o#get + 1
let id (o : < .. >) = Oo.id o
