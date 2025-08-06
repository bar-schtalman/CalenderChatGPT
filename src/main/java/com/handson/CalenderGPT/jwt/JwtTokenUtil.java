package com.handson.CalenderGPT.jwt;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Base64;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;

import io.jsonwebtoken.*;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.InputStream;
import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.time.Instant;
import java.util.Base64;
import java.util.Date;
import java.util.UUID;

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
    // 1. טען את המפתח הפרטי
    byte[] privateBytes = Files.readAllBytes(Paths.get(privateKeyPath));
    String privatePem = new String(privateBytes)
        .replace("-----BEGIN PRIVATE KEY-----", "")
        .replace("-----END PRIVATE KEY-----", "")
        .replaceAll("\\s+", "");
    byte[] decodedPriv = Base64.getDecoder().decode(privatePem);
    PKCS8EncodedKeySpec privKeySpec = new PKCS8EncodedKeySpec(decodedPriv);
    KeyFactory kf = KeyFactory.getInstance("RSA");
    this.privateKey = kf.generatePrivate(privKeySpec);

    // 2. טען את המפתח הציבורי
    byte[] publicBytes = Files.readAllBytes(Paths.get(publicKeyPath));
    String publicPem = new String(publicBytes)
        .replace("-----BEGIN PUBLIC KEY-----", "")
        .replace("-----END PUBLIC KEY-----", "")
        .replaceAll("\\s+", "");
    byte[] decodedPub = Base64.getDecoder().decode(publicPem);
    X509EncodedKeySpec pubKeySpec = new X509EncodedKeySpec(decodedPub);
    this.publicKey = kf.generatePublic(pubKeySpec);
}


    public String generateToken(UUID userId, String email, String fullName) {
        Instant now = Instant.now();
        Instant exp = now.plusSeconds(expirationMinutes * 60);

        return Jwts.builder().setSubject(userId.toString()).claim("email", email).claim("name", fullName).setIssuedAt(Date.from(now)).setExpiration(Date.from(exp)).setId(UUID.randomUUID().toString()).signWith(privateKey, SignatureAlgorithm.RS256).compact();
    }

    public Jws<Claims> validateToken(String token) throws JwtException {
        return Jwts.parserBuilder().setSigningKey(publicKey).build().parseClaimsJws(token);
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
