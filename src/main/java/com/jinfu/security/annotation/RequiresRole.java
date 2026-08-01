package com.jinfu.security.annotation;

import java.lang.annotation.*;

@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface RequiresRole {

    /**
     * Role key, e.g. "admin"
     */
    String value();

    /**
     * Logical relation between multiple roles (default AND)
     */
    Logical logical() default Logical.AND;

    enum Logical {
        AND, OR
    }
}
