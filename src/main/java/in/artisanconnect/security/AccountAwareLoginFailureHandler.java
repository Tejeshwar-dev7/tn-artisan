package in.artisanconnect.security;

import in.artisanconnect.repo.UserRepository;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.FlashMap;
import org.springframework.web.servlet.support.SessionFlashMapManager;
import java.io.IOException;

@Component
public class AccountAwareLoginFailureHandler implements AuthenticationFailureHandler {
    private final UserRepository users;
    public AccountAwareLoginFailureHandler(UserRepository users) { this.users = users; }
    @Override public void onAuthenticationFailure(HttpServletRequest request, HttpServletResponse response, AuthenticationException exception) throws IOException, ServletException {
        String email = request.getParameter("username");
        String role = request.getParameter("expectedRole");
        if ("ADMIN".equals(role)) { response.sendRedirect("/login/admin?error"); return; }
        if (exception instanceof BadCredentialsException && email != null && !email.isBlank()
                && users.findByEmailIgnoreCase(email.strip()).isEmpty()) {
            String destination = "ARTISAN".equals(role) ? "/register/artisan" : "CUSTOMER".equals(role) ? "/register/customer" : "/register";
            FlashMap flash = new FlashMap();
            flash.setTargetRequestPath(request.getContextPath() + destination);
            flash.put("missingAccountEmail", email.strip());
            new SessionFlashMapManager().saveOutputFlashMap(flash, request, response);
            response.sendRedirect(request.getContextPath() + destination);
        } else {
            String destination = "ARTISAN".equals(role) ? "/login/artisan" : "CUSTOMER".equals(role) ? "/login/customer" : "/login";
            response.sendRedirect(destination + "?error");
        }
    }
}
