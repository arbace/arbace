{-# LANGUAGE CPP #-}
{-# LINE 1 "AlexCpp.x" #-}
-- | Alex.hs through CPP (HR25): CPP's line markers name the module's file, its {-# LINE #-}
-- pragmas AlexCpp.x.
module AlexCpp (Tok (..), classify, table) where

data Tok = Digit | Space | Other
  deriving (Eq, Show)
{-# LINE 10 "AlexCpp.hs" #-}
-- the generator's own code, at the module's lines
table :: [Int]
table = [0, 1, 2,
  3, 4]
{-# LINE 20 "AlexCpp.x" #-}
classify :: Char -> Tok
classify c
  | c >= '0' && c <= '9' = Digit
  | c == ' ' = Space
  | otherwise = Other
