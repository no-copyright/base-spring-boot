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
import vn.springboot.dto.response.PageResponse;
import vn.springboot.dto.response.notification.NotificationResponse;
import vn.springboot.entity.notification.DeviceTokenEntity;
import vn.springboot.entity.notification.NotificationEntity;
import vn.springboot.entity.user.UserEntity;
import vn.springboot.mapper.NotificationMapper;
import vn.springboot.notification.push.PushSender;
import vn.springboot.repository.DeviceTokenRepository;
import vn.springboot.repository.NotificationRepository;
import vn.springboot.repository.UserRepository;
import vn.springboot.repository.specification.NotificationSpecification;
import vn.springboot.security.SecurityUtils;
import vn.springboot.service.NotificationService;

import java.time.Instant;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationServiceImpl implements NotificationService {

    private static final String USER_QUEUE = "/queue/notifications";
    private static final String BROADCAST_TOPIC = "/topic/notifications";
    private static final int MAX_PAGE_SIZE = 100;

    private final NotificationRepository notificationRepository;
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

        NotificationEntity entity = NotificationEntity.builder()
                .recipient(recipient)
                .type(command.getType())
                .title(command.getTitle())
                .content(command.getContent())
                .icon(command.getIcon())
                .imageUrl(command.getImageUrl())
                .linkUrl(command.getLinkUrl())
                .metadata(notificationMapper.writeJson(command.getData()))
                .read(false)
                .build();

        NotificationResponse response = notificationMapper.toResponse(notificationRepository.save(entity));
        dispatch(recipient, response);
        return response;
    }

    @Override
    public void broadcast(SendNotificationCommand command) {
        NotificationResponse response = NotificationResponse.builder()
                .type(command.getType())
                .category(command.getType() != null ? command.getType().getCategory() : null)
                .icon(command.getIcon() != null ? command.getIcon()
                        : (command.getType() != null ? command.getType().getIcon() : null))
                .title(command.getTitle())
                .content(command.getContent())
                .imageUrl(command.getImageUrl())
                .linkUrl(command.getLinkUrl())
                .data(command.getData())
                .read(false)
                .createdAt(Instant.now())
                .build();
        messagingTemplate.convertAndSend(BROADCAST_TOPIC, response);
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

    /** Real-time fan-out: WebSocket to the user's session(s), plus push to their devices. */
    private void dispatch(UserEntity recipient, NotificationResponse response) {
        messagingTemplate.convertAndSendToUser(recipient.getUsername(), USER_QUEUE, response);

        List<String> tokens = deviceTokenRepository.findByUserAndEnabledTrue(recipient).stream()
                .map(DeviceTokenEntity::getToken)
                .toList();
        if (!tokens.isEmpty()) {
            pushSender.sendToDevices(tokens, response);
        }
    }
}
