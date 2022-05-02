package no.fintlabs.configuration;

import lombok.Setter;
import lombok.Getter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;


@Getter
@Setter
@Configuration
//@EnableConfigurationProperties
@ConfigurationProperties(prefix = "fint.concent.endpoints")
public class FintEndpointConfiguration {

        private String baseUri;
        private String employeeUri;
        private String employeessnUri;
        private String studentUri;
        private String studentssnUri;
        private String consentUri;
        private String processorUri;
        private String processingUri;
        private String processingbaseUri;
        private String personaldataUri;

}
