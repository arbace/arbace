{-# LANGUAGE NoRebindableSyntax #-}
{-# OPTIONS_GHC -Wno-missing-import-lists #-}
{-# OPTIONS_GHC -w #-}
module Paths_Cabal (
    version,
    getBinDir, getLibDir, getDynLibDir, getDataDir, getLibexecDir,
    getDataFileName, getSysconfDir
  ) where

import qualified Control.Exception as Exception
import Data.Version (Version(..))
import System.Environment (getEnv)
import Prelude

catchIO :: IO a -> (Exception.IOException -> IO a) -> IO a
catchIO = Exception.catch

version :: Version
version = Version [3,16,0,0] []

getDataFileName :: FilePath -> IO FilePath
getDataFileName name = do
  dir <- getDataDir
  return (dir `joinFileName` name)

getBinDir, getLibDir, getDynLibDir, getDataDir, getLibexecDir, getSysconfDir :: IO FilePath

bindir, libdir, dynlibdir, datadir, libexecdir, sysconfdir :: FilePath
bindir     = "/usr/lib/ghc-9.14.1/bin"
libdir     = "/usr/lib/ghc-9.14.1/lib/x86_64-linux-ghc-9.14.1-07a2/Cabal-3.16.0.0-68e2"
dynlibdir  = "/usr/lib/ghc-9.14.1/lib/x86_64-linux-ghc-9.14.1-07a2"
datadir    = "/usr/lib/ghc-9.14.1/share/x86_64-linux-ghc-9.14.1-07a2/Cabal-3.16.0.0"
libexecdir = "/usr/lib/ghc-9.14.1/libexec/x86_64-linux-ghc-9.14.1-07a2/Cabal-3.16.0.0"
sysconfdir = "/usr/lib/ghc-9.14.1/etc"

getBinDir     = catchIO (getEnv "Cabal_bindir")     (\_ -> return bindir)
getLibDir     = catchIO (getEnv "Cabal_libdir")     (\_ -> return libdir)
getDynLibDir  = catchIO (getEnv "Cabal_dynlibdir")  (\_ -> return dynlibdir)
getDataDir    = catchIO (getEnv "Cabal_datadir")    (\_ -> return datadir)
getLibexecDir = catchIO (getEnv "Cabal_libexecdir") (\_ -> return libexecdir)
getSysconfDir = catchIO (getEnv "Cabal_sysconfdir") (\_ -> return sysconfdir)

joinFileName :: String -> String -> FilePath
joinFileName ""  fname = fname
joinFileName "." fname = fname
joinFileName dir ""    = dir
joinFileName dir fname
  | isPathSeparator (last dir) = dir ++ fname
  | otherwise                  = dir ++ pathSeparator : fname

pathSeparator :: Char
pathSeparator = '/'

isPathSeparator :: Char -> Bool
isPathSeparator c = c == '/'
