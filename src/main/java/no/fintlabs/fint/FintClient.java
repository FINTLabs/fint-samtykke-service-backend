package no.fintlabs.fint;

import lombok.Data;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Component
public class FintClient {
    private final WebClient webClient;

    private final Map<String, Long> sinceTimestamp = new ConcurrentHashMap<>();

    public FintClient(WebClient webClient) {
        this.webClient = webClient;
    }

    public Mono<List<Object>> getResourcesLastUpdated(String endpoint) {
        return getLastUpdated(endpoint)
                .flatMapIterable(ObjectResources::getContent)
                .collect(Collectors.toList());
    }

    public void resetLastUpdatedTimestamps() {
        this.sinceTimestamp.clear();
    }

    private Mono<ObjectResources> getLastUpdated(String endpoint) {
        return webClient.get()
                .uri(endpoint.concat("/last-updated"))
                .retrieve()
                .bodyToMono(LastUpdated.class)
                .flatMap(lastUpdated -> webClient.get()
                        .uri(endpoint, uriBuilder -> uriBuilder.queryParam("sinceTimeStamp", sinceTimestamp.getOrDefault(endpoint, 0L)).build())
                        .retrieve()
                        .bodyToMono(ObjectResources.class)
                        .doOnNext(it -> sinceTimestamp.put(endpoint, lastUpdated.getLastUpdated()))
                );
    }

    public Mono<Object> getResource(String endpoint) {
        return webClient.get()
                .uri(endpoint)
                .retrieve()
                .bodyToMono(Object.class);
    }

    public <T> Mono<T> getResource(String endpoint, Class<T> clazz) {
        return webClient.get()
                .uri(endpoint)
                .retrieve()
                .bodyToMono(clazz);
    }

    @Data
    private static class LastUpdated {
        private Long lastUpdated;
    }

    public void reset() {
        sinceTimestamp.clear();
    }
}