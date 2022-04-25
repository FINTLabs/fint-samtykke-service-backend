package no.fintlabs.client;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.core.type.TypeReference;
import no.fint.model.resource.AbstractCollectionResources;
import org.springframework.context.annotation.Configuration;

import java.util.List;

public class ObjectResources extends AbstractCollectionResources<Object> {
    @Override
    @JsonIgnore
    @Deprecated
    public TypeReference<List<Object>> getTypeReference() {
        return new TypeReference<>(){
        };
    }

    public ObjectResources() {
    }
}