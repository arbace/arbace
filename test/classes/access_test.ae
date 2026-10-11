(ns classes.access-test
  (:require [arbace.test :refer :all]
            [classes.helpers :refer :all]))

(def base-java "package classes.access_base;
public class Base {
  protected int pf = 1;
  protected int pm() { return 2; }
  protected static int ps() { return 3; }
  public String toString() { return \"b\"; }
}")

(deftest accessors
  ;; the superclass, in another package, defined by the class forms too
  (create-ns 'classes.access-base)
  (load-forms 'classes.access-base
              '[(^:public Base
                  (field ^:protected ^int pf 1)
                  (method ^:protected pm ^int [this] 2)
                  (method ^:protected ^:static ps ^int [] 3)
                  (method ^:public toString ^String [this] "b"))])
  (same-shapes? 'classes.access-test
    {"Base" base-java
     "Sub" "public class Sub extends classes.access_base.Base {
       public String toString() { return \"s\"; }
       class In {
         int a() { return Sub.this.pf + Sub.this.pm() + Sub.ps(); }
         String b() { return Sub.super.toString(); }
         void c() { Sub.this.pf = 5; }
       }
       String own() { return Sub.super.toString(); }
       Runnable r() { return () -> System.out.println(Sub.super.toString()); }
     }"}
    '[(^:public Sub
        :extends classes.access_base.Base
        (method ^:public toString ^String [this] "s")
        (defclass In
          (method a ^int [this] (unchecked-add-int (unchecked-add-int (.-pf Sub/this) (.pm Sub/this)) (Sub/ps)))
          (method b ^String [this] (.toString Sub/super))
          (method c ^void [this] (set! (.-pf Sub/this) 5)))
        (method own ^String [this] (.toString Sub/super))
        (method r ^Runnable [this] (lambda Runnable [] (.println System/out (.toString Sub/super)))))]
    :ignore [["classes/access_base/Base"]]))
