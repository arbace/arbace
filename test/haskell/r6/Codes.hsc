{-# LANGUAGE CPP #-}
-- | An .hsc module (R6): C constants through hsc2hs. Its #{enum} makes more lines than it takes,
-- so that GHC's positions after it repeat lines of the .hsc.
module Codes (Errno (..), eNOENT, eINTR, eAGAIN, eEXIST, eBADF, intSize, describe) where

#include <errno.h>

import Foreign.C.Types (CInt)

newtype Errno = Errno CInt
  deriving (Eq, Show)

#{enum Errno, Errno
 , eNOENT = ENOENT
 , eINTR = EINTR
 , eAGAIN = EAGAIN
 , eEXIST = EEXIST
 , eBADF = EBADF
 }
-- | The size of a C int, in bytes.
intSize :: Int
intSize = #{size int}

describe :: Errno -> String
describe e
#if defined(linux_HOST_OS)
  | e == eNOENT = "no such file (linux)"
#else
  | e == eNOENT = "no such file"
#endif
  | otherwise = "an error"
