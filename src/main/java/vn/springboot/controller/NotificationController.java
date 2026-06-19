package vn.springboot.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import vn.springboot.common.response.ApiResponse;
import vn.springboot.dto.request.notification.DeviceTokenRequest;
import vn.springboot.dto.request.notification.NotificationSearchRequest;
import vn.springboot.dto.response.PageResponse;
import vn.springboot.dto.response.notification.NotificationResponse;
import vn.springboot.dto.response.notification.UnreadCountResponse;
import vn.springboot.service.NotificationService;

/**
 * Notification centre for the authenticated user: list, unread badge, mark-read
 * and device-token registration (push groundwork). All endpoints operate on the
 * current user only.
 */
@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;

    @GetMapping
    public ApiResponse<PageResponse<NotificationResponse>> list(@ModelAttribute NotificationSearchRequest request) {
        return ApiResponse.success(notificationService.getMyNotifications(request));
    }

    @GetMapping("/unread-count")
    public ApiResponse<UnreadCountResponse> unreadCount() {
        return ApiResponse.success(new UnreadCountResponse(notificationService.countMyUnread()));
    }

    @PatchMapping("/{id}/read")
    public ApiResponse<Void> markAsRead(@PathVariable Long id) {
        notificationService.markAsRead(id);
        return ApiResponse.success("Marked as read", null);
    }

    @PatchMapping("/read-all")
    public ApiResponse<Void> markAllAsRead() {
        notificationService.markAllAsRead();
        return ApiResponse.success("All marked as read", null);
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        notificationService.delete(id);
        return ApiResponse.success("Deleted", null);
    }

    // --- Device tokens (FCM groundwork) -----------------------------------

    @PostMapping("/devices")
    public ApiResponse<Void> registerDevice(@Valid @RequestBody DeviceTokenRequest request) {
        notificationService.registerDeviceToken(request);
        return ApiResponse.success("Device registered", null);
    }

    @DeleteMapping("/devices/{token}")
    public ApiResponse<Void> unregisterDevice(@PathVariable String token) {
        notificationService.unregisterDeviceToken(token);
        return ApiResponse.success("Device unregistered", null);
    }
}
