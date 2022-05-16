package no.fintlabs.processors;

import no.fint.model.resource.personvern.samtykke.TjenesteResources;
import no.vigoiks.resourceserver.security.FintJwtEndUserPrincipal;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/api")
public class ProcessorController {
    private final ProcessorService processorService;

    public ProcessorController(ProcessorService processorService) { this.processorService = processorService; }

    @GetMapping("/processors")
    public Mono<TjenesteResources> getProcessors(@AuthenticationPrincipal Jwt jwt){
        return processorService.getProcessors(FintJwtEndUserPrincipal.from(jwt));
    }
}


