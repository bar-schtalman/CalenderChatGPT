package com.handson.CalenderGPT.jwt;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
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
        // Load private key (PKCS#8 PEM format)
        try (InputStream in = getClass().getResourceAsStream(privateKeyPath)) {
            byte[] keyBytes = in.readAllBytes();
            String keyPEM = new String(keyBytes)
                    .replace("-----BEGIN PRIVATE KEY-----", "")
                    .replace("-----END PRIVATE KEY-----", "")
                    .replaceAll("\n", "");
            byte[] decoded = Base64.getDecoder().decode(keyPEM);
            PKCS8EncodedKeySpec keySpec = new PKCS8EncodedKeySpec(decoded);
            KeyFactory keyFactory = KeyFactory.getInstance("RSA");
            privateKey = keyFactory.generatePrivate(keySpec);
        }

        // Load public key (X.509 PEM format)
        try (InputStream in = getClass().getResourceAsStream(publicKeyPath)) {
            byte[] keyBytes = in.readAllBytes();
            String keyPEM = new String(keyBytes)
                    .replace("-----BEGIN PUBLIC KEY-----", "")
                    .replace("-----END PUBLIC KEY-----", "")
                    .replaceAll("\n", "");
            byte[] decoded = Base64.getDecoder().decode(keyPEM);
            X509EncodedKeySpec keySpec = new X509EncodedKeySpec(decoded);
            KeyFactory keyFactory = KeyFactory.getInstance("RSA");
            publicKey = keyFactory.generatePublic(keySpec);
        }
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
