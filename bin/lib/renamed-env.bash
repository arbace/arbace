# The environment variables renamed on 2026-10-17 (doc/NAMING-SURVEY.md, "Decided" and "The
# rename as built"): G2A_GOROOT is ARBACE_GOROOT (TamaGo's Go tree, go1.27.1, default
# /root/tamago-go, the only Go any build uses), the other G2A_* are REPR_GO_*, J2A_* are
# REPR_JAVA_* and A2G_* are J2G_*. No script reads the old names any more, so a shell that still
# sets one would run with the defaults without a word. Sourced by the scripts that read
# ARBACE_GOROOT, REPR_GO_*, REPR_JAVA_* or J2G_*, which stop here (exit 2) when G2A_GOROOT is set
# and ARBACE_GOROOT is not, or when any other G2A_*, J2A_* or A2G_* is set, naming the new
# variable.
renamed_env_stale=
for renamed_env_v in $(compgen -e | grep -E '^(G2A|J2A|A2G)_' | LC_ALL=C sort); do
  case $renamed_env_v in
    G2A_GOROOT) [ -n "${ARBACE_GOROOT:-}" ] && continue; renamed_env_new=ARBACE_GOROOT ;;
    G2A_*) renamed_env_new=REPR_GO_${renamed_env_v#G2A_} ;;
    J2A_*) renamed_env_new=REPR_JAVA_${renamed_env_v#J2A_} ;;
    A2G_*) renamed_env_new=J2G_${renamed_env_v#A2G_} ;;
  esac
  echo "$renamed_env_v is set: it was renamed $renamed_env_new on 2026-10-17 (doc/NAMING-SURVEY.md); set $renamed_env_new instead and unset $renamed_env_v" >&2
  renamed_env_stale=1
done
[ -z "$renamed_env_stale" ] || exit 2
unset renamed_env_stale renamed_env_v renamed_env_new
