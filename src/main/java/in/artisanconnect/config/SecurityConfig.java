package in.artisanconnect.config;

import in.artisanconnect.repo.UserRepository;
import in.artisanconnect.security.AccountAwareLoginFailureHandler;
import in.artisanconnect.security.AccountAwareLoginSuccessHandler;
import in.artisanconnect.security.JwtAuthFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.userdetails.*;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
public class SecurityConfig {
 @Bean PasswordEncoder passwordEncoder(){return new BCryptPasswordEncoder();}
 @Bean UserDetailsService userDetailsService(UserRepository users){return username->users.findByEmailIgnoreCase(username).map(u->User.withUsername(u.email).password(u.passwordHash).roles(u.role.name()).disabled(!u.active).build()).orElseThrow(()->new UsernameNotFoundException("Account not found"));}
 @Bean SecurityFilterChain filterChain(HttpSecurity http,JwtAuthFilter jwtFilter,AccountAwareLoginFailureHandler failureHandler,AccountAwareLoginSuccessHandler successHandler)throws Exception{
  return http.authorizeHttpRequests(a->a.dispatcherTypeMatchers(jakarta.servlet.DispatcherType.FORWARD,jakarta.servlet.DispatcherType.ERROR).permitAll()
   .requestMatchers("/","/products/**","/artisan/**","/passport/**","/crafts/**","/collection/**","/reviews","/about","/faq","/mission","/for-sellers","/for-customers","/register/**","/login/**","/css/**","/js/**","/images/**","/api/auth/**","/api/products/**","/swagger-ui/**","/v3/api-docs/**","/dev-db/**","/webhooks/razorpay").permitAll()
   .requestMatchers("/cart","/cart/**").permitAll()
   .requestMatchers(org.springframework.http.HttpMethod.GET,"/checkout").permitAll()
   .requestMatchers("/admin/**").hasRole("ADMIN").requestMatchers("/artisan-area/**","/artisan/profile").hasRole("ARTISAN").requestMatchers("/coordinator/**").hasRole("COORDINATOR").requestMatchers("/customer/**","/customer/profile","/checkout/**").hasRole("CUSTOMER").anyRequest().authenticated())
   .formLogin(f->f.loginPage("/login").successHandler(successHandler).failureHandler(failureHandler).permitAll())
   .logout(l->l.logoutSuccessUrl("/"))
   .headers(h->h.frameOptions(fr->fr.sameOrigin()))
   .csrf(c->c.ignoringRequestMatchers("/api/**","/dev-db/**","/webhooks/razorpay"))
   .addFilterBefore(jwtFilter,UsernamePasswordAuthenticationFilter.class).build();
 }
}
