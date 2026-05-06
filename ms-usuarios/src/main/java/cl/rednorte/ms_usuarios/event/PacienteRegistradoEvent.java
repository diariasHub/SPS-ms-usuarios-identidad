package cl.rednorte.ms_usuarios.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PacienteRegistradoEvent {
    private String patientId;
    private String run;
    private String fullName;
    private String email;
    private LocalDateTime timestamp;
}
