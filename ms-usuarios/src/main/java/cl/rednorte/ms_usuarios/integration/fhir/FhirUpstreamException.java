package cl.rednorte.ms_usuarios.integration.fhir;

/**
 * Excepción lanzada cuando el servidor HAPI FHIR central responde con un
 * error inesperado (5xx, timeout, conexión rechazada). Se traduce a un
 * {@code OperationOutcome} 502/504 hacia el cliente.
 */
public class FhirUpstreamException extends RuntimeException {

    public FhirUpstreamException(String message, Throwable cause) {
        super(message, cause);
    }
}
