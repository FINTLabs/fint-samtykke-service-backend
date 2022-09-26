package no.fintlabs

import no.fint.model.felles.kompleksedatatyper.Identifikator
import no.fint.model.felles.kompleksedatatyper.Periode
import no.fint.model.resource.Link
import no.fint.model.resource.personvern.samtykke.BehandlingResource
import no.fint.model.resource.personvern.samtykke.SamtykkeResource
import no.fint.model.resource.personvern.samtykke.SamtykkeResources
import no.fintlabs.consent.ConsentService
import no.fintlabs.fint.FintClient
import no.fintlabs.fint.FintEndpointConfiguration
import no.fintlabs.person.PersonService
import spock.lang.Specification



class ConsentServiceSpec extends Specification{
    private ConsentService consentService

    private FintClient fintClient
    private FintEndpointConfiguration fintEndpointConfiguration
    private PersonService personService

    private BehandlingResource behandlingResource

    private SamtykkeResources samtykkeResources = new SamtykkeResources()
    private SamtykkeResource samtykkeResourceNewest = new SamtykkeResource()
    private SamtykkeResource samtykkeResourceOldest = new SamtykkeResource()


    def 'find newest consent'(){
        given:
        fintClient = Mock()
        fintEndpointConfiguration = Mock()
        personService = Mock()
        consentService = new ConsentService(
               fintClient,
               fintEndpointConfiguration,
               personService)

        behandlingResource = new BehandlingResource()
        Link behandlingSelfLink = new Link("abcd")
        behandlingResource.addSelf(behandlingSelfLink)

        Periode periodeNewest = new Periode()
        periodeNewest.setBeskrivelse("newest consent")
        periodeNewest.setStart(new Date(1664180000))
        Link samtykkeBehandlingsLink = new Link("abcd")
        samtykkeResourceNewest.addBehandling(samtykkeBehandlingsLink)
        Identifikator samtykkeNewestId = new Identifikator()
        samtykkeNewestId.setGyldighetsperiode(periodeNewest)
        samtykkeNewestId.setIdentifikatorverdi("1234")
        samtykkeResourceNewest.setSystemId(samtykkeNewestId)
        samtykkeResourceNewest.setGyldighetsperiode(periodeNewest)
        samtykkeResources.addResource(samtykkeResourceNewest)

        Periode periodeOldest = new Periode()
        periodeOldest.setBeskrivelse("Oldest consent")
        periodeOldest.setStart(new Date(1660000000))
        samtykkeResourceOldest.addBehandling(samtykkeBehandlingsLink)
        Identifikator samtykkeOldestId = new Identifikator()
        samtykkeOldestId.setGyldighetsperiode(periodeOldest)
        samtykkeOldestId.setIdentifikatorverdi("5678")
        samtykkeResourceOldest.setSystemId(samtykkeOldestId)
        samtykkeResourceOldest.setGyldighetsperiode(periodeOldest)
        samtykkeResources.addResource(samtykkeResourceOldest)

        when:
        def newestSamtykke = consentService
                .findNewestConsent(samtykkeResources,behandlingResource)

        then:
        newestSamtykke.get().systemId.identifikatorverdi == "1234"

    }

}
