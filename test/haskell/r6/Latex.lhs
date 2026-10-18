\documentclass{article}
\begin{document}
A literate module in \LaTeX{} style (R6).

\begin{code}
module Latex where

import Data.List (sortOn)

ordered :: Ord b => (a -> b) -> [a] -> [a]
ordered = sortOn
\end{code}

More prose between the code blocks.

\begin{code}
pairs :: [a] -> [(a, a)]
pairs xs = zip xs (drop 1 xs)
\end{code}
\end{document}
