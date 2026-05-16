package cl.rednorte.ms_usuarios.auth.dto;

/**
 * Resultado del login. Puede ser un par de tokens (sin MFA o tras OTP)
 * o un challenge de MFA que el cliente debe resolver llamando a /auth/otp.
 */
public sealed interface LoginResult permits TokenResponse, LoginChallengeResponse {
}
