package cl.rednorte.ms_usuarios.security;

import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.ArrayList;

@Service
public class CustomUserDetailsService implements UserDetailsService {

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        // TODO: Implement load from repository
        return CustomUserDetails.builder()
            .username(username)
            .password("") // Should be encoded
            .enabled(true)
            .authorities(new ArrayList<>())
            .build();
    }
}
