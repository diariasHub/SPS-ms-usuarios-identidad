package cl.rednorte.ms_usuarios.integration.fhir;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.rest.client.api.IGenericClient;
import ca.uhn.fhir.rest.server.exceptions.BaseServerResponseException;
import ca.uhn.fhir.rest.server.exceptions.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Patient;
import org.springframework.stereotype.Component;

import java.util.Optional;

/**
 * Wrapper sobre {@link IGenericClient} para operaciones sobre {@code Patient}
 * en el HAPI FHIR central. Aísla al resto de la app de las clases HAPI y
 * traduce errores de transporte/upstream a una excepción de dominio
 * ({@link FhirUpstreamException}) o a {@link Optional#empty()} para 404.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class FhirPatientClient {

    private final IGenericClient fhirClient;
    private final FhirContext fhirContext;

    /**
     * Lectura de un Patient por ID lógico. Vacío si HAPI responde 404.
     */
    public Optional<String> findAsFhirJson(String id) {
        try {
            Patient patient = fhirClient.read()
                    .resource(Patient.class)
                    .withId(id)
                    .execute();
            return Optional.of(fhirContext.newJsonParser().encodeResourceToString(patient));
        } catch (ResourceNotFoundException e) {
            return Optional.empty();
        } catch (BaseServerResponseException e) {
            log.warn("HAPI central respondió error sobre Patient/{}: status={} msg={}",
                    id, e.getStatusCode(), e.getMessage());
            throw new FhirUpstreamException(
                    "Servidor FHIR central respondió error " + e.getStatusCode(), e);
        } catch (RuntimeException e) {
            log.warn("Fallo de transporte al consultar Patient/{} en HAPI central: {}", id, e.toString());
            throw new FhirUpstreamException(
                    "No fue posible contactar al servidor FHIR central", e);
        }
    }

    /**
     * Búsqueda por identifier. {@code system} puede ser null para no filtrar
     * por sistema (no recomendado en clínica). Devuelve el Bundle searchset
     * serializado como FHIR R4 JSON.
     */
    public SearchResult searchByIdentifierAsFhirJson(String system, String value) {
        try {
            var query = fhirClient.search().forResource(Patient.class);
            Bundle bundle = (system != null && !system.isBlank()
                    ? query.where(Patient.IDENTIFIER.exactly().systemAndCode(system, value))
                    : query.where(Patient.IDENTIFIER.exactly().identifier(value))
            ).returnBundle(Bundle.class).execute();
            int total = bundle.getEntry().size();
            String json = fhirContext.newJsonParser().encodeResourceToString(bundle);
            return new SearchResult(json, total, bundle.getEntry().stream()
                    .map(e -> e.getResource() != null ? e.getResource().getIdPart() : null)
                    .toList());
        } catch (BaseServerResponseException e) {
            log.warn("HAPI central respondió error en búsqueda Patient identifier={}: status={}",
                    value, e.getStatusCode());
            throw new FhirUpstreamException(
                    "Servidor FHIR central respondió error " + e.getStatusCode(), e);
        } catch (RuntimeException e) {
            log.warn("Fallo de transporte al buscar Patient identifier={}: {}", value, e.toString());
            throw new FhirUpstreamException(
                    "No fue posible contactar al servidor FHIR central", e);
        }
    }

    /** Resultado de búsqueda: payload serializado + metadatos para auditoría. */
    public record SearchResult(String fhirJson, int total, java.util.List<String> resourceIds) {}
}
