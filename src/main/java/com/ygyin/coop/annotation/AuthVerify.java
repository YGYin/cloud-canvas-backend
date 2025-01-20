package com.ygyin.coop.annotation;


import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface AuthVerify {

    /**
     * 调用接口所需的角色权限
     */
    String requiredRole() default "";
}
