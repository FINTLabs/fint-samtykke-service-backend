package no.fintlabs.processors;

import lombok.extern.slf4j.Slf4j;
import no.fint.model.resource.personvern.samtykke.TjenesteResources;
import no.fintlabs.fint.FintClient;
import no.fintlabs.fint.FintEndpointConfiguration;
import no.vigoiks.resourceserver.security.FintJwtEndUserPrincipal;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

@Slf4j
@Component
public class ProcessorService {
    private final FintClient fintClient;
    private final FintEndpointConfiguration fintEndpointConfiguration;

    public ProcessorService(FintClient fintClient, FintEndpointConfiguration fintEndpointConfiguration) {
        this.fintClient = fintClient;
        this.fintEndpointConfiguration = fintEndpointConfiguration;
    }

    public Mono<TjenesteResources> getProcessors(FintJwtEndUserPrincipal principal){
        Mono<TjenesteResources> processors = fintClient.getResource(fintEndpointConfiguration.getBaseUri()+
                fintEndpointConfiguration.getProcessorsUri(),TjenesteResources.class);
        return processors;
    }
}
