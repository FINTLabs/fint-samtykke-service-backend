package no.fintlabs.fint;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.core.type.TypeReference;
import no.fint.model.resource.AbstractCollectionResources;

import java.util.List;

public class ObjectResources extends AbstractCollectionResources<Object> {
    @Override
    @JsonIgnore
    @Deprecated
    public TypeReference<List<Object>> getTypeReference() {
        return new TypeReference<>() {
        };
    }

    public ObjectResources() {
    }
}