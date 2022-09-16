package no.fintlabs.apiconsent;

import lombok.extern.slf4j.Slf4j;
import no.fint.model.felles.kompleksedatatyper.Periode;
import no.fint.model.resource.Link;
import no.fint.model.resource.personvern.kodeverk.BehandlingsgrunnlagResource;
import no.fint.model.resource.personvern.kodeverk.PersonopplysningResource;
import no.fint.model.resource.personvern.samtykke.BehandlingResource;
import no.fint.model.resource.personvern.samtykke.SamtykkeResource;
import no.fint.model.resource.personvern.samtykke.SamtykkeResources;
import no.fint.model.resource.personvern.samtykke.TjenesteResource;
import no.fintlabs.consent.ConsentService;
import no.fintlabs.fint.FintClient;
import no.fintlabs.fint.FintEndpointConfiguration;
import no.fintlabs.person.PersonService;
import no.fintlabs.processing.ProcessingService;
import no.vigoiks.resourceserver.security.FintJwtEndUserPrincipal;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.time.Clock;
import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

@Slf4j
@Service
public class ApiConsentService {

    private final FintClient fintClient;
    private final FintEndpointConfiguration fintEndpointConfiguration;
    private final ConsentService consentService;
    private final ProcessingService processingService;

    public ApiConsentService(FintClient fintClient,
                             FintEndpointConfiguration fintEndpointConfiguration,
                             ConsentService consentService,
                             ProcessingService processingService ) {
        this.fintClient = fintClient;
        this.fintEndpointConfiguration = fintEndpointConfiguration;
        this.consentService = consentService;
        this.processingService = processingService;
    }

    public Mono<List<ApiConsent>> getApiConsents(FintJwtEndUserPrincipal principal) throws ExecutionException, InterruptedException {

        SamtykkeResources samtykkeResources = Objects.requireNonNull(consentService.getFilteredConsents(principal).toFuture().get());

        List<ApiConsent> apiConsents = Objects.requireNonNull(processingService.getProcessings(principal).toFuture().get().getContent()).stream().map(behandlingResource -> {
            try {
                return buildApiConsentFromConsentList(samtykkeResources, behandlingResource, principal);
            } catch (ExecutionException | InterruptedException e) {
                throw new RuntimeException(e);
            }
        }).collect(Collectors.toList());

        return Mono.just(apiConsents);
    }

    public Mono<ApiConsent> addConsent(String processingId, FintJwtEndUserPrincipal principal) throws ExecutionException, InterruptedException {
        SamtykkeResource createdConsent = consentService.addConsent(processingId, principal);

        return Mono.just(buildApiConsent(createdConsent, true));
    }

    public Mono<ApiConsent> updateConsent(String consentId, String processingId, boolean active, FintJwtEndUserPrincipal principal) throws ExecutionException, InterruptedException {
        SamtykkeResource samtykkeResource = fintClient.getResource(fintEndpointConfiguration.getBaseUri() + fintEndpointConfiguration.getConsentUri() + "systemid/" + consentId, SamtykkeResource.class).toFuture().get();
        if (samtykkeResource != null) {
            // consent given -> consent withdrawn :: update consent
            if (samtykkeResource.getGyldighetsperiode().getStart() != null && samtykkeResource.getGyldighetsperiode().getSlutt() == null && !active) {
                Periode periode = new Periode();
                {
                    periode.setBeskrivelse("Oppdatering av samtykke");
                    periode.setStart(samtykkeResource.getGyldighetsperiode().getStart());
                    periode.setSlutt(Date.from(Clock.systemUTC().instant()));
                }
                samtykkeResource.setGyldighetsperiode(periode);

                ResponseEntity<Void> response = fintClient.putResource(fintEndpointConfiguration.getBaseUri() + fintEndpointConfiguration.getConsentUri() + "systemid/" + consentId, samtykkeResource, SamtykkeResource.class).toFuture().get();

                log.info("update consent : withdrawn sent : " + response.getStatusCode().name());
                ResponseEntity<Void> rs = fintClient.waitUntilCreated(response.getHeaders().getLocation().toString()).toFuture().get();
                log.info("updated consent : withdrawn confirmed : " + rs.getStatusCode().name());

                SamtykkeResource updatedConsent = fintClient.getResource(response.getHeaders().getLocation().toString(), SamtykkeResource.class).toFuture().get();
                return Mono.just(buildApiConsent(updatedConsent, false));

                // consent withdrawn -> consent given :: new consent
            } else if (samtykkeResource.getGyldighetsperiode().getSlutt() != null && active) {
                BehandlingResource behandlingResource = fintClient.getResource(fintEndpointConfiguration.getBaseUri() + fintEndpointConfiguration.getProcessingUri() + "systemid/" + processingId, BehandlingResource.class).toFuture().get();
                String behandlingsResourceId = behandlingResource.getSystemId().getIdentifikatorverdi();
                return addConsent(behandlingsResourceId, principal);

            }

        }
        log.info("Update consent : No consents found for systemId : " + consentId);
        return null;
    }

    private ApiConsent buildApiConsent(SamtykkeResource consent, boolean active) throws ExecutionException, InterruptedException {
        ApiConsent apiConsent = new ApiConsent();
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

        return apiConsent.builder().systemIdValue(consent.getSystemId().getIdentifikatorverdi()).processorName(processor.getNavn()).expirationDate(consent.getGyldighetsperiode()).active(active).personalDataName(personalData.getNavn()).processing(processing).processingBase(processingBase).build();
    }

    private ApiConsent buildApiConsentFromConsentList(SamtykkeResources samtykkeResources, BehandlingResource behandlingResource, FintJwtEndUserPrincipal principal) throws ExecutionException, InterruptedException {

        String behandlingSelfLink = String.valueOf(behandlingResource.getSelfLinks().get(0));
        log.debug("ProcessingSelfLink to compare : " + behandlingSelfLink);
        SamtykkeResource samtykkeResource = samtykkeResources.getContent().stream().filter(con -> consentService.getConsentProcessingUri(con).equals(behandlingSelfLink)).filter(con -> con.getGyldighetsperiode().getStart() != null).max(Comparator.comparing(p -> p.getGyldighetsperiode().getStart())).orElseGet(() -> {
            try {
                return consentService.addConsent(behandlingResource.getSystemId().getIdentifikatorverdi(), principal);
            } catch (ExecutionException | InterruptedException e) {
                throw new RuntimeException(e);
            }
        });


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

        return ApiConsent.builder().systemIdValue(samtykkeResource.getSystemId().getIdentifikatorverdi()).processorName(processor.getNavn()).expirationDate(samtykkeResource.getGyldighetsperiode()).active(samtykkeResource.getGyldighetsperiode().getSlutt() == null && samtykkeResource.getGyldighetsperiode().getStart() != null).personalDataName(personalData.getNavn()).processing(behandlingResource).processingBase(processingBase).build();
    }

}
