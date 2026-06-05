package com.ubs.billing.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "app.mail")
public class MailProperties {

    private String baseUrl = "http://localhost:8080";
    private String from = "no-reply@ubs.example.com";
}
