package com.ubs.billing.smoke;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ubs.billing.entity.User;
import com.ubs.billing.service.EmailService;
import com.ubs.billing.smoke.support.SmokeTestSupport;
import io.zonky.test.db.postgres.embedded.EmbeddedPostgres;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doNothing;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
public abstract class AbstractSmokeTest {

    private static final EmbeddedPostgres EMBEDDED_POSTGRES;

    static {
        try {
            EMBEDDED_POSTGRES = EmbeddedPostgres.builder().start();
        } catch (IOException ex) {
            throw new ExceptionInInitializerError(ex);
        }
    }

    @DynamicPropertySource
    static void configureEmbeddedDataSource(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", () -> EMBEDDED_POSTGRES.getJdbcUrl("postgres", "postgres"));
        registry.add("spring.datasource.username", () -> "postgres");
        registry.add("spring.datasource.password", () -> "postgres");
    }

    @Autowired
    protected MockMvc mockMvc;

    @Autowired
    protected ObjectMapper objectMapper;

    @MockBean
    protected EmailService emailService;

    protected SmokeTestSupport smoke;

    @BeforeEach
    void setUpSmokeSupport() {
        smoke = new SmokeTestSupport(mockMvc, objectMapper);
        configureEmailMocks();
    }

    private void configureEmailMocks() {
        doAnswer(invocation -> {
            smoke.capturedOtp().set(invocation.getArgument(1));
            return null;
        }).when(emailService).sendOtpEmail(any(User.class), anyString());

        doAnswer(invocation -> {
            smoke.capturedTemporaryPassword().set(invocation.getArgument(1));
            return null;
        }).when(emailService).sendStaffWelcomeEmail(any(User.class), anyString(), anyString());

        doAnswer(invocation -> {
            smoke.capturedTemporaryPassword().set(invocation.getArgument(1));
            return null;
        }).when(emailService).sendCustomerWelcomeEmail(any(User.class), anyString());

        doNothing().when(emailService).sendPasswordResetOtpEmail(any(User.class), anyString());
        doNothing().when(emailService).sendRoleChangeEmail(any(User.class), any());
        doNothing().when(emailService).sendBillApprovedEmail(any(User.class), any());
    }
}
