package no.fintlabs.consent;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import no.fint.model.felles.kompleksedatatyper.Periode;
import no.fint.model.resource.personvern.kodeverk.BehandlingsgrunnlagResource;
import no.fint.model.resource.personvern.samtykke.BehandlingResource;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ApiConsent {

    private String systemIdValue;

    private String processorName; //Tjeneste i GUI
    private Periode expirationDate;
    private boolean active;
    private String personalDataName; //personopplysning i GUI
    private BehandlingResource processing; // Formål i GUI (Prcessing.formal = text)
    private BehandlingsgrunnlagResource processingBase;

//    public Consent(String systemIdValue,
//                   String processorName,
//                   Periode expirationDate,
//                   boolean active,
//                   String personalDataName,
//                   BehandlingResource processing,
//                   BehandlingsgrunnlagResource processingBase) {
//        this.systemIdValue = systemIdValue;
//        this.processorName = processorName;
//        this.expirationDate = expirationDate;
//        this.active = active;
//        this.personalDataName = personalDataName;
//        this.processing = processing;
//        this.processingBase = processingBase;
//    }


}
