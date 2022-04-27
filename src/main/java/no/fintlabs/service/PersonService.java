package no.fintlabs.service;

import no.fint.model.resource.felles.PersonResource;
import no.fintlabs.client.FintClient;
import no.vigoiks.resourceserver.security.FintJwtEndUserPrincipal;
import org.springframework.stereotype.Component;

import java.util.concurrent.ExecutionException;


@Component
public class PersonService {

    private final FintClient fintClient;


    public PersonService(FintClient fintClient) {
        this.fintClient = fintClient;
    }


    public PersonResource getPerson(FintJwtEndUserPrincipal principal) throws ExecutionException, InterruptedException {
        PersonResource person = fintClient
                .getResource("/administrasjon/personal/personalressurs/ansattnummer/509545", PersonResource.class)
                .toFuture().get();

        return person;
    }
}
