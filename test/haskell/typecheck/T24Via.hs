{-# LANGUAGE DerivingVia, GeneralizedNewtypeDeriving, DerivingStrategies, StandaloneDeriving, DeriveFunctor #-}
module T24Via where

newtype Wrap a = Wrap a

instance Show a => Show (Wrap a) where
  showsPrec d (Wrap a) = showParen (d > 10) (showString "Wrap " . showsPrec 11 a)

newtype Sum' a = Sum' a

instance Num a => Semigroup (Sum' a) where
  Sum' a <> Sum' b = Sum' (a + b)

instance Num a => Monoid (Sum' a) where
  mempty = Sum' 0

newtype App f a = App (f a)

instance (Applicative f, Semigroup a) => Semigroup (App f a) where
  App a <> App b = App ((<>) <$> a <*> b)

instance (Applicative f, Monoid a) => Monoid (App f a) where
  mempty = App (pure mempty)

class Pretty a where
  pretty :: a -> String

instance Pretty Int where
  pretty n = "#" ++ show n

newtype Score = Score Int
  deriving (Semigroup, Monoid) via (Sum' Int)
  deriving Show via (Wrap Int)

newtype Age = Age Int
  deriving stock Eq
  deriving newtype Ord
  deriving (Pretty) via Int

newtype Lifted f a = Lifted (f a)
  deriving (Semigroup, Monoid) via (App f a)

newtype MaybeSum = MaybeSum (Maybe Int)
  deriving (Semigroup, Monoid) via (App Maybe (Sum' Int))

newtype L a = L [a]
  deriving Functor via []

newtype Counter = Counter Int
  deriving newtype (Eq, Show)
  deriving (Semigroup, Monoid) via (Sum' Int)

-- standalone deriving via
newtype Money = Money Int
newtype Total = Total Int
newtype Opaque a = Opaque a

deriving via (Wrap Int) instance Show Money
deriving via (Sum' Int) instance Semigroup Total
deriving via (Sum' Int) instance Monoid Total
deriving via (Wrap a) instance Show a => Show (Opaque a)

combine :: [Score] -> Score
combine = mconcat
