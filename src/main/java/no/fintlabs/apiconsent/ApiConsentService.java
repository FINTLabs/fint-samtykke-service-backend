package no.fintlabs.apiconsent;

import lombok.extern.slf4j.Slf4j;
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
import no.fintlabs.processing.ProcessingService;
import no.vigoiks.resourceserver.security.FintJwtEndUserPrincipal;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

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

        List<ApiConsent> apiConsents = Objects.requireNonNull(processingService.getProcessings(principal)
                .toFuture().get().getContent())
                .stream()
                .map(behandlingResource -> {
            try {
                return buildApiConsentFromConsentList(samtykkeResources, behandlingResource, principal);
            } catch (ExecutionException | InterruptedException e) {
                throw new RuntimeException(e);
            }
        }).collect(Collectors.toList());

        return Mono.just(apiConsents);
    }

    public Mono<ApiConsent> addConsent(String processingId,
                                       FintJwtEndUserPrincipal principal) throws ExecutionException, InterruptedException {
        SamtykkeResource createdConsent = consentService.addConsent(processingId, principal);

        return Mono.just(buildApiConsent(createdConsent, true));
    }

    public Mono<ApiConsent> updateConsent(String consentId,
                                          String processingId,
                                          boolean active,
                                          FintJwtEndUserPrincipal principal) throws ExecutionException, InterruptedException {
        SamtykkeResource samtykkeResource = fintClient
                .getResource(fintEndpointConfiguration
                        .getBaseUri() + fintEndpointConfiguration
                        .getConsentUri() + "systemid/" + consentId, SamtykkeResource.class)
                .toFuture()
                .get();

        if (samtykkeResource != null) {

            // consent given -> consent withdrawn :: update consent
            if (samtykkeResource.getGyldighetsperiode().getStart() != null
                    && samtykkeResource.getGyldighetsperiode().getSlutt() == null && !active) {

                SamtykkeResource updatedConsent = consentService.withdrawConsent(samtykkeResource,consentId);
                return Mono.just(buildApiConsent(updatedConsent, false));

                // consent withdrawn -> consent given :: new consent
            } else if (samtykkeResource.getGyldighetsperiode().getSlutt() != null && active) {
                BehandlingResource behandlingResource = fintClient.getResource(fintEndpointConfiguration
                        .getBaseUri() + fintEndpointConfiguration
                        .getProcessingUri() + "systemid/" + processingId, BehandlingResource.class)
                        .toFuture()
                        .get();
                String behandlingsResourceId = behandlingResource.getSystemId().getIdentifikatorverdi();
                return addConsent(behandlingsResourceId, principal);

            }

        }
        log.info("Update consent : No consents found for systemId : " + consentId);
        return null;
    }

    private ApiConsent buildApiConsent(SamtykkeResource consent, boolean active) throws ExecutionException, InterruptedException {
        ApiConsent apiConsent = new ApiConsent();

        BehandlingResource processing = fintClient
                .getResource(consent.getBehandling().get(0).getHref(), BehandlingResource.class)
                .toFuture()
                .get();
        TjenesteResource processor = fintClient
                .getResource(processing.getTjeneste().get(0).getHref(),TjenesteResource.class)
                .toFuture()
                .get();
        PersonopplysningResource personalData = fintClient
                .getResource(processing.getPersonopplysning().get(0).getHref(), PersonopplysningResource.class)
                .toFuture()
                .get();
        BehandlingsgrunnlagResource processingBase = fintClient
                .getResource(processing.getBehandlingsgrunnlag().get(0).getHref(), BehandlingsgrunnlagResource.class)
                .toFuture()
                .get();

        return apiConsent.builder().systemIdValue(consent.getSystemId()
                .getIdentifikatorverdi())
                .processorName(processor.getNavn())
                .expirationDate(consent.getGyldighetsperiode())
                .active(active)
                .personalDataName(personalData.getNavn())
                .processing(processing)
                .processingBase(processingBase)
                .build();
    }

    private ApiConsent buildApiConsentFromConsentList(SamtykkeResources samtykkeResources,
                                                      BehandlingResource behandlingResource,
                                                      FintJwtEndUserPrincipal principal) throws ExecutionException, InterruptedException {

        SamtykkeResource samtykkeResource = consentService.findNewestConsent(samtykkeResources, behandlingResource)
                .orElseGet(() -> getNewSamtykke(behandlingResource, principal));

        TjenesteResource processor;
        PersonopplysningResource personalData;
        BehandlingsgrunnlagResource processingBase;

        try {
            processor = fintClient.getResource(behandlingResource.getTjeneste().get(0).getHref(), TjenesteResource.class).toFuture().get();
            personalData = fintClient.getResource(behandlingResource.getPersonopplysning().get(0).getHref(), PersonopplysningResource.class).toFuture().get();
            processingBase = fintClient.getResource(behandlingResource.getBehandlingsgrunnlag().get(0).getHref(), BehandlingsgrunnlagResource.class).toFuture().get();
        } catch (InterruptedException | ExecutionException e) {
            throw new RuntimeException(e);
        }

        return ApiConsent.builder().systemIdValue(samtykkeResource.getSystemId()
                .getIdentifikatorverdi())
                .processorName(processor.getNavn())
                .expirationDate(samtykkeResource.getGyldighetsperiode())
                .active(samtykkeResource.getGyldighetsperiode().getSlutt() == null && samtykkeResource.getGyldighetsperiode().getStart() != null)
                .personalDataName(personalData.getNavn())
                .processing(behandlingResource)
                .processingBase(processingBase)
                .build();
    }

    private SamtykkeResource getNewSamtykke(BehandlingResource behandlingResource, FintJwtEndUserPrincipal principal) {
        try {
            return consentService.addConsent(behandlingResource.getSystemId().getIdentifikatorverdi(), principal);
        } catch (ExecutionException | InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

}
