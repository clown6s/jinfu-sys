package com.jinfu.security.annotation;

import java.lang.annotation.*;

@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface RequiresPermission {

    /**
     * Permission code, e.g. "system:user:add"
     */
    String value();

    /**
     * Logical relation between multiple permissions (default AND)
     */
    Logical logical() default Logical.AND;

    enum Logical {
        AND, OR
    }
}
