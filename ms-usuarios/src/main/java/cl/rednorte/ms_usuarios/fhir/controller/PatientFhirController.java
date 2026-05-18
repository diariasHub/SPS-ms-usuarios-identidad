package cl.rednorte.ms_usuarios.fhir.controller;

import ca.uhn.fhir.context.FhirContext;
import cl.rednorte.ms_usuarios.audit.model.AuditEventType;
import cl.rednorte.ms_usuarios.audit.service.AuditService;
import cl.rednorte.ms_usuarios.fhir.PatientFhirQueryService;
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
 * urgencia pueden consultar. El acceso queda auditado siempre; si el
 * cliente envía cabecera {@code X-Break-Glass: true}, queda marcado como
 * acceso de emergencia y exige una razón en {@code X-Break-Glass-Reason}.
 */
@RestController
@RequiredArgsConstructor
@Tag(name = "FHIR R4 - Patient")
public class PatientFhirController {

    private static final String FHIR_JSON = "application/fhir+json";

    private final PatientFhirQueryService patientFhirQueryService;
    private final FhirContext fhirContext;
    private final AuditService auditService;

    @Operation(summary = "Obtener Patient en FHIR R4",
               description = "Retorna el recurso Patient serializado en application/fhir+json. " +
                       "Toda llamada queda auditada. Usar X-Break-Glass: true + X-Break-Glass-Reason para emergencias.")
    @GetMapping(value = "/Patient/{id}", produces = FHIR_JSON)
    @PreAuthorize("hasAnyRole('MEDICO_URGENCIA','ENFERMERA_URGENCIA')")
    public ResponseEntity<String> getPatient(
            @PathVariable int id,
            @RequestHeader(value = "X-Break-Glass", required = false, defaultValue = "false") boolean breakGlass,
            @RequestHeader(value = "X-Break-Glass-Reason", required = false) String reason,
            @AuthenticationPrincipal CustomUserDetails principal,
            HttpServletRequest request) {

        if (breakGlass && (reason == null || reason.isBlank())) {
            return fhirError(400, OperationOutcome.IssueType.REQUIRED,
                    "X-Break-Glass requiere X-Break-Glass-Reason no vacío");
        }

        return patientFhirQueryService.findAsFhirJson(id)
                .map(body -> {
                    auditService.recordResourceAccess(
                            breakGlass ? AuditEventType.BREAK_GLASS : AuditEventType.PATIENT_ACCESSED,
                            "Patient", String.valueOf(id),
                            breakGlass, reason, request);
                    return ResponseEntity.ok()
                            .contentType(MediaType.parseMediaType(FHIR_JSON))
                            .body(body);
                })
                .orElseGet(() -> fhirError(404, OperationOutcome.IssueType.NOTFOUND,
                        "Patient/" + id + " no encontrado"));
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
