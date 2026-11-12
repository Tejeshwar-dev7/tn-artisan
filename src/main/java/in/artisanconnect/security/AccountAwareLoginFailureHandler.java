package in.artisanconnect.security;

import in.artisanconnect.repo.UserRepository;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriUtils;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

@Component
public class AccountAwareLoginFailureHandler implements AuthenticationFailureHandler {
    private final UserRepository users;
    public AccountAwareLoginFailureHandler(UserRepository users) { this.users = users; }
    @Override public void onAuthenticationFailure(HttpServletRequest request, HttpServletResponse response, AuthenticationException exception) throws IOException, ServletException {
        String email = request.getParameter("username");
        if ("ADMIN".equals(request.getParameter("expectedRole"))) { response.sendRedirect("/login/admin?error"); return; }
        if (email != null && users.findByEmailIgnoreCase(email.trim()).isEmpty()) {
            String role = request.getParameter("expectedRole");
            String destination = "ARTISAN".equals(role) ? "/register/artisan" : "CUSTOMER".equals(role) ? "/register/customer" : "/register";
            response.sendRedirect(destination + "?email=" + UriUtils.encode(email.trim(), StandardCharsets.UTF_8));
        } else {
            String role = request.getParameter("expectedRole");
            String destination = "ARTISAN".equals(role) ? "/login/artisan" : "CUSTOMER".equals(role) ? "/login/customer" : "/login";
            response.sendRedirect(destination + "?error");
        }
    }
}
