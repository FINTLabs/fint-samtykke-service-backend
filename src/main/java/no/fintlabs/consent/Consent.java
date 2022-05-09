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
    //Tjeneste i GUI
    private String ProcessorName;
    private Periode ExpirationDate;
    private boolean Active;
    //Personopplysning i GUI
    private String PersonalDataName;
    // Formål i GUI (Prcessing.formal = text)
    private BehandlingResource Processing;
    private BehandlingsgrunnlagResource ProcessingBase;

    public Consent(String processorName, String personalDataName, BehandlingResource processing, BehandlingsgrunnlagResource processingBase) {
        this.ProcessorName = processorName;
        this.PersonalDataName = personalDataName;
        this.Processing = processing;
        this.ProcessingBase = processingBase;
    }


}
