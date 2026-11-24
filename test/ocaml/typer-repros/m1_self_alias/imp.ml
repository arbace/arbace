module Sel = struct module Ast = struct module P = struct type t = int let v = 41 end end end
module P = Sel.Ast.P
let x : P.t = P.v + 1
