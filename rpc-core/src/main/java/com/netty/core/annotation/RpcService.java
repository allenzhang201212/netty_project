package com.netty.core.annotation;

import java.lang.annotation.*;

@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface RpcService {
    /** 可选：明确暴露的接口；不填则默认取实现类的所有接口 */
    Class<?> value() default void.class;
}