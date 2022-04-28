package no.fintlabs.configuration;

import lombok.Value;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import lombok.Getter;

@Getter
@Configuration
@ConfigurationProperties(prefix = "concent-config.endpoints.fint-uri")
public class FintEndpointConfiguration {

        private String baseUri;
        private String employeeUri;







}
