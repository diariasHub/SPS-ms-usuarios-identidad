package cl.rednorte.ms_usuarios.integration.fhir;

/**
 * Identifier systems propios publicados por RedNorte. Se usan tanto para
 * construir el campo {@code Patient.identifier[].system} como para
 * filtrar búsquedas FHIR ({@code GET /Patient?identifier=system|value}).
 */
public final class FhirIdentifierSystems {

    public static final String RUN = "http://rednorte.cl/fhir/identifier/run";

    private FhirIdentifierSystems() {}
}
