package in.artisanconnect.security;

import in.artisanconnect.repo.UserRepository;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

@Component
public class AccountAwareLoginSuccessHandler implements AuthenticationSuccessHandler {
 private final UserRepository users;
 public AccountAwareLoginSuccessHandler(UserRepository users){this.users=users;}
 @Override public void onAuthenticationSuccess(HttpServletRequest request,HttpServletResponse response,Authentication authentication)throws IOException,ServletException{
  String expected=request.getParameter("expectedRole");
  if(expected!=null&&!expected.isBlank()&&users.findByEmailIgnoreCase(authentication.getName()).map(u->!u.role.name().equals(expected)).orElse(true)){
   new org.springframework.security.web.authentication.logout.SecurityContextLogoutHandler().logout(request,response,authentication);
   response.sendRedirect("/login/"+("ADMIN".equals(expected)?"admin":"ARTISAN".equals(expected)?"artisan":"customer")+"?roleMismatch");return;
  }
  response.sendRedirect(request.getContextPath()+"/account");
 }
}
