type 'a infos = { v : 'a; n : int }
class virtual ['acc] fold = object (self)
  method virtual int : int -> 'acc -> 'acc
  method infos : 'a. ('a -> 'acc -> 'acc) -> 'a infos -> 'acc -> 'acc = fun f x acc -> f x.v (self#int x.n acc)
end
