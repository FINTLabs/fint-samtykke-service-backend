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

    private SamtykkeResources samtykkeResources
    private BehandlingResource behandlingResource

    private SamtykkeResource samtykkeResourceNewest
    private SamtykkeResource samtykkeResourceOldest





    def 'test'(){
        given:
        int i
        fintClient = Mock()

        when:
        i = 3

        then:
        i == 3
    }

    def 'find newest consent'(){
        given:
        FintClient fintClient = Mock()
        fintEndpointConfiguration = Mock()
        personService = Mock()
        consentService = new ConsentService(
               fintClient,
               fintEndpointConfiguration,
               personService)

        behandlingResource = new BehandlingResource()
        behandlingResource.addSelf("abcd" as Link)

        samtykkeResources = Mock(SamtykkeResources.class)

        Periode periodeNewest = new Periode()
        periodeNewest.setBeskrivelse("newest consent")
        periodeNewest.setStart("2022-09-19T13:23:27" as Date)
        samtykkeResourceNewest.addBehandling("abcd" as Link)
        samtykkeResourceNewest.setSystemId("1234" as Identifikator)
        samtykkeResourceNewest.setGyldighetsperiode(periodeNewest)
        samtykkeResources.addResource(samtykkeResourceNewest)

        Periode periodeOldest = new Periode()
        periodeOldest.setBeskrivelse("Oldest consent")
        periodeOldest.setStart("2022-09-21T13:23:27" as Date)
        samtykkeResourceOldest.addBehandling("abcd" as Link)
        samtykkeResourceOldest.setSystemId("5678" as Identifikator)
        samtykkeResourceOldest.setGyldighetsperiode(periodeOldest)
        samtykkeResources.addResource(samtykkeResourceOldest)

        when:
        def newestSamtykke = consentService
                .findNewestConsent(samtykkeResources,behandlingResource)

        then:
        newestSamtykke.get().systemId.identifikatorverdi == "1234"

    }

}
