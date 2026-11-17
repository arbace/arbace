{-# LANGUAGE TemplateHaskell #-}
module PostDeclSplice where

class C a where
  m :: a
  $(foo)

instance C Int where
  m = 1
  $(bar 'x)

f = 1
  where
    $(baz)
    y = 2
