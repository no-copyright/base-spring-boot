package vn.springboot.mapper;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import vn.springboot.dto.response.notification.NotificationResponse;
import vn.springboot.entity.notification.NotificationEntity;

/**
 * Maps {@link NotificationEntity} to {@link NotificationResponse}, resolving the
 * effective icon and (de)serializing the JSON {@code metadata} payload.
 * Hand-written (not MapStruct) because of the JSON handling.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class NotificationMapper {

    private final ObjectMapper objectMapper;

    public NotificationResponse toResponse(NotificationEntity entity) {
        return NotificationResponse.builder()
                .id(entity.getId())
                .type(entity.getType())
                .category(entity.getType() != null ? entity.getType().getCategory() : null)
                .icon(entity.resolveIcon())
                .title(entity.getTitle())
                .content(entity.getContent())
                .imageUrl(entity.getImageUrl())
                .linkUrl(entity.getLinkUrl())
                .data(readJson(entity.getMetadata()))
                .read(entity.isRead())
                .readAt(entity.getReadAt())
                .createdAt(entity.getCreatedAt())
                .build();
    }

    /** Serialize an arbitrary payload to a JSON string for storage (null-safe). */
    public String writeJson(Object data) {
        if (data == null) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(data);
        } catch (JsonProcessingException ex) {
            log.warn("Failed to serialize notification metadata: {}", ex.getMessage());
            return null;
        }
    }

    private Object readJson(String json) {
        if (json == null || json.isBlank()) {
            return null;
        }
        try {
            return objectMapper.readValue(json, Object.class);
        } catch (JsonProcessingException ex) {
            log.warn("Failed to parse notification metadata: {}", ex.getMessage());
            return null;
        }
    }
}
