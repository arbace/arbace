module R20Infix where

data E = Int :+: Int | E :*: E | L Int
infixl 6 :+:
infixl 7 :*:

data Lst a = Nil | a :> (Lst a)
infixr 5 :>

f :: E -> Int
f (a :+: b) = a + b
f (x :*: y) = f x * f y
f (L n) = n

g :: Lst Int -> Int
g (x :> y :> _) = x + y
g (x :> Nil) = x
g Nil = 0

h :: E
h = L 1 :*: L 2 :*: (3 :+: 4) :*: L 5

pat :: [Int] -> Int
pat (a : b : _ : _) = a
pat (a : _) = a
