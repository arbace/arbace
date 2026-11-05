(* classes defined by applying or re-exporting another (Translclass.transl_class_rebind), printed as the class *)
class c x = object method v : int = x end
class d = c 3
class e y = c (y + 1)
class f = c
let () = print_int ((new d)#v + (new e 1)#v + (new f 5)#v); print_newline ()
