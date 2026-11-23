# hs-pkgs-1122: the Hackage packages of Haskell's eval corpora (test/haskell/hackage.edn; the corpus's :hackage), for the own
# libraries' scripts (bin/lib/repr-haskell/lib-check, lib-stg). Sourced after $root and bin/lib/tools.bash.
#
#   hackage_pkg PKG        status 0 when PKG is one of the Hackage packages
#   hackage_dep_stg PKG    the own STG directories (each of stg/UNIT/M.stg.ae) of PKG's dependencies among the Hackage packages
#                          (:deps, transitively), joined by ":" with a leading ":" (nothing when none): LIB_STG_DEP_OWN_TARS=TAR:TAR...
#                          (the gate producers' PKG-own.tar outputs) unpacked into DIR (the second argument), else each one's
#                          lib-stg --package DEP --ifaces own in .tmp; fails when one is missing. GHC's packs hold none of them, so
#                          the front end learns their modules' unit (tostg's units-of) and constructors from these
hackage_pkg() { grep -q "{:name \"$1\" :version" "$root/test/haskell/hackage.edn"; }
hackage_dep_stg() {
  local pkg=$1 hd=$2 hdeps d n t out=""
  hackage_pkg "$pkg" || return 0
  hdeps=$(arbace_tool -e "(let [ps (:packages (read-string (slurp \"$root/test/haskell/hackage.edn\")))
                               hs (set (map :name ps)) by (into {} (map (juxt :name identity) ps))
                               go (fn go [n] (mapcat #(cons % (go %)) (filter hs (:deps (by n)))))]
                           (doseq [d (distinct (go \"$pkg\"))] (println d)))")
  [ -n "$hdeps" ] || return 0
  if [ -n "${LIB_STG_DEP_OWN_TARS:-}" ]; then
    rm -rf "$hd"; mkdir -p "$hd"; n=0
    IFS=: read -r -a dts <<< "$LIB_STG_DEP_OWN_TARS"
    for t in "${dts[@]}"; do
      [ -f "$t" ] || { echo "hackage: no $t (LIB_STG_DEP_OWN_TARS)" >&2; return 1; }
      n=$((n + 1)); mkdir -p "$hd/$n"; tar -C "$hd/$n" -xf "$t"; out=$out:$hd/$n/stg
    done
  else
    for d in $hdeps; do
      [ -d "$root/.tmp/repr-haskell/lib-stg-own/$d/out/stg" ] ||
        { echo "hackage: $pkg needs the own STG of $d (bin/repr-haskell lib-stg --package $d --ifaces own first)" >&2; return 1; }
      out=$out:$root/.tmp/repr-haskell/lib-stg-own/$d/out/stg
    done
  fi
  echo "== $pkg: the own STG of its Hackage dependencies beside the libraries ($(echo $hdeps))" >&2
  echo "$out"
}
