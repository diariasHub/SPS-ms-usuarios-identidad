package cl.rednorte.ms_usuarios.integration.fhir;

import ca.uhn.fhir.rest.client.api.IGenericClient;
import ca.uhn.fhir.rest.server.exceptions.BaseServerResponseException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Encounter;
import org.springframework.stereotype.Component;

/**
 * Wrapper sobre {@link IGenericClient} para consultas de {@code Encounter}
 * usadas en autorización ABAC: el motor comprueba si un practitioner tiene
 * un encuentro activo con un paciente antes de permitirle leer su ficha.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class FhirEncounterClient {

    private static final String[] ACTIVE_STATUSES = {"arrived", "triaged", "in-progress"};

    private final IGenericClient fhirClient;

    /**
     * ¿Existe al menos un Encounter activo en HAPI central donde el practitioner
     * dado participa en la atención del paciente dado? Fail-closed:
     * cualquier error de transporte se propaga como {@link FhirUpstreamException}
     * para que el caller responda 502 en lugar de permitir el acceso.
     */
    public boolean hasActiveEncounterForPractitioner(String patientFhirId, String practitionerFhirId) {
        try {
            Bundle bundle = fhirClient.search().forResource(Encounter.class)
                    .where(Encounter.SUBJECT.hasId("Patient/" + patientFhirId))
                    .and(Encounter.PARTICIPANT.hasId("Practitioner/" + practitionerFhirId))
                    .and(Encounter.STATUS.exactly().codes(ACTIVE_STATUSES))
                    .count(1)
                    .returnBundle(Bundle.class)
                    .execute();
            return bundle.getEntry() != null && !bundle.getEntry().isEmpty();
        } catch (BaseServerResponseException e) {
            log.warn("HAPI central respondió error buscando Encounter activo " +
                    "patient={} practitioner={} status={}",
                    patientFhirId, practitionerFhirId, e.getStatusCode());
            throw new FhirUpstreamException(
                    "Servidor FHIR central respondió error " + e.getStatusCode(), e);
        } catch (RuntimeException e) {
            log.warn("Fallo de transporte al buscar Encounter activo " +
                    "patient={} practitioner={}: {}",
                    patientFhirId, practitionerFhirId, e.toString());
            throw new FhirUpstreamException(
                    "No fue posible contactar al servidor FHIR central", e);
        }
    }
}
