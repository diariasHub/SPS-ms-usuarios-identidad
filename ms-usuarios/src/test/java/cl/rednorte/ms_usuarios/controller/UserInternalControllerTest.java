package cl.rednorte.ms_usuarios.controller;

import cl.rednorte.ms_usuarios.dto.UserDTO;
import cl.rednorte.ms_usuarios.service.UserService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Collections;
import java.util.Optional;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(UserInternalController.class)
@AutoConfigureMockMvc(addFilters = false)
public class UserInternalControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private UserService userService;

    @Test
    void getByUsername_Found_ShouldReturnUser() throws Exception {
        UserDTO userDTO = UserDTO.builder().username("authuser").build();
        when(userService.findByUsername("authuser")).thenReturn(Optional.of(userDTO));

        mockMvc.perform(get("/internal/users/by-username/authuser"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("authuser"));
    }

    @Test
    void getRoles_ShouldReturnList() throws Exception {
        when(userService.getRolesByUserId(1L)).thenReturn(Collections.singletonList("ROLE_ADMIN"));

        mockMvc.perform(get("/internal/users/1/roles"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0]").value("ROLE_ADMIN"));
    }
}
