package vn.springboot.enums;

import lombok.Getter;

/**
 * Catalogue of notification kinds. Each type carries a stable {@code icon} key
 * and a {@code category} so the front-end can render a consistent, production
 * look (icon + grouping) without hard-coding per-type logic.
 *
 * <p>The {@code icon} is an abstract key (not a file path) that the FE maps to
 * its own icon set — keep these names stable once shipped.
 */
@Getter
public enum NotificationType {

    PROMOTION("gift", "promotion"),
    ORDER_SUCCESS("shopping-bag", "order"),
    ORDER_CANCELLED("x-circle", "order"),
    SHIPPING("truck", "order"),
    DELIVERED("package-check", "order"),
    PAYMENT("credit-card", "order"),
    NEWS("newspaper", "news"),
    SECURITY("shield", "system"),
    ACCOUNT("user", "account"),
    SYSTEM("bell", "system");

    /** Abstract icon key for the FE to map to its icon set. */
    private final String icon;

    /** High-level grouping (e.g. order / promotion / system). */
    private final String category;

    NotificationType(String icon, String category) {
        this.icon = icon;
        this.category = category;
    }
}
