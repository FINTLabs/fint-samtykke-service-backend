package no.fintlabs.apiConsent;

import no.fint.model.resource.Link;
import no.fint.model.resource.personvern.kodeverk.BehandlingsgrunnlagResource;
import no.fint.model.resource.personvern.kodeverk.PersonopplysningResource;
import no.fint.model.resource.personvern.samtykke.BehandlingResource;
import no.fint.model.resource.personvern.samtykke.SamtykkeResource;
import no.fint.model.resource.personvern.samtykke.TjenesteResource;
import no.fintlabs.fint.FintClient;
import no.fintlabs.fint.FintEndpointConfiguration;
import no.vigoiks.resourceserver.security.FintJwtEndUserPrincipal;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;

@Component
public class ApiConsentService {

    private final FintClient fintClient;
    private final FintEndpointConfiguration fintEndpointConfiguration;

    public ApiConsentService(FintClient fintClient, FintEndpointConfiguration fintEndpointConfiguration) {
        this.fintClient = fintClient;
        this.fintEndpointConfiguration = fintEndpointConfiguration;
    }

    public ApiConsent create(
            FintJwtEndUserPrincipal principal,
            SamtykkeResource consent,
            boolean active) throws ExecutionException, InterruptedException {
        ApiConsent apiConsent = new ApiConsent();
        //String systemIdValue = consent.getSystemId().getIdentifikatorverdi();
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
                .active(active)
//                .active(consent.getGyldighetsperiode().getSlutt() == null &&
//                        consent.getGyldighetsperiode().getStart() != null)
                .personalDataName(personalData.getNavn())
                .processing(processing)
                .processingBase(processingBase)
                .build();
    }
}
