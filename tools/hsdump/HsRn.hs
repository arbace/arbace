{-# LANGUAGE ScopedTypeVariables, RankNTypes, LambdaCase, ExistentialQuantification #-}
-- repr/haskell's front-end oracle (doc/haskell/FRONTEND-SURVEY.md, HF1): GHC 9.14.1's renamer output
-- (HsGroup GhcRn, the imports and the export list resolved) and the libraries' interfaces, written
-- as Clojure-reader forms. Arbace's own code, written anew against GHC's API; it decides nothing about
-- Arbace's renamer, it records what GHC's answers (arbace.repr.haskell.rename is judged against it).
--
--   hsdump rn    -o OUTDIR [--libdir DIR] [--ghc FLAG... --files] FILE...
--       one OUTDIR/FILE.rn.edn per module (the modules loaded together, in dependency order, so
--       a module may import another of the list; -i. finds them): the reduced GhcRn forms of §HF1:
--         (hsrn/module "M")
--         (hsrn/imports [(import [span] "M" qualified as hiding implicit)...])
--         (hsrn/occs [[l1 c1 l2 c2 (ext ns "Mod" "occ") | (loc ns "occ" l1 c1 l2 c2)]...])
--             every located name of the renamed tree (declarations, imports, export list), sorted
--         (hsrn/ops [(opapp span op l r prec dir) (neg span e) (secl span e op) (secr span op e)
--                    (conop span op l r) (tyop span op l r)...]) the operator trees as resolved
--         (hsrn/groups [(vals rec|nonrec [name...])... (tycl [name...] n-instances)...])
--         (hsrn/exports [(avail name [child...])...])  the module's export Avails, sorted
--   hsdump iface -o OUTDIR [--libdir DIR] [--ghc FLAG... --files] MODULE...
--       one OUTDIR/MODULE.iface.edn per module: (hsiface/module ...) its exports (with the defining
--       module of each), its fixities, and the things it defines (types as GHC prints them)
module HsRn (rnVerb, ifaceVerb) where

import GHC
import GHC.Driver.Session
import GHC.Driver.Backend (noBackend)
import GHC.Driver.Env (hsc_HUG)
import GHC.Hs
import GHC.Types.FieldLabel (FieldLabel(..), flLabel)
import GHC.Unit.Types (moduleUnit)
import GHC.Hs.Binds (NHsValBindsLR(..))
import GHC.Tc.Types (TcGblEnv(..))
import GHC.Types.Name
import GHC.Types.Name.Reader (RdrName)
import GHC.Types.Avail
import GHC.Types.Fixity
import GHC.Types.Basic (RecFlag(..))
import GHC.Types.SrcLoc
import GHC.Types.Var (varName, varType)
import GHC.Types.TyThing
import GHC.Core.TyCon
import GHC.Core.DataCon
import GHC.Core.Class
import GHC.Core.InstEnv (ClsInst(..))
import GHC.Core.Type (Type, PredType)
import GHC.Unit.Module.ModIface

import GHC.Unit.Types (moduleName)
import GHC.Utils.Outputable (SDoc, showSDocUnsafe, ppr)
import GHC.Utils.Outputable as O
import GHC.Types.Name.Occurrence
import GHC.Unit.Module.ModSummary (ms_mod_name, ms_hspp_file)
import GHC.Unit.Module.Graph (mgModSummaries)
import GHC.Data.FastString (unpackFS)
import GHC.Utils.Panic (handleGhcException)

import Control.Monad (forM_, forM, unless, when)
import Control.Monad.IO.Class (liftIO)
import Data.Data hiding (Fixity, tyConName)
import qualified Data.Data as DD
import GHC.Core.ConLike (ConLike(..))
import Data.List (sortOn, sort, nub, isPrefixOf, intercalate)
import Data.Maybe (fromMaybe, mapMaybe, catMaybes)
import System.Directory (createDirectoryIfMissing)
import System.FilePath (takeDirectory, (</>))
import System.IO
import System.Exit

-- ----------------------------------------------------------------------------------------
-- forms

esc :: String -> String
esc s = '"' : concatMap e s ++ "\""
  where
    e '"' = "\\\""
    e '\\' = "\\\\"
    e '\n' = "\\n"
    e '\t' = "\\t"
    e '\r' = "\\r"
    e c = [c]

nsOf :: OccName -> String
nsOf o
  | isTcClsNameSpace ns = "tc"
  | isTvNameSpace ns = "tv"
  | isDataConNameSpace ns = "data"
  | isFieldNameSpace ns = "fld"
  | otherwise = "var"
  where ns = occNameSpace o

-- a name: (ext ns "Mod" "occ") for an external or wired-in name, (loc ns "occ" l1 c1 l2 c2) for a local
-- one, its binding site (a local name's identity: the uniques are not stable between runs)
nameF :: Name -> String
nameF n = case nameModule_maybe n of
  Just m -> "(ext " ++ nsOf o ++ " " ++ esc (moduleNameString (moduleName m)) ++ " " ++ esc (occNameString o) ++ ")"
  Nothing -> case nameSrcSpan n of
    RealSrcSpan r _ -> "(loc " ++ nsOf o ++ " " ++ esc (occNameString o) ++ " " ++ spanI r ++ ")"
    _ -> "(loc " ++ nsOf o ++ " " ++ esc (occNameString o) ++ " nil)"
  where o = getOccName n

spanI :: RealSrcSpan -> String
spanI r = unwords (map show [srcSpanStartLine r, srcSpanStartCol r, srcSpanEndLine r, srcSpanEndCol r])

spanF :: SrcSpan -> String
spanF (RealSrcSpan r _) = "[" ++ spanI r ++ "]"
spanF _ = "nil"

vec :: [String] -> String
vec xs = "[" ++ unwords xs ++ "]"

-- ----------------------------------------------------------------------------------------
-- the generic walk of a renamed tree

-- the first span inside a location (an EpAnn's anchor, or a SrcSpan)
firstSpan :: forall d. Data d => d -> Maybe SrcSpan
firstSpan x
  | Just (s :: SrcSpan) <- cast x = Just s
  | otherwise = case catMaybes (gmapQ firstSpan x) of
      (s : _) -> Just s
      [] -> Nothing

isL :: forall d. Data d => d -> Bool
isL x = DD.tyConName (typeRepTyCon (typeOf x)) == "GenLocated"

-- the span of a located child, Nothing for other children
locSpanOf :: forall d. Data d => d -> Maybe SrcSpan
locSpanOf x | isL x = gmapQi 0 firstSpan x
            | otherwise = Nothing

conOf :: forall d. Data d => d -> String
conOf = showConstr . toConstr

data Item = Occ ([Int], String) | Op ([Int], String) | Grp String

spanKey :: SrcSpan -> [Int]
spanKey (RealSrcSpan r _) = [srcSpanStartLine r, srcSpanStartCol r, srcSpanEndLine r, srcSpanEndCol r]
spanKey _ = []

data Some = forall d. Data d => Some d

-- the original of an expansion the renamer made (a section, a record update, ...): the tree below
-- the expansion is the renamer's own desugaring, which the dump leaves out (its generated names:
-- leftSection, rightSection...), the source thing is what the dump records
origOf :: forall e. Data e => e -> Maybe Some
origOf e
  | conOf e == "XExpr" = case [o | Just o <- gmapQ (\c -> if conOf c == "ExpandedThingRn" then gmapQi 0 (\t -> case gmapQ Some t of (o : _) -> Just o; [] -> Nothing) c else Nothing) e] of
      (o : _) -> Just o
      [] -> Nothing
  | otherwise = Nothing

-- every located name, operator node and binding group below x, in tree order
walk :: forall d. Data d => d -> [Item]
walk x
  | Just (g :: NHsValBindsLR GhcRn) <- cast x = case g of
      NValBinds bs sigs -> concatMap (\(_, b) -> walk b) bs ++ walk sigs ++ [Grp ("(vals " ++ (case rf of Recursive -> "rec"; NonRecursive -> "nonrec") ++ " " ++
                                vec (sort (map nameF (collectHsBindsBinders CollNoDictBinders b))) ++ ")")
                        | (rf, b) <- bs]
  | Just (n :: Name) <- cast x = [] `const` n
  | Just (Some o) <- origOf x = walk o
  | isL x =
      let sp = gmapQi 0 firstSpan x
          here = gmapQi 1 (inspect sp) x
      in here ++ concat (gmapQ walk x)
  | tyName == "FieldOcc" =
      case (gmapQi 0 (\e -> cast e :: Maybe Name) x, gmapQi 1 locSpanOf x) of
        (Just n, Just sp) -> [Occ (spanKey sp, vec [spanF sp, nameF n])] ++ concat (gmapQ walk x)
        _ -> concat (gmapQ walk x)
  | otherwise = concat (gmapQ walk x)
  where tyName = DD.tyConName (typeRepTyCon (typeOf x))

-- a located node's own contribution: a located name, an operator application
inspect :: forall e. Data e => Maybe SrcSpan -> e -> [Item]
inspect msp e
  | Just (n :: Name) <- cast e = [Occ (spanKey sp, vec [spanF sp, nameF n])]
  | Just (Some o) <- origOf e = inspect msp o
  | DD.tyConName (typeRepTyCon (typeOf e)) == "WithUserRdr" =
      [Occ (spanKey sp, vec [spanF sp, nameF n]) | Just n <- gmapQ (\c -> cast c :: Maybe Name) e]
  | otherwise = case conOf e of
      "OpApp" -> [Op (spanKey sp, "(opapp " ++ spanF sp ++ " " ++ kids (drop 1) ++ " " ++ fix ++ ")")]
        where fix = case gmapQi 0 (\f -> cast f :: Maybe Fixity) e of
                Just (Fixity p d) -> show p ++ " " ++ (case d of InfixL -> "l"; InfixR -> "r"; InfixN -> "n")
                Nothing -> "nil"
      "NegApp" -> [Op (spanKey sp, "(neg " ++ spanF sp ++ " " ++ kids id ++ ")")]
      "SectionL" -> [Op (spanKey sp, "(secl " ++ spanF sp ++ " " ++ kids id ++ ")")]
      "SectionR" -> [Op (spanKey sp, "(secr " ++ spanF sp ++ " " ++ kids id ++ ")")]
      "HsOpTy" -> [Op (spanKey sp, "(tyop " ++ spanF sp ++ " " ++ kids id ++ ")")]
      "ConPat" -> case gmapQ (\d -> (locSpanOf d, if conOf d == "InfixCon" then Just (gmapQ locSpanOf d) else Nothing)) e of
        [_, (Just con, _), (_, Just [Just l, Just r])] ->
          [Op (spanKey sp, "(conop " ++ spanF sp ++ " " ++ spanF con ++ " " ++ spanF l ++ " " ++ spanF r ++ ")")]
        _ -> []
      _ -> []
  where
    sp = fromMaybe noSrcSpan msp
    kids f = unwords [spanF s | Just s <- f (gmapQ locSpanOf e)]

-- (ConPat's field order: ext, con, details: the located con is the second child)

itemsOf :: forall d. Data d => d -> ([([Int], String)], [([Int], String)], [String])
itemsOf x = ([o | Occ o <- its], [o | Op o <- its], [g | Grp g <- its])
  where its = walk x

-- ----------------------------------------------------------------------------------------
-- avails

availF :: AvailInfo -> String
availF a = case a of
  Avail n -> "(avail " ++ nameF n ++ " [])"
  AvailTC n cs -> "(avail " ++ nameF n ++ " " ++ vec (sort (map nameF (filter (/= n) cs))) ++ ")"

-- ----------------------------------------------------------------------------------------
-- the rn verb

setup :: FilePath -> [String] -> Ghc ()
setup _ flags = do
  d0 <- getSessionDynFlags
  logger <- getLogger
  (d1, leftover, _) <- parseDynamicFlagsCmdLine logger d0 (map noLoc flags)
  unless (null leftover) $ liftIO $ do
    hPutStrLn stderr ("hsdump: unknown GHC flags: " ++ unwords (map unLoc leftover))
    exitWith (ExitFailure 2)
  _ <- setSessionDynFlags d1 { backend = noBackend, ghcLink = NoLink }
  return ()

rnVerb :: FilePath -> [String] -> FilePath -> [FilePath] -> IO Bool
rnVerb libdir flags outDir files = runGhc (Just libdir) $ do
  setup libdir flags
  targets <- mapM (\f -> guessTarget f Nothing Nothing) files
  setTargets targets
  _ <- load LoadAllTargets
  graph <- depanal [] False
  ok <- forM (mgModSummaries graph) $ \ms -> do
    let mn = moduleNameString (ms_mod_name ms)
        file = ms_hspp_file ms
    p <- parseModule ms
    t <- typecheckModule p
    let out = outDir </> (map (\c -> if c == '/' then '/' else c) file) ++ ".rn.edn"
    case tm_renamed_source t of
      Nothing -> return False
      Just (grp, imps, mexps, _, _) -> do
        let (o1, p1, g1) = itemsOf grp
            (o2, p2, g2) = itemsOf imps
            (o3, p3, g3) = itemsOf (fmap (map fst) mexps)
            tyclGs = [ "(tycl " ++ vec (sort [nameF (dn d) | d <- group_tyclds tg]) ++ " " ++ show (length (group_instds tg)) ++ ")"
                     | tg <- hs_tyclds grp ]
            (gbl, _) = tm_internals_ t
            exps = sort (map availF (tcg_exports gbl))
            impF (L l d) = "(import " ++ spanF (locA l) ++ " " ++ esc (moduleNameString (unLoc (ideclName d))) ++ " "
              ++ (case ideclQualified d of NotQualified -> "false"; _ -> "true") ++ " "
              ++ maybe "nil" (esc . moduleNameString . unLoc) (ideclAs d) ++ " "
              ++ (case ideclImportList d of Just (EverythingBut, _) -> "true"; _ -> "false") ++ " "
              ++ (case ideclExt d of XImportDeclPass _ _ imp -> if imp then "true" else "false") ++ ")"
            allOccs = map snd (sort (o1 ++ o2 ++ o3))
            groupsInOrder = g1 ++ g2 ++ g3 ++ tyclGs
        liftIO $ do
          createDirectoryIfMissing True (takeDirectory out)
          withFile out WriteMode $ \h -> do
            hSetEncoding h utf8
            hPutStrLn h ("(hsrn/module " ++ esc mn ++ ")")
            hPutStrLn h ("(hsrn/imports " ++ vec (map impF imps) ++ ")")
            -- the occurrences, operator trees and groups are sets: sorted, each once (the renamer keeps a
            -- name at both a binder's FunBind and its FunRhs, the tree's duplicates carry nothing)
            hPutStrLn h ("(hsrn/occs [" ++ intercalate "\n  " (nubSorted allOccs) ++ "])")
            hPutStrLn h ("(hsrn/ops [" ++ intercalate "\n  " (nubSorted (map snd (sort (p1 ++ p2 ++ p3)))) ++ "])")
            hPutStrLn h ("(hsrn/groups [" ++ intercalate "\n  " (sort groupsInOrder) ++ "])")
            -- GHC's order of the groups (the dependency analysis' order depends on the uniques of its
            -- names): reported by the check, not judged
            hPutStrLn h ("(hsrn/group-order [" ++ intercalate "\n  " groupsInOrder ++ "])")
            hPutStrLn h ("(hsrn/exports [" ++ intercalate "\n  " exps ++ "])")
        return True
  return (and ok)
  where
    nubSorted :: [String] -> [String]
    nubSorted (a : b : r) | a == b = nubSorted (b : r)
                          | otherwise = a : nubSorted (b : r)
    nubSorted r = r


-- ----------------------------------------------------------------------------------------
-- the iface verb

pprS :: Outputable a => a -> String
pprS = unwords . words . showSDocUnsafe . O.ppr

ifaceVerb :: FilePath -> [String] -> FilePath -> Bool -> [String] -> IO Bool
ifaceVerb libdir flags outDir closure mods = runGhc (Just libdir) $ do
  setup libdir flags
  ms <- forM mods $ \mn -> findModule (mkModuleName mn) Nothing
  let loop _ [] = return True
      loop seen (m : rest)
        | moduleNameString (moduleName m) `elem` seen = loop seen rest
        | otherwise = do
            (ok, defs) <- ifaceOf outDir m
            ok' <- loop (moduleNameString (moduleName m) : seen) (if closure then rest ++ defs else rest)
            return (ok && ok')
  loop [] ms

ifaceOf :: FilePath -> Module -> Ghc (Bool, [Module])
ifaceOf outDir m = do
  mi <- getModuleInfo m
  case mi >>= modInfoIface of
    Nothing -> liftIO (hPutStrLn stderr ("hsdump: no interface of " ++ moduleNameString (moduleName m))) >> return (False, [])
    Just iface -> do
      let dnames = nubNames [n | a <- mi_exports iface, n <- availNames a, nameModule_maybe n == Just m]
      things <- fmap catMaybes (mapM lookupName dnames)
      let mn = moduleNameString (moduleName m)
          exps = map availF (mi_exports iface)
          fixes = sortOn fst [ (occNameString o, "(fixity " ++ nsOf o ++ " " ++ esc (occNameString o) ++ " " ++ show p ++ " " ++ dirS d ++ ")")
                             | (o, Fixity p d) <- mi_fixities iface ]
          dirS d = case d of InfixL -> "l"; InfixR -> "r"; InfixN -> "n"
          defined = [t | t <- things, maybe False (== m) (nameModule_maybe (getName t))]
          out = outDir </> mn ++ ".iface.edn"
      liftIO $ do
        createDirectoryIfMissing True outDir
        withFile out WriteMode $ \h -> do
          hSetEncoding h utf8
          hPutStrLn h ("(hsiface/module " ++ esc mn ++ " {:unit " ++ esc (unitString' m) ++ "})")
          hPutStrLn h ("(hsiface/exports [" ++ intercalate "\n  " exps ++ "])")
          hPutStrLn h ("(hsiface/fixities [" ++ intercalate "\n  " (map snd fixes) ++ "])")
          hPutStrLn h ("(hsiface/things [" ++ intercalate "\n  " (sort (map thingF defined)) ++ "])")
      return (True, nubMods [nameModule n | a <- mi_exports iface, n <- availNames a, not (isInternalName n), not (isSystemName n), isExternalName n])
  where unitString' = pprS . moduleUnit

nubNames :: [Name] -> [Name]
nubNames = foldr (\x acc -> if x `elem` acc then acc else x : acc) []

-- instances: GHC's getModuleInfo does not give a package module's (Inspect.hs: "unimplemented"),
-- and mi_insts of a loaded interface panics ("No mi_insts in PIT"): HF3's loader reads them (HF1 left)

thingF :: TyThing -> String
thingF t = case t of
  AnId i -> "(id " ++ nameF (getName i) ++ " " ++ esc (pprS (varType i)) ++ ")"
  AConLike (RealDataCon dc) ->
    "(datacon " ++ nameF (getName dc) ++ " " ++ esc (pprS (dataConWrapperType dc)) ++ " " ++ show (dataConSourceArity dc)
      ++ " " ++ vec (map (esc . pprS . flLabel) (dataConFieldLabels dc)) ++ ")"
  AConLike (PatSynCon ps) -> "(patsyn " ++ nameF (getName ps) ++ ")"
  ATyCon tc
    | Just cls <- tyConClass_maybe tc ->
        "(class " ++ nameF (getName tc) ++ " " ++ vec (map (esc . pprS) (classSCTheta cls)) ++ " "
          ++ vec [ "(" ++ nameF (getName op) ++ " " ++ esc (pprS (varType op)) ++ ")" | op <- classMethods cls ] ++ ")"
    | otherwise ->
        "(tycon " ++ nameF (getName tc) ++ " " ++ esc (pprS (tyConKind tc)) ++ " "
          ++ (if isNewTyCon tc then "newtype" else if isAlgTyCon tc then "data" else if isTypeSynonymTyCon tc then "synonym" else if isFamilyTyCon tc then "family" else "other")
          ++ " " ++ vec (map (nameF . getName) (tyConDataCons tc)) ++ ")"
  ACoAxiom _ -> "(axiom)"

nubMods :: [Module] -> [Module]
nubMods = foldr (\x acc -> if x `elem` acc then acc else x : acc) []

dn :: LTyClDecl GhcRn -> Name
dn = tcdName . unLoc
