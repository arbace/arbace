#!/bin/bash
# bin/j2c-check --jdk [MODULE...]: the converted JDK compiled with the class forms compiler and
# compared with javac. Run through bin/j2c-check (from the repository root).
#
# Per module M of $JDK_SRC/*/share/classes (largest first):
#   convert   bin/j2c ... jdk: the module's Java files into class forms, $work/conv/M
#   javac     the same files compiled twice by javac, $work/ref1/M and $work/ref2/M, with
#             --patch-module against the running JDK (built from the same sources) and the
#             options the JDK build uses for the module (make/modules/*/Java.gmk)
#   check     bin/class-forms-check on chunks of the converted files, in parallel, against
#             $work/ref1/M (REF), results in $work/res/M.*.edn
#   report    test/j2c/jdk_report.clj: $work/report.md and $work/report.edn
set -uo pipefail
work=.tmp/j2c-jdk
src=${JDK_SRC:-/root/jdk26u/src}
jobs=${JDK_JOBS:-40}
steps=${JDK_STEPS:-convert,javac,check,report}
mkdir -p "$work"

if [ $# -gt 0 ]; then
  modules="$*"
else
  modules=$(for d in "$src"/*/share/classes; do
              m=${d#"$src"/}; m=${m%%/*}
              echo "$(find "$d" -name '*.java' | wc -l) $m"
            done | sort -rn | awk '{print $2}')
fi

# javac options of the JDK build that change class files (SPEC §3)
module_env() {
  case "$1" in
    java.base|jdk.compiler|jdk.jfr|jdk.jartool) echo "CONCAT=inline" ;;
    jdk.internal.vm.ci) echo "CONCAT=inline PARAMETERS=1" ;;
    *) echo "" ;;
  esac
}
javac_opts() {
  local e; e=$(module_env "$1")
  case "$e" in *CONCAT=inline*) printf '%s\n' -XDstringConcat=inline ;; esac
  case "$e" in *PARAMETERS=1*) printf '%s\n' -parameters ;; esac
}

has() { case ",$steps," in *",$1,"*) return 0 ;; esac; return 1; }

convert_one() {
  local m=$1
  rm -rf "$work/conv/$m"
  J2C_THREADS=8 J2C_JVM_OPTS=-Xmx10g bin/j2c -m arbace.j2c.main jdk --check "$work/conv/$m" "$m" \
    "$src/$m/share/classes" > "$work/log/convert-$m.log" 2>&1
  echo "convert $m: $(tail -1 "$work/log/convert-$m.log")"
}

javac_one() {
  local m=$1 s="$src/$1/share/classes" n
  find "$s" -name '*.java' ! -name module-info.java ! -path '*/snippet-files/*' | sort > "$work/log/javac-$m.list"
  for n in 1 2; do
    rm -rf "$work/ref$n/$m"
    mkdir -p "$work/ref$n/$m"
    if [ -s "$work/log/javac-$m.list" ]; then
      # shellcheck disable=SC2046
      javac -J-Xmx6g $(javac_opts "$m") --patch-module "$m=$s" -implicit:none --enable-preview -source 26 \
        -nowarn -XDsuppressNotes -d "$work/ref$n/$m" "@$work/log/javac-$m.list" \
        > "$work/log/javac$n-$m.log" 2>&1 || echo "javac $m (run $n) failed, see $work/log/javac$n-$m.log"
    fi
  done
  echo "javac $m: $(find "$work/ref1/$m" -name '*.class' | wc -l) classes"
}

export -f convert_one javac_one module_env javac_opts
export work src
mkdir -p "$work/log"

if has convert; then
  echo "== convert"
  printf '%s\n' $modules | xargs -P 6 -I{} bash -c 'convert_one {}'
fi
if has javac; then
  echo "== javac (twice)"
  printf '%s\n' $modules | xargs -P 16 -I{} bash -c 'javac_one {}'
fi
if has check; then
  echo "== compile the converted forms and compare with javac"
  mkdir -p "$work/res" "$work/chunks"
  for m in $modules; do rm -f "$work/res/$m".*.edn "$work/chunks/$m".*; done
  for m in $modules; do
    [ -d "$work/conv/$m" ] || continue
    (cd "$work/conv/$m" && find . -mindepth 2 -name '*.clj' | sed 's|^\./||' | sort) \
      | split -l 60 -d -a 4 - "$work/chunks/$m."
  done
  for m in $modules; do
    for c in "$work/chunks/$m".*; do [ -e "$c" ] && echo "$m ${c##*.}"; done
  done | xargs -P "$jobs" -L 1 bash -c '
    m=$0 k=$1
    env $(module_env "$m") FILES="$work/chunks/$m.$k" REF="$work/ref1/$m:$work/ref2/$m" \
      CLASS_FORMS_CP="$work/conv/$m" \
      OUT="$work/res/$m.$k.edn" JAVA_OPTS="-Xmx3g -XX:+UseSerialGC --add-modules ALL-SYSTEM" \
      timeout 1800 bin/class-forms-check "$work/conv/$m" > "$work/log/check-$m.$k.log" 2>&1
    [ -s "$work/res/$m.$k.edn" ] || echo "check $m.$k: no result, see $work/log/check-$m.$k.log"'
fi
if has report; then
  echo "== report"
  java -cp . clojure.main test/j2c/jdk_report.clj "$work" $modules
fi
