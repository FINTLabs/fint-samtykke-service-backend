package no.fintlabs.branding;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

@Slf4j
@RestController
@RequestMapping("/api/branding")
public class BrandingController {
    private final FrontEndBrandingConfiguration frontEndBrandingConfiguration;

    public BrandingController(FrontEndBrandingConfiguration frontEndBrandingConfiguration) {
        this.frontEndBrandingConfiguration = frontEndBrandingConfiguration;
    }

    @GetMapping
    public ResponseEntity<Mono<FrontEndBrandingConfiguration>> getBranding() {
        return ResponseEntity.ok(Mono.just(frontEndBrandingConfiguration));
    }
}
