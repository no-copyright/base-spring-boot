package vn.springboot.config;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Serves stored files: maps {@code <publicPath>/**} (default {@code /files/**})
 * to the on-disk storage root. Read access is public — see {@code SecurityConfig}.
 */
@Configuration
@RequiredArgsConstructor
@EnableConfigurationProperties(StorageProperties.class)
public class StaticResourceConfig implements WebMvcConfigurer {

    private final StorageProperties properties;

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        Path root = Paths.get(properties.getLocation()).toAbsolutePath().normalize();
        String pattern = properties.getPublicPath() + "/**";
        registry.addResourceHandler(pattern)
                .addResourceLocations(root.toUri().toString());
    }
}
