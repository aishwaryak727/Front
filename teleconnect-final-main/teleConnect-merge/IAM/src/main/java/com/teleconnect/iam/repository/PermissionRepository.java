package com.teleconnect.iam.repository;

import com.teleconnect.iam.entity.Permission;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface PermissionRepository extends JpaRepository<Permission, Integer> {
    Optional<Permission> findByPermissionName(String permissionName);
}
