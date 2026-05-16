package cl.rednorte.ms_usuarios.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record MfaEnableRequest(
        @NotBlank @Pattern(regexp = "\\d{6}") String code
) {}
