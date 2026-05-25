package cl.rednorte.ms_usuarios.config;

import cl.rednorte.ms_usuarios.model.Role;
import cl.rednorte.ms_usuarios.model.User;
import cl.rednorte.ms_usuarios.repository.RoleRepository;
import cl.rednorte.ms_usuarios.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;

/**
 * Carga datos mínimos de identidad para desarrollo: catálogo de roles y un
 * usuario médico de prueba. Solo corre en perfil dev y solo cuando la BD
 * está vacía.
 *
 * Los recursos clínicos (Patient/Practitioner) NO se siembran aquí: su
 * fuente de verdad es el servidor HAPI FHIR central. Para tener un paciente
 * demo, ejecutar el script en docs/orchestration/seed-fhir.sh contra el
 * HAPI levantado.
 */
@Slf4j
@Profile("dev")
@Component
@RequiredArgsConstructor
public class DevDataInitializer implements CommandLineRunner {

    private final RoleRepository roleRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional
    public void run(String... args) {
        seedRoles();
        seedMedicoDemo();
    }

    private void seedRoles() {
        ensureRole(Role.MEDICO_URGENCIA, "Médico de urgencias");
        ensureRole(Role.ENFERMERA_URGENCIA, "Enfermera/o de urgencias");
        ensureRole(Role.ADMIN, "Administrador del sistema (sin acceso clínico)");
        ensureRole(Role.INTEGRACION, "Cuenta de integración máquina-a-máquina");
    }

    private void ensureRole(String name, String description) {
        roleRepository.findByName(name).orElseGet(() ->
                roleRepository.save(Role.builder().name(name).description(description).build())
        );
    }

    private void seedMedicoDemo() {
        if (userRepository.findByUsername("medico.demo").isPresent()) {
            return;
        }
        Role medico = roleRepository.findByName(Role.MEDICO_URGENCIA).orElseThrow();
        User user = User.builder()
                .username("medico.demo")
                .password(passwordEncoder.encode("Demo1234!"))
                .email("medico.demo@rednorte.cl")
                .active(true)
                .mfaEnabled(false)
                .failedLoginAttempts(0)
                .roles(Set.of(medico))
                .build();
        userRepository.save(user);
        log.info("Seed: usuario medico.demo creado (password: Demo1234!).");
    }
}
