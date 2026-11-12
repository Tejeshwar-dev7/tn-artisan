package in.artisanconnect.security;
import in.artisanconnect.repo.UserRepository;import jakarta.servlet.*;import jakarta.servlet.http.*;import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;import org.springframework.security.core.authority.SimpleGrantedAuthority;import org.springframework.security.core.context.SecurityContextHolder;import org.springframework.stereotype.Component;import org.springframework.web.filter.OncePerRequestFilter;import java.io.IOException;import java.util.List;
@Component public class JwtAuthFilter extends OncePerRequestFilter{
 private final JwtService jwt;private final UserRepository users;
 public JwtAuthFilter(JwtService j,UserRepository u){jwt=j;users=u;}
 @Override protected void doFilterInternal(HttpServletRequest req,HttpServletResponse res,FilterChain chain)throws ServletException,IOException{String h=req.getHeader("Authorization");if(h!=null&&h.startsWith("Bearer ")){try{String email=jwt.subject(h.substring(7));var u=users.findByEmailIgnoreCase(email).orElseThrow();if(u.active)SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(u.email,null,List.of(new SimpleGrantedAuthority("ROLE_"+u.role.name()))));}catch(Exception ignored){SecurityContextHolder.clearContext();}}chain.doFilter(req,res);}
}
