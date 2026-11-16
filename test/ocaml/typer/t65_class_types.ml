(* parameterized classes, #-types, class types and class constraints, a self-returning object *)
class ['a] cell (x : 'a) = object
  val mutable v = x
  method get = v
  method set y = v <- y
end
let c = new cell 3
let () = c#set 4; print_int c#get; print_newline ()
let get_all (l : int #cell list) = List.map (fun o -> o#get) l
class type printable = object method print : unit end
class pt (n : int) : printable = object method print = print_int n end
let () = (new pt 7)#print; print_newline ()
let o = object (self) method me = self method v = 5 end
let () = print_int o#me#v; print_newline ()
