package vn.springboot.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import vn.springboot.entity.notification.DeviceTokenEntity;
import vn.springboot.entity.user.UserEntity;

import java.util.List;
import java.util.Optional;

@Repository
public interface DeviceTokenRepository extends JpaRepository<DeviceTokenEntity, Long> {

    Optional<DeviceTokenEntity> findByToken(String token);

    List<DeviceTokenEntity> findByUserAndEnabledTrue(UserEntity user);
}
