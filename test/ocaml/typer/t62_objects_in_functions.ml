(* immediate objects under a function (cached classes, captured variables), self, private methods, {< >} *)
let fresh () =
  let r = ref None in
  let dummy = object val x = r end in
  r := Some dummy;
  dummy
let mk n = object (self)
  val mutable v = n
  method get = v
  method set x = v <- x; self#get
  method dup = {< v = v + 1 >}
  method private hidden = n + 1
  method h = self#hidden
end
let () = let o = mk 3 in print_int (o#set 5); print_int (o#dup#get); print_int o#h
