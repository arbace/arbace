(* virtual classes and methods, inherit, super, coercions to a class type, private methods, initializers *)
class virtual shape name = object (self)
  method virtual area : float
  method name : string = name
  method describe = self#name ^ ": " ^ string_of_float self#area
end
class square side = object
  inherit shape "square"
  method area = side *. side
end
class circle r = object
  inherit shape "circle" as super
  method area = 3.0 *. r *. r
  method! describe = "round " ^ super#describe
end
let shapes = [ (new square 2.0 :> shape); (new circle 1.0 :> shape) ]
let () = List.iter (fun s -> print_endline s#describe) shapes
class counter = object
  val mutable n = 0
  method private step = n <- n + 1
  method tick = n
  initializer n <- 10
end

class counter2 = object (self)
  inherit counter
  method tock = self#step; n
end
let c = new counter2
let () = print_int c#tock; print_int c#tick; print_newline ()
