package sample;

import java.lang.annotation.*;

@Target({ElementType.TYPE_USE, ElementType.FIELD}) @Retention(RetentionPolicy.RUNTIME) @interface TypeUseC {}
