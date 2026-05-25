package cl.rednorte.ms_usuarios.auth;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Verifica el rate limit de Bucket4j sobre {@code POST /auth/login}.
 * Usa un contexto Spring aislado con capacidad baja para no interferir
 * con los buckets en memoria del resto de tests.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("dev")
@TestPropertySource(properties = {
        "app.security.rate-limit.login-attempts=3",
        "app.security.rate-limit.window-seconds=60"
})
class RateLimitingTest {

    @Autowired MockMvc mvc;

    @Test
    void login_repetido_superaLimite_devuelve429ConOperationOutcome() throws Exception {
        String badLogin = """
                {"username":"medico.demo","password":"password-incorrecta"}
                """;
        // 3 primeros intentos consumen el bucket → todos 401 (credencial errónea)
        for (int i = 0; i < 3; i++) {
            mvc.perform(post("/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(badLogin))
                    .andExpect(status().isUnauthorized());
        }
        // 4to intento es rechazado por rate limit
        mvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(badLogin))
                .andExpect(status().isTooManyRequests())
                .andExpect(jsonPath("$.resourceType").value("OperationOutcome"))
                .andExpect(jsonPath("$.issue[0].code").value("throttled"));
    }
}
