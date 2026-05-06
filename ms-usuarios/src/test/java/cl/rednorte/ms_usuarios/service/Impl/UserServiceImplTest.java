package cl.rednorte.ms_usuarios.service.Impl;

import cl.rednorte.ms_usuarios.dto.UserDTO;
import cl.rednorte.ms_usuarios.mapper.UsuarioMapper;
import cl.rednorte.ms_usuarios.model.Role;
import cl.rednorte.ms_usuarios.model.User;
import cl.rednorte.ms_usuarios.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class UserServiceImplTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private UsuarioMapper usuarioMapper;

    @InjectMocks
    private UserServiceImpl userService;

    private User user;
    private UserDTO userDTO;

    @BeforeEach
    void setUp() {
        Role role = Role.builder().name("ROLE_USER").build();
        user = User.builder()
                .userId(1L)
                .username("testuser")
                .password("password")
                .roles(Set.of(role))
                .active(true)
                .build();

        userDTO = UserDTO.builder()
                .userId(1L)
                .username("testuser")
                .roles(Set.of("ROLE_USER"))
                .active(true)
                .build();
    }

    @Test
    void findAll_ShouldReturnDtoList() {
        when(userRepository.findAll()).thenReturn(Collections.singletonList(user));
        when(usuarioMapper.toDto(user)).thenReturn(userDTO);

        List<UserDTO> result = userService.findAll();

        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("testuser", result.get(0).getUsername());
        verify(userRepository).findAll();
    }

    @Test
    void findByUsername_Found_ShouldReturnDto() {
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(user));
        when(usuarioMapper.toDto(user)).thenReturn(userDTO);

        Optional<UserDTO> result = userService.findByUsername("testuser");

        assertTrue(result.isPresent());
        assertEquals("testuser", result.get().getUsername());
    }

    @Test
    void findByUsername_NotFound_ShouldReturnEmpty() {
        when(userRepository.findByUsername("unknown")).thenReturn(Optional.empty());

        Optional<UserDTO> result = userService.findByUsername("unknown");

        assertFalse(result.isPresent());
    }

    @Test
    void getRolesByUserId_Found_ShouldReturnRoleNames() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));

        List<String> result = userService.getRolesByUserId(1L);

        assertEquals(1, result.size());
        assertEquals("ROLE_USER", result.get(0));
    }

    @Test
    void save_ShouldReturnSavedDto() {
        when(usuarioMapper.toEntity(userDTO)).thenReturn(user);
        when(userRepository.save(user)).thenReturn(user);
        when(usuarioMapper.toDto(user)).thenReturn(userDTO);

        UserDTO result = userService.save(userDTO);

        assertNotNull(result);
        assertEquals("testuser", result.getUsername());
        verify(userRepository).save(any(User.class));
    }

    @Test
    void deleteById_ShouldCallRepository() {
        doNothing().when(userRepository).deleteById(1L);

        userService.deleteById(1L);

        verify(userRepository).deleteById(1L);
    }
}
