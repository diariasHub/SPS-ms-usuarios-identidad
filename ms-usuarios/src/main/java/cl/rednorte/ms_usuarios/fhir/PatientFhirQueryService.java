package cl.rednorte.ms_usuarios.fhir;

import ca.uhn.fhir.context.FhirContext;
import cl.rednorte.ms_usuarios.repository.PatientRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

/**
 * Carga la entidad {@code Patient} y la mapea a FHIR dentro de una misma
 * transacción para inicializar las colecciones LAZY antes de salir de la
 * sesión Hibernate. Evita {@code MultipleBagFetchException} que ocurre al
 * hacer JOIN FETCH simultáneo de dos {@code List} sin orden.
 */
@Service
@RequiredArgsConstructor
public class PatientFhirQueryService {

    private final PatientRepository patientRepository;
    private final FhirMapper fhirMapper;
    private final FhirContext fhirContext;

    @Transactional(readOnly = true)
    public Optional<String> findAsFhirJson(int id) {
        return patientRepository.findById(id).map(p -> {
            org.hl7.fhir.r4.model.Patient fhir = fhirMapper.toFhir(p);
            return fhirContext.newJsonParser().encodeResourceToString(fhir);
        });
    }
}
