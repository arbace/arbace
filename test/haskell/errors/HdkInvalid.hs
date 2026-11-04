module HdkInvalid where

-- | first

-- | second
f :: Int
f = 1

g :: Int -> Int
g x = x -- ^ trailing after expression

h = (1 :: Int) -- ^ misplaced

-- ^ nothing before
