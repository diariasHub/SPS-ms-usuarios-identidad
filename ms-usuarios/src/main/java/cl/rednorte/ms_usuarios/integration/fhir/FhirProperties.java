package cl.rednorte.ms_usuarios.integration.fhir;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

/**
 * Configuración de la integración con el servidor HAPI FHIR central
 * que actúa como fuente de verdad de los recursos clínicos.
 */
@Data
@ConfigurationProperties(prefix = "app.fhir")
public class FhirProperties {

    /** URL base del servidor FHIR. Ejemplo: http://hapi-fhir:8080/fhir */
    private String serverUrl;

    /** Timeout de conexión TCP. */
    private Duration connectTimeout = Duration.ofSeconds(5);

    /** Timeout de lectura de respuesta. */
    private Duration readTimeout = Duration.ofSeconds(10);
}
