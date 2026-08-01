package com.jinfu.common.constant;

public class SecurityConstants {

    public static final String LOGIN_URL = "/system/auth/login";
    public static final String CAPTCHA_URL = "/system/auth/captcha";
    public static final String LOGOUT_URL = "/system/auth/logout";

    public static final String TOKEN_HEADER = "Authorization";
    public static final String TOKEN_PREFIX = "Bearer ";

    public static final String TOKEN_BLACKLIST_PREFIX = "token:blacklist:";
    public static final String LOGIN_USER_KEY = "login:user:";
    public static final String LOGIN_FAIL_PREFIX = "login:fail:";
    public static final String LOGIN_LOCK_PREFIX = "login:lock:";

    public static final String ROLE_ADMIN = "admin";

    private SecurityConstants() {
    }
}
