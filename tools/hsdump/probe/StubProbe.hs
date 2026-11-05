-- HES60's probe (doc/haskell/HASKELL-EVAL-SURVEY.md, "As built: HE6's second half"): what GHC 9.14.1 holds of a module's
-- foreign exports and "wrapper" imports at the two points hsdump sees, so that the helper's extension is designed on facts:
-- at the end of the Core pipeline (a plugin pass, as hsdump's core mode) the ModGuts' foreign stubs (mg_foreign: the
-- _stub.h header and the stub C GHC's C compiler would compile, which name the Haskell helpers they apply) and the
-- bindings (each with whether it is exported, its type); at the stgToCmm hook (hsdump's stg mode) the STG bindings' names.
-- Usage: stubprobe LIBDIR OUT FILE.hs... (the files compiled as ghc --make compiles them, -O0, no link). Not part of the
-- helper (tools/hsdump/*.hs, the dumps' key): built by bin/hsi stubs-build.
--   stubprobe --stubs LIBDIR DIR GHC-FLAG... -- FILE.hs...: HE6's second half's use of it (HES60 (a), bin/hsi check): every
--   module's foreign stubs written as GHC writes them for its C compiler, DIR/BASE_stub.h and DIR/BASE_stub.c (BASE the
--   source file's base name; GHC's prologue: HsFFI.h for the header, IN_STG_CODE 0 and Rts.h for the C), for ci to compile
--   with the program's own C; nothing for a module without stubs.
module Main (main) where

import GHC
import GHC.Driver.Session
import GHC.Driver.Env (HscEnv(..))
import GHC.Driver.Hooks (Hooks(..))
import GHC.Driver.Plugins (Plugin(..), defaultPlugin, StaticPlugin(..), PluginWithArgs(..), Plugins(..), purePlugin)
import GHC.Core.Opt.Pipeline.Types (CoreToDo(..))
import GHC.Unit.Module.ModGuts (ModGuts(..))
import GHC.Types.ForeignStubs (ForeignStubs(..), CHeader(..), CStub(..))
import GHC.Core (Bind(..), bindersOf)
import GHC.Types.Id (isExportedId, idType)
import GHC.Stg.Syntax (GenStgTopBinding(..), GenStgBinding(..))
import qualified GHC.StgToCmm as StgToCmm
import GHC.Utils.Outputable (Outputable, ppr, showSDocOneLine, defaultSDocContext, SDoc)
import GHC.Driver.Ppr (showSDoc)
import Control.Monad.IO.Class (liftIO)
import System.Environment (getArgs)
import System.IO
import System.Exit (exitWith, ExitCode(..))
import System.Directory (createDirectoryIfMissing)
import System.FilePath ((</>), takeBaseName)
import Control.Monad (unless)
import GHC.Data.FastString (unpackFS)
import GHC.Types.SrcLoc
import GHC.Unit.Module.ModSummary (ModSummary(..))
import GHC.Unit.Module.Graph (mgModSummaries)
import GHC.Unit.Module.Location (ModLocation(..))
import Data.IORef
import qualified Data.Map.Strict as Map

main :: IO ()
main = do
  args <- getArgs
  case args of
    "--stubs" : libdir : dir : rest -> stubs libdir dir (takeWhile (/= "--") rest) (drop 1 (dropWhile (/= "--") rest))
    _ -> probe args

stubs :: FilePath -> FilePath -> [String] -> [FilePath] -> IO ()
stubs libdir dir flags files = do
  createDirectoryIfMissing True dir
  runGhc (Just libdir) $ do
    df0 <- getSessionDynFlags
    logger <- getLogger
    (df1, leftover, _) <- parseDynamicFlagsCmdLine logger df0 (map noLoc flags)
    unless (null leftover) $ liftIO $ do
      hPutStrLn stderr ("stubprobe: unknown GHC flags: " ++ unwords (map unLoc leftover))
      exitWith (ExitFailure 2)
    _ <- setSessionDynFlags df1 { ghcLink = NoLink }
    env <- getSession
    srcs <- liftIO (newIORef Map.empty)
    let sd :: SDoc -> String
        sd = showSDoc df1
        corePass guts = do
          mp <- liftIO (readIORef srcs)
          -- the stub's name is the source file's (GHC's: conc059.hs's module Test writes conc059_stub.h)
          let base = case Map.lookup (mg_module guts) mp of
                Just f -> takeBaseName f
                Nothing -> case mg_loc guts of
                  RealSrcSpan r _ -> takeBaseName (unpackFS (srcSpanFile r))
                  _ -> moduleNameString (moduleName (mg_module guts))
          case mg_foreign guts of
            NoStubs -> return ()
            ForeignStubs hd st -> liftIO $ do
              writeFile (dir </> base ++ "_stub.h") ("#include <HsFFI.h>\n" ++ sd (getCHeader hd) ++ "\n")
              writeFile (dir </> base ++ "_stub.c") ("#define IN_STG_CODE 0\n#include <Rts.h>\n" ++ sd (getCStub st) ++ "\n")
          return guts
        plugin = defaultPlugin { installCoreToDos = \_ todos -> return (todos ++ [CoreDoPluginPass "stubs" corePass])
                               , pluginRecompile = purePlugin }
    setSession env { hsc_plugins = (hsc_plugins env) { staticPlugins = staticPlugins (hsc_plugins env) ++ [StaticPlugin (PluginWithArgs plugin []) False] } }
    targets <- mapM (\f -> guessTarget f Nothing Nothing) files
    setTargets targets
    mg <- depanal [] False
    liftIO $ writeIORef srcs (Map.fromList [ (ms_mod m, f) | m <- mgModSummaries mg, Just f <- [ml_hs_file (ms_location m)] ])
    ok <- load LoadAllTargets
    unless (succeeded ok) $ liftIO (exitWith (ExitFailure 1))

probe :: [String] -> IO ()
probe (libdir : out : files) = do
  h <- openFile out WriteMode
  hSetBuffering h LineBuffering
  let line = hPutStrLn h
  runGhc (Just libdir) $ do
    df0 <- getSessionDynFlags
    _ <- setSessionDynFlags (updOptLevel 0 df0) { ghcLink = NoLink }
    env <- getSession
    let sd :: SDoc -> String
        sd = showSDoc df0
        one :: Outputable a => a -> String
        one = showSDocOneLine defaultSDocContext . ppr
        corePass guts = do
          liftIO $ do
            line ("== core " ++ one (mg_module guts))
            case mg_foreign guts of
              NoStubs -> line "stubs: none"
              ForeignStubs hd st -> do
                line "-- stub header (_stub.h):"
                line (sd (getCHeader hd))
                line "-- stub C:"
                line (sd (getCStub st))
            line ("foreign files: " ++ show (length (mg_foreign_files guts)))
            mapM_ (\b -> line ("bind " ++ one b ++ (if isExportedId b then " exported" else "") ++ " :: " ++ one (idType b)))
                  (concatMap bindersOf (mg_binds guts))
          return guts
        hook cfg ipm tycons ccs binds = do
          liftIO $ do
            line "== stg"
            mapM_ (\b -> line ("stg " ++ b)) (concatMap names binds)
          fst <$> StgToCmm.codeGen (hsc_logger env) (hsc_tmpfs env) cfg ipm tycons ccs binds
        names (StgTopLifted (StgNonRec b _)) = [one b]
        names (StgTopLifted (StgRec bs)) = map (one . fst) bs
        names (StgTopStringLit b _) = [one b]
        plugin = defaultPlugin { installCoreToDos = \_ todos -> return (todos ++ [CoreDoPluginPass "stubprobe" corePass])
                               , pluginRecompile = purePlugin }
    setSession env { hsc_hooks = (hsc_hooks env) { stgToCmmHook = Just hook }
                   , hsc_plugins = (hsc_plugins env) { staticPlugins = staticPlugins (hsc_plugins env) ++ [StaticPlugin (PluginWithArgs plugin []) False] } }
    targets <- mapM (\f -> guessTarget f Nothing Nothing) files
    setTargets targets
    _ <- load LoadAllTargets
    return ()
  hClose h
probe _ = hPutStrLn stderr "usage: stubprobe LIBDIR OUT FILE.hs... | stubprobe --stubs LIBDIR DIR FLAG... -- FILE.hs..."
