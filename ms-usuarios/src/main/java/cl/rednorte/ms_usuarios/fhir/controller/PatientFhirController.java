package cl.rednorte.ms_usuarios.fhir.controller;

import ca.uhn.fhir.context.FhirContext;
import cl.rednorte.ms_usuarios.audit.model.AuditEventType;
import cl.rednorte.ms_usuarios.audit.service.AuditService;
import cl.rednorte.ms_usuarios.fhir.AbacService;
import cl.rednorte.ms_usuarios.fhir.AbacService.Decision;
import cl.rednorte.ms_usuarios.fhir.AbacService.DecisionReason;
import cl.rednorte.ms_usuarios.fhir.PatientFhirQueryService;
import cl.rednorte.ms_usuarios.integration.fhir.FhirIdentifierSystems;
import cl.rednorte.ms_usuarios.integration.fhir.FhirPatientClient.SearchResult;
import cl.rednorte.ms_usuarios.security.CustomUserDetails;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.hl7.fhir.r4.model.OperationOutcome;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

/**
 * Endpoint FHIR R4 sobre {@code /Patient}. Solo médicos/enfermeras de
 * urgencia pueden consultar. Política ABAC: el usuario debe tener un
 * Encounter activo con el paciente en el HAPI central. Excepción:
 * cabecera {@code X-Break-Glass: true} (con razón obligatoria) salta ABAC
 * y queda auditada como acceso de emergencia.
 */
@RestController
@RequiredArgsConstructor
@Tag(name = "FHIR R4 - Patient")
public class PatientFhirController {

    private static final String FHIR_JSON = "application/fhir+json";

    private final PatientFhirQueryService patientFhirQueryService;
    private final AbacService abacService;
    private final FhirContext fhirContext;
    private final AuditService auditService;

    @Operation(summary = "Obtener Patient en FHIR R4",
               description = "Retorna el recurso Patient serializado en application/fhir+json. " +
                       "ABAC: solo accesible si existe Encounter activo entre el usuario y el paciente. " +
                       "Usar X-Break-Glass: true + X-Break-Glass-Reason para emergencias (omite ABAC).")
    @GetMapping(value = "/Patient/{id}", produces = FHIR_JSON)
    @PreAuthorize("hasAnyRole('MEDICO_URGENCIA','ENFERMERA_URGENCIA')")
    public ResponseEntity<String> getPatient(
            @PathVariable String id,
            @RequestHeader(value = "X-Break-Glass", required = false, defaultValue = "false") boolean breakGlass,
            @RequestHeader(value = "X-Break-Glass-Reason", required = false) String reason,
            @AuthenticationPrincipal CustomUserDetails principal,
            HttpServletRequest request) {

        if (breakGlass && (reason == null || reason.isBlank())) {
            return fhirError(400, OperationOutcome.IssueType.REQUIRED,
                    "X-Break-Glass requiere X-Break-Glass-Reason no vacío");
        }

        if (!breakGlass) {
            Decision decision = abacService.canAccessPatient(principal, id);
            if (!decision.allowed()) {
                String denyReason = mapDenyReasonToMessage(decision.reason());
                auditService.recordResourceAccess(
                        AuditEventType.PATIENT_ACCESS_DENIED,
                        "Patient", id,
                        false, "ABAC: " + decision.reason().name(),
                        request);
                return fhirError(403, OperationOutcome.IssueType.FORBIDDEN, denyReason);
            }
        }

        return patientFhirQueryService.findAsFhirJson(id)
                .map(body -> {
                    auditService.recordResourceAccess(
                            breakGlass ? AuditEventType.BREAK_GLASS : AuditEventType.PATIENT_ACCESSED,
                            "Patient", id,
                            breakGlass, reason, request);
                    return ResponseEntity.ok()
                            .contentType(MediaType.parseMediaType(FHIR_JSON))
                            .body(body);
                })
                .orElseGet(() -> fhirError(404, OperationOutcome.IssueType.NOTFOUND,
                        "Patient/" + id + " no encontrado"));
    }

    @Operation(summary = "Buscar Patient por identifier (típicamente RUN)",
               description = "Búsqueda FHIR R4 estándar. Acepta {value} o {system|value}. " +
                       "Si solo se entrega {value}, se asume el system de RUN RedNorte. " +
                       "Retorna un Bundle searchset. La búsqueda se audita; el acceso al " +
                       "detalle de cada paciente se audita al pegar GET /Patient/{id}.")
    @GetMapping(value = "/Patient", produces = FHIR_JSON)
    @PreAuthorize("hasAnyRole('MEDICO_URGENCIA','ENFERMERA_URGENCIA')")
    public ResponseEntity<String> searchPatient(
            @RequestParam("identifier") String identifier,
            @AuthenticationPrincipal CustomUserDetails principal,
            HttpServletRequest request) {

        if (identifier == null || identifier.isBlank()) {
            return fhirError(400, OperationOutcome.IssueType.REQUIRED,
                    "Parámetro 'identifier' es obligatorio");
        }

        String system;
        String value;
        int pipe = identifier.indexOf('|');
        if (pipe >= 0) {
            system = identifier.substring(0, pipe);
            value = identifier.substring(pipe + 1);
            if (system.isBlank()) system = FhirIdentifierSystems.RUN;
        } else {
            system = FhirIdentifierSystems.RUN;
            value = identifier;
        }

        SearchResult result = patientFhirQueryService.searchByIdentifier(system, value);

        auditService.recordResourceAccess(
                AuditEventType.PATIENT_SEARCHED,
                "Patient", null,
                false,
                "identifier=" + system + "|" + value + " resultIds=" + result.resourceIds(),
                request);

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(FHIR_JSON))
                .body(result.fhirJson());
    }

    private String mapDenyReasonToMessage(DecisionReason reason) {
        return switch (reason) {
            case DENIED_USER_HAS_NO_PRACTITIONER_LINK ->
                    "Usuario no tiene Practitioner asociado en HAPI central. Contactar administrador.";
            case DENIED_NO_ACTIVE_ENCOUNTER ->
                    "No tiene Encounter activo con este paciente. " +
                    "Use X-Break-Glass si esta es una emergencia.";
            default -> "Acceso denegado por política ABAC";
        };
    }

    private ResponseEntity<String> fhirError(int status, OperationOutcome.IssueType issueType, String diagnostics) {
        OperationOutcome oo = new OperationOutcome();
        oo.addIssue()
                .setSeverity(OperationOutcome.IssueSeverity.ERROR)
                .setCode(issueType)
                .setDiagnostics(diagnostics);
        return ResponseEntity.status(status)
                .contentType(MediaType.parseMediaType(FHIR_JSON))
                .body(fhirContext.newJsonParser().encodeResourceToString(oo));
    }
}
