package no.fintlabs

import no.fint.model.felles.kompleksedatatyper.Periode
import no.fint.model.resource.personvern.samtykke.BehandlingResource
import no.fint.model.resource.personvern.samtykke.SamtykkeResource
import no.fint.model.resource.personvern.samtykke.SamtykkeResources
import no.fintlabs.consent.ConsentService
import no.fintlabs.fint.FintClient
import no.fintlabs.fint.FintEndpointConfiguration
import no.fintlabs.person.PersonService
import spock.lang.Specification

class ConsentServiceSpec extends Specification{
    private SamtykkeResources samtykkeResources
    private SamtykkeResource samtykkeResourceNewest
    private SamtykkeResource samtykkeResourceOldest
    private BehandlingResource behandlingResource

    def 'find newest consent'(){
        given:
        def fintClient = Mock(FintClient)
        def fintEndpointConfig = Mock(FintEndpointConfiguration)
        def personService = Mock(PersonService)
        def consentService = new ConsentService(fintClient,fintEndpointConfig,personService)
        behandlingResource = new BehandlingResource()
        behandlingResource.addSelf("abcd")
        samtykkeResources = Mock(SamtykkeResources.class)
        Periode periodeNewest = new Periode()
        periodeNewest.setBeskrivelse("newest consent")
        periodeNewest.setStart("2022-09-19T13:23:27")
        samtykkeResourceNewest.addBehandling("abcd")
        samtykkeResourceNewest.setSystemId("1234")
        samtykkeResourceNewest.setGyldighetsperiode(periodeNewest)
        samtykkeResources.addResource(samtykkeResourceNewest)

        Periode periodeOldest = new Periode()
        periodeOldest.setBeskrivelse("Oldest consent")
        periodeOldest.setStart("2022-09-21T13:23:27")
        samtykkeResourceOldest.addBehandling("abcd")
        samtykkeResourceOldest.setSystemId("5678")
        samtykkeResourceOldest.setGyldighetsperiode(periodeOldest)
        samtykkeResources.addResource(samtykkeResourceOldest)

        when:
        def newestSamtykke = consentService
                .findNewestConsent(samtykkeResources,behandlingResource)

        then:
        newestSamtykke.get().systemId.identifikatorverdi == "1234"

    }

}
