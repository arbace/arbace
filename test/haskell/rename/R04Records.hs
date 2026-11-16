{-# LANGUAGE RecordWildCards, NamedFieldPuns #-}
module R04Records (Person(..), Shape(Circle, radius), older, mk, name', area, unwrap, W(..)) where

data Person = Person { name :: String, age :: Int } deriving (Show, Eq)

data Shape = Circle { radius :: Double } | Rect { w, h :: Double }

newtype W = W { unW :: Int }

older :: Person -> Person
older p = p { age = age p + 1 }

mk :: String -> Int -> Person
mk name age = Person { name, age }

name' :: Person -> String
name' Person { name = n } = n

area :: Shape -> Double
area Circle { radius } = 3 * radius * radius
area Rect { .. } = w * h

unwrap :: W -> Int
unwrap W { unW = x } = x
