package no.fintlabs.fint;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;


@Getter
@Setter
@Configuration
@ConfigurationProperties(prefix = "fint.concent.endpoints")
public class FintEndpointConfiguration {

    private String baseUri;
    private String employeeUri;
    private String employeessnUri;
    private String studentUri;
    private String studentssnUri;
    private String consentUri;
    private String processorsUri;
    private String processorUri;
    private String processingUri;
    private String processingbaseUri;
    private String personaldataUri;
}
