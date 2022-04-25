package no.fintlabs.controller;


import lombok.extern.slf4j.Slf4j;
import no.fint.model.resource.felles.PersonResource;
import no.fintlabs.client.FintClient;
import no.fintlabs.service.PersonService;
import no.vigoiks.resourceserver.security.FintJwtEndUserPrincipal;
import org.springframework.boot.autoconfigure.security.oauth2.resource.OAuth2ResourceServerProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
@Slf4j
@RestController
@RequestMapping("/api/person")
public class PersonController {

    private final PersonService personService;

    public PersonController(PersonService personService){
        this.personService = personService;
    }

    @GetMapping
    public PersonResource getPerson(@AuthenticationPrincipal Jwt jwt){
        FintJwtEndUserPrincipal principal = FintJwtEndUserPrincipal.from(jwt);
        PersonResource person =  personService.getPerson(principal);
        return person;


    }
}
