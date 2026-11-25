module T = struct type 'a map = 'a -> 'a end
class map = object method int : int T.map = fun x -> x + 1 end
