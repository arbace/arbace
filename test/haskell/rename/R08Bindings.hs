module R08Bindings where

ev :: Int -> Bool
ev 0 = True
ev n = od (n - 1)

od :: Int -> Bool
od 0 = False
od n = ev (n - 1)

top :: Int
top = let a = 1
          b = a + c
          c = 2
          d = d + 1
      in a + b + c + d

w1 :: Int -> Int
w1 x = y + z
  where
    y = x * 2
    z = y + k
    k = 1

pairs :: [(Int, Int)]
pairs = [(i, j) | i <- [1 .. 3], let k = i * i, j <- [k .. 5], odd (i + j)]

(p, q) = (1 :: Int, 'c')
[r1, r2] = [1, 2 :: Int]

ap :: Maybe Int -> Int
ap m@(Just n) | n > 0, let k = n + 1 = k
              | Nothing <- m = 0
ap ~(Just _) = 1

lazyPat :: (Int, Int) -> Int
lazyPat ~(a, b) = a

bang :: Int -> Int
bang !x = x

fib :: Int -> Integer
fib = (fs !!) where fs = map f [0 ..]
                    f 0 = 0
                    f 1 = 1
                    f n = fs !! (n - 1) + fs !! (n - 2)
