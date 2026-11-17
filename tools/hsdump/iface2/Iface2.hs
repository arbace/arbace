{-# LANGUAGE ScopedTypeVariables #-}
-- tools/hsdump's second interface verb (HF9-P2 (b), HF9-P5 (a); doc/haskell/FRONTEND-SURVEY.md, "As built: HF9-P1, P2, P5,
-- P6, P7, P8"): an executable of its own, under a version of its own (iface2Version), so that the helper's identity
-- (tools/hsdump/*.hs and GHC_VERSION: hsr_helper_hash) and every pack keyed by it stay as they are. It is built with
-- tools/hsdump/HsRn.hs, its export list opened (bin/repr-haskell's build_iface2 copies it), so its forms are the iface
-- verb's own (thingF, typeF, nameF). Arbace's own code, written anew against GHC 9.14.1's API.
--
--   hsdump-iface2 iface2 -o OUTDIR --libdir DIR [--ghc FLAG... --files] MODULE...
--       one OUTDIR/MODULE.iface.edn per module and the modules its things name (the iface verb's closure), in the iface
--       verb's format, with three differences:
--       - a hidden module of a package (GHC-87110: not exposed, so findModule refuses it) is found among the units'
--         hidden modules and its interface read all the same (getModuleInfo loads a package module's interface by its
--         Module, whatever its exposure);
--       - (hsiface/things ...) also holds every type constructor the module defines that it does not export (a synonym, a
--         data or newtype, a family; not a class, not an implicit one, not a family instance's representation): GHC's
--         interface has their declarations (GHC.Iface.Tidy keeps every TyCon), and an exported type may expand to one
--         (CodeBuffer#, CodingBuffer# of GHC.Internal.IO.Encoding.Types);
--       - a pattern synonym is (patsyn NAME {:univ [TV...] :req [T...] :ex [TV...] :prov [T...] :args [T...]
--         :res T :fields ["f"...] :builder T|nil}): patSynSig's parts (GHC.Core.PatSyn), the builder's type when it has
--         one, where the iface verb writes (patsyn NAME) alone (GHC's pretty-printed pattern type is left out: it names the
--         variables by their uniques, which differ between sessions; the forms' "a#UNIQUE" are renumbered by a verify).
--       The header is (hsiface/module "M" {:unit "U" :verb "iface2" :iface2 N}).
module Main (main) where

import HsRn
import GHC
import GHC.Driver.Env (hscEPS, hsc_units)
import GHC.Unit.External (ExternalPackageState(..))
import GHC.Unit.State (listUnitInfo)
import GHC.Unit.Info (mkUnit, unitHiddenModules, unitPackageNameString)
import GHC.Types.TypeEnv (typeEnvElts)
import GHC.Types.TyThing (TyThing(..), isImplicitTyThing)
import GHC.Types.Name
import GHC.Types.Avail (availNames)
import GHC.Types.Fixity (Fixity(..), FixityDirection(..))
import GHC.Types.FieldLabel (flLabel)
import GHC.Core.ConLike (ConLike(..))
import GHC.Core.PatSyn (PatSyn, patSynSig, patSynFieldLabels, patSynBuilder)
import GHC.Core.TyCon (isFamInstTyCon, isClassTyCon, TyCon)
import GHC.Core.TyCo.Rep (scaledThing)
import GHC.Core.InstEnv (ClsInst(..), instEnvElts)
import GHC.Core.FamInstEnv (FamInst(..), famInstEnvElts)
import GHC.Core.Class (classOpItems)
import GHC.Core.TyCon (tyConClass_maybe)
import GHC.Types.Id (realIdUnfolding)
import GHC.Core (maybeUnfoldingTemplate)
import GHC.Types.Var (varType)
import GHC.Unit.Module.ModIface
import GHC.Unit.Module.Deps (dep_orphs)
import GHC.Core.Type (tyConsOfType)
import GHC.Core.DataCon (dataConWrapperType)
import GHC.Core.Class (classMethods)
import GHC.Types.Unique.Set (nonDetEltsUniqSet)
import Control.Monad (forM)
import Control.Monad.IO.Class (liftIO)
import Data.List (sortOn, sort, intercalate, isPrefixOf)
import Data.Maybe (catMaybes)
import System.Directory (createDirectoryIfMissing)
import System.Environment (getArgs)
import System.Exit
import System.FilePath ((</>))
import System.IO

-- the verb's version: bumped when its output changes (in every file's header; the lib-check pack keys by the sources)
iface2Version :: Int
iface2Version = 1

main :: IO ()
main = do
  hSetEncoding stdout utf8
  args <- getArgs
  case args of
    ("iface2" : rest) -> do
      let (out, libdir, flags, mods) = parse rest (".tmp/hsdump/iface2-out", "", [], [])
      if null libdir then hPutStrLn stderr "hsdump-iface2: --libdir DIR (ghc --print-libdir)" >> exitWith (ExitFailure 2) else return ()
      ok <- iface2Verb libdir flags out mods
      exitWith (if ok then ExitSuccess else ExitFailure 1)
    _ -> hPutStrLn stderr "usage: hsdump-iface2 iface2 -o OUTDIR --libdir DIR [--ghc FLAG... --files] MODULE..." >> exitWith (ExitFailure 2)
  where
    parse ("-o" : d : xs) (_, l, f, m) = parse xs (d, l, f, m)
    parse ("--libdir" : d : xs) (o, _, f, m) = parse xs (o, d, f, m)
    parse ("--ghc" : fs) (o, l, f, m) = let (gs, r) = break (== "--files") fs in parse (drop 1 r) (o, l, f ++ gs, m)
    parse (x : xs) (o, l, f, m) = parse xs (o, l, f, m ++ [x])
    parse [] acc = acc

-- a module by its name: exposed (findModule), else a unit's hidden module (ghc-internal's first)
moduleOf :: String -> Ghc (Maybe Module)
moduleOf mn = do
  r <- safely (\_ -> return Nothing) (Just <$> findModule (mkModuleName mn) Nothing)
  case r of
    Just m -> return (Just m)
    Nothing -> do
      hsc <- getSession
      let us = [ u | u <- listUnitInfo (hsc_units hsc), mkModuleName mn `elem` unitHiddenModules u ]
          pref = [ u | u <- us, unitPackageNameString u == "ghc-internal" ] ++ us
      case pref of
        (u : _) -> return (Just (mkModule (mkUnit u) (mkModuleName mn)))
        [] -> liftIO (hPutStrLn stderr ("hsdump-iface2: " ++ mn ++ ": no unit exposes or hides it")) >> return Nothing

iface2Verb :: FilePath -> [String] -> FilePath -> [String] -> IO Bool
iface2Verb libdir flags outDir mods = runGhc (Just libdir) $ do
  setup libdir flags
  ms <- fmap catMaybes (mapM moduleOf mods)
  let loop _ [] = return True
      loop seen (m : rest)
        | m `elem` seen = loop seen rest
        | otherwise = do
            (ok, defs) <- safely (\e -> do
                            liftIO (hPutStrLn stderr ("hsdump-iface2: " ++ moduleNameString (moduleName m) ++ ": " ++ takeWhile (/= '\n') e))
                            return (False, []))
                          (iface2Of outDir m)
            ok' <- loop (m : seen) (rest ++ defs)
            return (ok && ok')
  loop [] ms

iface2Of :: FilePath -> Module -> Ghc (Bool, [Module])
iface2Of outDir m = do
  mi <- getModuleInfo m
  case mi >>= modInfoIface of
    Nothing -> liftIO (hPutStrLn stderr ("hsdump-iface2: no interface of " ++ moduleNameString (moduleName m))) >> return (False, [])
    Just iface -> do
      hsc <- getSession
      eps <- liftIO (hscEPS hsc)
      let insts = instEnvElts (eps_inst_env eps)
          dnames = nubNames [n | a <- mi_exports iface, n <- availNames a, nameModule_maybe n == Just m]
      things <- fmap catMaybes (mapM lookupName dnames)
      -- every type constructor of the module's declarations it does not export (the external package state holds the
      -- interface's declarations once it is loaded)
      let exported = map getName things
          extra = [ t | t@(ATyCon tc) <- typeEnvElts (eps_PTE eps)
                      , nameModule_maybe (getName tc) == Just m, getName tc `notElem` exported
                      , not (isImplicitTyThing t), not (isFamInstTyCon tc), not (isClassTyCon tc) ]
          defined = [t | t <- things, maybe False (== m) (nameModule_maybe (getName t))]
      let dmNames = [ n | ATyCon tc <- things, Just cls <- [tyConClass_maybe tc], (_, Just (n, _)) <- classOpItems cls ]
      dms <- fmap catMaybes (mapM lookupName dmNames)
      let unfoldings = [ "(dm-unfolding " ++ nameF (getName i) ++ " " ++ coreF e ++ ")"
                       | AnId i <- dms, Just e <- [maybeUnfoldingTemplate (realIdUnfolding i)] ]
          mn = moduleNameString (moduleName m)
          exps = map availF (mi_exports iface)
          fixes = sortOn fst [ (occNameString o, "(fixity " ++ nsOf o ++ " " ++ esc (occNameString o) ++ " " ++ show p ++ " " ++ dirS d ++ ")")
                             | (o, Fixity p d) <- mi_fixities iface ]
          dirS d = case d of InfixL -> "l"; InfixR -> "r"; InfixN -> "n"
          out = outDir </> mn ++ ".iface.edn"
      liftIO $ do
        createDirectoryIfMissing True outDir
        withFile out WriteMode $ \h -> do
          hSetEncoding h utf8
          hPutStrLn h ("(hsiface/module " ++ esc mn ++ " {:unit " ++ esc (pprS (moduleUnit m)) ++ " :verb \"iface2\" :iface2 " ++ show iface2Version ++ "})")
          hPutStrLn h ("(hsiface/exports [" ++ intercalate "\n  " exps ++ "])")
          hPutStrLn h ("(hsiface/fixities [" ++ intercalate "\n  " (map snd fixes) ++ "])")
          hPutStrLn h ("(hsiface/things [" ++ intercalate "\n  " (sort (map thing2F (defined ++ extra))) ++ "])")
          hPutStrLn h ("(hsiface/instances [" ++ intercalate "\n  " (sort [ "(instance " ++ nameF (getName (is_dfun i)) ++ " " ++ typeF (varType (is_dfun i)) ++ ")"
                                                                      | i <- insts, nameModule_maybe (getName (is_dfun i)) == Just m ]) ++ "])")
          hPutStrLn h ("(hsiface/unfoldings [" ++ intercalate "\n  " (sort unfoldings) ++ "])")
          hPutStrLn h ("(hsiface/fam-instances [" ++ intercalate "\n  " (sort [ famInstF fi | fi <- famInstEnvElts (eps_fam_inst_env eps), nameModule_maybe (getName (fi_axiom fi)) == Just m ]) ++ "])")
      let tyOf t = case t of
            AnId i -> [varType i]
            AConLike (RealDataCon dc) -> [dataConWrapperType dc]
            ATyCon tc | Just cls <- tyConClass_maybe tc -> map varType (classMethods cls)
            _ -> []
          tcMods = [ nameModule n | t <- defined ++ extra, ty <- tyOf t, tc <- nonDetEltsUniqSet (tyConsOfType ty)
                                  , let n = getName tc, isExternalName n ]
      return (True, nubMods ([nameModule n | a <- mi_exports iface, n <- availNames a, not (isInternalName n), not (isSystemName n), isExternalName n] ++ tcMods ++ dep_orphs (mi_deps iface)))

-- the iface verb's thing, a pattern synonym with its signature
thing2F :: TyThing -> String
thing2F t = case t of
  AConLike (PatSynCon ps) -> patSynF ps
  _ -> thingF t

patSynF :: PatSyn -> String
patSynF ps =
  let (univ, req, ex, prov, args, res) = patSynSig ps
      tvs = vec . map (esc . tvNameU)
      tys = vec . map typeF
  in "(patsyn " ++ nameF (getName ps)
       ++ " {:univ " ++ tvs univ ++ " :req " ++ tys req ++ " :ex " ++ tvs ex ++ " :prov " ++ tys prov
       ++ " :args " ++ tys (map scaledThing args) ++ " :res " ++ typeF res
       ++ " :fields " ++ vec (map (esc . pprS . flLabel) (patSynFieldLabels ps))
       ++ " :builder " ++ (case patSynBuilder ps of Just (_, ty, _) -> typeF ty; Nothing -> "nil") ++ "})"
