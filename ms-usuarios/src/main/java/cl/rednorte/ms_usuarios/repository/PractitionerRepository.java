package cl.rednorte.ms_usuarios.repository;

import cl.rednorte.ms_usuarios.model.Practitioner;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PractitionerRepository extends JpaRepository<Practitioner, Integer> {
}
