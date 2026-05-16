package cl.rednorte.ms_usuarios.fhir;

import ca.uhn.fhir.context.FhirContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * HAPI {@link FhirContext} es caro de construir (parser FHIR R4 completo)
 * y thread-safe. Lo exponemos como singleton para reutilizarlo.
 */
@Configuration
public class FhirContextConfig {

    @Bean
    public FhirContext fhirContext() {
        return FhirContext.forR4();
    }
}
