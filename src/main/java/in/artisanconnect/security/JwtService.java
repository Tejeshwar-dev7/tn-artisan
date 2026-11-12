package in.artisanconnect.security;
import in.artisanconnect.domain.User; import io.jsonwebtoken.Jwts; import io.jsonwebtoken.security.Keys; import org.springframework.beans.factory.annotation.Value; import org.springframework.stereotype.Service; import javax.crypto.SecretKey; import java.nio.charset.StandardCharsets; import java.time.Instant; import java.util.Date;
@Service public class JwtService {
 private final SecretKey key; private final long expiry;
 public JwtService(@Value("${app.jwt.secret}") String secret,@Value("${app.jwt.expiration-minutes}") long minutes){byte[] b=secret.getBytes(StandardCharsets.UTF_8);this.key=Keys.hmacShaKeyFor(java.util.Arrays.copyOf(b,Math.max(32,b.length)));this.expiry=minutes;}
 public String issue(User user){Instant now=Instant.now();return Jwts.builder().subject(user.email).claim("role",user.role.name()).issuedAt(Date.from(now)).expiration(Date.from(now.plusSeconds(expiry*60))).signWith(key).compact();}
 public String subject(String token){return Jwts.parser().verifyWith(key).build().parseSignedClaims(token).getPayload().getSubject();}
}
