package com.project3.userservice.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Data
@Configuration
@ConfigurationProperties(prefix = "idp")
public class KeycloakProperties {
    private String url;
    private String clientId;
    private String clientSecret;
    private String realm;
    private String redirectUri;
    private String frontendUrl;
    private SmtpProperties smtp;
    
    @Data
    public static class SmtpProperties {
        private String host;
        private String port;
        private String from;
        private String fromDisplayName;
        private String user;
        private String password;
        private boolean auth = true;
        private boolean ssl = false;
        private boolean starttls = true;
    }
}

