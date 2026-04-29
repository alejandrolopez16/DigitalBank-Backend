/* 
package com.DigitalBank.backend.security;

import com.DigitalBank.backend.account.service.FinancialAccountService;
import com.DigitalBank.backend.customer.entity.AuthResponse;
import com.DigitalBank.backend.customer.service.CustomerService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class GraphQlSecurityIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

        @MockitoBean
    private CustomerService customerService;

        @MockitoBean
    private FinancialAccountService financialAccountService;

    @Test
    void shouldBlockProtectedGraphQlOperationWithoutToken() throws Exception {
        String body = """
                {
                  "query": "mutation { crearCuentaFinanciera(documentNumber: \\\"123\\\") { id } }"
                }
                """;

        mockMvc.perform(post("/graphql")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("Debe autenticarse con JWT para ejecutar esta operación"));
    }

    @Test
    void shouldAllowLoginWithoutToken() throws Exception {
        when(customerService.login(anyString(), anyString()))
                .thenReturn(AuthResponse.builder()
                        .token("fake-token")
                        .status("ACTIVE")
                        .role("ROLE_USER")
                        .message("Inicio de sesión exitoso")
                        .build());

        String body = """
                {
                  "query": "mutation { login(email: \\\"test@bank.com\\\", passwordHash: \\\"123456\\\") { token message } }"
                }
                """;

        mockMvc.perform(post("/graphql")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.login.token").value("fake-token"))
                .andExpect(jsonPath("$.data.login.message").value("Inicio de sesión exitoso"));
    }

    @Test
    void shouldBlockAccountInformationQueryWithoutToken() throws Exception {
        String body = """
                {
                  "query": "query { misCuentasFinancieras { id documentNumber balance } }"
                }
                """;

        mockMvc.perform(post("/graphql")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("Debe autenticarse con JWT para ejecutar esta operación"));
    }
}
*/