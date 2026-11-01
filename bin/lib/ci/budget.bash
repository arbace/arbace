# The run level's time budget of a program (CS216): ci_budget ID prints its seconds, 10 unless test/c/check-budget.edn
# (CI_BUDGETS: another file) lists the id, one `"ID" SECONDS` per line (the first with the map's brace). Sourced by bin/ci check and check --aot.
ci_budget() {
  local f=${CI_BUDGETS:-${root:-.}/test/c/check-budget.edn} b
  b=$(sed -n "s|^[{ ]*\"$1\" *\\([0-9][0-9]*\\).*|\\1|p" "$f" 2>/dev/null | head -1)
  echo "${b:-10}"
}
