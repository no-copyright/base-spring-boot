package vn.springboot.security;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import vn.springboot.common.exception.AppException;
import vn.springboot.common.exception.ErrorCode;
import vn.springboot.entity.user.UserEntity;

/**
 * Helpers for reading the authenticated principal from the security context.
 */
public final class SecurityUtils {

    private SecurityUtils() {
    }

    /** The currently authenticated user, or throws {@code UNAUTHENTICATED}. */
    public static UserEntity currentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof CustomUserDetails principal)) {
            throw new AppException(ErrorCode.UNAUTHENTICATED);
        }
        return principal.getUser();
    }
}
