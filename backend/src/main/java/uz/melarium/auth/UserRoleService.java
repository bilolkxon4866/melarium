package uz.melarium.auth;

import java.util.List;
import java.util.UUID;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserRoleService {
    private final JdbcTemplate jdbcTemplate;

    public UserRoleService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public List<String> rolesFor(UUID userId) {
        return jdbcTemplate.queryForList(
                "select r.code from roles r join user_roles ur on ur.role_id = r.id where ur.user_id = ? order by r.code",
                String.class,
                userId
        );
    }

    public long countUsersWithRole(String roleCode) {
        Long count = jdbcTemplate.queryForObject(
                "select count(distinct ur.user_id) from user_roles ur join roles r on r.id = ur.role_id where r.code = ?",
                Long.class,
                roleCode
        );
        return count == null ? 0L : count;
    }

    @Transactional
    public void assignIfAbsent(UUID userId, String roleCode, UUID assignedBy) {
        Integer roleId = jdbcTemplate.queryForObject("select id from roles where code = ?", Integer.class, roleCode);
        if (roleId == null) throw new IllegalArgumentException("Noma’lum rol: " + roleCode);
        jdbcTemplate.update(
                "insert into user_roles(user_id, role_id, assigned_by) values (?, ?, ?) on conflict (user_id, role_id) do nothing",
                userId, roleId, assignedBy
        );
    }

    @Transactional
    public void replaceRoles(UUID userId, List<String> roleCodes, UUID assignedBy) {
        List<String> safeRoles = roleCodes.stream().distinct().toList();
        if (safeRoles.isEmpty()) throw new IllegalArgumentException("Kamida bitta rol kerak");
        String placeholders = String.join(",", java.util.Collections.nCopies(safeRoles.size(), "?"));
        List<String> valid = jdbcTemplate.queryForList("select code from roles where code in (" + placeholders + ")", String.class, safeRoles.toArray());
        if (valid.size() != safeRoles.size()) throw new IllegalArgumentException("Rol kodi noto‘g‘ri");
        jdbcTemplate.update("delete from user_roles where user_id = ?", userId);
        for (String role : safeRoles) assignIfAbsent(userId, role, assignedBy);
    }
}
