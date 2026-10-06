mkdir -p _ && (cd _/ && git clone --depth 1 https://gitlab.ow2.org/asm/asm.git)
mkdir -p _ && (cd _/ && git clone --depth 1 https://github.com/clojure/clojure.git)

(cd _/asm/asm/src/main/java/org/objectweb/ && find asm -name '*.java' | sort | while read f; do echo "s3mm1s3m clojure/$f" && cat -s $f && echo && echo s3mm1s3m; done) > s3mm1s3m.clj
(cd _/asm/asm-commons/src/main/java/org/objectweb/ && find asm -name '*.java' | grep -w 'GeneratorAdapter\|InstructionAdapter\|LocalVariablesSorter\|Method\|TableSwitchGenerator' | sort |
while read f; do echo "s3mm1s3m clojure/$f" && cat -s $f && echo && echo s3mm1s3m; done) >> s3mm1s3m.clj
(cd _/clojure/src/jvm/ && find clojure -name '*.java' | grep -vw clojure/asm | sort | while read f; do echo "s3mm1s3m $f" && cat -s $f && echo && echo s3mm1s3m; done) >> s3mm1s3m.clj
(echo "s3mm1s3m clojure/version.properties" && echo "version=$(sed -n 's#^ *<version>\(.*-master-SNAPSHOT\)</version>$#\1#p' _/clojure/pom.xml)" && echo s3mm1s3m) >> s3mm1s3m.clj
(cd _/clojure/src/clj/ && find clojure -name '*.clj' | grep -vw clojure/parallel | sort | while read f; do echo "s3mm1s3m $f" && cat -s $f && echo && echo s3mm1s3m; done) >> s3mm1s3m.clj

/root/bin/ex s3mm1s3m.clj << 'EOF'
:%s#^\(s3mm1s3m\) \(.*\)/\(.*\)$#mkdir -p \2 \&\& cat > \2/\3 << '\1'#
:%s#\n\ze\ns3mm1s3m$##
:se ts=2
:retab
:%s#\s\+$##
:%s#\<org\.objectweb\>#clojure#g
:%s#\ze(:require \[\%]\%( :as spec\]\)\@<=)#\#_#
:%s#(if spec\n *\zs\ze(#nil \#_#
:%s#\ze\<\(clojure.spec.alpha/\*explain-out\*\) \1#; # 
:%s#\ze(when-let \[fnspec (spec/#nil \#_#
:%s# :doc '\~(spec/describe name)##
:%s#! Boolean.getBoolean("clojure.spec.skip-macros")#false#
:wq
EOF

bash s3mm1s3m.clj
javac -g $(find clojure -name '*.java')

echo '(reduce + (map inc [1 2 3]))' | java clojure.main

rm -fr _/ s3mm1s3m.clj
