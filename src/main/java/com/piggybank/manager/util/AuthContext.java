package com.piggybank.manager.util;

import com.piggybank.manager.dto.UserPrincipal;

public final class AuthContext {
    private static final ThreadLocal<UserPrincipal> CURRENT = new ThreadLocal<>();

    private AuthContext() {
    }

    public static void set(UserPrincipal principal) {
        CURRENT.set(principal);
    }

    public static UserPrincipal get() {
        return CURRENT.get();
    }

    public static void clear() {
        CURRENT.remove();
    }
}
