package no.fintlabs.controller;

import lombok.extern.slf4j.Slf4j;
import no.fintlabs.configuration.FrontEndBrandingConfiguration;
import no.fintlabs.service.BrandingService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/api/branding")
public class BrandingController {
    private final BrandingService brandingService;

    public BrandingController(BrandingService brandingService) { this.brandingService = brandingService; }

    @GetMapping
    public FrontEndBrandingConfiguration getBranding(){
        FrontEndBrandingConfiguration frontEndBrandingConfiguration = brandingService.geBranding();
        System.out.println("Controller: " + frontEndBrandingConfiguration.getLogo());

        return frontEndBrandingConfiguration;
    }

}
