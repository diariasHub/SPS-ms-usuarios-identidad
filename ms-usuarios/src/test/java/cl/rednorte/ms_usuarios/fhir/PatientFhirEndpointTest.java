package cl.rednorte.ms_usuarios.fhir;

import cl.rednorte.ms_usuarios.audit.model.AuditEventType;
import cl.rednorte.ms_usuarios.audit.repository.AuditLogRepository;
import cl.rednorte.ms_usuarios.repository.UserRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import com.github.tomakehurst.wiremock.client.WireMock;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integración del endpoint FHIR Patient contra un HAPI FHIR simulado con
 * WireMock. Cubre lectura, búsqueda por identifier, break-glass, errores
 * upstream y reglas ABAC (Encounter activo necesario para acceder).
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("dev")
class PatientFhirEndpointTest {

    private static final String PRACTITIONER_FHIR_ID = "PR-MEDICO-DEMO";

    // Arrancado en bloque static para que @DynamicPropertySource (que Spring
    // evalúa antes de @BeforeEach) tenga ya el baseUrl disponible.
    private static final WireMockServer wireMockServer;

    static {
        wireMockServer = new WireMockServer(WireMockConfiguration.options().dynamicPort());
        wireMockServer.start();
        Runtime.getRuntime().addShutdownHook(new Thread(wireMockServer::stop));
    }

    @Autowired MockMvc mvc;
    @Autowired ObjectMapper objectMapper;
    @Autowired AuditLogRepository auditLogRepository;
    @Autowired UserRepository userRepository;

    @BeforeEach
    void setUp() {
        wireMockServer.resetAll();
        // Por defecto medico.demo tiene un Practitioner asociado; los tests
        // que validan la rama "sin practitioner" lo desasocian explícitamente.
        userRepository.findByUsername("medico.demo").ifPresent(u -> {
            u.setPractitionerFhirId(PRACTITIONER_FHIR_ID);
            userRepository.save(u);
        });
    }

    @AfterEach
    void tearDown() {
        userRepository.findByUsername("medico.demo").ifPresent(u -> {
            u.setPractitionerFhirId(null);
            userRepository.save(u);
        });
    }

    @DynamicPropertySource
    static void registerFhirServerUrl(DynamicPropertyRegistry registry) {
        registry.add("app.fhir.server-url", () -> wireMockServer.baseUrl() + "/fhir");
    }

    private String loginAndGetAccess() throws Exception {
        String body = mvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"username":"medico.demo","password":"Demo1234!"}
                                """))
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(body).get("accessToken").asText();
    }

    private void stubPatientOk(String id) {
        String fhirPatient = """
                {
                  "resourceType":"Patient",
                  "id":"%s",
                  "identifier":[{"system":"http://rednorte.cl/fhir/identifier/run","value":"12345678-9"}],
                  "active":true,
                  "name":[{"given":["Juan"],"family":"Pérez"}],
                  "gender":"male"
                }
                """.formatted(id);
        wireMockServer.stubFor(WireMock.get(urlEqualTo("/fhir/Patient/" + id))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/fhir+json;charset=UTF-8")
                        .withBody(fhirPatient)));
    }

    /**
     * Stub para que ABAC encuentre un Encounter activo del practitioner con
     * el paciente. Sin este stub el ABAC deniega.
     */
    private void stubActiveEncounter(String patientId) {
        String bundle = """
                {
                  "resourceType":"Bundle",
                  "type":"searchset",
                  "total":1,
                  "entry":[{"resource":{"resourceType":"Encounter","id":"ENC-1","status":"in-progress"}}]
                }
                """;
        wireMockServer.stubFor(WireMock.get(urlPathEqualTo("/fhir/Encounter"))
                .withQueryParam("subject", equalTo("Patient/" + patientId))
                .withQueryParam("participant", equalTo("Practitioner/" + PRACTITIONER_FHIR_ID))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/fhir+json;charset=UTF-8")
                        .withBody(bundle)));
    }

    /** Stub para que ABAC reciba 0 Encounters → deniega acceso. */
    private void stubNoActiveEncounter() {
        String empty = """
                {"resourceType":"Bundle","type":"searchset","total":0,"entry":[]}
                """;
        wireMockServer.stubFor(WireMock.get(urlPathEqualTo("/fhir/Encounter"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/fhir+json;charset=UTF-8")
                        .withBody(empty)));
    }

    // ---------------------- Auth básico ----------------------

    @Test
    void getPatient_sinToken_devuelve401ConOperationOutcome() throws Exception {
        mvc.perform(get("/Patient/1"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.resourceType").value("OperationOutcome"));
    }

    @Test
    void searchPatient_sinToken_devuelve401() throws Exception {
        mvc.perform(get("/Patient").param("identifier", "12345678-9"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.resourceType").value("OperationOutcome"));
    }

    // ---------------------- Lectura con ABAC ----------------------

    @Test
    void getPatient_conEncounterActivo_retornaRecursoFhirYAudita() throws Exception {
        stubActiveEncounter("1");
        stubPatientOk("1");
        String token = loginAndGetAccess();
        long auditBefore = auditLogRepository.count();

        mvc.perform(get("/Patient/1").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith("application/fhir+json"))
                .andExpect(jsonPath("$.resourceType").value("Patient"))
                .andExpect(jsonPath("$.id").value("1"))
                .andExpect(jsonPath("$.identifier[0].value").value("12345678-9"))
                .andExpect(jsonPath("$.gender").value("male"));

        assertThat(auditLogRepository.count()).isEqualTo(auditBefore + 1);
        var last = auditLogRepository.findAll().getLast();
        assertThat(last.getEventType()).isEqualTo(AuditEventType.PATIENT_ACCESSED);
        assertThat(last.getResourceId()).isEqualTo("1");
        assertThat(last.getUsername()).isEqualTo("medico.demo");
        assertThat(last.isBreakGlass()).isFalse();
    }

    @Test
    void getPatient_sinEncounterActivo_devuelve403YAuditaDenegacion() throws Exception {
        stubNoActiveEncounter();
        String token = loginAndGetAccess();
        long auditBefore = auditLogRepository.count();

        mvc.perform(get("/Patient/1").header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.resourceType").value("OperationOutcome"))
                .andExpect(jsonPath("$.issue[0].code").value("forbidden"));

        assertThat(auditLogRepository.count()).isEqualTo(auditBefore + 1);
        var last = auditLogRepository.findAll().getLast();
        assertThat(last.getEventType()).isEqualTo(AuditEventType.PATIENT_ACCESS_DENIED);
        assertThat(last.getReason()).contains("DENIED_NO_ACTIVE_ENCOUNTER");
    }

    @Test
    void getPatient_sinPractitionerFhirId_devuelve403() throws Exception {
        // Override del setUp: deja al usuario sin practitionerFhirId.
        userRepository.findByUsername("medico.demo").ifPresent(u -> {
            u.setPractitionerFhirId(null);
            userRepository.save(u);
        });
        String token = loginAndGetAccess();
        long auditBefore = auditLogRepository.count();

        mvc.perform(get("/Patient/1").header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.issue[0].diagnostics")
                        .value(org.hamcrest.Matchers.containsString("Practitioner")));

        assertThat(auditLogRepository.count()).isEqualTo(auditBefore + 1);
        assertThat(auditLogRepository.findAll().getLast().getReason())
                .contains("DENIED_USER_HAS_NO_PRACTITIONER_LINK");
    }

    // ---------------------- Break-glass ----------------------

    @Test
    void getPatient_breakGlassSinReason_devuelve400() throws Exception {
        String token = loginAndGetAccess();
        mvc.perform(get("/Patient/1")
                        .header("Authorization", "Bearer " + token)
                        .header("X-Break-Glass", "true"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.resourceType").value("OperationOutcome"));
    }

    @Test
    void getPatient_breakGlassConReason_omiteAbacYAuditaComoBreakGlass() throws Exception {
        // Nota: NO stubeamos Encounter — el break-glass debe omitir ABAC.
        stubPatientOk("1");
        String token = loginAndGetAccess();
        long auditBefore = auditLogRepository.count();

        mvc.perform(get("/Patient/1")
                        .header("Authorization", "Bearer " + token)
                        .header("X-Break-Glass", "true")
                        .header("X-Break-Glass-Reason", "Paciente inconsciente, sin tiempo para flujo regular"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.resourceType").value("Patient"));

        assertThat(auditLogRepository.count()).isEqualTo(auditBefore + 1);
        var last = auditLogRepository.findAll().getLast();
        assertThat(last.getEventType()).isEqualTo(AuditEventType.BREAK_GLASS);
        assertThat(last.isBreakGlass()).isTrue();
        assertThat(last.getReason()).contains("Paciente inconsciente");
    }

    // ---------------------- Edge cases de lectura ----------------------

    @Test
    void getPatient_idInexistenteEnHapi_devuelve404OperationOutcome() throws Exception {
        stubActiveEncounter("999999");
        wireMockServer.stubFor(WireMock.get(urlEqualTo("/fhir/Patient/999999"))
                .willReturn(aResponse()
                        .withStatus(404)
                        .withHeader("Content-Type", "application/fhir+json")
                        .withBody("{\"resourceType\":\"OperationOutcome\"}")));

        String token = loginAndGetAccess();
        mvc.perform(get("/Patient/999999").header("Authorization", "Bearer " + token))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.resourceType").value("OperationOutcome"))
                .andExpect(jsonPath("$.issue[0].code").value("not-found"));
    }

    @Test
    void getPatient_hapiCaido_devuelve502OperationOutcome() throws Exception {
        stubActiveEncounter("1");
        wireMockServer.stubFor(WireMock.get(urlEqualTo("/fhir/Patient/1"))
                .willReturn(aResponse().withStatus(503)));

        String token = loginAndGetAccess();
        mvc.perform(get("/Patient/1").header("Authorization", "Bearer " + token))
                .andExpect(status().isBadGateway())
                .andExpect(jsonPath("$.resourceType").value("OperationOutcome"))
                .andExpect(jsonPath("$.issue[0].code").value("transient"));
    }

    // ---------------------- Búsqueda (no afectada por ABAC) ----------------------

    @Test
    void searchByRun_conResultado_retornaBundleYAudita() throws Exception {
        String bundleJson = """
                {
                  "resourceType":"Bundle",
                  "type":"searchset",
                  "total":1,
                  "entry":[
                    {
                      "resource":{
                        "resourceType":"Patient",
                        "id":"42",
                        "identifier":[{"system":"http://rednorte.cl/fhir/identifier/run","value":"12345678-9"}],
                        "name":[{"given":["Juan"],"family":"Pérez"}]
                      }
                    }
                  ]
                }
                """;
        wireMockServer.stubFor(WireMock.get(urlPathEqualTo("/fhir/Patient"))
                .withQueryParam("identifier",
                        equalTo("http://rednorte.cl/fhir/identifier/run|12345678-9"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/fhir+json;charset=UTF-8")
                        .withBody(bundleJson)));

        String token = loginAndGetAccess();
        long auditBefore = auditLogRepository.count();

        mvc.perform(get("/Patient").param("identifier", "12345678-9")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith("application/fhir+json"))
                .andExpect(jsonPath("$.resourceType").value("Bundle"))
                .andExpect(jsonPath("$.entry[0].resource.id").value("42"));

        assertThat(auditLogRepository.count()).isEqualTo(auditBefore + 1);
        var last = auditLogRepository.findAll().getLast();
        assertThat(last.getEventType()).isEqualTo(AuditEventType.PATIENT_SEARCHED);
        assertThat(last.getReason()).contains("12345678-9").contains("42");
        assertThat(last.getResourceId()).isNull();
    }

    @Test
    void searchByRun_sinResultados_retornaBundleVacioYAudita() throws Exception {
        String emptyBundle = """
                {"resourceType":"Bundle","type":"searchset","total":0,"entry":[]}
                """;
        wireMockServer.stubFor(WireMock.get(urlPathEqualTo("/fhir/Patient"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/fhir+json;charset=UTF-8")
                        .withBody(emptyBundle)));

        String token = loginAndGetAccess();
        long auditBefore = auditLogRepository.count();

        mvc.perform(get("/Patient").param("identifier", "99999999-9")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.resourceType").value("Bundle"))
                .andExpect(jsonPath("$.total").value(0));

        assertThat(auditLogRepository.count()).isEqualTo(auditBefore + 1);
        assertThat(auditLogRepository.findAll().getLast().getEventType())
                .isEqualTo(AuditEventType.PATIENT_SEARCHED);
    }
}
