package cl.rednorte.ms_usuarios.config;

import cl.rednorte.ms_usuarios.security.FhirAccessDeniedHandler;
import cl.rednorte.ms_usuarios.security.FhirAuthenticationEntryPoint;
import cl.rednorte.ms_usuarios.security.JwtAuthenticationFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final FhirAuthenticationEntryPoint authEntryPoint;
    private final FhirAccessDeniedHandler accessDeniedHandler;
    private final ObjectProvider<JwtDecoder> jwtDecoderProvider;
    private final ObjectProvider<UserDetailsService> userDetailsServiceProvider;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf.disable())
            .cors(Customizer.withDefaults())
            .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .exceptionHandling(eh -> eh
                    .authenticationEntryPoint(authEntryPoint)
                    .accessDeniedHandler(accessDeniedHandler))
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/auth/login", "/auth/otp", "/auth/refresh").permitAll()
                .requestMatchers("/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html").permitAll()
                .requestMatchers("/h2-console/**").permitAll()
                .requestMatchers("/actuator/health", "/actuator/info").permitAll()
                .requestMatchers("/internal/**").hasRole(cl.rednorte.ms_usuarios.model.Role.INTEGRACION)
                .anyRequest().authenticated()
            )
            // H2 console usa frames
            .headers(h -> h.frameOptions(f -> f.sameOrigin()));

        // El filtro JWT solo se enchufa si están disponibles los beans (perfil dev).
        JwtDecoder decoder = jwtDecoderProvider.getIfAvailable();
        UserDetailsService uds = userDetailsServiceProvider.getIfAvailable();
        if (decoder != null && uds != null) {
            http.addFilterBefore(new JwtAuthenticationFilter(decoder, uds),
                    UsernamePasswordAuthenticationFilter.class);
        }
        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(12);
    }

    @Bean
    public CorsFilter corsFilter() {
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowCredentials(true);
        config.addAllowedOriginPattern("*");
        config.addAllowedHeader("*");
        config.addAllowedMethod("*");
        source.registerCorsConfiguration("/**", config);
        return new CorsFilter(source);
    }
}
