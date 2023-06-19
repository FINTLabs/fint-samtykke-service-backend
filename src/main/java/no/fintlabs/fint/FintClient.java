package no.fintlabs.fint;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import reactor.retry.Repeat;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Slf4j
@Service
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


    public <K, T> Mono<ResponseEntity<Void>> postResource(String url, T request, Class<K> clazz) {
        return webClient.post()
                .uri(url)
                .body(BodyInserters.fromValue(request))
                .retrieve()
                .toBodilessEntity();
    }

    public ResponseEntity<Void> waitUntilCreatedTest(String url) {
        int count = 0;
        ResponseEntity<Void> responseEntity;

        while (count++ < 60) {
            log.info("Getting Location Status");
            try {
                 responseEntity = webClient.head()
                        .uri(url)
                        .retrieve()
                        .toBodilessEntity()
                        .block();
            } catch (Exception exception) {
                log.error("Error while fetching location status", exception);
                throw exception;
            }

             if (responseEntity.getStatusCode() == HttpStatus.CREATED) {
                 log.info("Status is Created");
                 return responseEntity;
             } else {
                 log.info("Status: " + responseEntity.getStatusCode());
             }

            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                log.error("Failed to sleep");
            }
        }
        throw new RuntimeException("Error while fetching location url");
    }

    public Mono<ResponseEntity<Void>> waitUntilCreated(String url) {
        return waitUntilCreated(url, 1000, 5000);
    }

    public Mono<ResponseEntity<Void>> waitUntilCreated(String url, int firstBackoff, int maxBackOff) {
        int maxAttempts = 50;

        return webClient.head()
                .uri(url)
                .retrieve()
                .toBodilessEntity()
                .doOnEach(signal -> {
                    if (signal.isOnNext()) {
                        log.info("Received status: " + signal.get().getStatusCode().name());
                    } else if (signal.isOnError()) {
                        log.error("Error occurred: ", signal.getThrowable());
                    } else {
                        log.debug("Signal: " + signal);
                    }
                })
                .filter(response -> response.getStatusCode() == HttpStatus.CREATED)
                .repeatWhenEmpty(Repeat.onlyIf(repeatContext -> repeatContext.iteration() < maxAttempts)
                        .exponentialBackoff(Duration.ofMillis(firstBackoff), Duration.ofMillis(maxBackOff))
                        .timeout(Duration.ofSeconds(60)))
                .doOnSuccess(responseEntity -> log.info("Final response entity: " + responseEntity.toString()))
                .doOnError(error -> log.error(error.getMessage(), error));
    }

    public <K, T> Mono<ResponseEntity<Void>> putResource(String url, T request, Class<K> clazz) {

        return webClient.put()
                .uri(url)
                .body(BodyInserters.fromValue(request))
                .retrieve()
                .toBodilessEntity();
    }

    @Data
    private static class LastUpdated {
        private Long lastUpdated;
    }

    public void reset() {
        sinceTimestamp.clear();
    }
}