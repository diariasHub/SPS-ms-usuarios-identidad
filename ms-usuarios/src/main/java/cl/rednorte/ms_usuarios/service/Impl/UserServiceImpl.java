package cl.rednorte.ms_usuarios.service.Impl;

import cl.rednorte.ms_usuarios.dto.UserDTO;
import cl.rednorte.ms_usuarios.mapper.UsuarioMapper;
import cl.rednorte.ms_usuarios.model.Role;
import cl.rednorte.ms_usuarios.model.User;
import cl.rednorte.ms_usuarios.repository.UserRepository;
import cl.rednorte.ms_usuarios.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final UsuarioMapper usuarioMapper;

    @Override
    public List<UserDTO> findAll() {
        return userRepository.findAll().stream()
                .map(usuarioMapper::toDto)
                .collect(Collectors.toList());
    }

    @Override
    public Optional<UserDTO> findById(Long id) {
        return userRepository.findById(id).map(usuarioMapper::toDto);
    }

    @Override
    public Optional<UserDTO> findByUsername(String username) {
        return userRepository.findByUsername(username).map(usuarioMapper::toDto);
    }

    @Override
    public List<String> getRolesByUserId(Long id) {
        return userRepository.findById(id)
                .map(user -> user.getRoles().stream()
                        .map(Role::getName)
                        .collect(Collectors.toList()))
                .orElse(Collections.emptyList());
    }

    @Override
    public UserDTO save(UserDTO userDTO) {
        User user = usuarioMapper.toEntity(userDTO);
        return usuarioMapper.toDto(userRepository.save(user));
    }

    @Override
    public UserDTO update(Long id, UserDTO userDTO) {
        return userRepository.findById(id)
                .map(existingUser -> {
                    existingUser.setUsername(userDTO.getUsername());
                    existingUser.setEmail(userDTO.getEmail());
                    existingUser.setActive(userDTO.isActive());
                    return usuarioMapper.toDto(userRepository.save(existingUser));
                }).orElse(null);
    }

    @Override
    public void deleteById(Long id) {
        userRepository.deleteById(id);
    }
}
