module T04Local where

f x = g x + h x
  where g y = y * 2
        h y = y + 1

evens [] = []
evens (x : xs) = x : odds xs
odds [] = []
odds (_ : xs) = evens xs

useLocal xs = let sq y = y * y in map sq xs

pairShow x = let s = show x in (s, s)

localPoly = let i z = z in (i 'c', i True)

monoLocal x = let n = 10 in x + n
