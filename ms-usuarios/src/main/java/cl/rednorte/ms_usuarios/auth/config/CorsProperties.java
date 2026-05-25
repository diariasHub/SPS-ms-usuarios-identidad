package cl.rednorte.ms_usuarios.auth.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

/**
 * Política CORS configurable. En prod deja vacío
 * {@code allowed-origins} cualquier dominio no listado.
 */
@Data
@ConfigurationProperties(prefix = "app.cors")
public class CorsProperties {

    /** Lista explícita de orígenes permitidos. Vacío = ningún origen aceptado. */
    private List<String> allowedOrigins = List.of();

    /** Métodos HTTP permitidos en cross-origin. */
    private List<String> allowedMethods = List.of("GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS");

    /** Headers que el cliente puede enviar en cross-origin. */
    private List<String> allowedHeaders = List.of("Authorization", "Content-Type", "X-Break-Glass", "X-Break-Glass-Reason");

    /** Si se permite enviar credenciales (cookies, Authorization en cross-origin). */
    private boolean allowCredentials = true;

    /** Tiempo en segundos que el navegador puede cachear la respuesta del preflight. */
    private long maxAgeSeconds = 3600;
}
