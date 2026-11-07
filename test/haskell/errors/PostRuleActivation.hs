module PostRuleActivation where

{-# RULES "r1" [+ 1] forall x. foo x = x #-}
{-# RULES "r2" [~ 1] forall x. foo x = x #-}
{-# INLINE [- 2] foo #-}
foo x = x
