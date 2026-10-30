package vn.springboot.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import vn.springboot.modules.user.entity.Permission;
import vn.springboot.modules.user.entity.Role;
import vn.springboot.modules.user.entity.User;
import vn.springboot.modules.user.repository.PermissionRepository;
import vn.springboot.modules.user.repository.RoleRepository;
import vn.springboot.modules.user.repository.UserRepository;

import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Seeds baseline permissions, roles (ADMIN, USER) and an admin account on
 * startup. Idempotent: only missing rows are created. Toggle with
 * {@code app.init.enabled=false}.
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "app.init.enabled", havingValue = "true", matchIfMissing = true)
public class DataInitializer implements CommandLineRunner {

    private static final List<String> PERMISSIONS = List.of(
            "USER_READ", "USER_WRITE", "USER_DELETE",
            "ROLE_READ", "ROLE_WRITE");

    private final PermissionRepository permissionRepository;
    private final RoleRepository roleRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${app.init.admin-username:admin}")
    private String adminUsername;

    @Value("${app.init.admin-email:admin@springboot.vn}")
    private String adminEmail;

    @Value("${app.init.admin-password:admin123}")
    private String adminPassword;

    @Override
    @Transactional
    public void run(String... args) {
        Set<Permission> allPermissions = seedPermissions();
        Role adminRole = seedRole("ADMIN", "Full system access", allPermissions);
        seedRole("USER", "Standard user", filter(allPermissions, "USER_READ"));
        seedAdminUser(adminRole);
    }

    private Set<Permission> seedPermissions() {
        return PERMISSIONS.stream()
                .map(name -> permissionRepository.findByName(name)
                        .orElseGet(() -> permissionRepository.save(Permission.builder()
                                .name(name)
                                .description(name.replace('_', ' ').toLowerCase())
                                .build())))
                .collect(Collectors.toSet());
    }

    private Role seedRole(String name, String description, Set<Permission> permissions) {
        return roleRepository.findByName(name)
                .orElseGet(() -> roleRepository.save(Role.builder()
                        .name(name)
                        .description(description)
                        .permissions(permissions)
                        .build()));
    }

    private void seedAdminUser(Role adminRole) {
        if (userRepository.existsByUsername(adminUsername)) {
            return;
        }
        userRepository.save(User.builder()
                .username(adminUsername)
                .email(adminEmail)
                .password(passwordEncoder.encode(adminPassword))
                .fullName("Administrator")
                .enabled(true)
                .roles(Set.of(adminRole))
                .build());
        log.info("Seeded admin user '{}' (change the default password!)", adminUsername);
    }

    private Set<Permission> filter(Set<Permission> permissions, String... names) {
        Set<String> wanted = Set.copyOf(Arrays.asList(names));
        return permissions.stream()
                .filter(p -> wanted.contains(p.getName()))
                .collect(Collectors.toSet());
    }
}
