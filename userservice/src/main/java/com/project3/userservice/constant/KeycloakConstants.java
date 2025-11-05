package com.project3.userservice.constant;

public final class KeycloakConstants {
    private KeycloakConstants() {
        throw new UnsupportedOperationException("Utility class");
    }
    
    public static final String VERIFY_EMAIL_ACTION = "VERIFY_EMAIL";
    public static final String GRANT_TYPE_CLIENT_CREDENTIALS = "client_credentials";
    public static final String GRANT_TYPE_PASSWORD = "password";
    public static final String GRANT_TYPE_AUTHORIZATION_CODE = "authorization_code";
    public static final String SCOPE_OPENID = "openid";
    public static final String SCOPE_PROFILE = "profile";
    public static final String SCOPE_EMAIL = "email";
    public static final String BEARER_PREFIX = "Bearer ";
    public static final String REALMS_PATH = "/realms/";
    public static final String AUTH_REALMS_PATH = "/auth/realms/";
    public static final String ADMIN_REALMS_PATH = "/admin/realms/";
}

