package com.teleconnect.iam.repository;

import com.teleconnect.iam.entity.Role;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RoleRepositoryTest {

    @Mock
    private RoleRepository roleRepository;

    @Test
    void findByRoleName_returnsRole() {
        Role role = new Role();
        role.setRoleName("S");

        when(roleRepository.findByRoleName("S"))
                .thenReturn(Optional.of(role));

        Optional<Role> result = roleRepository.findByRoleName("S");

        assertThat(result).isPresent();
        assertThat(result.get().getRoleName()).isEqualTo("S");
    }
}
