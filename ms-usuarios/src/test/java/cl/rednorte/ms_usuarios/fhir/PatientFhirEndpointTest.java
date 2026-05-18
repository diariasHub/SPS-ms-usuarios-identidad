package cl.rednorte.ms_usuarios.fhir;

import cl.rednorte.ms_usuarios.audit.model.AuditEventType;
import cl.rednorte.ms_usuarios.audit.repository.AuditLogRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("dev")
class PatientFhirEndpointTest {

    @Autowired MockMvc mvc;
    @Autowired ObjectMapper objectMapper;
    @Autowired AuditLogRepository auditLogRepository;

    private String loginAndGetAccess() throws Exception {
        String body = mvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"username":"medico.demo","password":"Demo1234!"}
                                """))
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(body).get("accessToken").asText();
    }

    @Test
    void getPatient_sinToken_devuelve401ConOperationOutcome() throws Exception {
        mvc.perform(get("/Patient/1"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.resourceType").value("OperationOutcome"));
    }

    @Test
    void getPatient_conMedicoUrgencia_retornaRecursoFhirYAudita() throws Exception {
        String token = loginAndGetAccess();
        long auditBefore = auditLogRepository.count();

        mvc.perform(get("/Patient/1").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith("application/fhir+json"))
                .andExpect(jsonPath("$.resourceType").value("Patient"))
                .andExpect(jsonPath("$.id").value("1"))
                .andExpect(jsonPath("$.identifier[0].system")
                        .value(FhirMapper.IDENTIFIER_SYSTEM_RUN))
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
    void getPatient_breakGlassSinReason_devuelve400() throws Exception {
        String token = loginAndGetAccess();
        mvc.perform(get("/Patient/1")
                        .header("Authorization", "Bearer " + token)
                        .header("X-Break-Glass", "true"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.resourceType").value("OperationOutcome"));
    }

    @Test
    void getPatient_breakGlassConReason_auditaComoBreakGlass() throws Exception {
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

    @Test
    void getPatient_idInexistente_devuelve404OperationOutcome() throws Exception {
        String token = loginAndGetAccess();
        mvc.perform(get("/Patient/999999").header("Authorization", "Bearer " + token))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.resourceType").value("OperationOutcome"))
                .andExpect(jsonPath("$.issue[0].code").value("not-found"));
    }
}
