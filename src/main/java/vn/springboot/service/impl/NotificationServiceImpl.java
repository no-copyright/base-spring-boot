package vn.springboot.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.springboot.common.exception.AppException;
import vn.springboot.common.exception.ErrorCode;
import vn.springboot.dto.request.notification.DeviceTokenRequest;
import vn.springboot.dto.request.notification.NotificationSearchRequest;
import vn.springboot.dto.request.notification.SendNotificationCommand;
import vn.springboot.dto.request.notification.UpdateNotificationPreferencesRequest;
import vn.springboot.dto.response.PageResponse;
import vn.springboot.dto.response.notification.NotificationPreferenceResponse;
import vn.springboot.dto.response.notification.NotificationResponse;
import vn.springboot.entity.notification.DeviceTokenEntity;
import vn.springboot.entity.notification.NotificationEntity;
import vn.springboot.entity.notification.NotificationPreferenceEntity;
import vn.springboot.entity.user.UserEntity;
import vn.springboot.enums.NotificationType;
import vn.springboot.mapper.NotificationMapper;
import vn.springboot.notification.push.PushSender;
import vn.springboot.repository.DeviceTokenRepository;
import vn.springboot.repository.NotificationPreferenceRepository;
import vn.springboot.repository.NotificationRepository;
import vn.springboot.repository.UserRepository;
import vn.springboot.repository.specification.NotificationSpecification;
import vn.springboot.security.SecurityUtils;
import vn.springboot.service.NotificationService;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationServiceImpl implements NotificationService {

    private static final String USER_QUEUE = "/queue/notifications";
    private static final String BROADCAST_TOPIC = "/topic/notifications";
    private static final int MAX_PAGE_SIZE = 100;

    private final NotificationRepository notificationRepository;
    private final NotificationPreferenceRepository preferenceRepository;
    private final DeviceTokenRepository deviceTokenRepository;
    private final UserRepository userRepository;
    private final NotificationMapper notificationMapper;
    private final SimpMessagingTemplate messagingTemplate;
    private final PushSender pushSender;

    @Override
    @Transactional
    public NotificationResponse notifyUser(String username, SendNotificationCommand command) {
        UserEntity recipient = userRepository.findByUsername(username)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));

        Long userId = recipient.getId();
        NotificationType type = command.getType();
        boolean inAppMuted = preferenceRepository.isInAppMuted(userId, type);
        boolean pushMuted = preferenceRepository.isPushMuted(userId, type);

        if (inAppMuted && pushMuted) {
            log.debug("Notification type {} fully muted for user {}", type, username);
            return null;
        }

        NotificationResponse response;
        if (inAppMuted) {
            // User opted out of the in-app centre but still wants push: don't persist.
            response = toTransientResponse(command);
        } else {
            NotificationEntity entity = NotificationEntity.builder()
                    .recipient(recipient)
                    .type(type)
                    .title(command.getTitle())
                    .content(command.getContent())
                    .icon(command.getIcon())
                    .imageUrl(command.getImageUrl())
                    .linkUrl(command.getLinkUrl())
                    .metadata(notificationMapper.writeJson(command.getData()))
                    .read(false)
                    .build();
            response = notificationMapper.toResponse(notificationRepository.save(entity));
            messagingTemplate.convertAndSendToUser(recipient.getUsername(), USER_QUEUE, response);
        }

        if (!pushMuted) {
            pushToDevices(recipient, response);
        }
        return response;
    }

    @Override
    public void broadcast(SendNotificationCommand command) {
        messagingTemplate.convertAndSend(BROADCAST_TOPIC, toTransientResponse(command));
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<NotificationResponse> getMyNotifications(NotificationSearchRequest request) {
        UserEntity me = SecurityUtils.currentUser();

        Sort.Direction direction = "ASC".equalsIgnoreCase(request.getSortDirection())
                ? Sort.Direction.ASC
                : Sort.Direction.DESC;
        Pageable pageable = PageRequest.of(
                Math.max(0, request.getPage()),
                Math.clamp(request.getSize(), 1, MAX_PAGE_SIZE),
                Sort.by(direction, "createdAt"));

        Specification<NotificationEntity> spec = NotificationSpecification.build(me, request);
        Page<NotificationEntity> page = notificationRepository.findAll(spec, pageable);

        List<NotificationResponse> content = page.getContent().stream()
                .map(notificationMapper::toResponse)
                .toList();

        return PageResponse.<NotificationResponse>builder()
                .content(content)
                .pageNumber(page.getNumber())
                .pageSize(page.getSize())
                .totalElements(page.getTotalElements())
                .totalPages(page.getTotalPages())
                .first(page.isFirst())
                .last(page.isLast())
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public long countMyUnread() {
        return notificationRepository.countByRecipientAndReadFalse(SecurityUtils.currentUser());
    }

    @Override
    @Transactional
    public void markAsRead(Long id) {
        UserEntity me = SecurityUtils.currentUser();
        NotificationEntity notification = notificationRepository.findByIdAndRecipient(id, me)
                .orElseThrow(() -> new AppException(ErrorCode.NOTIFICATION_NOT_FOUND));
        if (!notification.isRead()) {
            notification.setRead(true);
            notification.setReadAt(Instant.now());
            notificationRepository.save(notification);
        }
    }

    @Override
    @Transactional
    public void markAllAsRead() {
        notificationRepository.markAllReadByRecipient(SecurityUtils.currentUser(), Instant.now());
    }

    @Override
    @Transactional
    public void delete(Long id) {
        UserEntity me = SecurityUtils.currentUser();
        NotificationEntity notification = notificationRepository.findByIdAndRecipient(id, me)
                .orElseThrow(() -> new AppException(ErrorCode.NOTIFICATION_NOT_FOUND));
        notificationRepository.delete(notification);
    }

    @Override
    @Transactional(readOnly = true)
    public List<NotificationPreferenceResponse> getMyPreferences() {
        Long userId = SecurityUtils.currentUser().getId();
        Map<NotificationType, NotificationPreferenceEntity> saved = new HashMap<>();
        for (NotificationPreferenceEntity p : preferenceRepository.findByUserId(userId)) {
            saved.put(p.getType(), p);
        }

        List<NotificationPreferenceResponse> result = new ArrayList<>(NotificationType.values().length);
        for (NotificationType type : NotificationType.values()) {
            NotificationPreferenceEntity p = saved.get(type);
            result.add(NotificationPreferenceResponse.builder()
                    .type(type)
                    .category(type.getCategory())
                    .icon(type.getIcon())
                    .inAppEnabled(p == null || p.isInAppEnabled())
                    .pushEnabled(p == null || p.isPushEnabled())
                    .build());
        }
        return result;
    }

    @Override
    @Transactional
    public List<NotificationPreferenceResponse> updateMyPreferences(UpdateNotificationPreferencesRequest request) {
        Long userId = SecurityUtils.currentUser().getId();
        for (UpdateNotificationPreferencesRequest.Item item : request.getPreferences()) {
            NotificationPreferenceEntity entity = preferenceRepository
                    .findByUserIdAndType(userId, item.getType())
                    .orElseGet(() -> NotificationPreferenceEntity.builder()
                            .userId(userId)
                            .type(item.getType())
                            .build());
            entity.setInAppEnabled(item.getInAppEnabled());
            entity.setPushEnabled(item.getPushEnabled());
            preferenceRepository.save(entity);
        }
        return getMyPreferences();
    }

    @Override
    @Transactional
    public void registerDeviceToken(DeviceTokenRequest request) {
        UserEntity me = SecurityUtils.currentUser();
        DeviceTokenEntity entity = deviceTokenRepository.findByToken(request.getToken())
                .map(existing -> {
                    existing.setUser(me);
                    existing.setPlatform(request.getPlatform());
                    existing.setEnabled(true);
                    return existing;
                })
                .orElseGet(() -> DeviceTokenEntity.builder()
                        .user(me)
                        .token(request.getToken())
                        .platform(request.getPlatform())
                        .enabled(true)
                        .build());
        deviceTokenRepository.save(entity);
    }

    @Override
    @Transactional
    public void unregisterDeviceToken(String token) {
        deviceTokenRepository.findByToken(token).ifPresent(deviceTokenRepository::delete);
    }

    /** Push fan-out to the recipient's enabled devices (FCM). No-op when none registered. */
    private void pushToDevices(UserEntity recipient, NotificationResponse response) {
        List<String> tokens = deviceTokenRepository.findByUserAndEnabledTrue(recipient).stream()
                .map(DeviceTokenEntity::getToken)
                .toList();
        if (!tokens.isEmpty()) {
            pushSender.sendToDevices(tokens, response);
        }
    }

    /** Build a non-persisted response from a command (broadcast / push-only paths). */
    private NotificationResponse toTransientResponse(SendNotificationCommand command) {
        NotificationType type = command.getType();
        return NotificationResponse.builder()
                .type(type)
                .category(type != null ? type.getCategory() : null)
                .icon(command.getIcon() != null ? command.getIcon()
                        : (type != null ? type.getIcon() : null))
                .title(command.getTitle())
                .content(command.getContent())
                .imageUrl(command.getImageUrl())
                .linkUrl(command.getLinkUrl())
                .data(command.getData())
                .read(false)
                .createdAt(Instant.now())
                .build();
    }
}
