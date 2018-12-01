package com.awb.mvcframework.annotation;

import java.lang.annotation.*;

@Target(ElementType.PARAMETER)
@Documented
@Retention(RetentionPolicy.RUNTIME)
public @interface RequestParam {
    String value() default "";
}
