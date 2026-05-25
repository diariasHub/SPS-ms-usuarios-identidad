package cl.rednorte.ms_usuarios.fhir;

import cl.rednorte.ms_usuarios.integration.fhir.FhirPatientClient;
import cl.rednorte.ms_usuarios.integration.fhir.FhirPatientClient.SearchResult;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Optional;

/**
 * Fachada de lectura/búsqueda de {@code Patient}: delega en el HAPI FHIR
 * central (fuente de verdad) vía {@link FhirPatientClient}.
 */
@Service
@RequiredArgsConstructor
public class PatientFhirQueryService {

    private final FhirPatientClient fhirPatientClient;

    public Optional<String> findAsFhirJson(String id) {
        return fhirPatientClient.findAsFhirJson(id);
    }

    public SearchResult searchByIdentifier(String system, String value) {
        return fhirPatientClient.searchByIdentifierAsFhirJson(system, value);
    }
}
