package org.project.digital_logistics.config;

import lombok.RequiredArgsConstructor;
import org.project. digital_logistics.service.CustomUserDetailsService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
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
import org.springframework.security.web.SecurityFilterChain;
import org. springframework.security.web.authentication. UsernamePasswordAuthenticationFilter;

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
        return config.getAuthenticationManager();
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)

                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/api/auth/jwt/**", "/api/public/**").permitAll()

                        // ========== CARRIERS ==========
                        . requestMatchers(HttpMethod.GET, "/api/carriers/**")
                        .hasAnyRole("ADMIN", "WAREHOUSE_MANAGER", "CLIENT")
                        .requestMatchers(HttpMethod.POST, "/api/carriers").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.POST, "/api/carriers/reset-daily-shipments").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.PUT, "/api/carriers/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.PATCH, "/api/carriers/*/assign-shipment/*")
                        . hasAnyRole("ADMIN", "WAREHOUSE_MANAGER")
                        .requestMatchers(HttpMethod.PATCH, "/api/carriers/*/assign-multiple")
                        .hasAnyRole("ADMIN", "WAREHOUSE_MANAGER")
                        .requestMatchers(HttpMethod.PATCH, "/api/carriers/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.DELETE, "/api/carriers/**").hasRole("ADMIN")

                        // ========== CLIENTS ==========
                        .requestMatchers(HttpMethod.GET, "/api/clients/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.POST, "/api/clients").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.PUT, "/api/clients/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.DELETE, "/api/clients/**").hasRole("ADMIN")

                        // ========== INVENTORY ==========
                        .requestMatchers(HttpMethod.GET, "/api/inventories/**")
                        .hasAnyRole("ADMIN", "WAREHOUSE_MANAGER")
                        .requestMatchers(HttpMethod.POST, "/api/inventories").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.PUT, "/api/inventories/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.PATCH, "/api/inventories/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.DELETE, "/api/inventories/**").hasRole("ADMIN")

                        // ========== INVENTORY MOVEMENTS ==========
                        .requestMatchers(HttpMethod.GET, "/api/inventory-movements/**")
                        . hasAnyRole("ADMIN", "WAREHOUSE_MANAGER")
                        .requestMatchers(HttpMethod.POST, "/api/inventory-movements")
                        .hasAnyRole("ADMIN", "WAREHOUSE_MANAGER")
                        .requestMatchers(HttpMethod.PUT, "/api/inventory-movements/**")
                        .hasAnyRole("ADMIN", "WAREHOUSE_MANAGER")
                        .requestMatchers(HttpMethod.DELETE, "/api/inventory-movements/**")
                        .hasAnyRole("ADMIN", "WAREHOUSE_MANAGER")

                        // ========== PRODUCTS ==========
                        .requestMatchers(HttpMethod.GET, "/api/products/active")
                        .hasAnyRole("ADMIN", "WAREHOUSE_MANAGER", "CLIENT")
                        .requestMatchers(HttpMethod.GET, "/api/products/**")
                        .hasAnyRole("ADMIN", "WAREHOUSE_MANAGER")
                        .requestMatchers(HttpMethod.POST, "/api/products").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.POST, "/api/products/*/image").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.POST, "/api/products/*/image/s3").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.POST, "/api/products/S3").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.PUT, "/api/products/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.DELETE, "/api/products/**").hasRole("ADMIN")

                        // ========== PURCHASE ORDERS ==========
                        .requestMatchers(HttpMethod.GET, "/api/purchase-orders/**")
                        .hasAnyRole("ADMIN", "WAREHOUSE_MANAGER")
                        .requestMatchers(HttpMethod.POST, "/api/purchase-orders").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.PUT, "/api/purchase-orders/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.PATCH, "/api/purchase-orders/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.DELETE, "/api/purchase-orders/**").hasRole("ADMIN")

                        // ========== SALES ORDERS ==========
                        .requestMatchers(HttpMethod.POST, "/api/sales-orders").hasRole("CLIENT")
                        .requestMatchers(HttpMethod.GET, "/api/sales-orders/my-orders").hasRole("CLIENT")
                        .requestMatchers(HttpMethod.GET, "/api/sales-orders/**")
                        .hasAnyRole("ADMIN", "WAREHOUSE_MANAGER")
                        .requestMatchers(HttpMethod.PATCH, "/api/sales-orders/**")
                        .hasAnyRole("ADMIN", "WAREHOUSE_MANAGER")

                        // ========== SHIPMENTS ==========
                        .requestMatchers(HttpMethod.GET, "/api/shipments/track/*")
                        .hasAnyRole("ADMIN", "WAREHOUSE_MANAGER", "CLIENT")
                        .requestMatchers(HttpMethod.GET, "/api/shipments/**")
                        .hasAnyRole("ADMIN", "WAREHOUSE_MANAGER")
                        .requestMatchers(HttpMethod.PATCH, "/api/shipments/**")
                        .hasAnyRole("ADMIN", "WAREHOUSE_MANAGER")
                        .requestMatchers(HttpMethod.DELETE, "/api/shipments/**")
                        .hasAnyRole("ADMIN", "WAREHOUSE_MANAGER")

                        // ========== SUPPLIERS ==========
                        . requestMatchers(HttpMethod.GET, "/api/suppliers/**")
                        .hasAnyRole("ADMIN", "WAREHOUSE_MANAGER")
                        .requestMatchers(HttpMethod.POST, "/api/suppliers").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.PUT, "/api/suppliers/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.DELETE, "/api/suppliers/**").hasRole("ADMIN")

                        // ========== USERS ==========
                        .requestMatchers(HttpMethod.GET, "/api/users/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.POST, "/api/users").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.PUT, "/api/users/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.DELETE, "/api/users/**").hasRole("ADMIN")

                        // ========== WAREHOUSES (Entrepôts) ==========
                        // ADMIN:  CRUD | WAREHOUSE_MANAGER: R
                        .requestMatchers(HttpMethod.GET, "/api/warehouses/**")
                        .hasAnyRole("ADMIN", "WAREHOUSE_MANAGER")
                        .requestMatchers(HttpMethod.POST, "/api/warehouses").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.PUT, "/api/warehouses/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.DELETE, "/api/warehouses/**").hasRole("ADMIN")

                        . anyRequest().authenticated()
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