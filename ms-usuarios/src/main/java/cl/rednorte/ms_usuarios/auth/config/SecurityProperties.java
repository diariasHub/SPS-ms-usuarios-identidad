package cl.rednorte.ms_usuarios.auth.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties(prefix = "app.security")
public class SecurityProperties {

    private Jwt jwt = new Jwt();
    private Lockout lockout = new Lockout();
    private Otp otp = new Otp();

    @Data
    public static class Jwt {
        private String issuer = "ms-usuarios";
        private int accessTokenTtlMinutes = 10;
        private int refreshTokenTtlHours = 8;
        private boolean devKeypairEnabled = false;
    }

    @Data
    public static class Lockout {
        private int maxAttempts = 5;
        private int lockMinutes = 15;
    }

    @Data
    public static class Otp {
        private String issuer = "RedNorte-Urgencias";
        private int digits = 6;
        private int periodSeconds = 30;
        private int allowedDiscrepancy = 1;
    }
}
