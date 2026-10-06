(ns classes.switch-test
  (:require [clojure.test :refer :all]
            [classes.helpers :refer :all]))

(deftest switch-shapes
  (same-shapes? 'classes.switch-test
    {"Col" "public enum Col { RED, GREEN, BLUE }"
     "W" "public class W {
       enum In { A, B }
       static int i(int x) { switch (x) { case 1: return 10; case 2: case 3: return 20; case 100: return 5; default: return 0; } }
       static String e(int x) { return switch (x) { case 1 -> \"a\"; case 5, 6 -> { String t = \"b\"; yield t; } default -> \"z\"; }; }
       static int s(String s) { switch (s) { case \"a\": return 1; case \"b\": case \"c\": return 2; case \"Aa\": case \"BB\": return 3; default: return 0; } }
       static int c(Col c) { switch (c) { case RED: return 1; case GREEN: case BLUE: return 2; default: return 0; } }
       static int in(In c) { switch (c) { case A: return 1; default: return 0; } }
       static int ch(char c) { switch (c) { case 'a': return 1; case 'z': return 2; } return 0; }
     }"}
    '[(^:public ^:enum Col (constants RED GREEN BLUE))
      (^:public W
        (defclass ^:enum In (constants A B))
        (method ^:static i ^int [^int x] (switch x 1 10 (2 3) 20 100 5 0))
        (method ^:static e ^String [^int x]
          (label :sw (switch x 1 "a" (5 6) (let [t "b"] (break :sw t)) "z")))
        (method ^:static s ^int [^String s] (switch s "a" 1 ("b" "c") 2 ("Aa" "BB") 3 0))
        (method ^:static c ^int [^Col c] (switch c RED 1 (GREEN BLUE) 2 0))
        (method ^:static in ^int [^In c] (switch c A 1 0))
        (method ^:static ch ^int [^char c] (switch c \a 1 \z 2) 0))]))

(deftest pattern-shapes
  (same-shapes? 'classes.switch-test
    {"Col2" "public enum Col2 { RED, GREEN }"
     "P" "public class P {
       sealed interface Shape permits Circle, Square {}
       record Circle(double r) implements Shape {}
       record Square(double side) implements Shape {}
       static String t(Object o) {
         return switch (o) {
           case null -> \"null\";
           case String s -> s;
           case Integer i when i > 0 -> \"pos\";
           case Integer i -> \"int\";
           default -> \"other\";
         };
       }
       static double area(Shape s) {
         return switch (s) {
           case Circle c -> Math.PI * c.r() * c.r();
           case Square(double side) -> side * side;
         };
       }
       static int en(Col2 c) { return switch (c) { case null -> -1; case RED -> 1; default -> 0; }; }
       static boolean ii(Object o) {
         if (o instanceof String s && s.length() > 2) return true;
         return o instanceof Circle(double r) && r > 1;
       }
     }"}
    '[(^:public ^:enum Col2 (constants RED GREEN))
      (^:public P
        (defclass ^:sealed ^:interface Shape :permits [Circle Square])
        (defclass ^:record Circle [^double r] :implements [Shape])
        (defclass ^:record Square [^double side] :implements [Shape])
        (method ^:static t ^String [^Object o]
          (switch o
            nil "null"
            [^String s] s
            [^Integer i :when (> (.intValue i) 0)] "pos"
            [^Integer i] "int"
            "other"))
        (method ^:static area ^double [^Shape s]
          (switch s
            [^Circle c] (unchecked-multiply (unchecked-multiply Math/PI (.r c)) (.r c))
            [(Square ^double side)] (unchecked-multiply side side)
            (throw (MatchException. nil nil))))
        (method ^:static en ^int [^Col2 c] (switch c nil -1 RED 1 0))
        (method ^:static ii ^boolean [^Object o]
          (when (if-instance [^String s o] (> (.length s) 2) false) (return true))
          (if-instance [(Circle r) o] (> r 1.0) false)))]
    ))

(deftest switch-behaviour
  (let [[C] (load-forms 'classes.switch-test
              '[(^:public Sw
                  (method ^:public ^:static kind ^String [^Object o]
                    (switch o
                      nil "nil"
                      [^String s :when (.isEmpty s)] "empty"
                      [^String s] (java-str "str:" s)
                      [^Long l] "long"
                      "other"))
                  (method ^:public ^:static day ^String [^int d]
                    (switch d 0 "Sun" (1 2 3 4 5) "work" "Sat"))
                  (method ^:public ^:static word ^int [^String s]
                    (switch s "one" 1 "two" 2 ("Aa" "BB") 99 -1)))])
        call (fn [m & args] (clojure.lang.Reflector/invokeStaticMethod C (name m) (object-array args)))]
    (is (= ["nil" "empty" "str:x" "long" "other"] (map #(call 'kind %) [nil "" "x" 5 1.0])))
    (is (= ["Sun" "work" "Sat"] (map #(call 'day (int %)) [0 3 6])))
    (is (= [1 2 99 99 -1] (map #(call 'word %) ["one" "two" "Aa" "BB" "x"])))))
