{-# OPTIONS_GHC -fno-warn-unused-binds #-}
{-# LINE 1 "Alex.x" #-}
-- | A generated module (HR25), as alex writes one: its {-# LINE #-} pragmas name the lines of
-- its source, Alex.x, so that GHC's positions are Alex.x's.
module Alex (Tok (..), classify, table) where

data Tok = Digit | Space | Other
  deriving (Eq, Show)
{-# LINE 10 "Alex.hs" #-}
-- the generator's own code, at the module's lines
table :: [Int]
table = [0, 1, 2,
  3, 4]
{-# LINE 20 "Alex.x" #-}
classify :: Char -> Tok
classify c
  | c >= '0' && c <= '9' = Digit
  | c == ' ' = Space
  | otherwise = Other
