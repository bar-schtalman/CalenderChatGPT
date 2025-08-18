package com.handson.CalenderGPT.jwt;

import io.jsonwebtoken.*;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;  // <-- הוסף
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

    @PostConstruct
    public void loadKeys() throws Exception {
        System.out.println(">>> jwt.privateKeyPath = " + privateKeyPath);
        System.out.println(">>> jwt.publicKeyPath  = " + publicKeyPath);
        System.out.println(">>> jwt.expirationMinutes = " + expirationMinutes);

        // PRIVATE KEY (PKCS#8, -----BEGIN PRIVATE KEY-----)
        byte[] privateBytes = Files.readAllBytes(Paths.get(privateKeyPath));
        String privatePem = new String(privateBytes)
                .replace("-----BEGIN PRIVATE KEY-----", "")
                .replace("-----END PRIVATE KEY-----", "")
                .replaceAll("\\s+", "");
        byte[] decodedPriv = Base64.getDecoder().decode(privatePem);
        PKCS8EncodedKeySpec privSpec = new PKCS8EncodedKeySpec(decodedPriv);
        KeyFactory kf = KeyFactory.getInstance("RSA");
        this.privateKey = kf.generatePrivate(privSpec);

        // PUBLIC KEY (X.509 SPKI, -----BEGIN PUBLIC KEY-----)
        byte[] publicBytes = Files.readAllBytes(Paths.get(publicKeyPath));
        String publicPem = new String(publicBytes)
                .replace("-----BEGIN PUBLIC KEY-----", "")
                .replace("-----END PUBLIC KEY-----", "")
                .replaceAll("\\s+", "");
        byte[] decodedPub = Base64.getDecoder().decode(publicPem);
        X509EncodedKeySpec pubSpec = new X509EncodedKeySpec(decodedPub);
        this.publicKey = kf.generatePublic(pubSpec);
    }

    public UsernamePasswordAuthenticationToken buildAuthentication(String jwt) {
        Claims claims = Jwts.parserBuilder()
                .setSigningKey(getPublicKey())   // משתמשים במפתח שנטען
                .build()
                .parseClaimsJws(jwt)
                .getBody();

        String username = claims.getSubject();      // "sub"
        List<String> roles = claims.get("roles", List.class); // אם אין roles בטוקן, יחזור null

        List<GrantedAuthority> authorities = (roles == null)
                ? List.of()
                : roles.stream()
                .map(SimpleGrantedAuthority::new)
                .collect(Collectors.toList());

        return new UsernamePasswordAuthenticationToken(username, null, authorities);
    }

    // <-- זה היה חסר
    public PublicKey getPublicKey() {
        return this.publicKey;
    }

    public String generateToken(UUID userId, String email, String fullName) {
        Instant now = Instant.now();
        Instant exp = now.plusSeconds(expirationMinutes * 60);

        return Jwts.builder()
                .setSubject(userId.toString())
                .claim("email", email)
                .claim("name", fullName)
                .setIssuedAt(Date.from(now))
                .setExpiration(Date.from(exp))
                .setId(UUID.randomUUID().toString())
                .signWith(privateKey, SignatureAlgorithm.RS256)
                .compact();
    }

    public Jws<Claims> validateToken(String token) throws JwtException {
        return Jwts.parserBuilder()
                .setSigningKey(publicKey)
                .build()
                .parseClaimsJws(token);
    }

    public UUID getUserIdFromToken(String token) {
        return UUID.fromString(validateToken(token).getBody().getSubject());
    }

    public Date getExpiration(String token) {
        return validateToken(token).getBody().getExpiration();
    }

    public Date getIssuedAt(String token) {
        return validateToken(token).getBody().getIssuedAt();
    }

    public String getEmail(String token) {
        return validateToken(token).getBody().get("email", String.class);
    }

    public String getName(String token) {
        return validateToken(token).getBody().get("name", String.class);
    }
}
