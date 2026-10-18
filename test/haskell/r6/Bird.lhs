#!/usr/bin/env runghc

A literate module in bird style (R6): this prose is a comment, the
lines that begin with > are the program.

> {-# LANGUAGE LambdaCase #-}
> module Bird (twice, describe) where

A function, documented:

> -- | Applies f twice.
> twice :: (a -> a) -> a -> a
> twice f = f . f

Prose with a	tab, and a line ending in spaces:   

> describe :: Int -> String
> describe = \case
>   0 -> "zero"
>   _ -> "many"
