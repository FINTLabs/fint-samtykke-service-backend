package no.fintlabs.person;

import lombok.extern.slf4j.Slf4j;
import no.fint.model.resource.administrasjon.personal.PersonalressursResource;
import no.fint.model.resource.felles.PersonResource;
import no.fint.model.resource.utdanning.elev.ElevResource;
import no.fintlabs.fint.FintClient;
import no.fintlabs.fint.FintEndpointConfiguration;
import no.vigoiks.resourceserver.security.FintJwtEndUserPrincipal;
import org.springframework.stereotype.Service;


import java.util.concurrent.ExecutionException;

@Slf4j
@Service
public class PersonService {

    private final FintClient fintClient;
    private final FintEndpointConfiguration fintEndpointConfiguration;

    public PersonService(FintClient fintClient, FintEndpointConfiguration fintEndpointConfiguration) {
        this.fintClient = fintClient;
        this.fintEndpointConfiguration = fintEndpointConfiguration;
    }

    public String getPersonUri(FintJwtEndUserPrincipal principal) throws ExecutionException, InterruptedException {

        if (principal.getEmployeeId() != null) {
            PersonalressursResource personalressursResource = fintClient
                    .getResource(fintEndpointConfiguration.getEmployeeUri() + principal.getEmployeeId(),PersonalressursResource.class)
                    .toFuture()
                    .get();
            String personLink = personalressursResource.getPerson().get(0).getHref();
            log.info("Found person : " + principal.getGivenName() + " " + principal.getSurname() + " as employee");
            return personLink;

        } else if (principal.getStudentNumber() != null) {
            ElevResource elevResource = fintClient
                    .getResource(fintEndpointConfiguration.getStudentUri() + principal.getStudentNumber(), ElevResource.class)
                    .toFuture()
                    .get();

            String personLink = elevResource.getPerson().get(0).getHref();
            log.info("Found person : " + principal.getGivenName() + " " + principal.getSurname() + " as student");
            return personLink;

        } else {
            log.info("Token does not contain employeeNumber or studentId");
            return null;
        }

    }

    public PersonResource getPersonResource(FintJwtEndUserPrincipal principal) throws ExecutionException, InterruptedException {
        PersonResource personResource;
        String personLink = getPersonUri(principal);
        personResource = fintClient.getResource(personLink, PersonResource.class).toFuture().get();
        return personResource;
    }
}
