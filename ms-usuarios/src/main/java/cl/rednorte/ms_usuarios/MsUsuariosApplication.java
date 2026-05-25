package cl.rednorte.ms_usuarios;

import cl.rednorte.ms_usuarios.auth.config.CorsProperties;
import cl.rednorte.ms_usuarios.auth.config.SecurityProperties;
import cl.rednorte.ms_usuarios.integration.fhir.FhirProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@EnableAsync
@EnableConfigurationProperties({SecurityProperties.class, FhirProperties.class, CorsProperties.class})
public class MsUsuariosApplication {

	public static void main(String[] args) {
		SpringApplication.run(MsUsuariosApplication.class, args);
	}

}
