package uz.melarium.api;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.authorization.AuthorizationDeniedException;

class ApiExceptionHandlerTest {
    @Test
    void authorizationDeniedReturnsForbiddenInsteadOfServerError() {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/admin/system/status");

        var response = new ApiExceptionHandler().handleAccessDenied(
                new AuthorizationDeniedException("Access Denied", () -> false), request);

        assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(403, response.getBody().status());
        assertEquals("/api/v1/admin/system/status", response.getBody().path());
    }
}
