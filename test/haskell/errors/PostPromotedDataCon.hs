{-# LANGUAGE DataKinds #-}
module PostPromotedDataCon where

data T = 'MkT Int

data U = Int ':+ Bool
