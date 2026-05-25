package cl.rednorte.ms_usuarios.auth;

import cl.rednorte.ms_usuarios.repository.UserRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.samstevens.totp.code.DefaultCodeGenerator;
import dev.samstevens.totp.code.HashingAlgorithm;
import dev.samstevens.totp.time.SystemTimeProvider;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Cobertura del flujo MFA (TOTP) end-to-end:
 *  - setup: POST /auth/mfa/setup devuelve secret y QR
 *  - enable: POST /auth/mfa/enable con código válido habilita MFA
 *  - login posterior: devuelve challenge en lugar de tokens
 *  - /auth/otp: con código TOTP válido emite tokens; con inválido 401
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("dev")
class MfaFlowIntegrationTest {

    @Autowired MockMvc mvc;
    @Autowired ObjectMapper objectMapper;
    @Autowired UserRepository userRepository;

    private final SystemTimeProvider timeProvider = new SystemTimeProvider();
    private final DefaultCodeGenerator codeGenerator =
            new DefaultCodeGenerator(HashingAlgorithm.SHA1, 6);

    @AfterEach
    void disableMfaForMedicoDemo() {
        userRepository.findByUsername("medico.demo").ifPresent(u -> {
            u.setMfaEnabled(false);
            u.setMfaSecret(null);
            userRepository.save(u);
        });
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

    private String currentTotpCode(String secret) throws Exception {
        long bucket = timeProvider.getTime() / 30;
        return codeGenerator.generate(secret, bucket);
    }

    @Test
    void setupMfa_devuelveSecretYQr() throws Exception {
        String token = loginAndGetAccess();

        String body = mvc.perform(post("/auth/mfa/setup")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.secret").isNotEmpty())
                .andExpect(jsonPath("$.qrDataUri").isNotEmpty())
                .andReturn().getResponse().getContentAsString();
        JsonNode json = objectMapper.readTree(body);
        assertThat(json.get("qrDataUri").asText()).startsWith("data:image/png;base64,");
        // El secret está guardado pero MFA aún no activo.
        var user = userRepository.findByUsername("medico.demo").orElseThrow();
        assertThat(user.getMfaSecret()).isEqualTo(json.get("secret").asText());
        assertThat(user.isMfaEnabled()).isFalse();
    }

    @Test
    void enableMfa_conCodigoValido_activaMfa() throws Exception {
        String token = loginAndGetAccess();
        String setupBody = mvc.perform(post("/auth/mfa/setup")
                        .header("Authorization", "Bearer " + token))
                .andReturn().getResponse().getContentAsString();
        String secret = objectMapper.readTree(setupBody).get("secret").asText();

        String code = currentTotpCode(secret);
        mvc.perform(post("/auth/mfa/enable")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"code\":\"" + code + "\"}"))
                .andExpect(status().isNoContent());

        assertThat(userRepository.findByUsername("medico.demo").orElseThrow().isMfaEnabled()).isTrue();
    }

    @Test
    void enableMfa_conCodigoInvalido_devuelve401() throws Exception {
        String token = loginAndGetAccess();
        mvc.perform(post("/auth/mfa/setup").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());

        mvc.perform(post("/auth/mfa/enable")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"code\":\"000000\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.resourceType").value("OperationOutcome"));
    }

    @Test
    void loginConMfaActivo_retornaChallengeYOtpEmiteTokens() throws Exception {
        // Activamos MFA primero (mismo flujo del test anterior).
        String token = loginAndGetAccess();
        String setupBody = mvc.perform(post("/auth/mfa/setup")
                        .header("Authorization", "Bearer " + token))
                .andReturn().getResponse().getContentAsString();
        String secret = objectMapper.readTree(setupBody).get("secret").asText();
        mvc.perform(post("/auth/mfa/enable")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"code\":\"" + currentTotpCode(secret) + "\"}"))
                .andExpect(status().isNoContent());

        // Login ahora debe devolver MFA_REQUIRED en vez de tokens.
        String loginBody = mvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"username":"medico.demo","password":"Demo1234!"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("MFA_REQUIRED"))
                .andExpect(jsonPath("$.mfaChallenge").isNotEmpty())
                .andReturn().getResponse().getContentAsString();
        String challenge = objectMapper.readTree(loginBody).get("mfaChallenge").asText();

        // POST /auth/otp con código válido → tokens
        String otpBody = mvc.perform(post("/auth/otp")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"mfaChallenge\":\"" + challenge + "\","
                                + "\"code\":\"" + currentTotpCode(secret) + "\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.refreshToken").isNotEmpty())
                .andReturn().getResponse().getContentAsString();
        assertThat(objectMapper.readTree(otpBody).get("tokenType").asText()).isEqualTo("Bearer");
    }

    @Test
    void otpConCodigoInvalido_devuelve401() throws Exception {
        // Setup MFA
        String token = loginAndGetAccess();
        String setupBody = mvc.perform(post("/auth/mfa/setup")
                        .header("Authorization", "Bearer " + token))
                .andReturn().getResponse().getContentAsString();
        String secret = objectMapper.readTree(setupBody).get("secret").asText();
        mvc.perform(post("/auth/mfa/enable")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"code\":\"" + currentTotpCode(secret) + "\"}"))
                .andExpect(status().isNoContent());

        // Login → challenge
        String loginBody = mvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"username":"medico.demo","password":"Demo1234!"}
                                """))
                .andReturn().getResponse().getContentAsString();
        String challenge = objectMapper.readTree(loginBody).get("mfaChallenge").asText();

        // OTP inválido
        mvc.perform(post("/auth/otp")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"mfaChallenge\":\"" + challenge + "\",\"code\":\"000000\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.resourceType").value("OperationOutcome"));
    }
}
