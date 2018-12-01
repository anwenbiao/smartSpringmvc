package com.awb.mvcframework.annotation;

import java.lang.annotation.*;


//@Target(ElementType.TYPE)   //接口、类、枚举、注解
// @Target(ElementType.FIELD) //字段、枚举的常量
// @Target(ElementType.METHOD) //方法
// @Target(ElementType.PARAMETER) //方法参数
//@Target(ElementType.CONSTRUCTOR)  //构造函数
// @Target(ElementType.LOCAL_VARIABLE)//局部变量
//@Target(ElementType.ANNOTATION_TYPE)//注解
//　@Target(ElementType.PACKAGE) ///包   

@Target({ElementType.TYPE,ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface RequestMapping {
    String value() default "";
}
