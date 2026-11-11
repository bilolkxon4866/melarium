package uz.melarium.auth;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.JdbcTemplate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserRoleServiceTest {
    @Mock JdbcTemplate jdbcTemplate;

    @Test
    void superAdminAuthenticationGrantIncludesAdminRole() {
        List<String> lookedUpRoles = new ArrayList<>();
        when(jdbcTemplate.queryForObject(anyString(), eq(Integer.class), any(Object[].class)))
                .thenAnswer(invocation -> {
                    lookedUpRoles.add(invocation.getArgument(2).toString());
                    return lookedUpRoles.size();
                });

        new UserRoleService(jdbcTemplate).syncAuthenticationRoles(UUID.randomUUID(), "SUPER_ADMIN");

        assertEquals(List.of("ADMIN", "SUPER_ADMIN"), lookedUpRoles);
    }
}
