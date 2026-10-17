-- | A module whose text is another file's, a CPP inclusion (filepath's System.FilePath.Posix,
-- doc/haskell/HASKELL-NOTES.md, "R5")
module Incl (greet, after) where

greet :: String
greet = GREETING
