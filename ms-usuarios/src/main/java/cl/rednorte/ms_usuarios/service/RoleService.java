package cl.rednorte.ms_usuarios.service;

import cl.rednorte.ms_usuarios.model.Role;
import java.util.List;

public interface RoleService {
    List<Role> findAll();
    Role save(Role role);
}
