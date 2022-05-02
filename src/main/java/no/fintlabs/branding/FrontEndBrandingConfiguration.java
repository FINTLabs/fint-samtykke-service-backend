package no.fintlabs.configuration;


import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Data
@ConfigurationProperties(prefix = "fint.branding")
public class FrontEndBrandingConfiguration {
    private String logo;
    private String primaryColor;
    private String primaryColorLight;
    private String secondaryColor;
    private String featureColor1;
    private String featureColor2;
    private String phoneNumber;
    private String mail;
    private String countyName;
}
