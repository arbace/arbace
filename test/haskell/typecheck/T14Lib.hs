module T14Lib (Shape(..), HasArea(..), Named(..), Queue, emptyQ, push, pop, totalArea, Label) where

type Label = String

data Shape = Square Double | Tri Double Double

class HasArea a where
  areaOf :: a -> Double
  scaled :: Double -> a -> Double
  scaled k x = k * areaOf x

instance HasArea Shape where
  areaOf (Square s) = s * s
  areaOf (Tri b h) = b * h / 2

class HasArea a => Named a where
  label :: a -> Label

instance Named Shape where
  label (Square _) = "square"
  label (Tri _ _) = "triangle"

newtype Queue a = Queue ([a], [a])

emptyQ = Queue ([], [])

push x (Queue (f, b)) = Queue (f, x : b)

pop (Queue ([], [])) = Nothing
pop (Queue ([], b)) = pop (Queue (reverse b, []))
pop (Queue (x : f, b)) = Just (x, Queue (f, b))

totalArea :: HasArea a => [a] -> Double
totalArea = sum . map areaOf
