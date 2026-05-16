package cl.rednorte.ms_usuarios.auth.dto;

public record TokenResponse(
        String accessToken,
        String refreshToken,
        String tokenType,
        long expiresInSeconds
) implements LoginResult {
    public static TokenResponse bearer(String access, String refresh, long ttlSeconds) {
        return new TokenResponse(access, refresh, "Bearer", ttlSeconds);
    }
}
