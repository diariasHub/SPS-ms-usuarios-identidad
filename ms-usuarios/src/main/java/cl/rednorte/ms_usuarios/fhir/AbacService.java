package cl.rednorte.ms_usuarios.fhir;

import cl.rednorte.ms_usuarios.integration.fhir.FhirEncounterClient;
import cl.rednorte.ms_usuarios.security.CustomUserDetails;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * Motor ABAC mínimo: un usuario clínico puede leer {@code Patient/{id}}
 * solo si el HAPI central tiene un {@code Encounter} activo donde su
 * Practitioner asociado participa.
 *
 * Fail-closed: si no hay {@code practitionerFhirId} en el usuario, o si
 * no hay Encounter activo, se deniega. Si HAPI no responde, la excepción
 * burbujea y el controller responde 502 (no se puede confirmar autorización).
 */
@Service
@RequiredArgsConstructor
public class AbacService {

    private final FhirEncounterClient encounterClient;

    public enum DecisionReason {
        ALLOWED,
        DENIED_USER_HAS_NO_PRACTITIONER_LINK,
        DENIED_NO_ACTIVE_ENCOUNTER
    }

    public record Decision(boolean allowed, DecisionReason reason) {
        public static Decision allow() {
            return new Decision(true, DecisionReason.ALLOWED);
        }
        public static Decision deny(DecisionReason reason) {
            return new Decision(false, reason);
        }
    }

    public Decision canAccessPatient(CustomUserDetails principal, String patientFhirId) {
        String practitionerFhirId = principal.getPractitionerFhirId();
        if (practitionerFhirId == null || practitionerFhirId.isBlank()) {
            return Decision.deny(DecisionReason.DENIED_USER_HAS_NO_PRACTITIONER_LINK);
        }
        if (!encounterClient.hasActiveEncounterForPractitioner(patientFhirId, practitionerFhirId)) {
            return Decision.deny(DecisionReason.DENIED_NO_ACTIVE_ENCOUNTER);
        }
        return Decision.allow();
    }
}
