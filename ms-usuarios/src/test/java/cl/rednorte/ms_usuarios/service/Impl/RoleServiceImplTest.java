package cl.rednorte.ms_usuarios.service.Impl;

import cl.rednorte.ms_usuarios.model.Role;
import cl.rednorte.ms_usuarios.repository.RoleRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class RoleServiceImplTest {

    @Mock
    private RoleRepository roleRepository;

    @InjectMocks
    private RoleServiceImpl roleService;

    @Test
    void findAll_ShouldReturnList() {
        Role role = Role.builder().name("ROLE_ADMIN").build();
        when(roleRepository.findAll()).thenReturn(Collections.singletonList(role));

        List<Role> result = roleService.findAll();

        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("ROLE_ADMIN", result.get(0).getName());
    }

    @Test
    void save_ShouldReturnSavedRole() {
        Role role = Role.builder().name("ROLE_NEW").build();
        when(roleRepository.save(role)).thenReturn(role);

        Role result = roleService.save(role);

        assertNotNull(result);
        assertEquals("ROLE_NEW", result.getName());
        verify(roleRepository).save(role);
    }
}
