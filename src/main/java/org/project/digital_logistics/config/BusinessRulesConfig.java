package org.project.digital_logistics.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "business")
@Data
public class BusinessRulesConfig {

    private Integer reservationTtlHours = 24;

    private Integer shipmentCutoffHour = 15;

    private Integer shipmentWaitHours = 12;
}

