# The repo runs Java 26 explicitly (the user, 2026-11-02): the frozen jar is class file version 70,
# and the system's default JVM (Alpine's /usr/lib/jvm/default-jvm) may be another release. The
# JDK is ${ARBACE_FROZEN_JAVA_HOME:-/usr/lib/jvm/jdk-26}. Sourced. Defines:
#   arbace_java_home   prints the JDK's home, or fails with a message when it is missing or not 26
#   arbace_use_java26  puts its bin/ first on PATH (java, javac, jar, ... of every script after it)
arbace_java_home() {
  local h=${ARBACE_FROZEN_JAVA_HOME:-/usr/lib/jvm/jdk-26} v
  if [ ! -x "$h/bin/java" ] || [ ! -x "$h/bin/javac" ]; then
    echo "java26: no JDK 26 at $h (java, javac): install it there or set ARBACE_FROZEN_JAVA_HOME" >&2; return 1
  fi
  v=$("$h/bin/java" -version 2>&1 | sed -n '1s/^[^"]*"\([0-9]*\).*/\1/p')
  [ "$v" = 26 ] || { echo "java26: $h is Java ${v:-unknown}, not 26 (ARBACE_FROZEN_JAVA_HOME)" >&2; return 1; }
  echo "$h"
}
arbace_use_java26() {
  local h
  h=$(arbace_java_home) || return 1
  case ":$PATH:" in ":$h/bin:"*) ;; *) PATH=$h/bin:$PATH ;; esac
  export ARBACE_FROZEN_JAVA_HOME=$h
}
