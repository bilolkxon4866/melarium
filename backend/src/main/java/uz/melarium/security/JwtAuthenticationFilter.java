package uz.melarium.security;

import java.io.IOException;
import java.util.List;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import uz.melarium.auth.UserRoleService;
import uz.melarium.domain.UserAccount;
import uz.melarium.repository.UserAccountRepository;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {
    private final JwtService jwtService;
    private final ObjectMapper objectMapper;
    private final UserAccountRepository userRepository;
    private final UserRoleService roleService;

    public JwtAuthenticationFilter(JwtService jwtService, ObjectMapper objectMapper,
                                   UserAccountRepository userRepository, UserRoleService roleService) {
        this.jwtService = jwtService;
        this.objectMapper = objectMapper;
        this.userRepository = userRepository;
        this.roleService = roleService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String header = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (header == null || !header.startsWith("Bearer ")) {
            chain.doFilter(request, response);
            return;
        }

        AuthenticatedUser claimed;
        try {
            claimed = jwtService.parseAccessToken(header.substring(7).trim());
        } catch (JwtException | IllegalArgumentException exception) {
            unauthorized(response, "Access token yaroqsiz yoki muddati tugagan");
            return;
        }

        UserAccount account = userRepository.findById(claimed.id()).orElse(null);
        if (account == null || !"ACTIVE".equals(account.getAccountStatus())) {
            unauthorized(response, "Hisob faol emas yoki mavjud emas");
            return;
        }
        List<String> currentRoles = roleService.rolesFor(claimed.id());
        if (currentRoles.isEmpty()) {
            unauthorized(response, "Foydalanuvchiga rol tayinlanmagan");
            return;
        }
        AuthenticatedUser user = new AuthenticatedUser(claimed.id(), currentRoles);
        List<SimpleGrantedAuthority> authorities = currentRoles.stream()
                .map(role -> new SimpleGrantedAuthority("ROLE_" + role))
                .toList();
        var authentication = new UsernamePasswordAuthenticationToken(user, null, authorities);
        SecurityContextHolder.getContext().setAuthentication(authentication);
        chain.doFilter(request, response);
    }

    private void unauthorized(HttpServletResponse response, String message) throws IOException {
        SecurityContextHolder.clearContext();
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        objectMapper.writeValue(response.getOutputStream(), java.util.Map.of("message", message));
    }
}
