package com.ubs.billing.security;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "app.otp")
public class OtpProperties {

    private int expirationMinutes = 10;
    private int length = 6;
}
