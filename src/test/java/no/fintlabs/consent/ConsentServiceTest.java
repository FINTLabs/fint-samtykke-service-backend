package no.fintlabs.consent;

import no.fint.model.felles.kompleksedatatyper.Identifikator;
import no.fint.model.felles.kompleksedatatyper.Periode;
import no.fint.model.resource.Link;
import no.fint.model.resource.personvern.samtykke.BehandlingResource;
import no.fint.model.resource.personvern.samtykke.SamtykkeResource;
import no.fint.model.resource.personvern.samtykke.SamtykkeResources;
import no.fintlabs.fint.FintEndpointConfiguration;
import no.fintlabs.person.PersonService;
import no.vigoiks.resourceserver.security.FintJwtEndUserPrincipal;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Date;
import java.util.Optional;
import java.util.concurrent.ExecutionException;

import static org.mockito.BDDMockito.given;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

@ExtendWith(MockitoExtension.class)
class ConsentServiceTest {

    @Mock
    private FintEndpointConfiguration fintEndpointConfiguration;

    @Mock
    private PersonService personService;

    @InjectMocks
    private ConsentService consentService;

    private SamtykkeResources samtykkeResources;
    private BehandlingResource behandlingResource;
    private FintJwtEndUserPrincipal principal;

    @BeforeEach
    void setUp() {
        samtykkeResources = new SamtykkeResources();
        SamtykkeResource samtykkeResourceNewest = new SamtykkeResource();
        SamtykkeResource samtykkeResourceOldest = new SamtykkeResource();

        behandlingResource = new BehandlingResource();
        Link behandlingsSelfLink = new Link("abcd");
        behandlingResource.addSelf(behandlingsSelfLink);

        Link samtykkeBehandlingsLink = new Link("abcd");

        Periode periodeNewest = new Periode();
        periodeNewest.setBeskrivelse("Newest concent");
        periodeNewest.setStart(new Date(1664180000));
        samtykkeResourceNewest.addBehandling(samtykkeBehandlingsLink);
        Identifikator samtykkeNewestId = new Identifikator();
        samtykkeNewestId.setIdentifikatorverdi("1234");
        samtykkeNewestId.setGyldighetsperiode(periodeNewest);
        samtykkeResourceNewest.setSystemId(samtykkeNewestId);
        samtykkeResourceNewest.setGyldighetsperiode(periodeNewest);
        samtykkeResources.addResource(samtykkeResourceNewest);

        Periode periodeOldest = new Periode();
        periodeOldest.setBeskrivelse("Oldest concent");
        periodeOldest.setStart(new Date(1660000000));
        samtykkeResourceOldest.addBehandling(samtykkeBehandlingsLink);
        Identifikator samtykkeOldestId = new Identifikator();
        samtykkeOldestId.setIdentifikatorverdi("5678");
        samtykkeOldestId.setGyldighetsperiode(periodeOldest);
        samtykkeResourceOldest.setSystemId(samtykkeOldestId);
        samtykkeResourceOldest.setGyldighetsperiode(periodeOldest);
        samtykkeResources.addResource(samtykkeResourceOldest);
    }

    @Test
    void findNewestConsentTest() {

        Optional<SamtykkeResource> newestConsent = consentService.findNewestConsent(samtykkeResources, behandlingResource);

        assertThat(newestConsent.get().getSystemId().getIdentifikatorverdi()).isEqualTo("1234");

    }

    @Test
    void addNewEmptyConsentTest() throws ExecutionException, InterruptedException {

        principal = new FintJwtEndUserPrincipal();
        principal.setEmployeeId("empId1");

        given(fintEndpointConfiguration.getBaseUri()).willReturn("http://baseuri");
        given(fintEndpointConfiguration.getProcessingUri()).willReturn("/processinguri");
        given(personService.getPersonUri(principal)).willReturn("personLink");

        SamtykkeResource emptyConcent = consentService.addConsent("001", principal, false);
        String concentDescription = emptyConcent.getGyldighetsperiode().getBeskrivelse();

        assertThat(concentDescription).isEqualTo("Tom samtykke opprettet");
    }

    @Test
    void addConcentSamtykkeGivenTest() throws ExecutionException, InterruptedException {
        principal = new FintJwtEndUserPrincipal();
        principal.setEmployeeId("empId1");

        given(fintEndpointConfiguration.getBaseUri()).willReturn("http://baseuri");
        given(fintEndpointConfiguration.getProcessingUri()).willReturn("/processinguri");
        given(personService.getPersonUri(principal)).willReturn("personLink");

        SamtykkeResource newConcentSamtykkeGiven = consentService.addConsent("001", principal, true);
        String concentDescription = newConcentSamtykkeGiven.getGyldighetsperiode().getBeskrivelse();

        assertThat(concentDescription).isEqualTo("Samtykke gitt");
    }
}