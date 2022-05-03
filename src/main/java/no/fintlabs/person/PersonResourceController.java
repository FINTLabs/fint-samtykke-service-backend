package no.fintlabs.person;


import lombok.extern.slf4j.Slf4j;
import no.fint.model.felles.Person;
import no.fint.model.resource.felles.PersonResource;
import no.vigoiks.resourceserver.security.FintJwtEndUserPrincipal;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.concurrent.ExecutionException;

@Slf4j
@RestController
@RequestMapping("/api/person")
public class PersonResourceController {

    private final PersonService personService;

    public PersonResourceController(PersonService personService) {
        this.personService = personService;
    }

    @GetMapping("/personresource")
    public PersonResource getPersonResource(@AuthenticationPrincipal Jwt jwt) throws ExecutionException, InterruptedException {
        return personService.getPersonResource(FintJwtEndUserPrincipal.from(jwt));
    }

    @GetMapping
    public Person getPerson(@AuthenticationPrincipal Jwt jwt) throws ExecutionException, InterruptedException {
        return personService.getPerson(FintJwtEndUserPrincipal.from(jwt));
    }
}
