{-# LANGUAGE EmptyDataDeriving #-}
module T20Derive3 where

import Data.Ix

data T a b = T a [b] (Maybe a) deriving (Eq, Ord, Show, Read)

data N = N [Maybe Int] Char Double deriving (Eq, Ord, Show, Read)

-- many constructors: Ord by tags, Show and Read with and without fields
data X = A Int | B | C | D | E Int Bool | F | G deriving (Eq, Ord, Show, Read)

infixl 6 :*:
infix 4 `Foo`
data Op2 = Int :*: Int | Int `Foo` Int | Neg Op2 deriving (Eq, Ord, Show, Read)

data V3 = V3 Int Int Int deriving (Eq, Ord, Show, Read, Bounded, Ix)

data Day = Mon | Tue | Wed | Thu | Fri | Sat | Sun deriving (Eq, Ord, Show, Read, Enum, Bounded, Ix)

data Rec a = Rec { first :: a, second :: Int, (%%) :: Bool } | Other { first :: a } deriving (Eq, Ord, Show, Read)

data Void2 deriving (Eq, Ord, Show)

data Unit = Unit deriving (Eq, Ord, Show, Read, Enum, Bounded, Ix)

data Strict = Strict !Int !Int deriving (Eq, Ord, Show)

newtype Id a = Id { runId :: a } deriving (Eq, Ord, Show, Read)

newtype Cnt = Cnt Int deriving (Eq, Ord, Show, Read, Bounded, Ix)

weekend :: Day -> Bool
weekend d = d >= Sat

week :: [Day]
week = [minBound .. maxBound]

cmp :: X -> X -> Ordering
cmp = compare

parseX :: String -> X
parseX = read

inDay :: Day -> Bool
inDay = inRange (Mon, Fri)
