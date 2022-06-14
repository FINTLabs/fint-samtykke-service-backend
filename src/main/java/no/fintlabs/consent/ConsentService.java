package no.fintlabs.consent;

import lombok.extern.slf4j.Slf4j;
import no.fint.model.resource.Link;
import no.fint.model.resource.personvern.kodeverk.BehandlingsgrunnlagResource;
import no.fint.model.resource.personvern.kodeverk.PersonopplysningResource;
import no.fint.model.resource.personvern.samtykke.*;
import no.fintlabs.fint.FintClient;
import no.fintlabs.fint.FintEndpointConfiguration;
import no.fintlabs.processing.ProcessingService;
import no.fintlabs.processors.ProcessorService;
import no.vigoiks.resourceserver.security.FintJwtEndUserPrincipal;
import org.springframework.stereotype.Component;
import no.fintlabs.person.PersonService;
import reactor.core.publisher.Mono;
import java.util.*;
import java.util.concurrent.ExecutionException;

@Slf4j
@Component
public class ConsentService {

    private final FintClient fintClient;
    private final FintEndpointConfiguration fintEndpointConfiguration;
    private final PersonService personService;
    private final ProcessingService processingService;
    private final ProcessorService processorService;

    public ConsentService(FintClient fintClient,
                          FintEndpointConfiguration fintEndpointConfiguration,
                          PersonService personService,
                          ProcessingService processingService,
                          ProcessorService processorService) {
        this.fintClient = fintClient;
        this.fintEndpointConfiguration = fintEndpointConfiguration;
        this.personService = personService;
        this.processingService = processingService;
        this.processorService = processorService;
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
                    TjenesteResource processor ;
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

//    public Mono<ApiConsent> addApiConsent(String processingId, FintJwtEndUserPrincipal from) {
//
//    }

//    public Mono<List<Consent>> getApiConsents(FintJwtEndUserPrincipal principal) throws ExecutionException, InterruptedException {
//        List<Consent> apiConsent = new ArrayList<>();
//        List<BehandlingResource> processings = (processingService.getProcessings(principal).toFuture().get()).getContent();
//
//        for (BehandlingResource processing : processings) {
//            String processingId = processing.getSystemId().getIdentifikatorverdi();
//            List<SamtykkeResource> consentsTotal = (getFilteredConsents(principal).toFuture().get()).getContent();
//
//            if (consentsTotal.isEmpty()) {
//                //TODO: create empty samtykke
//            } else if (consentsTotal.size() == 1) {
//                //TODO: create apisamtykke with this concent. No need to find newest consent
//            } else if (consentsTotal.size() > 1) {
//                Comparator<SamtykkeResource> comparator = (p1, p2) -> p1.getGyldighetsperiode().getStart().compareTo(p2.getGyldighetsperiode().getStart());
//                SamtykkeResource newestConcent = consentsTotal.stream().filter(con -> con.getGyldighetsperiode().getStart() != null).max(comparator).get();
//                Periode experiationDate = newestConcent.getGyldighetsperiode();
//                boolean active = experiationDate.getSlutt() == null && experiationDate.getStart() != null;
//
//                apiConsent.add(createApiConsent(
//                        principal,
//                        newestConcent,
//                        processing,
//                        active));
//
//            }
//        }
//        return Mono.just(apiConsent);
//    }


//    public Consent createApiConsent(
//            FintJwtEndUserPrincipal principal,
//            SamtykkeResource consent,
//            BehandlingResource processing,
//            boolean active) throws ExecutionException, InterruptedException {
//
//        String systemIdValue = consent.getSystemId().getIdentifikatorverdi();
//        Map<String, List<Link>> processingsLinks = processing.getLinks();
//        String processingBaseLink = String.valueOf(processingsLinks.get("behandlingsgrunnlag").get(0));
//        String personalDataLink = String.valueOf(processingsLinks.get("personopplysning").get(0));
//        String processorLink = String.valueOf(processingsLinks.get("tjeneste").get(0));
//        TjenesteResource processor = fintClient.getResource(processorLink, TjenesteResource.class).toFuture().get();
//        PersonopplysningResource personalData = fintClient.getResource(personalDataLink, PersonopplysningResource.class).toFuture().get();
//        BehandlingsgrunnlagResource processingBase = fintClient.getResource(processingBaseLink, BehandlingsgrunnlagResource.class).toFuture().get();
//
//        Consent apiConsent = new Consent(
//                systemIdValue,
//                processor.getNavn(),
//                consent.getGyldighetsperiode(),
//                active,
//                personalData.getNavn(),
//                processing,
//                processingBase);
//
//        return apiConsent;
//    }


//    public String getConsentProcessingUri(SamtykkeResource consent) {
//        Map<String, List<Link>> consentLinks = consent.getLinks();
//        List<Link> consentProcessingLinks = consentLinks.get("behandling");
//        String consentProcessingLink = consentProcessingLinks.get(0).toString();
//        return consentProcessingLink;
//    }

}
