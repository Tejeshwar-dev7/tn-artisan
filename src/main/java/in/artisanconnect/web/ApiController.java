package in.artisanconnect.web;
import in.artisanconnect.domain.*; import in.artisanconnect.repo.*; import in.artisanconnect.security.JwtService; import jakarta.validation.Valid; import jakarta.validation.constraints.*; import org.springframework.http.HttpStatus; import org.springframework.security.crypto.password.PasswordEncoder; import org.springframework.web.bind.annotation.*; import org.springframework.web.server.ResponseStatusException; import java.math.BigDecimal; import java.util.*;
@RestController @RequestMapping("/api")
public class ApiController {
 private final UserRepository users;private final ProductRepository products;private final PasswordEncoder encoder;private final JwtService jwt;
 public ApiController(UserRepository u,ProductRepository p,PasswordEncoder e,JwtService j){users=u;products=p;encoder=e;jwt=j;}
 public record Login(@Email String email,@NotBlank String password){}
 @PostMapping("/auth/token") public Map<String,Object> token(@Valid @RequestBody Login r){User u=users.findByEmailIgnoreCase(r.email()).orElseThrow(()->new ResponseStatusException(HttpStatus.UNAUTHORIZED));if(!u.active||!encoder.matches(r.password(),u.passwordHash))throw new ResponseStatusException(HttpStatus.UNAUTHORIZED,"Invalid email or password");return Map.of("accessToken",jwt.issue(u),"tokenType","Bearer","expiresInSeconds",7200,"role",u.role.name());}
 @GetMapping("/me") public Map<String,Object> me(org.springframework.security.core.Authentication auth){User u=users.findByEmailIgnoreCase(auth.getName()).orElseThrow();return Map.of("id",u.id,"name",u.name,"email",u.email,"role",u.role.name());}
 @GetMapping("/products") public List<Map<String,Object>> products(){return products.findByActiveTrueOrderByCreatedAtDesc().stream().map(p->Map.<String,Object>of("id",p.id,"name",p.name,"description",p.description,"price",p.price,"stock",p.stock,"district",p.district,"materials",p.materials,"artisan",p.artisan.name)).toList();}
 @GetMapping("/products/{id}") public Map<String,Object> product(@PathVariable Long id){Product p=products.findById(id).filter(x->x.active).orElseThrow(()->new ResponseStatusException(HttpStatus.NOT_FOUND));return Map.of("id",p.id,"name",p.name,"description",p.description,"price",p.price,"stock",p.stock,"district",p.district,"materials",p.materials,"technique",p.technique,"artisan",p.artisan.name);}
}
