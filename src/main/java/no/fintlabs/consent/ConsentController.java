package no.fintlabs.consent;

import lombok.extern.slf4j.Slf4j;
import no.fint.model.resource.personvern.samtykke.SamtykkeResource;
import no.vigoiks.resourceserver.security.FintJwtEndUserPrincipal;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.concurrent.ExecutionException;

@Slf4j
@RestController
@RequestMapping("/api")
public class ConsentController {
    private final ConsentService consentService;

    public ConsentController(ConsentService concentService, ConsentService consentService) {
        this.consentService = consentService;
    }

    @GetMapping("/consents")
    public SamtykkeResource getConsents(@AuthenticationPrincipal Jwt jwt) throws ExecutionException, InterruptedException {
        return consentService.getConsents(FintJwtEndUserPrincipal.from(jwt));


    }
}
