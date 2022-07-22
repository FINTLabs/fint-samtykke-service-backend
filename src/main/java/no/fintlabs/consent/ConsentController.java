package no.fintlabs.consent;

import lombok.extern.slf4j.Slf4j;
import no.vigoiks.resourceserver.security.FintJwtEndUserPrincipal;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.concurrent.ExecutionException;

@Slf4j
@RestController
@RequestMapping("/api")

public class ConsentController {
    private final ConsentService consentService;

    public ConsentController(ConsentService consentService) {
        this.consentService = consentService;
    }

    @GetMapping("/consents")
    public Mono<List<ApiConsent>> getApiConsents(@AuthenticationPrincipal Jwt jwt) throws ExecutionException, InterruptedException {
        return consentService.getApiConsents(FintJwtEndUserPrincipal.from(jwt));
    }

    @PostMapping("/{processingId}")
    public Mono<ApiConsent> addApiConsent(@PathVariable String processingId, @AuthenticationPrincipal Jwt jwt) throws ExecutionException, InterruptedException {
        return consentService.addConsent(processingId, FintJwtEndUserPrincipal.from(jwt));
    }

    @PutMapping("/{consentId}/{processingId}/{active}")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public Mono<ApiConsent> updateApiConsent(@PathVariable String consentId, String processingId, boolean active, @AuthenticationPrincipal Jwt jwt) throws ExecutionException, InterruptedException {
        return consentService.updateConsent(consentId, processingId, active, FintJwtEndUserPrincipal.from(jwt));
    }




}
