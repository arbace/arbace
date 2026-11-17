module R21Wired where

u :: ()
u = ()

t2 :: (Int, Bool)
t2 = (1, True)

t3 :: (Int, Int, Int) -> Int
t3 (a, b, c) = a + b + c

l1 :: [Int]
l1 = []

l2 :: [Int]
l2 = 1 : 2 : []

f :: (a -> b) -> [a] -> [b]
f g = map g

c1 :: (,) Int Bool
c1 = (,) 1 True

c2 :: Maybe [()]
c2 = Just [()]

h :: [] Int
h = [] 

fn :: (->) Int Int
fn = (+ 1)

e :: Either String Int
e = Right 1
