package com.ubs.billing.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "app.billing")
public class BillingProperties {

    private int dueDays = 30;
    private int overdueDisconnectDays = 60;
}
