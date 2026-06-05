package com.ubs.billing.service;

import com.ubs.billing.config.MailProperties;
import com.ubs.billing.entity.Bill;
import com.ubs.billing.entity.User;
import com.ubs.billing.exception.BadRequestException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

import java.util.Collection;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmailService {

    private final JavaMailSender mailSender;
    private final MailProperties mailProperties;

    public void sendOtpEmail(User user, String otpCode) {
        sendEmail(
                user,
                "Verify your Utility Billing System account",
                buildOtpEmailBody(user, otpCode)
        );
    }

    public void sendPasswordResetOtpEmail(User user, String otpCode) {
        sendEmail(
                user,
                "Reset your Utility Billing System password",
                buildPasswordResetOtpEmailBody(user, otpCode)
        );
    }

    public void sendStaffWelcomeEmail(User user, String temporaryPassword, String roleName) {
        sendEmail(
                user,
                "Your Utility Billing System staff account",
                buildStaffWelcomeEmailBody(user, temporaryPassword, roleName)
        );
    }

    public void sendCustomerWelcomeEmail(User user, String temporaryPassword) {
        sendEmail(
                user,
                "Your Utility Billing System customer account",
                buildCustomerWelcomeEmailBody(user, temporaryPassword)
        );
    }

    public void sendRoleChangeEmail(User user, Collection<String> roles) {
        sendEmail(
                user,
                "Your Utility Billing System role has been updated",
                buildRoleChangeEmailBody(user, roles)
        );
    }

    public void sendBillApprovedEmail(User user, Bill bill) {
        if (user == null) {
            log.warn("Skipping bill approval email because no user account is linked to customer email");
            return;
        }
        sendEmailSafely(
                user,
                "Your utility bill has been approved",
                buildBillApprovedEmailBody(user, bill)
        );
    }

    private void sendEmail(User user, String subject, String body) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(mailProperties.getFrom());
        message.setTo(user.getEmail());
        message.setSubject(subject);
        message.setText(body);

        try {
            mailSender.send(message);
            log.info("Email sent to {} with subject '{}'", user.getEmail(), subject);
        } catch (MailException ex) {
            log.error("Failed to send email to {}", user.getEmail(), ex);
            throw new BadRequestException("Unable to send email. Please try again later.");
        }
    }

    private void sendEmailSafely(User user, String subject, String body) {
        try {
            sendEmail(user, subject, body);
        } catch (BadRequestException ex) {
            log.warn("Bill approval email delivery failed for {}: {}", user.getEmail(), ex.getMessage());
        }
    }

    private String buildOtpEmailBody(User user, String otpCode) {
        String displayName = user.getFullName() != null ? user.getFullName() : user.getUsername();
        return """
                Hello %s,

                Thank you for registering with Utility Billing System.

                Your email verification code is: %s

                This code expires in a few minutes. If you did not request this, please ignore this email.

                Verify your account at: %s/api/auth/verify-otp

                Regards,
                Utility Billing System Team
                """.formatted(displayName, otpCode, mailProperties.getBaseUrl());
    }

    private String buildPasswordResetOtpEmailBody(User user, String otpCode) {
        String displayName = user.getFullName() != null ? user.getFullName() : user.getUsername();
        return """
                Hello %s,

                We received a request to reset your Utility Billing System password.

                Your password reset code is: %s

                This code expires in a few minutes. If you did not request a password reset, please ignore this email.

                After verifying the OTP, reset your password at: %s/api/auth/password/reset

                Regards,
                Utility Billing System Team
                """.formatted(displayName, otpCode, mailProperties.getBaseUrl());
    }

    private String buildCustomerWelcomeEmailBody(User user, String temporaryPassword) {
        String displayName = user.getFullName() != null ? user.getFullName() : user.getUsername();
        return """
                Hello %s,

                An administrator has registered you as a Utility Billing System customer.

                Full Name: %s
                Email: %s
                Temporary Password: %s

                Login instructions:
                1. Sign in at %s/api/auth/login using your email and temporary password.
                2. You will be required to change your password before accessing customer endpoints.
                3. Use POST %s/api/auth/change-temporary-password after login.

                After signing in, view your bills at %s/api/my/bills

                Security notice:
                - Do not share your temporary password.
                - Change it immediately on first login.
                - Contact your administrator if you did not expect this account.

                Regards,
                Utility Billing System Team
                """.formatted(
                displayName,
                displayName,
                user.getEmail(),
                temporaryPassword,
                mailProperties.getBaseUrl(),
                mailProperties.getBaseUrl(),
                mailProperties.getBaseUrl());
    }

    private String buildStaffWelcomeEmailBody(User user, String temporaryPassword, String roleName) {
        String displayName = user.getFullName() != null ? user.getFullName() : user.getUsername();
        return """
                Hello %s,

                An administrator has created your Utility Billing System staff account.

                Full Name: %s
                Email: %s
                Assigned Role: %s
                Temporary Password: %s

                Login instructions:
                1. Sign in at %s/api/auth/login using your email and temporary password.
                2. You will be required to change your password before accessing business endpoints.
                3. Use POST %s/api/auth/change-temporary-password after login.

                Security notice:
                - Do not share your temporary password.
                - Change it immediately on first login.
                - Contact your administrator if you did not expect this account.

                Regards,
                Utility Billing System Team
                """.formatted(
                displayName,
                displayName,
                user.getEmail(),
                roleName,
                temporaryPassword,
                mailProperties.getBaseUrl(),
                mailProperties.getBaseUrl());
    }

    private String buildRoleChangeEmailBody(User user, Collection<String> roles) {
        String displayName = user.getFullName() != null ? user.getFullName() : user.getUsername();
        String roleList = String.join(", ", roles);
        return """
                Hello %s,

                Your Utility Billing System access has been updated.

                New role(s): %s

                Access privileges notice:
                - Your permissions now reflect the assigned role(s).
                - Sign out and sign back in if you do not see expected access immediately.

                Login instructions:
                - Sign in at %s/api/auth/login with your email and password.

                If you did not expect this change, contact your administrator immediately.

                Regards,
                Utility Billing System Team
                """.formatted(displayName, roleList, mailProperties.getBaseUrl());
    }

    private String buildBillApprovedEmailBody(User user, Bill bill) {
        String displayName = user.getFullName() != null ? user.getFullName() : user.getUsername();
        String dueDateText = bill.getDueDate() != null
                ? bill.getDueDate().toString()
                : "the due date shown in your account";
        return """
                Hello %s,

                Your utility bill %s has been approved.

                Total amount due: %s FRW
                Due date: %s

                Sign in to view your bills at %s/api/my/bills

                Regards,
                Utility Billing System Team
                """.formatted(
                displayName,
                bill.getBillReference(),
                bill.getTotalAmount(),
                dueDateText,
                mailProperties.getBaseUrl());
    }
}
