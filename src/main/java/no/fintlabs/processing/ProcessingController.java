package no.fintlabs.processing;

import no.fint.model.resource.personvern.samtykke.BehandlingResources;
import no.vigoiks.resourceserver.security.FintJwtEndUserPrincipal;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/api")
public class ProcessingController {
    private final ProcessingService processingService;

    public ProcessingController(ProcessingService processingService) {
        this.processingService = processingService;
    }

    @GetMapping("/processing")
    public Mono<BehandlingResources> getProcessings(@AuthenticationPrincipal Jwt jwt){
        return processingService.getProcessings(FintJwtEndUserPrincipal.from(jwt));

    }
}
