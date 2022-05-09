package no.fintlabs.consent;

import lombok.extern.slf4j.Slf4j;
import no.fint.model.resource.personvern.samtykke.SamtykkeResource;
import no.fint.model.resource.personvern.samtykke.SamtykkeResources;
import no.fintlabs.fint.FintClient;
import no.fintlabs.fint.FintEndpointConfiguration;
import no.vigoiks.resourceserver.security.FintJwtEndUserPrincipal;
import org.springframework.stereotype.Component;
import no.fintlabs.person.PersonService;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.concurrent.ExecutionException;
@Slf4j
@Component
public class ConsentService {

    private final FintClient fintClient;
    private final FintEndpointConfiguration fintEndpointConfiguration;
    private final PersonService personService;

    public ConsentService(FintClient fintClient, FintEndpointConfiguration fintEndpointConfiguration, PersonService personService) {
        this.fintClient = fintClient;
        this.fintEndpointConfiguration = fintEndpointConfiguration;
        this.personService = personService;
    }

    public Mono<SamtykkeResources> getConsents(FintJwtEndUserPrincipal principal) throws ExecutionException, InterruptedException {
        String oDataFilter = "links/person/any(a:a/href eq '" + personService.getPersonUri(principal) +"')";
        log.info("Samtykke URI ::: " + fintEndpointConfiguration.getBaseUri() +
                fintEndpointConfiguration.getConsentUri() + "?$filter=" + oDataFilter);

        return fintClient.getResource(fintEndpointConfiguration.getBaseUri() +
                fintEndpointConfiguration.getConsentUri() + "?$filter=" + oDataFilter, SamtykkeResources.class);


//        Mono<SamtykkeResource> consents = fintClient.getResource(fintEndpointConfiguration.getBaseUri() +
//                fintEndpointConfiguration.getConsentUri() + "?$filter=" + oDataFilter, SamtykkeResource.class);   //toFuture().get();
//        return consents;


    }
}
