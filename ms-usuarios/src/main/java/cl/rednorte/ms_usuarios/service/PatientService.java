package cl.rednorte.ms_usuarios.service;

import cl.rednorte.ms_usuarios.dto.PatientDTO;
import java.util.List;
import java.util.Optional;

public interface PatientService {
    List<PatientDTO> findAll();
    Optional<PatientDTO> findById(int id);
    PatientDTO save(PatientDTO patientDTO);
    PatientDTO update(int id, PatientDTO patientDTO);
    void deleteById(int id);
}
