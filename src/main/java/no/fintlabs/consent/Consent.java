package no.fintlabs.consent;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import no.fint.model.felles.kompleksedatatyper.Periode;
import no.fint.model.personvern.samtykke.*;
import no.fint.model.resource.personvern.kodeverk.BehandlingsgrunnlagResource;
import no.fint.model.resource.personvern.samtykke.BehandlingResource;

@Data
public class Consent {

    private String SystemIdValue;

    private String ProcessorName; //Tjeneste i GUI
    private Periode ExpirationDate;
    private boolean Active;
    private String PersonalDataName; //personopplysning i GUI
    private BehandlingResource Processing; // Formål i GUI (Prcessing.formal = text)
    private BehandlingsgrunnlagResource ProcessingBase;

    public Consent(String systemIdValue,
                   String processorName,
                   Periode expirationDate,
                   boolean active,
                   String personalDataName,
                   BehandlingResource processing,
                   BehandlingsgrunnlagResource processingBase) {
        this.SystemIdValue = systemIdValue;
        this.ProcessorName = processorName;
        this.ExpirationDate = expirationDate;
        this.Active = active;
        this.PersonalDataName = personalDataName;
        this.Processing = processing;
        this.ProcessingBase = processingBase;
    }


}
