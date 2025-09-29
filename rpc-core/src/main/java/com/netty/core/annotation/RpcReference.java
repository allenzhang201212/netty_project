package com.netty.core.annotation;

import java.lang.annotation.*;

@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface RpcReference {
    /** 可选：当字段是 Object 或泛型不明确时可指定接口类型 */
    Class<?> interfaceClass() default void.class;
}