package org.project.digital_logistics. config;

import lombok.RequiredArgsConstructor;
import org.project.digital_logistics.service.CustomUserDetailsService;
import org.springframework.context.annotation. Bean;
import org.springframework. context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security. config.annotation.web.builders. HttpSecurity;
import org.springframework.security.config.annotation. web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security. crypto.password.PasswordEncoder;
import org.springframework.security. web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final CustomUserDetailsService userDetailsService;
    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final PasswordEncoder passwordEncoder;
    private final CustomAuthenticationEntryPoint authenticationEntryPoint;
    private final CustomAccessDeniedHandler accessDeniedHandler;

    @Bean
    public AuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider authProvider = new DaoAuthenticationProvider();
        authProvider.setUserDetailsService(userDetailsService);
        authProvider.setPasswordEncoder(passwordEncoder);
        return authProvider;
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config. getAuthenticationManager();
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)

                .authorizeHttpRequests(auth -> auth
                        . requestMatchers("/api/auth/jwt/**", "/api/public/**").permitAll()

                        .requestMatchers(HttpMethod.GET, "/api/carriers/**")
                        .hasAnyRole("ADMIN", "WAREHOUSE_MANAGER", "CLIENT")

                        .requestMatchers(HttpMethod.POST, "/api/carriers").hasRole("ADMIN")
                        . requestMatchers(HttpMethod.POST, "/api/carriers/reset-daily-shipments").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.PUT, "/api/carriers/**").hasRole("ADMIN")

                        .requestMatchers(HttpMethod.PATCH, "/api/carriers/*/assign-shipment/*")
                        .hasAnyRole("ADMIN", "WAREHOUSE_MANAGER")
                        . requestMatchers(HttpMethod.PATCH, "/api/carriers/*/assign-multiple")
                        . hasAnyRole("ADMIN", "WAREHOUSE_MANAGER")
                        . requestMatchers(HttpMethod.PATCH, "/api/carriers/**").hasRole("ADMIN")

                        .requestMatchers(HttpMethod.DELETE, "/api/carriers/**").hasRole("ADMIN")

                        .anyRequest().authenticated()
                )

                .sessionManagement(session ->
                        session.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                )

                .exceptionHandling(exception -> exception
                        .authenticationEntryPoint(authenticationEntryPoint)
                        .accessDeniedHandler(accessDeniedHandler)
                )

                .authenticationProvider(authenticationProvider())
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}