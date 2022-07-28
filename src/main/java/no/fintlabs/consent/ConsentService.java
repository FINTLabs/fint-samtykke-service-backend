package no.fintlabs.consent;

import lombok.extern.slf4j.Slf4j;
import no.fint.model.felles.kompleksedatatyper.Identifikator;
import no.fint.model.felles.kompleksedatatyper.Periode;
import no.fint.model.resource.Link;
import no.fint.model.resource.personvern.kodeverk.BehandlingsgrunnlagResource;
import no.fint.model.resource.personvern.kodeverk.PersonopplysningResource;
import no.fint.model.resource.personvern.samtykke.*;
import no.fintlabs.fint.FintClient;
import no.fintlabs.fint.FintEndpointConfiguration;
import no.fintlabs.processing.ProcessingService;
import no.fintlabs.processors.ProcessorService;
import no.vigoiks.resourceserver.security.FintJwtEndUserPrincipal;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import no.fintlabs.person.PersonService;
import reactor.core.publisher.Mono;

import java.time.Clock;
import java.util.*;
import java.util.concurrent.ExecutionException;

@Slf4j
@Component
public class ConsentService {

    private final FintClient fintClient;
    private final FintEndpointConfiguration fintEndpointConfiguration;
    private final PersonService personService;
    private final ProcessingService processingService;

    public ConsentService(FintClient fintClient,
                          FintEndpointConfiguration fintEndpointConfiguration,
                          PersonService personService,
                          ProcessingService processingService,
                          ProcessorService processorService) {
        this.fintClient = fintClient;
        this.fintEndpointConfiguration = fintEndpointConfiguration;
        this.personService = personService;
        this.processingService = processingService;
    }

    public Mono<SamtykkeResources> getFilteredConsents(FintJwtEndUserPrincipal principal) throws ExecutionException, InterruptedException {
        String oDataFilter = "links/person/any(a:a/href eq '" + personService.getPersonUri(principal) + "')";
        return fintClient.getResource(fintEndpointConfiguration.getBaseUri() +
                fintEndpointConfiguration.getConsentUri() + "?$filter=" + oDataFilter, SamtykkeResources.class);
    }


    public Mono<List<ApiConsent>> getApiConsents(FintJwtEndUserPrincipal principal) throws ExecutionException, InterruptedException {

        List<ApiConsent> apiConsents = new ArrayList<>();

        SamtykkeResources samtykkeResources = Objects.requireNonNull(getFilteredConsents(principal).toFuture().get());

        Objects.requireNonNull(processingService.getProcessings(principal).toFuture().get().getContent())
                .forEach(behandlingResource -> {
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

                    apiConsents.add(ApiConsent.builder()
                            .systemIdValue(samtykkeResource.getSystemId().getIdentifikatorverdi())
                            .processorName(processor.getNavn())
                            .expirationDate(samtykkeResource.getGyldighetsperiode())
                            .active(samtykkeResource.getGyldighetsperiode().getSlutt() == null &&
                                    samtykkeResource.getGyldighetsperiode().getStart() != null)
                            .personalDataName(personalData.getNavn())
                            .processing(behandlingResource)
                            .processingBase(processingBase)
                            .build());


                });

        return Mono.just(apiConsents);

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

        ResponseEntity response = fintClient.postResource(fintEndpointConfiguration.getBaseUri()
                + fintEndpointConfiguration.getConsentUri(), consent, SamtykkeResource.class).toFuture().get();

        log.info("New consent created : " + response.getHeaders().getLocation().toString());
        SamtykkeResource createdConsent = fintClient.getResource(response.getHeaders().getLocation().toString(), SamtykkeResource.class).toFuture().get();

        return Mono.just(createApiConsent(principal, createdConsent, true));
    }

    public ApiConsent createApiConsent(
            FintJwtEndUserPrincipal principal,
            SamtykkeResource consent,
            boolean active) throws ExecutionException, InterruptedException {
        ApiConsent apiConsent = new ApiConsent();
        String systemIdValue = consent.getSystemId().getIdentifikatorverdi();
        Map<String, List<Link>> consentLinks = consent.getLinksIfPresent();
        String consentProcessingLink = String.valueOf(consentLinks.get("behandling").get(0));
        BehandlingResource processing = fintClient.getResource(consentProcessingLink, BehandlingResource.class).toFuture().get();
        Map<String, List<Link>> processingLinks = processing.getLinksIfPresent();
        String processingBaseLink = String.valueOf(processingLinks.get("behandlingsgrunnlag").get(0));
        String personalDataLink = String.valueOf(processingLinks.get("personopplysning").get(0));
        String processorLink = String.valueOf(processingLinks.get("tjeneste").get(0));
        TjenesteResource processor = fintClient.getResource(processorLink, TjenesteResource.class).toFuture().get();
        PersonopplysningResource personalData = fintClient.getResource(personalDataLink, PersonopplysningResource.class).toFuture().get();
        BehandlingsgrunnlagResource processingBase = fintClient.getResource(processingBaseLink, BehandlingsgrunnlagResource.class).toFuture().get();

        return apiConsent.builder()
                .systemIdValue(consent.getSystemId().getIdentifikatorverdi())
                .processorName(processor.getNavn())
                .expirationDate(consent.getGyldighetsperiode())
                .active(consent.getGyldighetsperiode().getSlutt() == null &&
                        consent.getGyldighetsperiode().getStart() != null)
                .personalDataName(personalData.getNavn())
                .processing(processing)
                .processingBase(processingBase)
                .build();
    }

    public Mono<ApiConsent> updateConsent(String consentId, String processingId, boolean active, FintJwtEndUserPrincipal principal) throws ExecutionException, InterruptedException {
        SamtykkeResource samtykkeResource = fintClient.getResource(fintEndpointConfiguration.getBaseUri()
                + fintEndpointConfiguration.getConsentUri() + "systemid/" + consentId, SamtykkeResource.class).toFuture().get();
        if (samtykkeResource != null) {
            if (samtykkeResource.getGyldighetsperiode().getStart() != null
                    && samtykkeResource.getGyldighetsperiode().getSlutt() == null
                    && !active) {
                samtykkeResource.getGyldighetsperiode().setSlutt(Date.from(Clock.systemUTC().instant()));
                Mono.just(fintClient.putResource(fintEndpointConfiguration.getBaseUri()
                        + fintEndpointConfiguration.getConsentUri()
                        + "systemid/" + consentId, samtykkeResource, SamtykkeResource.class));
                return Mono.just(createApiConsent(principal, samtykkeResource, false));
            } else if (samtykkeResource.getGyldighetsperiode().getSlutt() != null && active) {
                BehandlingResource behandlingResource = fintClient.getResource(fintEndpointConfiguration.getBaseUri()
                        + fintEndpointConfiguration.getProcessingUri()
                        + "systemid" + processingId, BehandlingResource.class).toFuture().get();
                String behandlingsResourceId = behandlingResource.getSystemId().getIdentifikatorverdi();
                return addConsent(behandlingsResourceId, principal);

            }

        }
        log.info("No consents found for systemId : " + consentId);
        return null;
    }

}
