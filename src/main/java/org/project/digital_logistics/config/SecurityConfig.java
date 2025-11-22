package org.project.digital_logistics.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private final PasswordEncoder passwordEncoder;

    public SecurityConfig(PasswordEncoder passwordEncoder) {
        this.passwordEncoder = passwordEncoder;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .cors(cors -> cors.disable())
                .authorizeHttpRequests(auth -> auth
                        // Admin endpoints
                        .requestMatchers("/api/admin/**").hasRole("ADMIN")

                        // Products
                        .requestMatchers("/api/products/**").hasAnyRole("ADMIN", "WAREHOUSE_MANAGER", "CLIENT")

                        // Inventory
                        .requestMatchers("/api/inventories/**").hasAnyRole("ADMIN", "WAREHOUSE_MANAGER")
                        .requestMatchers("/api/inventory-movements/**").hasAnyRole("ADMIN", "WAREHOUSE_MANAGER")
                        .requestMatchers("/api/warehouses/**").hasAnyRole("ADMIN", "WAREHOUSE_MANAGER")

                        // Orders
                        .requestMatchers("/api/sales-orders/**").hasAnyRole("ADMIN", "WAREHOUSE_MANAGER", "CLIENT")
                        .requestMatchers("/api/purchase-orders/**").hasAnyRole("ADMIN", "WAREHOUSE_MANAGER")

                        // Shipments
                        .requestMatchers("/api/shipments/**").hasAnyRole("ADMIN", "WAREHOUSE_MANAGER")
                        .requestMatchers("/api/carriers/**").hasAnyRole("ADMIN", "WAREHOUSE_MANAGER")

                        // Suppliers
                        .requestMatchers("/api/suppliers/**").hasAnyRole("ADMIN", "WAREHOUSE_MANAGER")

                        // Users et Clients
                        .requestMatchers("/api/users/**").hasRole("ADMIN")
                        .requestMatchers("/api/clients/**").hasAnyRole("ADMIN", "CLIENT")

                        // Images
                        .requestMatchers("/api/images/**").authenticated()

                        // Tout autre endpoint
                        .anyRequest().authenticated()
                )
                .httpBasic(httpBasic -> {})
                .sessionManagement(session ->
                        session.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                );

        return http.build();
    }

    @Bean
    public UserDetailsService userDetailsService() {
        UserDetails admin = User.builder()
                .username("admin")
                .password(passwordEncoder.encode("admin123"))
                .roles("ADMIN")
                .build();

        UserDetails warehouseManager = User.builder()
                .username("manager")
                .password(passwordEncoder.encode("manager123"))
                .roles("WAREHOUSE_MANAGER")
                .build();

        UserDetails client = User.builder()
                .username("client")
                .password(passwordEncoder.encode("client123"))
                .roles("CLIENT")
                .build();

        return new InMemoryUserDetailsManager(admin, warehouseManager, client);
    }
}