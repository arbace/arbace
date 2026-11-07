module PostUnpackDataCon where

data T = {-# UNPACK #-} MkT Int

data U = V Int | {-# NOUNPACK #-} W
