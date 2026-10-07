package sample;

import java.lang.annotation.*;

@Target(ElementType.TYPE_USE) @interface TypeUseB { int value() default 0; }
