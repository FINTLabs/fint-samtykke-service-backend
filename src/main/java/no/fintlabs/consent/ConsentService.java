package no.fintlabs.consent;

import lombok.extern.slf4j.Slf4j;
import no.fint.model.felles.kompleksedatatyper.Identifikator;
import no.fint.model.felles.kompleksedatatyper.Periode;
import no.fint.model.resource.Link;
import no.fint.model.resource.personvern.kodeverk.BehandlingsgrunnlagResource;
import no.fint.model.resource.personvern.kodeverk.PersonopplysningResource;
import no.fint.model.resource.personvern.samtykke.*;
import no.fintlabs.apiConsent.ApiConsent;
import no.fintlabs.apiConsent.ApiConsentService;
import no.fintlabs.fint.FintClient;
import no.fintlabs.fint.FintEndpointConfiguration;
import no.fintlabs.processing.ProcessingService;
import no.vigoiks.resourceserver.security.FintJwtEndUserPrincipal;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import no.fintlabs.person.PersonService;
import reactor.core.publisher.Mono;

import java.time.Clock;
import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

@Slf4j
@Component
public class ConsentService {

    private final FintClient fintClient;
    private final FintEndpointConfiguration fintEndpointConfiguration;
    private final PersonService personService;

    public ConsentService(FintClient fintClient,
                          FintEndpointConfiguration fintEndpointConfiguration,
                          PersonService personService) {
        this.fintClient = fintClient;
        this.fintEndpointConfiguration = fintEndpointConfiguration;
        this.personService = personService;
    }

    public Mono<SamtykkeResources> getFilteredConsents(FintJwtEndUserPrincipal principal) throws ExecutionException, InterruptedException {
        String oDataFilter = "links/person/any(a:a/href eq '" + personService.getPersonUri(principal) + "')";
        return fintClient.getResource(fintEndpointConfiguration.getBaseUri() +
                fintEndpointConfiguration.getConsentUri() + "?$filter=" + oDataFilter, SamtykkeResources.class);
    }


}
