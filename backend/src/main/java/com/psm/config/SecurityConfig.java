package com.psm.config;

import com.psm.service.UserDetailsServiceImpl;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final UserDetailsServiceImpl userDetailsService;
    private final JwtAuthFilter jwtAuthFilter;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .csrf(AbstractHttpConfigurer::disable)
            .cors(Customizer.withDefaults())
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .exceptionHandling(exceptions -> exceptions
                .authenticationEntryPoint((request, response, exception) -> {
                    response.setStatus(401);
                    response.setContentType(MediaType.APPLICATION_JSON_VALUE);
                    response.setCharacterEncoding("UTF-8");
                    response.getWriter().write("{\"message\":\"E-mail ou mot de passe incorrect.\"}");
                })
                .accessDeniedHandler((request, response, exception) -> {
                    response.setStatus(403);
                    response.setContentType(MediaType.APPLICATION_JSON_VALUE);
                    response.setCharacterEncoding("UTF-8");
                    response.getWriter().write("{\"message\":\"Vous n'êtes pas autorisé à effectuer cette action.\"}");
                }))
            .authorizeHttpRequests(auth -> auth
                .requestMatchers(HttpMethod.GET, "/api/auth/me").authenticated()
                .requestMatchers(HttpMethod.PUT, "/api/auth/me").authenticated()
                .requestMatchers("/api/auth/**", "/error").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/users/directory").hasAnyAuthority("ADMIN", "COLLABORATEUR")
                .requestMatchers("/api/users/**", "/api/roles/**").hasAuthority("ADMIN")
                .requestMatchers(HttpMethod.POST, "/api/projects/*/members/**").hasAuthority("ADMIN")
                .requestMatchers(HttpMethod.PUT, "/api/projects/*/members/**").hasAuthority("ADMIN")
                .requestMatchers(HttpMethod.DELETE, "/api/projects/*/members/**").hasAuthority("ADMIN")
                .requestMatchers(HttpMethod.GET, "/api/clients/**", "/api/projects/**").hasAnyAuthority("ADMIN", "COLLABORATEUR")
                .requestMatchers(HttpMethod.POST, "/api/clients/**", "/api/projects/**").hasAnyAuthority("ADMIN", "COLLABORATEUR")
                .requestMatchers(HttpMethod.PUT, "/api/clients/**", "/api/projects/**").hasAuthority("ADMIN")
                .requestMatchers(HttpMethod.DELETE, "/api/clients/**", "/api/projects/**").hasAuthority("ADMIN")
                .requestMatchers(HttpMethod.GET, "/api/invoices/**").hasAnyAuthority("ADMIN", "COLLABORATEUR")
                .requestMatchers(HttpMethod.POST, "/api/invoices/*/files").hasAnyAuthority("ADMIN", "COLLABORATEUR")
                .requestMatchers(HttpMethod.POST, "/api/invoices/**").hasAuthority("ADMIN")
                .requestMatchers(HttpMethod.PUT, "/api/invoices/**").hasAuthority("ADMIN")
                .requestMatchers(HttpMethod.PATCH, "/api/invoices/**").hasAuthority("ADMIN")
                .requestMatchers(HttpMethod.DELETE, "/api/invoices/**").hasAuthority("ADMIN")
                .requestMatchers(HttpMethod.GET, "/api/tasks/**", "/api/milestones/**").hasAnyAuthority("ADMIN", "COLLABORATEUR")
                .requestMatchers(HttpMethod.POST, "/api/tasks/**").hasAnyAuthority("ADMIN", "COLLABORATEUR")
                .requestMatchers(HttpMethod.PUT, "/api/tasks/**").hasAnyAuthority("ADMIN", "COLLABORATEUR")
                .requestMatchers(HttpMethod.PATCH, "/api/tasks/**").hasAnyAuthority("ADMIN", "COLLABORATEUR")
                .requestMatchers(HttpMethod.DELETE, "/api/tasks/*/comments/**", "/api/tasks/*/time-entries/**").hasAnyAuthority("ADMIN", "COLLABORATEUR")
                .requestMatchers(HttpMethod.DELETE, "/api/tasks/**").hasAuthority("ADMIN")
                .requestMatchers(HttpMethod.POST, "/api/milestones/**").hasAuthority("ADMIN")
                .requestMatchers(HttpMethod.PUT, "/api/milestones/**").hasAuthority("ADMIN")
                .requestMatchers(HttpMethod.PATCH, "/api/milestones/**").hasAuthority("ADMIN")
                .requestMatchers(HttpMethod.DELETE, "/api/milestones/**").hasAuthority("ADMIN")
                .requestMatchers("/api/**").authenticated()
                .anyRequest().permitAll()
            )
            .authenticationProvider(authenticationProvider())
            .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public AuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider(userDetailsService);
        provider.setPasswordEncoder(passwordEncoder());
        return provider;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration configuration) throws Exception {
        return configuration.getAuthenticationManager();
    }
}
