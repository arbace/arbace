module T = struct type ('a, 'res) lift = 'a -> 'res end
class type ['res] std_lifters = object
  method int : (int, 'res) T.lift
  method other : string
end
