package com.teleconnect.iam.repository;

import com.teleconnect.iam.entity.Permission;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PermissionRepositoryTest {

    @Mock
    private PermissionRepository permissionRepository;

    @Test
    void findById_returnsPermission() {
        Permission permission = new Permission();
        permission.setPermissionId(1);
        permission.setPermissionName("VIEW_ALL_USERS");

        when(permissionRepository.findById(1)).thenReturn(Optional.of(permission));

        Optional<Permission> result = permissionRepository.findById(1);

        assertThat(result).isPresent();
        assertThat(result.get().getPermissionName()).isEqualTo("VIEW_ALL_USERS");
    }
}
