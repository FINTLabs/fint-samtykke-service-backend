package no.fintlabs.processing;

import no.fint.model.resource.personvern.samtykke.BehandlingResources;
import no.fintlabs.fint.FintClient;
import no.fintlabs.fint.FintEndpointConfiguration;
import no.vigoiks.resourceserver.security.FintJwtEndUserPrincipal;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

@Service
public class ProcessingService {
    private final FintClient fintclient;
    private final FintEndpointConfiguration fintEndpointConfiguration;

    public ProcessingService(FintClient fintclient, FintEndpointConfiguration fintEndpointConfiguration) {
        this.fintclient = fintclient;
        this.fintEndpointConfiguration = fintEndpointConfiguration;
    }

    public Mono<BehandlingResources> getProcessings(FintJwtEndUserPrincipal principal) {
        String processingsUrl = fintEndpointConfiguration.getBaseUri() +
                fintEndpointConfiguration.getProcessingUri();

        return fintclient.getResource(processingsUrl, BehandlingResources.class);
    }

}
