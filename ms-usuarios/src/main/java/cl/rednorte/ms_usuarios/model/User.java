package cl.rednorte.ms_usuarios.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.Set;

@Entity
@Table(name = "users")
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long userId;

    @Column(unique = true, nullable = false, length = 60)
    private String username;

    // BCrypt hash, no la contraseña en claro.
    @Column(nullable = false, length = 100)
    private String password;

    @Column(unique = true, nullable = false, length = 150)
    private String email;

    @Column(nullable = false)
    private boolean active;

    // --- MFA (TOTP) ---
    @Column(name = "mfa_secret", length = 64)
    private String mfaSecret;

    @Column(name = "mfa_enabled", nullable = false)
    @Builder.Default
    private boolean mfaEnabled = false;

    // --- Lockout por intentos fallidos ---
    @Column(name = "failed_login_attempts", nullable = false)
    @Builder.Default
    private int failedLoginAttempts = 0;

    @Column(name = "locked_until")
    private Instant lockedUntil;

    @Column(name = "last_login_at")
    private Instant lastLoginAt;

    // Logical ID del recurso Practitioner asociado en el servidor HAPI FHIR central
    // (cuando el usuario es médico/enfermera). NO es una FK local — el recurso vive
    // fuera de este MS. Se usa para correlacionar audit logs y para llamadas FHIR.
    @Column(name = "practitioner_fhir_id", length = 64)
    private String practitionerFhirId;

    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(
        name = "user_roles",
        joinColumns = @JoinColumn(name = "user_id"),
        inverseJoinColumns = @JoinColumn(name = "role_id")
    )
    private Set<Role> roles;
}
