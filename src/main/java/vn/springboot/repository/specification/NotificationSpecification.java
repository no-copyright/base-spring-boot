package vn.springboot.repository.specification;

import org.springframework.data.jpa.domain.Specification;
import vn.springboot.dto.request.notification.NotificationSearchRequest;
import vn.springboot.entity.notification.NotificationEntity;
import vn.springboot.entity.user.UserEntity;
import vn.springboot.enums.NotificationType;

public class NotificationSpecification {

    public static Specification<NotificationEntity> build(UserEntity recipient, NotificationSearchRequest request) {
        return Specification.allOf(
                forRecipient(recipient),
                hasType(request.getType()),
                hasReadState(request.getRead()));
    }

    private static Specification<NotificationEntity> forRecipient(UserEntity recipient) {
        return (root, query, cb) -> cb.equal(root.get("recipient"), recipient);
    }

    private static Specification<NotificationEntity> hasType(NotificationType type) {
        return (root, query, cb) -> type == null ? cb.conjunction() : cb.equal(root.get("type"), type);
    }

    private static Specification<NotificationEntity> hasReadState(Boolean read) {
        return (root, query, cb) -> read == null ? cb.conjunction() : cb.equal(root.get("read"), read);
    }
}
