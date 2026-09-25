package me.ardelys.hac.checks;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface CheckInfo {
    String name();
    CheckType type();
    String description() default "";
    double defaultThreshold() default 15.0;
    double defaultDecay() default 0.5;
}
