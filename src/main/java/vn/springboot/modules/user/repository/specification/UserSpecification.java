package vn.springboot.modules.user.repository.specification;

import org.springframework.data.jpa.domain.Specification;
import vn.springboot.modules.user.dto.request.UserSearchRequest;
import vn.springboot.modules.user.entity.User;

public class UserSpecification {

    public static Specification<User> build(UserSearchRequest request) {
        return Specification
                .allOf(hasUsername(request.getUsername()), hasEmail(request.getEmail()));
    }

    private static Specification<User> hasUsername(String username) {
        return (root, query, criteriaBuilder) -> {
            if (username == null || username.trim().isEmpty()) {
                return criteriaBuilder.conjunction();
            }
            return criteriaBuilder.like(
                    criteriaBuilder.lower(root.get("username")),
                    "%" + username.toLowerCase() + "%"
            );
        };
    }

    private static Specification<User> hasEmail(String email) {
        return (root, query, criteriaBuilder) -> {
            if (email == null || email.trim().isEmpty()) {
                return criteriaBuilder.conjunction();
            }
            return criteriaBuilder.like(
                    criteriaBuilder.lower(root.get("email")),
                    "%" + email.toLowerCase() + "%"
            );
        };
    }
}
