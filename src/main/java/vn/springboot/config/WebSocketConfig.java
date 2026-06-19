package vn.springboot.config;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;
import vn.springboot.websocket.StompAuthChannelInterceptor;

/**
 * STOMP-over-WebSocket configuration for real-time notifications.
 *
 * <ul>
 *   <li>Handshake endpoint: {@code /ws} (SockJS fallback enabled).</li>
 *   <li>Broadcast destinations: {@code /topic/**}.</li>
 *   <li>Per-user destinations: {@code /user/queue/**} (Spring routes
 *       {@code /user/{username}/queue/...} to the matching session).</li>
 *   <li>Client -> server messages are prefixed {@code /app}.</li>
 * </ul>
 *
 * Authentication is handled by {@link StompAuthChannelInterceptor} on the
 * inbound channel (JWT in the STOMP CONNECT frame).
 */
@Configuration
@EnableWebSocketMessageBroker
@RequiredArgsConstructor
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    private final StompAuthChannelInterceptor stompAuthChannelInterceptor;

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint("/ws")
                .setAllowedOriginPatterns("*")
                .withSockJS();
    }

    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        // Simple in-memory broker. Swap for a relay (RabbitMQ/ActiveMQ) when scaling out.
        registry.enableSimpleBroker("/topic", "/queue");
        registry.setApplicationDestinationPrefixes("/app");
        registry.setUserDestinationPrefix("/user");
    }

    @Override
    public void configureClientInboundChannel(ChannelRegistration registration) {
        registration.interceptors(stompAuthChannelInterceptor);
    }
}
