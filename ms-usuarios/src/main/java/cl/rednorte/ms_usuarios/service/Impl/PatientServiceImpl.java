package cl.rednorte.ms_usuarios.service.Impl;

import cl.rednorte.ms_usuarios.dto.PatientDTO;
import cl.rednorte.ms_usuarios.model.Patient;
import cl.rednorte.ms_usuarios.reposity.PatientRepository;
import cl.rednorte.ms_usuarios.service.PatientService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PatientServiceImpl implements PatientService {

    private final PatientRepository patientRepository;

    @Override
    public List<PatientDTO> findAll() {
        return patientRepository.findAll()
                .stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    @Override
    public Optional<PatientDTO> findById(int id) {
        return patientRepository.findById(id)
                .map(this::convertToDTO);
    }

    @Override
    public PatientDTO save(PatientDTO patientDTO) {
        Patient patient = convertToEntity(patientDTO);
        return convertToDTO(patientRepository.save(patient));
    }

    @Override
    public PatientDTO update(int id, PatientDTO patientDTO) {
        return patientRepository.findById(id)
                .map(existingPatient -> {
                    existingPatient.setRunPatient(patientDTO.getRunPatient());
                    existingPatient.setFirstNamePatient(patientDTO.getFirstNamePatient());
                    existingPatient.setLastNamePatient(patientDTO.getLastNamePatient());
                    existingPatient.setActivatePatient(patientDTO.isActivatePatient());
                    existingPatient.setGenderPatient(patientDTO.getGenderPatient());
                    existingPatient.setBithdayPatient(patientDTO.getBithdayPatient());
                    existingPatient.setNationalityPatient(patientDTO.getNationalityPatient());
                    return convertToDTO(patientRepository.save(existingPatient));
                }).orElse(null);
    }

    @Override
    public void deleteById(int id) {
        patientRepository.deleteById(id);
    }

    private PatientDTO convertToDTO(Patient p) {
        return PatientDTO.builder()
                .patientId(p.getPatientId())
                .runPatient(p.getRunPatient())
                .firstNamePatient(p.getFirstNamePatient())
                .lastNamePatient(p.getLastNamePatient())
                .activatePatient(p.isActivatePatient())
                .genderPatient(p.getGenderPatient())
                .bithdayPatient(p.getBithdayPatient())
                .nationalityPatient(p.getNationalityPatient())
                .build();
    }

    private Patient convertToEntity(PatientDTO dto) {
        Patient p = new Patient();
        p.setPatientId(dto.getPatientId());
        p.setRunPatient(dto.getRunPatient());
        p.setFirstNamePatient(dto.getFirstNamePatient());
        p.setLastNamePatient(dto.getLastNamePatient());
        p.setActivatePatient(dto.isActivatePatient());
        p.setGenderPatient(dto.getGenderPatient());
        p.setBithdayPatient(dto.getBithdayPatient());
        p.setNationalityPatient(dto.getNationalityPatient());
        return p;
    }
}
