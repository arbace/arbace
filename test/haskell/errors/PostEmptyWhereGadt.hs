module PostEmptyWhereGadt where
data T a where
  T :: { f :: Int } %1 -> T a
