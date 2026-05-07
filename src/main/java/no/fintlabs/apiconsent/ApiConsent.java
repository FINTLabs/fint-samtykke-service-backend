package no.fintlabs.apiconsent;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import no.novari.fint.model.felles.kompleksedatatyper.Periode;
import no.novari.fint.model.resource.personvern.kodeverk.BehandlingsgrunnlagResource;
import no.novari.fint.model.resource.personvern.samtykke.BehandlingResource;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ApiConsent {

    private String systemIdValue;
    private String processorName;
    private Periode expirationDate;
    private boolean active;
    private String personalDataName;
    private BehandlingResource processing;
    private BehandlingsgrunnlagResource processingBase;

}
