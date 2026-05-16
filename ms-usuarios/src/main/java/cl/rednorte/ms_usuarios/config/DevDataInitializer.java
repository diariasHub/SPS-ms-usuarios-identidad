package cl.rednorte.ms_usuarios.config;

import cl.rednorte.ms_usuarios.model.*;
import cl.rednorte.ms_usuarios.repository.PatientRepository;
import cl.rednorte.ms_usuarios.repository.RoleRepository;
import cl.rednorte.ms_usuarios.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Set;

/**
 * Carga datos mínimos para desarrollo: catálogo de roles, un usuario médico
 * de prueba y un paciente para probar el endpoint FHIR. Solo corre en perfil
 * dev y solo cuando la BD está vacía.
 */
@Slf4j
@Profile("dev")
@Component
@RequiredArgsConstructor
public class DevDataInitializer implements CommandLineRunner {

    private final RoleRepository roleRepository;
    private final UserRepository userRepository;
    private final PatientRepository patientRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional
    public void run(String... args) {
        seedRoles();
        seedMedicoDemo();
        seedPatientDemo();
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

    private void seedPatientDemo() {
        if (patientRepository.count() > 0) {
            return;
        }
        Address direccion = new Address();
        direccion.setUseAddress("home");
        direccion.setLineAddress("Av. Principal 123");
        direccion.setCityAddress("Antofagasta");
        direccion.setDistrictAddress("Antofagasta");
        direccion.setStateAddress("Antofagasta");
        direccion.setCountryAddress("CL");

        ContactPoint telefono = new ContactPoint();
        telefono.setSystemContact("phone");
        telefono.setValueContatc("+56912345678");

        Patient patient = new Patient();
        patient.setRunPatient("12345678-9");
        patient.setFirstNamePatient("Juan");
        patient.setLastNamePatient("Pérez");
        patient.setActivatePatient(true);
        patient.setGenderPatient("M");
        patient.setBithdayPatient(Timestamp.from(
                LocalDate.of(1980, 5, 12).atStartOfDay().toInstant(ZoneOffset.UTC)));
        patient.setNationalityPatient("Chilena");
        patient.setAddressesPatient(List.of(direccion));
        patient.setContactPointsPatient(List.of(telefono));

        patientRepository.save(patient);
        log.info("Seed: paciente demo creado (id={}, run={}).",
                patient.getPatientId(), patient.getRunPatient());
    }
}
