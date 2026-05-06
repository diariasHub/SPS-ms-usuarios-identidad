package cl.rednorte.ms_usuarios.service.Impl;

import cl.rednorte.ms_usuarios.model.Role;
import cl.rednorte.ms_usuarios.repository.RoleRepository;
import cl.rednorte.ms_usuarios.service.RoleService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class RoleServiceImpl implements RoleService {

    private final RoleRepository roleRepository;

    @Override
    public List<Role> findAll() {
        return roleRepository.findAll();
    }

    @Override
    public Role save(Role role) {
        return roleRepository.save(role);
    }
}
