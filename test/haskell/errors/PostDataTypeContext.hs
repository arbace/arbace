module PostDataTypeContext where

data Eq a => T a = MkT a

newtype (Show a, Eq a) => N a = MkN a
