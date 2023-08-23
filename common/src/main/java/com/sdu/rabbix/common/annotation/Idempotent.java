package com.sdu.rabbix.common.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.concurrent.TimeUnit;

/**
 * 幂等注解
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface Idempotent {

    /**
     * key的前缀，默认取方法全限定名
     *
     * @return key的前缀
     */
    String prefix() default "";

    String key() default "";

    /**
     * 等待锁的时间，默认-1，不等待直接失败,redisson默认也是-1
     *
     * @return 单位秒
     */
    long waitTime() default -1;

    /**
     * 等待锁的时间单位，默认秒
     *
     * @return 单位
     */
    TimeUnit unit() default TimeUnit.SECONDS;
}
