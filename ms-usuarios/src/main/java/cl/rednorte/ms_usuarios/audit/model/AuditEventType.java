package cl.rednorte.ms_usuarios.audit.model;

public enum AuditEventType {
    LOGIN_OK,
    LOGIN_FAILED,
    MFA_OK,
    MFA_FAILED,
    PATIENT_SEARCHED,
    PATIENT_ACCESSED,
    PATIENT_ACCESS_DENIED,
    PRACTITIONER_ACCESSED,
    BREAK_GLASS
}
