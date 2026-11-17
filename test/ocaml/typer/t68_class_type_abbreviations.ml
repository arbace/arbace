(* a parameterized class type, its abbreviation by another class type, both used as types *)
class type ['a] storage = object ('self)
  method get : int -> 'a
  method concat : 'a storage -> 'self
end
class type ustorage = [char] storage
let f (s : ustorage) (t : char storage) = s#concat t
let g (s : char storage) = (s : ustorage)
class alfa = object method x = 1 end
let f (o : #alfa) = o
class bravo (a : #alfa) = object val y = a end
let () = print_endline "ok"
