package no.fintlabs.service;

import no.fintlabs.configuration.FrontEndBrandingConfiguration;
import org.springframework.stereotype.Component;

@Component
public class BrandingService {
    private final FrontEndBrandingConfiguration frontEndBrandingConfiguration;

    public BrandingService(FrontEndBrandingConfiguration frontEndBrandingConfiguration) {
        this.frontEndBrandingConfiguration = frontEndBrandingConfiguration;
    }


    public FrontEndBrandingConfiguration getBranding() {
        //System.out.println("Service: " + frontEndBrandingConfiguration.getLogo());
        return frontEndBrandingConfiguration;
    }
}
