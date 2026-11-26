module T : sig type 'a t val none : _ t val some : 'a -> 'a t val value_unsafe : 'a t -> 'a end = struct
  type +'a t
  let none_substitute : _ t = Obj.obj (Obj.new_block Obj.abstract_tag 1)
  let none : _ t = Obj.magic `none
  let some (type a) (x : a) : a t = if Obj.repr x == Obj.repr none then none_substitute else Obj.magic x
  let value_unsafe (type a) (x : a t) : a = if x == none_substitute then Obj.magic none else Obj.magic x
end
let () = print_int (T.value_unsafe (T.some 42)); print_newline ()
