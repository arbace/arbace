{-# LANGUAGE MultiWayIf, LambdaCase, EmptyCase #-}
-- | The layout layout's cases (doc/haskell/HASKELL-NOTES.md, "R4"): blocks the layout rule
-- closes by a line, by a token GHC's parse-error(t) rule takes, or not at all (explicit braces).
module Layout (Void, absurd, f, f2, g, h, k, m, n, p, q) where

data Void

absurd :: Void -> a
absurd v = case v of {}

-- a block ended by a token on its last line that would continue it: braced
f :: Int -> Int
f x = case x of { 0 -> 1; _ -> 2 } + 3

-- two nested blocks ended so
f2 :: Int -> Int
f2 x = case x of { 0 -> case x of { _ -> 1 }; _ -> do { 2 } } * 2

-- a where after a case on the same line: braced
g :: Int -> Int
g x = case x of { 0 -> y; _ -> x } where y = 4

-- blocks closed by ) ] , then else of in
h :: Int -> [Int]
h x = [case x of 0 -> 1; _ -> 2, (do x) + 1, if x > 0 then do x else do 0, let y = x in y]

-- explicit braces on lines of their own
k :: Int -> Int
k x = case x of
  {
    0 -> 1
  ; _ -> 2
  }

-- a multi-way if, a let whose bindings begin a line, an operator after a block on the next line
m :: Int -> Maybe Int
m x =
  if | x > 0 -> Just x
     | otherwise -> Nothing
  `orElse` let
             z = x
           in Just z
  where
    orElse (Just a) _ = Just a
    orElse Nothing b = b

-- nested blocks, a do ending in a case before a where
n :: Int -> IO Int
n x = do
  y <- return x
  case y of
    0 -> return 1
    _ -> do
      print y
      return y
  where
    _unused = ()

-- \case, two items on one line, a guard after a block's item
p :: Int -> Int
p = \case
  0 -> 1; 1 -> 2
  z | z > 9 -> case z of 10 -> 0; _ -> 1
    | otherwise -> z

-- a let block followed by ; in a do on one line
q :: IO Int
q = do { let { a = 1 }; return a }
