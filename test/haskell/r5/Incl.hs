{-# LANGUAGE CPP #-}
#define GREETING "hello"
#include "Incl/Body.hs"

-- back in this file after the inclusion
after :: Int
after = 1
