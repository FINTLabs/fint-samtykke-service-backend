package no.fintlabs.consent;

import lombok.extern.slf4j.Slf4j;
import no.fint.model.felles.kompleksedatatyper.Identifikator;
import no.fint.model.felles.kompleksedatatyper.Periode;
import no.fint.model.resource.Link;
import no.fint.model.resource.personvern.samtykke.*;
import no.fintlabs.apiconsent.ApiConsent;
import no.fintlabs.fint.FintClient;
import no.fintlabs.fint.FintEndpointConfiguration;
import no.vigoiks.resourceserver.security.FintJwtEndUserPrincipal;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import no.fintlabs.person.PersonService;
import reactor.core.publisher.Mono;

import java.time.Clock;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;

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


    public String getConsentProcessingUri(SamtykkeResource consent) {
        Map<String, List<Link>> consentLinks = consent.getLinks();
        String consentProcessingLink = String.valueOf(consentLinks.get("behandling").get(0));
        return consentProcessingLink;
    }
    public SamtykkeResource addConsent(String processingId, FintJwtEndUserPrincipal principal) throws ExecutionException, InterruptedException {
        Link processingLink = new Link(fintEndpointConfiguration.getBaseUri() + fintEndpointConfiguration.getProcessingUri()
                + "systemid/" + processingId);
        Link personLink = new Link(personService.getPersonUri(principal));

        Periode consentTime = new Periode();
        {
            consentTime.setBeskrivelse("Opprettelse av samtykke");
            consentTime.setStart(Date.from(Clock.systemUTC().instant()));
        }

        Identifikator consentSystemId = new Identifikator();
        {
            consentSystemId.setGyldighetsperiode(consentTime);
            consentSystemId.setIdentifikatorverdi("systemId");
        }

        SamtykkeResource consent = new SamtykkeResource();
        {
            consent.setGyldighetsperiode(consentTime);
            consent.setOpprettet(consentTime.getStart());
            consent.setSystemId(consentSystemId);
            consent.addPerson(personLink);
            consent.addBehandling(processingLink);
        }

        ResponseEntity<Void> response = fintClient.postResource(fintEndpointConfiguration.getBaseUri()
                + fintEndpointConfiguration.getConsentUri(), consent, SamtykkeResource.class).toFuture().get();
        log.info("Added new consent with status : " + response.getStatusCode().name());
        log.info("Location uri til new consent : " + response.getHeaders().getLocation().toString());

        ResponseEntity<Void> rs = fintClient.waitUntilCreated(response.getHeaders().getLocation().toString()).toFuture().get();
        log.info("Created new consent with status :" + rs.getStatusCode().name());

        SamtykkeResource createdConsent = fintClient.getResource(response.getHeaders().getLocation().toString(), SamtykkeResource.class).toFuture().get();

        return createdConsent;


    }
}
