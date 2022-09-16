package no.fintlabs.person;

import lombok.extern.slf4j.Slf4j;
import no.fint.model.felles.Person;
import no.fint.model.resource.Link;
import no.fint.model.resource.felles.PersonResource;
import no.fintlabs.fint.FintClient;
import no.fintlabs.fint.FintEndpointConfiguration;
import no.vigoiks.resourceserver.security.FintJwtEndUserPrincipal;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
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

    public Person getPerson(FintJwtEndUserPrincipal principal) throws ExecutionException, InterruptedException {
        return fintClient.getResource(getPersonUri(principal), Person.class).toFuture().get();
    }

    public String getPersonUri(FintJwtEndUserPrincipal principal) throws ExecutionException, InterruptedException {
        PersonResource personResource = fintClient.getResource(fintEndpointConfiguration.getEmployeeUri() + principal.getEmployeeId(), PersonResource.class).toFuture().get();
        Map<String, List<Link>> personResourceLinks = personResource.getLinks();
        List<Link> personLinks = personResourceLinks.get("person");
        return personLinks.get(0).toString();
    }

    public PersonResource getPersonResource(FintJwtEndUserPrincipal principal) throws ExecutionException, InterruptedException {
        return fintClient.getResource(fintEndpointConfiguration.getEmployeeUri() + principal.getEmployeeId(), PersonResource.class).toFuture().get();
    }
}
