{-# LANGUAGE UnicodeSyntax, MagicHash #-}
{-# OPTIONS_GHC -Wno-tabs #-}
-- | Mode :full's cases (doc/haskell/HASKELL-NOTES.md, "R3"): white space the printer would
-- not put (tabs, a CRLF, trailing blanks, form feeds), explicit braces and semicolons,
-- spellings (literals, pragmas, Unicode syntax), comments everywhere, no final newline.
module Space ( f , g,
  T (..) , (∘)	) where

import qualified Data.List as L   

infixr 9 ∘
(∘) ∷ (b → c) → (a → b) → a → c
(f' ∘ g') x = f' (g' x)  -- 😀 an emoji in a comment

data T = A | B {- 😀 -} deriving (Eq, Show)

f :: Int -> Int
f x = let { y = x + 0x1F ; z = 0o17 } in y * z	-- tab before

g ∷ ∀ a . [a] → Int
g xs = case xs of { [] -> 0 ; (_ : r) -> 1 + g r ; }

h :: String
h = "a\SOH\x41\&1\
     \gap"
{-# INLINEABLE h #-}

k :: Char
k = '\x10FFFF'
   where {
	unused = 1 ;; other = 2
   }
-- the end, without a newline