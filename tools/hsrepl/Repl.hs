{-# LANGUAGE LambdaCase, ScopedTypeVariables #-}
-- hsrepl, HE7's session server (doc/haskell/HASKELL-EVAL-SURVEY.md, "As built: HE7 (write-ahead)"): a GHC API
-- session as `ghc --interactive` keeps it (the interactive context, Prelude imported, GHCi's flags), reading
-- requests on stdin and answering on stdout, one line each. A statement goes through GHC.Driver.Main's
-- hscParsedStmt as GHCi's execStmt' does; the one step GHCi leaves to its interpreter, hscCompileCoreExpr, is
-- hooked (GHC.Driver.Hooks.hscCompileCoreExprHook): the hook simplifies, tidies and prepares the Core as
-- hscCompileCoreExpr' does, runs coreToStg and stg2stg, and writes the STG as a module GhciStmtN of unit main
-- (the serializer of tools/hsdump/Stg.hs), instead of byteCodeGen. The host (hsi) runs the module's `stmt`, an
-- IO [HValue], and answers COMMIT with the values bound (GHCi's final ic: extendInteractiveContextWithIds) or
-- ABORT (an exception: the context is left as it was).
--
-- Requests: "STMT N TEXT" (N the input line's number, TEXT escaped: \n and \\), "COMMIT", "ABORT", "SETUP".
-- Replies, each line ended by "OK" or "FAIL": "OUT TEXT" (stdout), "ERR TEXT" (stderr), "RUN MODULE NAME..." (names:
-- the module's `stmt` first, then the bound names, external names of unit main).
-- Arbace's own code, written anew; GHC's GHCi.UI and GHC.Runtime.Eval are the models.
module Main (main) where

import GHC
import GHC.Driver.Main (hscParseStmtWithLocation, hscParsedStmt, hscImport, hscParseDeclsWithLocation, hscDesugar', hscSimplify, hscTidy, ioMsgMaybe, getHscEnv)
import GHC.Types.SourceError (SourceError)
import GHC.Driver.Errors.Types (hoistTcRnMessage)
import GHC.Tc.Module (tcRnDeclsi)
import GHC.Tc.Types (TcGblEnv(..))
import GHC.Unit.Module.ModGuts (ModGuts(..), CgGuts(..))
import GHC.Unit.Module.ModDetails (ModDetails(..))
import GHC.Unit.Module.Location (ModLocation(..))
import GHC.CoreToStg.AddImplicitBinds (addImplicitBinds)
import GHC.CoreToStg.Prep (corePrepPgm)
import GHC.Driver.Config.CoreToStg.Prep (initCorePrepPgmConfig)
import GHC.Core (bindersOfBinds)
import GHC.Core.TyCon (isImplicitTyCon)
import GHC.Types.Id (isDFunId, isImplicitId, idName)
import GHC.Types.TyThing (TyThing(..))
import GHC.Core.ConLike (ConLike(..))
import GHC.Utils.Misc (filterOut)
import GHC.Driver.Env
import GHC.Driver.Hooks (Hooks(..))
import GHC.Driver.Session
import GHC.Driver.Config.Core.Opt.Simplify (initSimplifyExprOpts)
import GHC.Driver.Config.CoreToStg (initCoreToStgOpts)
import GHC.Driver.Config.CoreToStg.Prep (initCorePrepConfig)
import GHC.Driver.Config.Stg.Pipeline (initStgPipelineOpts)
import GHC.Core.Opt.Simplify (simplifyExpr)
import GHC.Core (CoreExpr, Bind(..))
import qualified GHC.Core.Utils as CU
import GHC.Core.Tidy (tidyExpr)
import GHC.CoreToStg.Prep (corePrepExpr)
import GHC.CoreToStg (coreToStg)
import GHC.Stg.Pipeline (stg2stg)
import GHC.Runtime.Context (extendInteractiveContextWithIds, extendInteractiveContext, InteractiveContext(..))
import GHC.Core.Lint.Interactive (interactiveInScope)
import GHC.Types.Id.Make ()
import GHC.Types.Id (mkExportedVanillaId, Id)
import GHC.Types.Name (mkExternalName, getName, nameOccName, isExternalName, nameModule)
import GHC.Types.Name.Occurrence (mkVarOccFS, occName)
import GHC.Types.Var.Env (emptyTidyEnv)
import GHC.Types.Unique.Supply (uniqFromTag)
import GHC.Types.Unique.DFM (emptyUDFM)
import GHC.Types.SrcLoc (noSrcSpan)
import GHC.Types.TyThing (tyThingId)
import GHC.Unit.Types (Module, mkModule, moduleName, moduleUnit, interactiveUnit)
import GHC.Unit.Module (unitString)
import GHC.Unit.Env (ue_eps)
import GHC.Data.FastString (fsLit)
import GHC.Utils.Outputable
import GHC.Driver.Ppr (showSDoc)
import GHC.Core.TyCo.Ppr (pprSigmaType)
import GHC.Stg.Syntax (CgStgTopBinding)
import qualified GHC.LanguageExtensions as LangExt
import GHCi.RemoteTypes (mkRemoteRef, mkForeignRef, ForeignHValue)
import GHC.Runtime.Interpreter ()
import Stg (stgModule, writeAtomically)

import Control.Exception (SomeException, try, evaluate, throwIO)
import Control.Monad
import Control.Monad.IO.Class (liftIO)
import Data.IORef
import Data.List (isPrefixOf, stripPrefix, dropWhileEnd)
import Data.Char (isSpace)
import System.Environment (getArgs)
import System.FilePath ((</>))
import System.IO
import System.IO.Unsafe (unsafePerformIO)
import Unsafe.Coerce (unsafeCoerce)
import qualified Data.ByteString.Builder as BB

esc :: String -> String
esc = concatMap (\case '\n' -> "\\n"; '\\' -> "\\\\"; c -> [c])

unesc :: String -> String
unesc ('\\' : 'n' : r) = '\n' : unesc r
unesc ('\\' : '\\' : r) = '\\' : unesc r
unesc (c : r) = c : unesc r
unesc [] = []

reply :: String -> String -> IO ()
reply k v = putStrLn (k ++ (if null v then "" else " " ++ esc v)) >> hFlush stdout

outDir :: IORef FilePath
outDir = unsafePerformIO (newIORef ".")
{-# NOINLINE outDir #-}

main :: IO ()
main = do
  args <- getArgs
  let opt k = case dropWhile (/= k) args of (_ : v : _) -> v; _ -> error ("hsrepl: " ++ k)
      libdir = opt "--libdir"; out = opt "--out"
  writeIORef outDir out
  hSetEncoding stdin utf8; hSetEncoding stdout utf8; hSetBuffering stdout LineBuffering
  cnt <- newIORef (0 :: Int)
  res <- newIORef (Nothing :: Maybe String)
  runGhc (Just libdir) $ do
    dflags0 <- getSessionDynFlags
    _ <- setSessionDynFlags dflags0 { ghcLink = LinkInMemory, backend = interpreterBackend }
    -- GHCi's flags for statements (GHCi.UI: ExtendedDefaultRules on, the monomorphism restriction off)
    idf <- getInteractiveDynFlags
    setInteractiveDynFlags (((`gopt_set` Opt_ImplicitImportQualified) . (`xopt_set` LangExt.ExtendedDefaultRules)
                              . (`xopt_unset` LangExt.MonomorphismRestriction)) idf)
    env0 <- getSession
    setSession env0 { hsc_hooks = (hsc_hooks env0) { hscCompileCoreExprHook = Just (hook cnt res out) } }
    setContext [IIDecl (simpleImportDecl (mkModuleName "Prelude"))]
    liftIO (reply "READY" "")
    loop res
  where
    loop res = do
      eof <- liftIO isEOF
      unless eof $ do
        l <- liftIO getLine
        request res l
        loop res

request :: IORef (Maybe String) -> String -> Ghc ()
request res l = case words l of
  ("STMT" : n : _) -> do
    let txt = unesc (drop 1 (dropWhile (/= ' ') (drop 1 (dropWhile (/= ' ') l))))
    stmt res (read n) (dropWhileEnd isSpace txt)
  ["SETUP"] -> do
    -- the statement wrapper hsi runs every statement under: an exception's message as GHCi prints it, Nothing for
    -- the statement's values then. Run in an environment of its own (module Ghci900, imports of its own), dropped after.
    saved <- getSession
    ds <- mapM parseImportDecl ["import qualified Control.Exception as E", "import qualified System.IO as IO", "import qualified Control.Exception.Context as X"]
    setContext (IIDecl (simpleImportDecl (mkModuleName "Prelude")) : map IIDecl ds)
    e9 <- getSession
    setSession e9 { hsc_IC = (hsc_IC e9) { ic_mod_index = 900 } }
    stmt res 0 setupText
    setSession saved
  _ -> liftIO (reply "FAIL" ("bad request " ++ l))

setupText :: String
setupText = "let hsiStep__ = \\act -> E.try act >>= \\r -> IO.hFlush IO.stdout >> case r of { Right v -> return (Just v); Left e -> IO.hPutStrLn IO.stderr (\"*** Exception: \" ++ E.displayException (e :: E.SomeException) ++ (let c = X.displayExceptionContext (E.someExceptionContext e) in if null c then \"\" else \"\\n\" ++ c)) >> return Nothing }"

-- the statement or command TEXT of input line N
stmt :: IORef (Maybe String) -> Int -> String -> Ghc ()
stmt res n txt = handleSourceError (\e -> printException e >> liftIO (reply "FAIL" "")) $
  case txt of
    ':' : cmd -> command cmd
    _ | "import " `isPrefixOf` txt || "import\t" `isPrefixOf` txt -> do
          d <- parseImportDecl txt
          ctx <- getContext
          setContext (IIDecl d : ctx)
          liftIO (reply "OK" "")
    _ -> do
      env <- getSession
      let ic = hsc_IC env
          ic' = ic { ic_dflags = ic_dflags ic `wopt_unset` Opt_WarnUnusedLocalBinds }
          env' = mkInteractiveHscEnv (env { hsc_IC = ic' })
      mb <- liftIO (try (runInteractiveHsc env' (hscParseStmtWithLocation "<interactive>" n txt)))
      case mb of
        -- not a statement: GHCi tries declarations (data, class, instance, a function's equations, ...); the
        -- statement's error stands when they do not parse either
        Left (e :: SourceError) -> do
          md <- liftIO (try (hscParseDeclsWithLocation env' "<interactive>" n txt))
          case md of
            Right ds | not (null ds) -> decls res n ds
            Right _ -> liftIO (reply "OK" "")
            Left (_ :: SourceError) -> throwGhcException' e
        Right Nothing -> liftIO (reply "OK" "")
        Right (Just ps) -> do
          liftIO (writeIORef res Nothing)
          r <- liftIO (hscParsedStmt env' ps)
          case r of
            Nothing -> liftIO (reply "OK" "")
            Just (ids, _, _) -> do
              Just nm <- liftIO (readIORef res)
              liftIO (reply "RUN" (unwords (nm : map extern ids)))
              ack <- liftIO getLine
              when (ack == "COMMIT") $ do
                env2 <- getSession
                setSession env2 { hsc_IC = extendInteractiveContextWithIds (hsc_IC env2) ids }
              liftIO (reply "OK" "")

extern :: Id -> String
extern i = let n = getName i; m = nameModule n in "main:" ++ moduleNameString (moduleName m) ++ "/" ++ occNameString' n
  where occNameString' = \n -> showSDocOneLine defaultSDocContext (ppr (nameOccName n))

command :: String -> Ghc ()
command c = case words c of
  (w : _) | w `elem` ["t", "type"] -> do
    let e = trimS (drop (length w) c)
    ty <- exprType TM_Inst e
    dfs <- getInteractiveDynFlags
    liftIO (reply "OUT" (e ++ " :: " ++ showSDoc dfs (pprSigmaType ty)))
    liftIO (reply "OK" "")
  (w : _) | w `elem` ["k", "kind"] -> do
    let e = trimS (drop (length w) c)
    (_, k) <- typeKind True e
    dfs <- getInteractiveDynFlags
    liftIO (reply "OUT" (e ++ " :: " ++ showSDoc dfs (pprTypeForUser k)))
    liftIO (reply "OK" "")
  _ -> liftIO (hPutStrLn stderr ("unknown command ':" ++ c ++ "'") >> reply "FAIL" "")
  where trimS = dropWhileEnd isSpace . dropWhile isSpace

pprTypeForUser :: Type -> SDoc
pprTypeForUser = ppr

-- hscCompileCoreExpr' (GHC.Driver.Main) up to the STG, the STG written as a module
hook :: IORef Int -> IORef (Maybe String) -> FilePath -> HscEnv -> SrcSpan -> CoreExpr
     -> IO (ForeignHValue, [a], b)
hook cnt res out hsc_env _loc ds_expr = do
  let dflags = hsc_dflags hsc_env
      logger = hsc_logger hsc_env
      ic = hsc_IC hsc_env
      unit_env = hsc_unit_env hsc_env
  simpl <- simplifyExpr logger (ue_eps unit_env) (initSimplifyExprOpts dflags ic) ds_expr
  n <- atomicModifyIORef' cnt (\k -> (k + 1, k + 1))
  let mname = "GhciStmt" ++ show n
      this_mod = mkModule interactiveUnit (mkModuleName mname)
  u <- uniqFromTag 'I'
  let nm = mkExternalName u this_mod (mkVarOccFS (fsLit "stmt")) noSrcSpan
      bid = mkExportedVanillaId nm (CU.exprType simpl)
      tidy = tidyExpr emptyTidyEnv simpl
  cp_cfg <- initCorePrepConfig hsc_env
  prepd <- corePrepExpr logger cp_cfg tidy
  let (stg_binds, _denv, _ccs) = coreToStg (initCoreToStgOpts dflags) this_mod noLoc' [NonRec bid prepd]
  (withfvs, _) <- stg2stg logger (interactiveInScope ic) (initStgPipelineOpts dflags False) this_mod stg_binds
  let binds = map fst withfvs :: [CgStgTopBinding]
  b <- stgModule [("ghc", show ("9.14.1" :: String)), ("repl", "true")] this_mod "<interactive>" [] binds
  writeAtomically (out </> mname ++ ".stg.ae") b
  writeIORef res (Just ("main:" ++ mname ++ "/stmt"))
  fhv <- mkRemoteRef (unsafeCoerce ()) >>= \r -> mkForeignRef r (return ())
  return (fhv, [], unsafeCoerce emptyUDFM)
  where noLoc' = error "hsrepl: no module location"

throwGhcException' :: SourceError -> Ghc a
throwGhcException' = liftIO . throwIO

-- declarations as GHC.Driver.Main's hscParsedDecls compiles them, the code written as the module of the line (GhciN,
-- the interactive context's own module) in place of byte code; its names are found by hsi by module
decls :: IORef (Maybe String) -> Int -> [LHsDecl GhcPs] -> Ghc ()
decls _ _ ds = do
  hsc_env0 <- getSession
  let hsc_env = mkInteractiveHscEnv hsc_env0
  new_ic <- liftIO $ runInteractiveHsc hsc_env $ do
    tc_gblenv <- ioMsgMaybe $ hoistTcRnMessage $ tcRnDeclsi hsc_env ds
    let defaults = tcg_default tc_gblenv
        loc = OsPathModLocation { ml_hs_file_ospath = Nothing, ml_hi_file_ospath = error "hsrepl: no interface"
                                , ml_obj_file_ospath = error "hsrepl: no object", ml_dyn_obj_file_ospath = error "hsrepl: no object"
                                , ml_dyn_hi_file_ospath = error "hsrepl: no interface", ml_hie_file_ospath = error "hsrepl: no hie" }
    ds_result <- hscDesugar' loc tc_gblenv
    simpl_mg <- liftIO $ do
      plugins <- readIORef (tcg_th_coreplugins tc_gblenv)
      hscSimplify hsc_env plugins ds_result
    (tidy_cg, mod_details) <- liftIO $ hscTidy hsc_env simpl_mg
    let CgGuts { cg_module = this_mod, cg_binds = core_binds, cg_tycons = tycons } = tidy_cg
        dflags = hsc_dflags hsc_env
        logger = hsc_logger hsc_env
        ic = hsc_IC hsc_env
    liftIO $ do
      binds_to_prep <- addImplicitBinds (initCorePrepPgmConfig dflags (interactiveInScope ic)) loc tycons core_binds
      cp_cfg <- initCorePrepConfig hsc_env
      prepd <- corePrepPgm logger cp_cfg (initCorePrepPgmConfig dflags (interactiveInScope ic)) this_mod binds_to_prep
      let (stg_binds, _, _) = coreToStg (initCoreToStgOpts dflags) this_mod loc prepd
      (withfvs, _) <- stg2stg logger (interactiveInScope ic) (initStgPipelineOpts dflags False) this_mod stg_binds
      b <- stgModule [("ghc", show ("9.14.1" :: String)), ("repl", "true")] this_mod "<interactive>" tycons (map fst withfvs)
      out <- readIORef outDir
      writeAtomically (out </> moduleNameString (moduleName this_mod) ++ ".stg.ae") b
    let tcs = filterOut isImplicitTyCon (mg_tcs simpl_mg)
        patsyns = mg_patsyns simpl_mg
        ext_ids = [ i | i <- bindersOfBinds core_binds, isExternalName (idName i), not (isDFunId i || isImplicitId i) ]
        new_tythings = map AnId ext_ids ++ map ATyCon tcs ++ map (AConLike . PatSynCon) patsyns
    return (extendInteractiveContext ic new_tythings (md_insts mod_details) (md_fam_insts mod_details) defaults (tcg_fix_env tc_gblenv))
  env1 <- getSession
  setSession env1 { hsc_IC = new_ic }
  liftIO (reply "OK" "")
