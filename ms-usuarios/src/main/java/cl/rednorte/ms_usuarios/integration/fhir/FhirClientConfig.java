package cl.rednorte.ms_usuarios.integration.fhir;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.rest.client.api.IGenericClient;
import ca.uhn.fhir.rest.client.api.ServerValidationModeEnum;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Cliente REST hacia el servidor HAPI FHIR central, configurado a partir
 * de {@link FhirProperties}. Es la fachada que ms-usuarios usa para
 * leer/escribir recursos clínicos: la fuente de verdad vive en el HAPI
 * central, no en la BD local.
 */
@Configuration
@RequiredArgsConstructor
public class FhirClientConfig {

    private final FhirContext fhirContext;
    private final FhirProperties fhirProperties;

    @Bean
    public IGenericClient fhirClient() {
        var factory = fhirContext.getRestfulClientFactory();
        factory.setConnectTimeout((int) fhirProperties.getConnectTimeout().toMillis());
        factory.setSocketTimeout((int) fhirProperties.getReadTimeout().toMillis());
        // Evita el GET /metadata implícito de la primera llamada — el HAPI
        // central es fuente de verdad, no necesitamos validar su versión.
        factory.setServerValidationMode(ServerValidationModeEnum.NEVER);
        return fhirContext.newRestfulGenericClient(fhirProperties.getServerUrl());
    }
}
