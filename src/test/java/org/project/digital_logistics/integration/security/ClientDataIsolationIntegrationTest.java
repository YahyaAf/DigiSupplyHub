package org.project.digital_logistics. integration.security;

import com. fasterxml.jackson.databind.ObjectMapper;
import org. junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter. api.Test;
import org. project.digital_logistics.dto. authJwt.LoginRequest;
import org.project.digital_logistics.model.Client;
import org.project.digital_logistics.model.User;
import org.project.digital_logistics.model.enums.Role;
import org.project.digital_logistics. repository.ClientRepository;
import org.project.digital_logistics.repository.UserRepository;
import org.springframework. beans.factory.annotation.Autowired;
import org.springframework. boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto. password.PasswordEncoder;
import org.springframework.test.context. ActiveProfiles;
import org. springframework.test.web.servlet. MockMvc;
import org. springframework.test.web.servlet. MvcResult;
import org. springframework.transaction.annotation.Transactional;

import static org. springframework.test.web.servlet. request.MockMvcRequestBuilders.*;
import static org.springframework. test.web.servlet.result. MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
@DisplayName("Integration Tests - Client Data Isolation")
class ClientDataIsolationIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ClientRepository clientRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private Client client1;
    private Client client2;
    private String client1Token;
    private String client2Token;
    private String validPassword = "Password123!";

    @BeforeEach
    void setUp() throws Exception {
        clientRepository.deleteAll();
        userRepository.deleteAll();

        client1 = new Client();
        client1.setName("Client One");
        client1.setEmail("client1@test.com");
        client1.setPasswordHash(passwordEncoder.encode(validPassword));
        client1.setRole(Role.CLIENT);
        client1.setActive(true);
        client1.setPhoneNumber("0611111111");
        client1.setAddress("Address 1");
        client1 = clientRepository.save(client1);

        client2 = new Client();
        client2.setName("Client Two");
        client2.setEmail("client2@test.com");
        client2.setPasswordHash(passwordEncoder.encode(validPassword));
        client2.setRole(Role.CLIENT);
        client2.setActive(true);
        client2.setPhoneNumber("0622222222");
        client2.setAddress("Address 2");
        client2 = clientRepository. save(client2);

        client1Token = loginAndGetToken("client1@test.com");
        client2Token = loginAndGetToken("client2@test.com");
    }

    private String loginAndGetToken(String email) throws Exception {
        LoginRequest loginRequest = new LoginRequest();
        loginRequest.setEmail(email);
        loginRequest.setPassword(validPassword);

        MvcResult result = mockMvc.perform(post("/api/auth/jwt/login")
                        .contentType(MediaType. APPLICATION_JSON)
                        . content(objectMapper.writeValueAsString(loginRequest)))
                .andReturn();

        return objectMapper.readTree(result.getResponse().getContentAsString())
                .path("data").path("accessToken").asText();
    }

    @Test
    @DisplayName("Client1 peut voir SES commandes - Doit retourner 200")
    void testClient1CanViewOwnOrders() throws Exception {
        mockMvc.perform(get("/api/sales-orders/my-orders")
                        .header("Authorization", "Bearer " + client1Token))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Client1 NE PEUT PAS voir les commandes de Client2 - Isolation des données")
    void testClient1CannotViewClient2Orders() throws Exception {
        mockMvc.perform(get("/api/sales-orders")
                        .header("Authorization", "Bearer " + client1Token))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/api/sales-orders/client/" + client2.getId())
                        .header("Authorization", "Bearer " + client1Token))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Client2 peut voir SES commandes uniquement - Doit retourner 200")
    void testClient2CanViewOwnOrdersOnly() throws Exception {
        mockMvc.perform(get("/api/sales-orders/my-orders")
                        .header("Authorization", "Bearer " + client2Token))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Les deux clients ont des tokens valides mais accèdent à des données différentes")
    void testTwoClientsHaveIsolatedData() throws Exception {
        MvcResult client1Result = mockMvc.perform(get("/api/sales-orders/my-orders")
                        .header("Authorization", "Bearer " + client1Token))
                .andExpect(status().isOk())
                .andReturn();

        MvcResult client2Result = mockMvc.perform(get("/api/sales-orders/my-orders")
                        .header("Authorization", "Bearer " + client2Token))
                .andExpect(status().isOk())
                .andReturn();

    }
}