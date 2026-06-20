package vn.springboot.service.impl;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import vn.springboot.dto.request.notification.SendNotificationCommand;
import vn.springboot.dto.request.notification.UpdateNotificationPreferencesRequest;
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
import vn.springboot.security.CustomUserDetails;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class NotificationServiceImplTest {

    private static final String USERNAME = "bob";
    private static final NotificationType TYPE = NotificationType.ORDER_SUCCESS;

    @Mock NotificationRepository notificationRepository;
    @Mock NotificationPreferenceRepository preferenceRepository;
    @Mock DeviceTokenRepository deviceTokenRepository;
    @Mock UserRepository userRepository;
    @Mock NotificationMapper notificationMapper;
    @Mock SimpMessagingTemplate messagingTemplate;
    @Mock PushSender pushSender;

    @InjectMocks NotificationServiceImpl service;

    @AfterEach
    void clearContext() {
        SecurityContextHolder.clearContext();
    }

    private UserEntity user() {
        UserEntity user = UserEntity.builder().username(USERNAME).build();
        user.setId(1L);
        return user;
    }

    private SendNotificationCommand command() {
        return SendNotificationCommand.builder().type(TYPE).title("Order placed").content("#123").build();
    }

    private void authenticate(UserEntity user) {
        var auth = new UsernamePasswordAuthenticationToken(new CustomUserDetails(user), null, List.of());
        SecurityContextHolder.getContext().setAuthentication(auth);
    }

    @Test
    void notifyUser_whenNotMuted_persistsAndPushes() {
        UserEntity user = user();
        NotificationResponse response = NotificationResponse.builder().id(7L).type(TYPE).build();
        when(userRepository.findByUsername(USERNAME)).thenReturn(Optional.of(user));
        when(preferenceRepository.isInAppMuted(1L, TYPE)).thenReturn(false);
        when(preferenceRepository.isPushMuted(1L, TYPE)).thenReturn(false);
        when(notificationRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(notificationMapper.toResponse(any())).thenReturn(response);
        when(deviceTokenRepository.findByUserAndEnabledTrue(user))
                .thenReturn(List.of(DeviceTokenEntity.builder().token("tok").build()));

        NotificationResponse result = service.notifyUser(USERNAME, command());

        assertEquals(response, result);
        verify(notificationRepository).save(any(NotificationEntity.class));
        verify(messagingTemplate).convertAndSendToUser(eq(USERNAME), eq("/queue/notifications"), eq(response));
        verify(pushSender).sendToDevices(eq(List.of("tok")), eq(response));
    }

    @Test
    void notifyUser_whenFullyMuted_returnsNullAndDoesNothing() {
        UserEntity user = user();
        when(userRepository.findByUsername(USERNAME)).thenReturn(Optional.of(user));
        when(preferenceRepository.isInAppMuted(1L, TYPE)).thenReturn(true);
        when(preferenceRepository.isPushMuted(1L, TYPE)).thenReturn(true);

        NotificationResponse result = service.notifyUser(USERNAME, command());

        assertNull(result);
        verify(notificationRepository, never()).save(any());
        verify(pushSender, never()).sendToDevices(any(), any());
        verify(messagingTemplate, never()).convertAndSendToUser(any(), any(), any(Object.class));
    }

    @Test
    void notifyUser_whenInAppMutedButPushOn_pushesWithoutPersisting() {
        UserEntity user = user();
        when(userRepository.findByUsername(USERNAME)).thenReturn(Optional.of(user));
        when(preferenceRepository.isInAppMuted(1L, TYPE)).thenReturn(true);
        when(preferenceRepository.isPushMuted(1L, TYPE)).thenReturn(false);
        when(deviceTokenRepository.findByUserAndEnabledTrue(user))
                .thenReturn(List.of(DeviceTokenEntity.builder().token("tok").build()));

        NotificationResponse result = service.notifyUser(USERNAME, command());

        assertEquals(TYPE, result.getType());
        verify(notificationRepository, never()).save(any());
        verify(messagingTemplate, never()).convertAndSendToUser(any(), any(), any(Object.class));
        verify(pushSender).sendToDevices(eq(List.of("tok")), any());
    }

    @Test
    void getMyPreferences_defaultsToAllEnabledWhenNoRows() {
        UserEntity user = user();
        authenticate(user);
        when(preferenceRepository.findByUserId(1L)).thenReturn(List.of());

        List<NotificationPreferenceResponse> result = service.getMyPreferences();

        assertEquals(NotificationType.values().length, result.size());
        assertTrue(result.stream().allMatch(p -> p.isInAppEnabled() && p.isPushEnabled()));
    }

    @Test
    void updateMyPreferences_upsertsSuppliedTypes() {
        UserEntity user = user();
        authenticate(user);
        var item = UpdateNotificationPreferencesRequest.Item.builder()
                .type(NotificationType.PROMOTION).inAppEnabled(true).pushEnabled(false).build();
        var request = UpdateNotificationPreferencesRequest.builder().preferences(List.of(item)).build();

        when(preferenceRepository.findByUserIdAndType(1L, NotificationType.PROMOTION)).thenReturn(Optional.empty());
        when(preferenceRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(preferenceRepository.findByUserId(1L)).thenReturn(List.of());

        List<NotificationPreferenceResponse> result = service.updateMyPreferences(request);

        ArgumentCaptor<NotificationPreferenceEntity> captor =
                ArgumentCaptor.forClass(NotificationPreferenceEntity.class);
        verify(preferenceRepository).save(captor.capture());
        NotificationPreferenceEntity saved = captor.getValue();
        assertEquals(1L, saved.getUserId());
        assertEquals(NotificationType.PROMOTION, saved.getType());
        assertTrue(saved.isInAppEnabled());
        assertFalse(saved.isPushEnabled());
        assertEquals(NotificationType.values().length, result.size());
    }
}
