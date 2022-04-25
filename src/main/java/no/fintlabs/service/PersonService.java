package no.fintlabs.service;
import no.fint.model.resource.felles.PersonResource;
import no.fintlabs.client.FintClient;
import no.vigoiks.resourceserver.security.FintJwtEndUserPrincipal;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;


@Component
public class PersonService {

    private final FintClient fintClient;


    public PersonService ( FintClient fintClient) {
        this.fintClient = fintClient;
    }


    public PersonResource getPerson(FintJwtEndUserPrincipal principal){
        PersonResource person = (PersonResource) fintClient.getResource("administrasjon/personal/personalressurs/ansattnummer/509545").block();
        return person;
    }
}
