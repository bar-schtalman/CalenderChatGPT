package com.handson.CalenderGPT.jwt;

import com.handson.CalenderGPT.model.User;
import com.handson.CalenderGPT.repository.UserRepository;
import io.jsonwebtoken.*;
import jakarta.annotation.PostConstruct;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Component;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.time.Instant;
import java.util.Base64;
import java.util.Date;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Getter
@Component
public class JwtTokenUtil {

    @Value("${jwt.expirationMinutes:15}")
    private long expirationMinutes;

    @Value("${jwt.privateKeyPath}")
    private String privateKeyPath;

    @Value("${jwt.publicKeyPath}")
    private String publicKeyPath;

    private PrivateKey privateKey;
    private PublicKey publicKey;

    @Autowired
    private UserRepository userRepository;

    @PostConstruct
    public void loadKeys() throws Exception {
        log.info(">>> jwt.privateKeyPath = {}", privateKeyPath);
        log.info(">>> jwt.publicKeyPath  = {}", publicKeyPath);
        log.info(">>> jwt.expirationMinutes = {}", expirationMinutes);

        KeyFactory kf = KeyFactory.getInstance("RSA");

        // PRIVATE KEY (PKCS#8)
        String privatePem = new String(Files.readAllBytes(Paths.get(privateKeyPath)))
                .replace("-----BEGIN PRIVATE KEY-----", "")
                .replace("-----END PRIVATE KEY-----", "")
                .replaceAll("\\s+", "");
        this.privateKey = kf.generatePrivate(new PKCS8EncodedKeySpec(Base64.getDecoder().decode(privatePem)));

        // PUBLIC KEY (X.509 SPKI)
        String publicPem = new String(Files.readAllBytes(Paths.get(publicKeyPath)))
                .replace("-----BEGIN PUBLIC KEY-----", "")
                .replace("-----END PUBLIC KEY-----", "")
                .replaceAll("\\s+", "");
        this.publicKey = kf.generatePublic(new X509EncodedKeySpec(Base64.getDecoder().decode(publicPem)));
    }

    /**
     * בונה Authentication מתוך ה־JWT
     */
    public UsernamePasswordAuthenticationToken buildAuthentication(String jwt) {
        Claims claims = validateToken(jwt).getBody();

        // subject = email (לא id)
        String email = claims.getSubject();
        List<String> roles = claims.get("roles", List.class);

        // חיפוש היוזר מה־DB לפי אימייל
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("User not found: " + email));

        List<GrantedAuthority> authorities = (roles == null)
                ? List.of()
                : roles.stream().map(SimpleGrantedAuthority::new).collect(Collectors.toList());

        return new UsernamePasswordAuthenticationToken(user, null, authorities);
    }

    /**
     * ולידציה לטוקן
     */
    public Jws<Claims> validateToken(String token) throws JwtException {
        try {
            return Jwts.parserBuilder()
                    .setSigningKey(publicKey)
                    .build()
                    .parseClaimsJws(token);
        } catch (JwtException e) {
            System.err.println("❌ JWT validation failed: " + e.getMessage());
            throw e;
        }
    }

    /**
     * יצירת טוקן חדש
     */
    public String generateToken(UUID userId, String email, String fullName) {
        Instant now = Instant.now();
        Instant exp = now.plusSeconds(expirationMinutes * 60);

        return Jwts.builder()
                .setSubject(email)  // 👈 email הוא ה־subject
                .claim("userId", userId.toString())
                .claim("name", fullName)
                .setIssuedAt(Date.from(now))
                .setExpiration(Date.from(exp))
                .setId(UUID.randomUUID().toString())
                .signWith(privateKey, SignatureAlgorithm.RS256)
                .compact();
    }

    /**
     * עוזרים לשליפה מתוך טוקן
     */
    public UUID getUserIdFromToken(String token) {
        String userIdStr = validateToken(token).getBody().get("userId", String.class);
        return (userIdStr != null) ? UUID.fromString(userIdStr) : null;
    }

    public String getEmail(String token) {
        return validateToken(token).getBody().getSubject();
    }

    public String getName(String token) {
        return validateToken(token).getBody().get("name", String.class);
    }

    public Date getExpiration(String token) {
        return validateToken(token).getBody().getExpiration();
    }

    public Date getIssuedAt(String token) {
        return validateToken(token).getBody().getIssuedAt();
    }
}
