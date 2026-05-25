package cl.rednorte.ms_usuarios.auth.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties(prefix = "app.security")
public class SecurityProperties {

    private Jwt jwt = new Jwt();
    private Lockout lockout = new Lockout();
    private Otp otp = new Otp();
    private RateLimit rateLimit = new RateLimit();

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

    /**
     * Rate limiting por IP en endpoints sensibles. Aplica una ventana
     * deslizante por IP; defaults conservadores para producción, los
     * tests los sobrescriben con valores más pequeños.
     */
    @Data
    public static class RateLimit {
        /** Intentos permitidos por IP en la ventana para POST /auth/login. */
        private int loginAttempts = 10;
        /** Intentos permitidos por IP en la ventana para POST /auth/otp. */
        private int otpAttempts = 10;
        /** Ventana de tiempo en segundos. */
        private int windowSeconds = 60;
    }
}
