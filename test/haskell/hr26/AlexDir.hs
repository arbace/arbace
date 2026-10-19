{-# LANGUAGE CPP #-}
{-# LINE 1 "AlexDir.x" #-}
-- | A generated module through CPP (HR26): its {-# LINE #-} pragma names AlexDir.x, then a CPP
-- directive (an #include) after which CPP's line marker names the module's own lines, as alex's
-- and hsc2hs's outputs keep them.
module AlexDir (Tok (..), classify, width) where

data Tok = Digit | Space | Other
  deriving (Eq, Show)
#include "AlexDir.h"
-- the generator's own code, at the module's lines after the directive
width :: Int
width = WIDTH

classify :: Char -> Tok
classify c
  | c >= '0' && c <= '9' = Digit
  | c == ' ' = Space
  | otherwise = Other
