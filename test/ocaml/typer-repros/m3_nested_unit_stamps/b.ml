module T = struct type ('a, 'acc) fold = 'a -> 'acc -> 'acc end
class ['acc] fold = object method attr : (int, 'acc) T.fold = fun _ acc -> acc end
