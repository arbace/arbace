(* Objects and classes (SPEC §6). *)
class point (x0 : int) =
  object (self)
    val mutable x = x0
    val y = 0
    val virtual z : int
    method get_x = x
    method private secret = y
    method virtual v : int
    method move d = x <- x + d; self
    method poly : 'a. 'a -> 'a = fun a -> a
    initializer print_int x
    constraint 'a = int
  end
class virtual ['a] v = object method virtual m : 'a end
class sub x = object inherit point x as super method! get_x = super#get_x + 1 end
class type ct = object method m : int end
class c2 = [int] v
class c3 = fun ~l ?(o = 1) (p : int) -> object end
class c4 = let z = 1 in object val q = z end
class c5 : ct = object method m = 1 end
class c6 = object end
and c7 = object end
let o = object method m = 1 end
let o2 = new point 1
let n = o2#get_x
let cl = let p = {< >} in p
let ov = object val x = 1 method copy = {< x = 2 >} end
let imm = let open List in object end
