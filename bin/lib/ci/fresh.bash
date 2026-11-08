# ci's sources on the tools' host (doc/C23-SURVEY.md, "As built: the own target's triage"): sourced by bin/ci and bin/repr-c
# after bin/lib/tools.bash. Stage 0 embeds ci (arbace/c.ae, arbace/c/) compiled since C9.2's re-seed, and RT.load runs a
# namespace through its compiled loader whenever the executable has one (RT.toolSource reloads only arbace/repr/, arbace/j2g/,
# arbace/build/, arbace/c2j/ and arbace/go/ from the checkout when they differ): a checkout whose ci differs from the host's
# embedded copy ran the seed's checker and builder, not its own (night 7 and 8: LLS21 (a)'s atomics and function-pointer
# widths, c-deferred's VLA structures, complex integers and the re-entered main file's read, all after the seed d5548e72,
# failed on the consolidated tree though each passed on its branch, proved there over the checkout's ci).
#   c_ci_fresh   when a source of ci in the checkout differs from the host's embedded copy (or the host has none of it but
#                the checkout does), exports ARBACE_PATH_FIRST=1 ARBACE_NO_AOT=1 ARBACE_NO_IMAGE=1 (PB.6's way, the gate's C
#                steps' from the backlog's merge to the re-seed: every namespace evaluated, the checkout's first) and says
#                so once on the standard error; nothing when they are the host's own (after a re-seed: ci compiled, as fast as
#                before), when ARBACE_NO_AOT is already set, or under CI_FRESH=0 (the host's ci on purpose). The scripts that
#                run CI_EXE (an executable built from the checkout) unset the three on it, as before.
c_ci_fresh() {
  [ "${CI_FRESH:-1}" = 0 ] && return 0
  [ -n "${ARBACE_NO_AOT:-}" ] && return 0
  local exe out
  . "$root/bin/lib/host.bash"
  exe=$(arbace_host_exe) || return 1
  mkdir -p "$root/.tmp"
  out=$root/.tmp/ci-fresh.$BASHPID.out
  (cd "$root" && "$exe" -e '
(let [fs (->> (file-seq (java.io.File. "arbace/c")) (map #(.getPath ^java.io.File %)) (filter #(.endsWith ^String % ".ae"))
              (cons "arbace/c.ae") sort)
      emb (fn [f] (when-let [ins (arbace.lang.RT/resourceAsStream nil f)] (try (.readAllBytes ins) (finally (.close ins)))))]
  (doseq [f fs
          :let [c (java.nio.file.Files/readAllBytes (.toPath (java.io.File. ^String f))) e (emb f)]
          :when (not (and e (java.util.Arrays/equals ^bytes c ^bytes e)))]
    (println f)))') > "$out" 2>&1 || { echo "c_ci_fresh: the host's embedded sources not read ($exe): $(tail -1 "$out")" >&2; rm -f "$out"; return 1; }
  if [ -s "$out" ]; then
    export ARBACE_PATH_FIRST=1 ARBACE_NO_AOT=1 ARBACE_NO_IMAGE=1
    [ -n "${CI_FRESH_SAID:-}" ] || echo "ci: the checkout's ci differs from the host's embedded copy ($(wc -l < "$out") sources: $(head -3 "$out" | tr '\n' ' ')...): evaluated from the checkout (ARBACE_PATH_FIRST=1 ARBACE_NO_AOT=1 ARBACE_NO_IMAGE=1) until the next re-seed" >&2
    export CI_FRESH_SAID=1
  fi
  rm -f "$out"
}
