package no.fintlabs.consent;

import lombok.extern.slf4j.Slf4j;
import no.fint.model.felles.kompleksedatatyper.Identifikator;
import no.fint.model.felles.kompleksedatatyper.Periode;
import no.fint.model.resource.Link;
import no.fint.model.resource.personvern.samtykke.BehandlingResource;
import no.fint.model.resource.personvern.samtykke.SamtykkeResource;
import no.fint.model.resource.personvern.samtykke.SamtykkeResources;
import no.fintlabs.fint.FintClient;
import no.fintlabs.fint.FintEndpointConfiguration;
import no.fintlabs.person.PersonService;
import no.vigoiks.resourceserver.security.FintJwtEndUserPrincipal;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.time.Clock;
import java.util.*;
import java.util.concurrent.ExecutionException;


@Slf4j
@Service
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
        log.info("Fetching consents for: " + principal.getSurname() + " " + principal.getGivenName());
        String oDataFilter = "links/person/any(a:a/href eq '" + personService.getPersonUri(principal) + "')";
        return fintClient.getResource(fintEndpointConfiguration.getBaseUri() +
                fintEndpointConfiguration.getConsentUri() + "?$filter=" + oDataFilter, SamtykkeResources.class);
    }


    public String getConsentProcessingUri(SamtykkeResource consent) {
        Map<String, List<Link>> consentLinks = consent.getLinks();
        return String.valueOf(consentLinks.get("behandling").get(0));
    }

    public SamtykkeResource addConsent(String processingId, FintJwtEndUserPrincipal principal, boolean isConsentGiven) throws ExecutionException, InterruptedException {
        Link processingLink = new Link(fintEndpointConfiguration.getBaseUri() + fintEndpointConfiguration.getProcessingUri()
                + "systemid/" + processingId);
        Link personLink = new Link(personService.getPersonUri(principal));

        Periode consentTime = new Periode();
        {
            if (isConsentGiven) {
                consentTime.setBeskrivelse("Samtykke gitt");
                consentTime.setStart(Date.from(Clock.systemUTC().instant()));
            } else {
                consentTime.setBeskrivelse("Tom samtykke opprettet");
            }
        }

        Identifikator consentSystemId = new Identifikator();
        {
            consentSystemId.setIdentifikatorverdi(UUID.randomUUID().toString());
        }

        SamtykkeResource consent = new SamtykkeResource();
        {
            consent.setGyldighetsperiode(consentTime);
            consent.setOpprettet(consentTime.getStart());
            consent.setSystemId(consentSystemId);
            consent.addPerson(personLink);
            consent.addBehandling(processingLink);
        }

        return consent;
    }

    public SamtykkeResource saveConsentToDatabase(SamtykkeResource samtykkeResource) throws ExecutionException, InterruptedException {
        try {
            ResponseEntity<Void> response = fintClient.postResource(fintEndpointConfiguration.getBaseUri()
                    + fintEndpointConfiguration.getConsentUri(), samtykkeResource, SamtykkeResource.class).toFuture().get();
            String locationUrl = response.getHeaders().getLocation().toString();
            log.info("Added new consent with status : " + response.getStatusCode());
            log.debug("Location uri til new consent : " + locationUrl);
            ResponseEntity<Void> rs = fintClient.waitUntilCreated(locationUrl);
            log.info("Created new consent with status :" + rs.getStatusCode());

            return fintClient.getResource(response.getHeaders().getLocation().toString(), SamtykkeResource.class).toFuture().get();
        } catch (Exception exception) {
            log.error("Error in saveConsentToDatabase", exception);
            throw exception;
        }
    }

    public SamtykkeResource updateConsentToFINT(SamtykkeResource samtykkeResource) throws ExecutionException, InterruptedException {
        Periode periode = new Periode();
        {
            periode.setBeskrivelse("Gir samtykke");
            periode.setStart(Date.from(Clock.systemUTC().instant()));
            periode.setSlutt(null);
        }
        samtykkeResource.setGyldighetsperiode(periode);

        String consentId = samtykkeResource.getSystemId().getIdentifikatorverdi();

        try {
            ResponseEntity<Void> response = fintClient.putResource(fintEndpointConfiguration.getBaseUri()
                    + fintEndpointConfiguration.getConsentUri() + "systemid/" + consentId, samtykkeResource, SamtykkeResource.class).toFuture().get();
            String locationUrl = response.getHeaders().getLocation().toString();
            log.info("Added new consent with status : " + response.getStatusCode());
            log.debug("Location uri til new consent : " + locationUrl);

            ResponseEntity<Void> rs = fintClient.waitUntilCreated(locationUrl);
            log.info("Created new consent with status :" + rs.getStatusCode());

            return fintClient.getResource(locationUrl, SamtykkeResource.class).toFuture().get();
        } catch (Exception exception) {
            log.error("Error in updateConsentToFint", exception);
            throw exception;
        }
    }

    public SamtykkeResource withdrawConsent(SamtykkeResource samtykkeResource, String consentId) throws ExecutionException, InterruptedException {

        Periode periode = new Periode();
        {
            periode.setBeskrivelse("Samtykke trukket");
            periode.setStart(samtykkeResource.getGyldighetsperiode().getStart());
            periode.setSlutt(Date.from(Clock.systemUTC().instant()));
        }
        samtykkeResource.setGyldighetsperiode(periode);

        try {
            ResponseEntity<Void> response = fintClient.putResource(fintEndpointConfiguration
                    .getBaseUri() + fintEndpointConfiguration
                    .getConsentUri() + "systemid/" + consentId, samtykkeResource, SamtykkeResource.class).toFuture().get();

            log.info("update consent : withdrawn sent : " + response.getStatusCode());
            ResponseEntity<Void> rs = fintClient.waitUntilCreated(response.getHeaders().getLocation().toString());
            log.info("updated consent : withdrawn confirmed : " + rs.getStatusCode());

            SamtykkeResource withdrawnConsent = fintClient.getResource(response.getHeaders().getLocation().toString(), SamtykkeResource.class).toFuture().get();

            return withdrawnConsent;
        } catch (Exception exception) {
            log.error("Error in withdrawConsent", exception);
            throw exception;
        }
    }

    public Optional<SamtykkeResource> findNewestConsent(SamtykkeResources samtykkeResources, BehandlingResource behandlingResource) {
        String behandlingSelfLink = String.valueOf(behandlingResource.getSelfLinks().get(0));
        log.debug("ProcessingSelfLink to compare : " + behandlingSelfLink);
        return samtykkeResources.getContent()
                .stream()
                .filter(con -> getConsentProcessingUri(con)
                        .equals(behandlingSelfLink))
                .filter(con -> con.getGyldighetsperiode().getStart() != null)
                .max(Comparator.comparing(p -> p.getGyldighetsperiode().getStart()));
    }
}
