package cl.rednorte.ms_usuarios.auth.service;

import cl.rednorte.ms_usuarios.auth.config.SecurityProperties;
import cl.rednorte.ms_usuarios.security.CustomUserDetails;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.jwt.*;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@ConditionalOnProperty(name = "app.security.jwt.dev-keypair-enabled", havingValue = "true")
public class JwtService {

    public enum TokenPurpose { ACCESS, REFRESH, MFA_CHALLENGE }

    private final JwtEncoder encoder;
    private final JwtDecoder decoder;
    private final SecurityProperties props;

    public String issueAccessToken(CustomUserDetails user) {
        Duration ttl = Duration.ofMinutes(props.getJwt().getAccessTokenTtlMinutes());
        return issue(user, TokenPurpose.ACCESS, ttl, authorities(user));
    }

    public String issueRefreshToken(CustomUserDetails user) {
        Duration ttl = Duration.ofHours(props.getJwt().getRefreshTokenTtlHours());
        return issue(user, TokenPurpose.REFRESH, ttl, List.of());
    }

    /** Token de corta vida emitido tras el login OK, antes de validar OTP. */
    public String issueMfaChallenge(CustomUserDetails user) {
        return issue(user, TokenPurpose.MFA_CHALLENGE, Duration.ofMinutes(5), List.of());
    }

    public Jwt decode(String token) {
        return decoder.decode(token);
    }

    private String issue(CustomUserDetails user, TokenPurpose purpose, Duration ttl, List<String> roles) {
        Instant now = Instant.now();
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer(props.getJwt().getIssuer())
                .issuedAt(now)
                .expiresAt(now.plus(ttl))
                .subject(user.getUsername())
                .id(UUID.randomUUID().toString())
                .claim("uid", user.getUserId())
                .claim("purpose", purpose.name())
                .claim("roles", roles)
                .build();
        JwsHeader header = JwsHeader.with(() -> "RS256").build();
        return encoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();
    }

    private List<String> authorities(CustomUserDetails user) {
        return user.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .toList();
    }
}
