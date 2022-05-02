package no.fintlabs.service;

import no.fint.model.resource.felles.PersonResource;
import no.fintlabs.client.FintClient;
import no.fintlabs.configuration.FintEndpointConfiguration;
import no.vigoiks.resourceserver.security.FintJwtEndUserPrincipal;
import org.springframework.stereotype.Component;

import java.util.concurrent.ExecutionException;


@Component
public class PersonService {

    private final FintClient fintClient;
    private final FintEndpointConfiguration fintEndpointConfiguration;

    public PersonService(FintClient fintClient, FintEndpointConfiguration fintEndpointConfiguration) {
        this.fintClient = fintClient;
        this.fintEndpointConfiguration = fintEndpointConfiguration;
    }

    public PersonResource getPerson(FintJwtEndUserPrincipal principal) throws ExecutionException, InterruptedException {
        PersonResource person = fintClient
            .getResource(fintEndpointConfiguration.getEmployeeUri() + principal.getEmployeeId(), PersonResource.class)
            .toFuture().get();

        return person;
    }
}
