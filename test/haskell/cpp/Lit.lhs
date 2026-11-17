A literate module through CPP: unlit first, then the preprocessor.

> {-# LANGUAGE CPP #-}
> module Lit where

#if MIN_VERSION_base(4,0,0)

> x :: Int
> x = __GLASGOW_HASKELL__

#else

> x = 0

#endif

\begin{code}
  y :: Int
  y = WORDS
#define WORDS 2
  z = WORDS
\end{code}
