package com.ubs.billing.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;

@Service
@RequiredArgsConstructor
public class JwtService {

    public static final String CLAIM_TOKEN_TYPE = "typ";
    public static final String CLAIM_PASSWORD_CHANGE_REQUIRED = "pcr";
    public static final String TOKEN_TYPE_ACCESS = "access";
    public static final String TOKEN_TYPE_REFRESH = "refresh";

    private final JwtProperties jwtProperties;

    public String extractUsername(String token) {
        return extractClaim(token, Claims::getSubject);
    }

    public String extractJti(String token) {
        return extractClaim(token, claims -> claims.getId());
    }

    public Date extractExpiration(String token) {
        return extractClaim(token, Claims::getExpiration);
    }

    public String extractTokenType(String token) {
        return extractClaim(token, claims -> claims.get(CLAIM_TOKEN_TYPE, String.class));
    }

    public <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
        Claims claims = extractAllClaims(token);
        return claimsResolver.apply(claims);
    }

    public String generateAccessToken(UserDetails userDetails) {
        return generateAccessToken(userDetails, false);
    }

    public String generateAccessToken(UserDetails userDetails, boolean passwordChangeRequired) {
        Map<String, Object> claims = buildClaims(TOKEN_TYPE_ACCESS, jwtProperties.getExpirationMs());
        claims.put(CLAIM_PASSWORD_CHANGE_REQUIRED, passwordChangeRequired);
        return generateToken(claims, userDetails);
    }

    public String generateRefreshToken(UserDetails userDetails) {
        return generateRefreshToken(userDetails, false);
    }

    public String generateRefreshToken(UserDetails userDetails, boolean passwordChangeRequired) {
        Map<String, Object> claims = buildClaims(TOKEN_TYPE_REFRESH, jwtProperties.getRefreshExpirationMs());
        claims.put(CLAIM_PASSWORD_CHANGE_REQUIRED, passwordChangeRequired);
        return generateToken(claims, userDetails);
    }

    public boolean extractPasswordChangeRequired(String token) {
        Boolean value = extractClaim(token, claims -> claims.get(CLAIM_PASSWORD_CHANGE_REQUIRED, Boolean.class));
        return Boolean.TRUE.equals(value);
    }

    public String generateToken(UserDetails userDetails) {
        return generateAccessToken(userDetails);
    }

    public String generateToken(Map<String, Object> extraClaims, UserDetails userDetails) {
        return Jwts.builder()
                .claims(extraClaims)
                .id((String) extraClaims.getOrDefault(Claims.ID, UUID.randomUUID().toString()))
                .subject(userDetails.getUsername())
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + (Long) extraClaims.get("expMs")))
                .signWith(getSigningKey())
                .compact();
    }

    public long getExpirationMs() {
        return jwtProperties.getExpirationMs();
    }

    public long getRefreshExpirationMs() {
        return jwtProperties.getRefreshExpirationMs();
    }

    public boolean isTokenValid(String token, UserDetails userDetails) {
        return isAccessToken(token)
                && extractUsername(token).equals(userDetails.getUsername())
                && !isTokenExpired(token);
    }

    public boolean isAccessToken(String token) {
        try {
            return TOKEN_TYPE_ACCESS.equals(extractTokenType(token));
        } catch (Exception ex) {
            return false;
        }
    }

    public boolean isRefreshToken(String token) {
        try {
            return TOKEN_TYPE_REFRESH.equals(extractTokenType(token));
        } catch (Exception ex) {
            return false;
        }
    }

    private boolean isTokenExpired(String token) {
        return extractClaim(token, Claims::getExpiration).before(new Date());
    }

    private Claims extractAllClaims(String token) {
        return Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    private Map<String, Object> buildClaims(String tokenType, long expirationMs) {
        Map<String, Object> claims = new HashMap<>();
        claims.put(Claims.ID, UUID.randomUUID().toString());
        claims.put(CLAIM_TOKEN_TYPE, tokenType);
        claims.put("expMs", expirationMs);
        return claims;
    }

    private SecretKey getSigningKey() {
        String secret = jwtProperties.getSecret();
        byte[] keyBytes = isHexSecret(secret)
                ? java.util.HexFormat.of().parseHex(secret)
                : Decoders.BASE64.decode(secret);
        return Keys.hmacShaKeyFor(keyBytes);
    }

    private boolean isHexSecret(String secret) {
        return secret.matches("^[0-9A-Fa-f]+$") && secret.length() % 2 == 0;
    }
}
