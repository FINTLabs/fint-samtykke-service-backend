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
    private final FrontEndBrandingConfiguration frontEndBrandingConfiguration;

    public BrandingController(BrandingService brandingService, FrontEndBrandingConfiguration frontEndBrandingConfiguration) { this.brandingService = brandingService;
        this.frontEndBrandingConfiguration = frontEndBrandingConfiguration;
    }

    @GetMapping
    public FrontEndBrandingConfiguration getBranding(){

        return frontEndBrandingConfiguration;
    }
//    @GetMapping
//    public Mono<FrontEndBrandingConfiguration> getBranding(){
//        FrontEndBrandingConfiguration frontEndBrandingConfiguration = brandingService.getBranding();
//        return Mono.just(frontEndBrandingConfiguration);
//    }


}
