{-# LANGUAGE MultiWayIf, LambdaCase, EmptyCase #-}
-- | The layout layout's cases (doc/haskell/HASKELL-NOTES.md, "R4"): blocks the layout rule
-- closes by a line, by a token GHC's parse-error(t) rule takes, or not at all (explicit braces).
module Layout (Void, absurd, f, f2, g, h, k, m, n, p, q) where

data Void

absurd :: Void -> a
absurd v = case v of {}


f :: Int -> Int
f x = case x of { 0 -> 1; _ -> 2 } + 3


f2 :: Int -> Int
f2 x = case x of { 0 -> case x of { _ -> 1 }; _ -> do { 2 } } * 2


g :: Int -> Int
g x = case x of { 0 -> y; _ -> x } where y = 4


h :: Int -> [Int]
h x = [case x of 0 -> 1; _ -> 2, (do x) + 1, if x > 0 then do x else do 0, let y = x in y]


k :: Int -> Int
k x = case x of
  {
    0 -> 1;
    _ -> 2 }



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


p :: Int -> Int
p = \case
    0 -> 1; 1 -> 2
    z | z > 9 -> case z of 10 -> 0; _ -> 1
      | otherwise -> z


q :: IO Int
q = do let { a = 1 }; return a
