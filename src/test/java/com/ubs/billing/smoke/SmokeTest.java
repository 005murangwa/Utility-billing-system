package com.ubs.billing.smoke;

import com.fasterxml.jackson.core.type.TypeReference;
import com.ubs.billing.dto.request.CreateStaffRequest;
import com.ubs.billing.dto.request.CreateTariffRequest;
import com.ubs.billing.dto.response.AuthResponse;
import com.ubs.billing.dto.response.BillResponse;
import com.ubs.billing.dto.response.CustomerResponse;
import com.ubs.billing.dto.response.MeterReadingResponse;
import com.ubs.billing.dto.response.MeterResponse;
import com.ubs.billing.dto.response.PaymentResponse;
import com.ubs.billing.dto.response.UserResponse;
import com.ubs.billing.entity.BillStatus;
import com.ubs.billing.entity.MeterType;
import com.ubs.billing.smoke.support.SmokeTestData;
import com.ubs.billing.smoke.support.SmokeTestSupport;
import com.ubs.billing.util.ApiResponse;
import com.ubs.billing.util.Constants;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Tag("smoke")
@DisplayName("Utility Billing System Smoke Tests")
class SmokeTest extends AbstractSmokeTest {

    @Test
    @DisplayName("1. Authentication flow - admin login returns JWT")
    void adminLoginReturnsJwtToken() throws Exception {
        MvcResult result = mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "email": "%s",
                                  "password": "%s"
                                }
                                """.formatted(SmokeTestData.ADMIN_EMAIL, SmokeTestData.ADMIN_PASSWORD)))
                .andExpect(status().isOk())
                .andReturn();

        ApiResponse<AuthResponse> response = objectMapper.readValue(
                result.getResponse().getContentAsString(),
                new TypeReference<>() {
                });

        assertNotNull(response);
        assertTrue(response.isSuccess());
        assertNotNull(response.getData());
        assertNotNull(response.getData().getAccessToken());
    }

    @Test
    @DisplayName("2. Swagger UI is accessible")
    void swaggerUiIsAccessible() throws Exception {
        mockMvc.perform(get("/swagger-ui/index.html"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("3. Admin can create operator staff")
    void adminCanCreateOperatorStaff() throws Exception {
        String adminToken = smoke.adminToken();

        CreateStaffRequest request = CreateStaffRequest.builder()
                .fullName("Smoke Operator")
                .email(SmokeTestData.uniqueEmail("operator"))
                .phoneNumber(SmokeTestData.uniquePhone())
                .role(Constants.ROLE_OPERATOR)
                .build();

        MvcResult result = mockMvc.perform(post("/users/staff")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andReturn();

        ApiResponse<UserResponse> response = objectMapper.readValue(
                result.getResponse().getContentAsString(),
                new TypeReference<>() {
                });

        assertNotNull(response);
        assertTrue(response.isSuccess());
        assertNotNull(response.getData());
        assertNotNull(response.getData().getId());
        assertTrue(response.getData().getRoles().contains(Constants.ROLE_OPERATOR));
    }

    @Test
    @DisplayName("4. Admin can create customer")
    void adminCanCreateCustomer() throws Exception {
        String adminToken = smoke.adminToken();

        CustomerResponse customer = smoke.createCustomer(adminToken);

        assertNotNull(customer.getId());
    }

    @Test
    @DisplayName("5. Admin can create meter for customer")
    void adminCanCreateMeter() throws Exception {
        String adminToken = smoke.adminToken();
        smoke.createTariff(adminToken);
        CustomerResponse customer = smoke.createCustomer(adminToken);

        MeterResponse meter = smoke.createMeter(adminToken, customer.getId());

        assertNotNull(meter.getId());
        assertNotNull(meter.getMeterNumber());
    }

    @Test
    @DisplayName("6. Operator can create meter reading")
    void operatorCanCreateMeterReading() throws Exception {
        SmokeTestSupport.BillingContext context = smoke.prepareBillingContext();

        MeterReadingResponse reading = context.reading();

        assertNotNull(reading.getId());
        assertTrue(reading.getConsumption().compareTo(BigDecimal.ZERO) > 0);
    }

    @Test
    @DisplayName("7. Bill is generated from meter reading")
    void billIsGeneratedFromMeterReading() throws Exception {
        SmokeTestSupport.BillingContext context = smoke.prepareBillingContext();

        BillResponse bill = smoke.generateBill(context.adminToken(), context.reading().getId());

        assertNotNull(bill.getId());
        assertNotNull(bill.getBillReference());
        assertTrue(bill.getTotalAmount().compareTo(BigDecimal.ZERO) > 0);
    }

    @Test
    @DisplayName("8. Admin can approve bill")
    void adminCanApproveBill() throws Exception {
        SmokeTestSupport.BillingContext context = smoke.prepareBillingContext();
        BillResponse generated = smoke.generateBill(context.adminToken(), context.reading().getId());

        BillResponse approved = smoke.approveBill(context.adminToken(), generated.getId());

        assertEquals(BillStatus.APPROVED, approved.getStatus());
        assertTrue(approved.getApproved());
        assertNotNull(approved.getDueDate());
    }

    @Test
    @DisplayName("9. Finance can record payment")
    void financeCanRecordPayment() throws Exception {
        SmokeTestSupport.BillingContext context = smoke.prepareBillingContext();
        BillResponse generated = smoke.generateBill(context.adminToken(), context.reading().getId());
        BillResponse approved = smoke.approveBill(context.adminToken(), generated.getId());
        String financeToken = smoke.createAndActivateStaff(Constants.ROLE_FINANCE, context.adminToken());

        PaymentResponse payment = smoke.recordPayment(
                financeToken,
                approved.getId(),
                approved.getBalance());

        assertNotNull(payment.getBillBalanceAfterPayment());
        assertTrue(payment.getBillBalanceAfterPayment().compareTo(BigDecimal.ZERO) >= 0);
    }

    @Test
    @DisplayName("10. Self-registered customer can access customer portal")
    void selfRegisteredCustomerCanAccessCustomerPortal() throws Exception {
        String customerToken = smoke.registerAndVerifyCustomer();

        mockMvc.perform(get("/my/bills")
                        .header("Authorization", "Bearer " + customerToken))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("11. Customer cannot access admin tariff endpoint")
    void customerCannotAccessAdminTariffEndpoint() throws Exception {
        String customerToken = smoke.registerAndVerifyCustomer();

        CreateTariffRequest request = CreateTariffRequest.builder()
                .meterType(MeterType.WATER)
                .rate(new BigDecimal("450.00"))
                .serviceCharge(new BigDecimal("1500.00"))
                .vat(new BigDecimal("18.00"))
                .penaltyRate(new BigDecimal("50.00"))
                .effectiveDate(LocalDate.of(2026, 1, 1))
                .active(true)
                .build();

        mockMvc.perform(post("/tariffs")
                        .header("Authorization", "Bearer " + customerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }
}
