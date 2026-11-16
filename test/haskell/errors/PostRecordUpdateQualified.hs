{-# LANGUAGE OverloadedRecordDot, OverloadedRecordUpdate, RebindableSyntax #-}
module PostRecordUpdateQualified where

x = r { M.field = 1 }
