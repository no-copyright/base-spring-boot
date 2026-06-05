package vn.springboot.repository.specification;

import org.springframework.data.jpa.domain.Specification;
import vn.springboot.dto.request.user.UserSearchRequest;
import vn.springboot.entity.user.UserEntity;

public class UserSpecification {

    public static Specification<UserEntity> build(UserSearchRequest request) {
        return Specification
                .allOf(hasUsername(request.getUsername()), hasEmail(request.getEmail()));
    }

    private static Specification<UserEntity> hasUsername(String username) {
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

    private static Specification<UserEntity> hasEmail(String email) {
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
