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
    private final ProcessingService processingService;

    private final ApiConsentService apiConsentService;

    public ConsentService(FintClient fintClient,
                          FintEndpointConfiguration fintEndpointConfiguration,
                          PersonService personService,
                          ProcessingService processingService,
                          ApiConsentService apiConsentService) {
        this.fintClient = fintClient;
        this.fintEndpointConfiguration = fintEndpointConfiguration;
        this.personService = personService;
        this.processingService = processingService;
        this.apiConsentService = apiConsentService;
    }

    public Mono<SamtykkeResources> getFilteredConsents(FintJwtEndUserPrincipal principal) throws ExecutionException, InterruptedException {
        String oDataFilter = "links/person/any(a:a/href eq '" + personService.getPersonUri(principal) + "')";
        return fintClient.getResource(fintEndpointConfiguration.getBaseUri() +
                fintEndpointConfiguration.getConsentUri() + "?$filter=" + oDataFilter, SamtykkeResources.class);
    }


    public Mono<List<ApiConsent>> getApiConsents(FintJwtEndUserPrincipal principal) throws ExecutionException, InterruptedException {

        SamtykkeResources samtykkeResources = Objects.requireNonNull(getFilteredConsents(principal).toFuture().get());

        List<ApiConsent> apiConsents = Objects.requireNonNull(processingService.getProcessings(principal).toFuture().get().getContent())
                .stream()
                .map(behandlingResource -> buildApiConsent(samtykkeResources, behandlingResource))
                .collect(Collectors.toList());

        return Mono.just(apiConsents);
    }

    private ApiConsent buildApiConsent(SamtykkeResources samtykkeResources, BehandlingResource behandlingResource) {

        SamtykkeResource samtykkeResource = samtykkeResources.getContent().stream()
                .filter(con -> con.getGyldighetsperiode().getStart() != null)
                .max(Comparator.comparing(p -> p.getGyldighetsperiode().getStart()))
                .orElse(new SamtykkeResource());

        Map<String, List<Link>> processingsLinks = behandlingResource.getLinks();
        String processingBaseLink = String.valueOf(processingsLinks.get("behandlingsgrunnlag").get(0));
        String personalDataLink = String.valueOf(processingsLinks.get("personopplysning").get(0));
        String processorLink = String.valueOf(processingsLinks.get("tjeneste").get(0));
        TjenesteResource processor;
        try {
            processor = fintClient.getResource(processorLink, TjenesteResource.class).toFuture().get();
        } catch (InterruptedException | ExecutionException e) {
            throw new RuntimeException(e);
        }
        PersonopplysningResource personalData;
        try {
            personalData = fintClient.getResource(personalDataLink, PersonopplysningResource.class).toFuture().get();
        } catch (InterruptedException | ExecutionException e) {
            throw new RuntimeException(e);
        }
        BehandlingsgrunnlagResource processingBase;
        try {
            processingBase = fintClient.getResource(processingBaseLink, BehandlingsgrunnlagResource.class).toFuture().get();
        } catch (InterruptedException | ExecutionException e) {
            throw new RuntimeException(e);
        }

        return ApiConsent.builder()
                .systemIdValue(samtykkeResource.getSystemId().getIdentifikatorverdi())
                .processorName(processor.getNavn())
                .expirationDate(samtykkeResource.getGyldighetsperiode())
                .active(samtykkeResource.getGyldighetsperiode().getSlutt() == null &&
                        samtykkeResource.getGyldighetsperiode().getStart() != null)
                .personalDataName(personalData.getNavn())
                .processing(behandlingResource)
                .processingBase(processingBase)
                .build();
    }

    public Mono<ApiConsent> addConsent(String processingId, FintJwtEndUserPrincipal principal) throws ExecutionException, InterruptedException {
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

        return Mono.just(apiConsentService.create(principal, createdConsent, true));
    }

    public Mono<ApiConsent> updateConsent(String consentId, String processingId, boolean active, FintJwtEndUserPrincipal principal) throws ExecutionException, InterruptedException {
        SamtykkeResource samtykkeResource = fintClient.getResource(fintEndpointConfiguration.getBaseUri()
                + fintEndpointConfiguration.getConsentUri() + "systemid/" + consentId, SamtykkeResource.class).toFuture().get();
        if (samtykkeResource != null) {
            // consent given -> consent withdrawn :: update consent
            if (samtykkeResource.getGyldighetsperiode().getStart() != null
                    && samtykkeResource.getGyldighetsperiode().getSlutt() == null
                    && !active) {
                Periode periode = new Periode();
                {
                    periode.setBeskrivelse("Oppdatering av samtykke");
                    periode.setStart(samtykkeResource.getGyldighetsperiode().getStart());
                    periode.setSlutt(Date.from(Clock.systemUTC().instant()));
                }
                samtykkeResource.setGyldighetsperiode(periode);

                ResponseEntity<Void> response = fintClient.putResource(fintEndpointConfiguration.getBaseUri()
                        + fintEndpointConfiguration.getConsentUri()
                        + "systemid/" + consentId, samtykkeResource, SamtykkeResource.class).toFuture().get();

                log.info("Consent updated : withdrawn sent : " + response.getStatusCode().name());
                ResponseEntity<Void> rs = fintClient.waitUntilCreated(response.getHeaders().getLocation().toString()).toFuture().get();
                log.info("Consent updated : withdrawn confirmed : " + rs.getStatusCode().name());

                SamtykkeResource updatedConsent = fintClient.getResource(response.getHeaders().getLocation().toString(),SamtykkeResource.class).toFuture().get();
                return Mono.just(apiConsentService.create(principal, updatedConsent, false));

            // consent withdrawn -> consent given :: new consent
            } else if (samtykkeResource.getGyldighetsperiode().getSlutt() != null && active) {
                BehandlingResource behandlingResource = fintClient.getResource(fintEndpointConfiguration.getBaseUri()
                        + fintEndpointConfiguration.getProcessingUri()
                        + "systemid/" + processingId, BehandlingResource.class).toFuture().get();
                String behandlingsResourceId = behandlingResource.getSystemId().getIdentifikatorverdi();
                return addConsent(behandlingsResourceId, principal);

            }

        }
        log.info("Update consent : No consents found for systemId : " + consentId);
        return null;
    }

}
