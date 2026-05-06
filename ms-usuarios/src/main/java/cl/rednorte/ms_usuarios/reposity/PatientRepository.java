package cl.rednorte.ms_usuarios.reposity;

import cl.rednorte.ms_usuarios.model.Patient;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PatientRepository extends JpaRepository<Patient, Integer> {
}
