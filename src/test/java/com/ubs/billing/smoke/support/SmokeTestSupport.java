package com.ubs.billing.smoke.support;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ubs.billing.dto.request.ChangeTemporaryPasswordRequest;
import com.ubs.billing.dto.request.CreateCustomerRequest;
import com.ubs.billing.dto.request.CreateMeterReadingRequest;
import com.ubs.billing.dto.request.CreateMeterRequest;
import com.ubs.billing.dto.request.CreatePaymentRequest;
import com.ubs.billing.dto.request.CreateStaffRequest;
import com.ubs.billing.dto.request.CreateTariffRequest;
import com.ubs.billing.dto.request.GenerateBillRequest;
import com.ubs.billing.dto.request.LoginRequest;
import com.ubs.billing.dto.request.RegisterRequest;
import com.ubs.billing.dto.request.VerifyOtpRequest;
import com.ubs.billing.dto.response.AuthResponse;
import com.ubs.billing.dto.response.BillResponse;
import com.ubs.billing.dto.response.CustomerResponse;
import com.ubs.billing.dto.response.MeterReadingResponse;
import com.ubs.billing.dto.response.MeterResponse;
import com.ubs.billing.dto.response.PaymentResponse;
import com.ubs.billing.dto.response.UserResponse;
import com.ubs.billing.entity.BillStatus;
import com.ubs.billing.entity.CustomerStatus;
import com.ubs.billing.entity.MeterStatus;
import com.ubs.billing.entity.MeterType;
import com.ubs.billing.entity.PaymentMethod;
import com.ubs.billing.util.ApiResponse;
import com.ubs.billing.util.Constants;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

public class SmokeTestSupport {

    private final MockMvc mockMvc;
    private final ObjectMapper objectMapper;
    private final AtomicReference<String> capturedOtp = new AtomicReference<>();
    private final AtomicReference<String> capturedTemporaryPassword = new AtomicReference<>();

    public SmokeTestSupport(MockMvc mockMvc, ObjectMapper objectMapper) {
        this.mockMvc = mockMvc;
        this.objectMapper = objectMapper;
    }

    public AtomicReference<String> capturedOtp() {
        return capturedOtp;
    }

    public AtomicReference<String> capturedTemporaryPassword() {
        return capturedTemporaryPassword;
    }

    public String adminToken() throws Exception {
        return login(SmokeTestData.ADMIN_EMAIL, SmokeTestData.ADMIN_PASSWORD);
    }

    public String login(String email, String password) throws Exception {
        LoginRequest request = LoginRequest.builder()
                .email(email)
                .password(password)
                .build();

        MvcResult result = mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andReturn();

        ApiResponse<AuthResponse> response = readResponse(result, new TypeReference<>() {
        });
        assertNotNull(response);
        assertTrue(response.isSuccess());
        assertNotNull(response.getData());
        assertNotNull(response.getData().getAccessToken());
        return response.getData().getAccessToken();
    }

    public String createAndActivateStaff(String role, String adminToken) throws Exception {
        String email = SmokeTestData.uniqueEmail("staff");
        String phone = SmokeTestData.uniquePhone();

        CreateStaffRequest request = CreateStaffRequest.builder()
                .fullName("Smoke Staff User")
                .email(email)
                .phoneNumber(phone)
                .role(role)
                .build();

        MvcResult createResult = mockMvc.perform(authorized(post("/users/staff"), adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andReturn();

        ApiResponse<UserResponse> createResponse = readResponse(createResult, new TypeReference<>() {
        });
        assertNotNull(createResponse.getData());
        assertNotNull(createResponse.getData().getId());

        String temporaryPassword = capturedTemporaryPassword.get();
        assertNotNull(temporaryPassword, "Temporary password should be captured from staff welcome email");

        String firstLoginToken = login(email, temporaryPassword);

        ChangeTemporaryPasswordRequest changeRequest = ChangeTemporaryPasswordRequest.builder()
                .oldPassword(temporaryPassword)
                .newPassword(SmokeTestData.STAFF_PASSWORD)
                .confirmPassword(SmokeTestData.STAFF_PASSWORD)
                .build();

        MvcResult changeResult = mockMvc.perform(authorized(post("/auth/change-temporary-password"), firstLoginToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(changeRequest)))
                .andExpect(status().isOk())
                .andReturn();

        ApiResponse<AuthResponse> changeResponse = readResponse(changeResult, new TypeReference<>() {
        });
        assertNotNull(changeResponse.getData());
        assertNotNull(changeResponse.getData().getAccessToken());
        return changeResponse.getData().getAccessToken();
    }

    public String registerAndVerifyCustomer() throws Exception {
        String email = SmokeTestData.uniqueEmail("customer");
        String phone = SmokeTestData.uniquePhone();

        RegisterRequest registerRequest = RegisterRequest.builder()
                .fullName("Smoke Test Customer")
                .email(email)
                .phoneNumber(phone)
                .nationalId(SmokeTestData.uniqueNationalId())
                .address("Kigali, Gasabo, Remera")
                .password(SmokeTestData.STAFF_PASSWORD)
                .build();

        mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registerRequest)))
                .andExpect(status().isCreated());

        String otp = capturedOtp.get();
        assertNotNull(otp, "OTP should be captured from verification email");

        VerifyOtpRequest verifyRequest = VerifyOtpRequest.builder()
                .email(email)
                .otp(otp)
                .build();

        mockMvc.perform(post("/auth/verify-otp")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(verifyRequest)))
                .andExpect(status().isOk());

        return login(email, SmokeTestData.STAFF_PASSWORD);
    }

    public CustomerResponse createCustomer(String adminToken) throws Exception {
        CreateCustomerRequest request = CreateCustomerRequest.builder()
                .fullName("Smoke Customer")
                .nationalId(SmokeTestData.uniqueNationalId())
                .email(SmokeTestData.uniqueEmail("cust"))
                .phoneNumber(SmokeTestData.uniquePhone())
                .address("Kigali, Gasabo, Remera")
                .status(CustomerStatus.ACTIVE)
                .build();

        MvcResult result = mockMvc.perform(authorized(post("/customers"), adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andReturn();

        ApiResponse<CustomerResponse> response = readResponse(result, new TypeReference<>() {
        });
        assertNotNull(response.getData());
        assertNotNull(response.getData().getId());
        return response.getData();
    }

    public void createTariff(String adminToken) throws Exception {
        CreateTariffRequest request = CreateTariffRequest.builder()
                .meterType(MeterType.WATER)
                .rate(new BigDecimal("450.00"))
                .serviceCharge(new BigDecimal("1500.00"))
                .vat(new BigDecimal("18.00"))
                .penaltyRate(new BigDecimal("50.00"))
                .effectiveDate(LocalDate.of(2026, 1, 1))
                .active(true)
                .build();

        MvcResult result = mockMvc.perform(authorized(post("/tariffs"), adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andReturn();

        ApiResponse<?> response = readResponse(result, new TypeReference<>() {
        });
        assertNotNull(response);
        assertTrue(response.isSuccess());
    }

    public MeterResponse createMeter(String token, Long customerId) throws Exception {
        CreateMeterRequest request = CreateMeterRequest.builder()
                .meterNumber(SmokeTestData.uniqueMeterNumber())
                .meterType(MeterType.WATER)
                .installationDate(LocalDate.of(2026, 1, 15))
                .status(MeterStatus.ACTIVE)
                .customerId(customerId)
                .build();

        MvcResult result = mockMvc.perform(authorized(post("/meters"), token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andReturn();

        ApiResponse<MeterResponse> response = readResponse(result, new TypeReference<>() {
        });
        assertNotNull(response.getData());
        assertNotNull(response.getData().getId());
        assertNotNull(response.getData().getMeterNumber());
        return response.getData();
    }

    public MeterReadingResponse createMeterReading(String operatorToken, Long meterId) throws Exception {
        LocalDate today = LocalDate.now();
        CreateMeterReadingRequest request = CreateMeterReadingRequest.builder()
                .meterId(meterId)
                .previousReading(new BigDecimal("100.00"))
                .currentReading(new BigDecimal("125.50"))
                .readingDate(today)
                .month(today.getMonthValue())
                .year(today.getYear())
                .build();

        MvcResult result = mockMvc.perform(authorized(post("/meter-readings"), operatorToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andReturn();

        ApiResponse<MeterReadingResponse> response = readResponse(result, new TypeReference<>() {
        });
        assertNotNull(response.getData());
        assertNotNull(response.getData().getId());
        return response.getData();
    }

    public BillResponse generateBill(String adminToken, Long meterReadingId) throws Exception {
        GenerateBillRequest request = GenerateBillRequest.builder()
                .meterReadingId(meterReadingId)
                .build();

        MvcResult result = mockMvc.perform(authorized(post("/bills/generate"), adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andReturn();

        ApiResponse<BillResponse> response = readResponse(result, new TypeReference<>() {
        });
        assertNotNull(response.getData());
        assertNotNull(response.getData().getBillReference());
        assertTrue(response.getData().getTotalAmount().compareTo(BigDecimal.ZERO) > 0);
        return response.getData();
    }

    public BillResponse approveBill(String adminToken, Long billId) throws Exception {
        MvcResult result = mockMvc.perform(authorized(patch("/bills/{id}/approve", billId), adminToken))
                .andExpect(status().isOk())
                .andReturn();

        ApiResponse<BillResponse> response = readResponse(result, new TypeReference<>() {
        });
        assertNotNull(response.getData());
        assertNotNull(response.getData().getStatus());
        return response.getData();
    }

    public PaymentResponse recordPayment(String financeToken, Long billId, BigDecimal amount) throws Exception {
        CreatePaymentRequest request = CreatePaymentRequest.builder()
                .billId(billId)
                .amountPaid(amount)
                .paymentMethod(PaymentMethod.MOMO)
                .paymentDate(LocalDate.now())
                .build();

        MvcResult result = mockMvc.perform(authorized(post("/payments"), financeToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andReturn();

        ApiResponse<PaymentResponse> response = readResponse(result, new TypeReference<>() {
        });
        assertNotNull(response.getData());
        assertNotNull(response.getData().getId());
        return response.getData();
    }

    public BillingContext prepareBillingContext() throws Exception {
        String adminToken = adminToken();
        createTariff(adminToken);
        CustomerResponse customer = createCustomer(adminToken);
        MeterResponse meter = createMeter(adminToken, customer.getId());
        String operatorToken = createAndActivateStaff(Constants.ROLE_OPERATOR, adminToken);
        MeterReadingResponse reading = createMeterReading(operatorToken, meter.getId());
        return new BillingContext(adminToken, operatorToken, customer, meter, reading);
    }

    public record BillingContext(
            String adminToken,
            String operatorToken,
            CustomerResponse customer,
            MeterResponse meter,
            MeterReadingResponse reading) {
    }

    private MockHttpServletRequestBuilder authorized(MockHttpServletRequestBuilder builder, String token) {
        return builder.header("Authorization", "Bearer " + token);
    }

    private <T> ApiResponse<T> readResponse(MvcResult result, TypeReference<ApiResponse<T>> typeReference)
            throws Exception {
        return objectMapper.readValue(result.getResponse().getContentAsString(), typeReference);
    }
}
