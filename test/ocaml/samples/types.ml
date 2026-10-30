(* Type declarations of every kind (SPEC §3, §7). *)
type t = int
type 'a l = Nil | Cons of 'a * 'a l
type r = { a : int; mutable b : string; c : 'a. 'a -> 'a }
type +'a cov = 'a list
type -'a contra = 'a -> unit
type !'a inj = 'a ref
type ('a, 'b) pair = 'a * 'b constraint 'a = int
type priv = private A | B
type pm = private int
type e = ..
type e += E1 of int | E2
type _ g = I : int -> int g | S : string -> string g | P : 'a g * 'b g -> ('a * 'b) g
type 'a ex = Ex : 'b * ('b -> 'a) -> 'a ex
type inl = C of { x : int; y : int } | D
type 'a obj = < m : int; n : 'a -> unit; .. >
type cls = #object_class
and object_class = < m : int >
type pv = [ `A | `B of int | `C of int & string ]
type pvo = [> `A | `B ]
type pvl = [< `A | `B > `A ]
type pvi = [ pv | `D ]
type lt = x:int * string
type fn = int -> ?opt:int -> lbl:string -> unit
type pk = (module Set.S with type elt = int)
type al = (int * int) as 'x
type rf = t -> t
type nonrec t2 = t
exception Exn of int
exception Exn2 = Exn
let _ = (fun (type a) (x : a) -> x)
