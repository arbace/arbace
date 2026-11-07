module PostInvalidInfixDataCon where

data T = Int :+ Bool :* Char

data U = !Int :+ Bool :* Char
