package cl.rednorte.ms_usuarios.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.sql.Timestamp;

@Builder
@Data
@AllArgsConstructor
@NoArgsConstructor
public class PatientDTO {
    private int patientId;
    private String runPatient;
    private String firstNamePatient;
    private String lastNamePatient;
    private boolean activatePatient;
    private String genderPatient;
    private Timestamp bithdayPatient;
    private String nationalityPatient;

}
