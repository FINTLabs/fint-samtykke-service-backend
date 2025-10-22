package no.fintlabs.apiconsent;


import lombok.extern.slf4j.Slf4j;
import no.fintlabs.consent.ConsentService;
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
@RequestMapping("/consent")
public class ApiConsentController {
    private final ApiConsentService apiConsentService;



    public ApiConsentController(ApiConsentService apiConsentService) {
        this.apiConsentService = apiConsentService;
    }

    @GetMapping
    public Mono<List<ApiConsent>> getApiConsents(@AuthenticationPrincipal Jwt jwt) throws ExecutionException, InterruptedException {
        log.info("Getting list of processings");
        return apiConsentService.getApiConsents(FintJwtEndUserPrincipal.from(jwt));
    }

    @PostMapping("/{processingId}")
    public Mono<ApiConsent> addApiConsent(@PathVariable String processingId,
                                          @AuthenticationPrincipal Jwt jwt) throws ExecutionException, InterruptedException {
        log.debug("Add new consent to processing {}", processingId);
        return apiConsentService.addConsent(processingId, FintJwtEndUserPrincipal.from(jwt));

    }

    @PutMapping("/{consentId}/{processingId}/{active}")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public Mono<ApiConsent> updateApiConsent(@PathVariable String consentId,
                                             @PathVariable String processingId,
                                             @PathVariable boolean active,
                                             @AuthenticationPrincipal Jwt jwt) throws ExecutionException, InterruptedException {
        log.debug("Update consent to processing {}", consentId);
        try {
            log.info("Trying to update consent ");
            FintJwtEndUserPrincipal prinsipal = FintJwtEndUserPrincipal.from(jwt);
            log.info("Updated consent {} for {}", consentId,prinsipal.getGivenName());
            return apiConsentService.updateConsent(consentId, processingId, active, prinsipal);
        } catch (Exception e){
            log.error("Something went wrong during update of consent", e);
            return Mono.error(e);
        }
    }
}
