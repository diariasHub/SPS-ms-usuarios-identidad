package cl.rednorte.ms_usuarios.fhir.controller;

import ca.uhn.fhir.context.FhirContext;
import cl.rednorte.ms_usuarios.integration.fhir.FhirUpstreamException;
import lombok.RequiredArgsConstructor;
import org.hl7.fhir.r4.model.OperationOutcome;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * Convierte fallos al hablar con el HAPI FHIR central en respuestas
 * {@code OperationOutcome} con código 502 (Bad Gateway). Se prioriza con
 * {@code @Order(Integer.MIN_VALUE + 1)} para no chocar con el manejador
 * de auth y para ganarle al {@code GlobalExceptionHandler} genérico.
 */
@Order(Integer.MIN_VALUE + 1)
@RestControllerAdvice
@RequiredArgsConstructor
public class FhirExceptionAdvice {

    private final FhirContext fhirContext;

    @ExceptionHandler(FhirUpstreamException.class)
    public ResponseEntity<String> handleUpstream(FhirUpstreamException ex) {
        OperationOutcome oo = new OperationOutcome();
        oo.addIssue()
                .setSeverity(OperationOutcome.IssueSeverity.ERROR)
                .setCode(OperationOutcome.IssueType.TRANSIENT)
                .setDiagnostics(ex.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_GATEWAY)
                .contentType(MediaType.parseMediaType("application/fhir+json"))
                .body(fhirContext.newJsonParser().encodeResourceToString(oo));
    }
}
