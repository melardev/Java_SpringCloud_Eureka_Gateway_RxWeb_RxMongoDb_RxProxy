package com.melardev.spring.rest.controllers;

import com.melardev.spring.rest.models.Todo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import javax.validation.Valid;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("todos")
public class TodoProxyController {

    @Autowired
    @Qualifier("loadBalancedRxWebClientBuilder")
    WebClient.Builder rxWebClientBuilder;


    @ExceptionHandler(WebClientResponseException.class)
    public ResponseEntity<Map<String, Object>> onException(WebClientResponseException ex) {
        Map<String, Object> res = new HashMap<>();
        res.put("success", false);
        res.put("full_messages", new String[]{ex.getResponseBodyAsString()});
        return new ResponseEntity<>(res, ex.getStatusCode());
    }

    @GetMapping
    public Mono<ResponseEntity<String>> getAllAsMono() {
        return fetchAsMonoEntity();
    }

    @GetMapping("/flux")
    public Flux<Todo> getAllAsFlux() {
        return rxWebClientBuilder
                .defaultHeader(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
                .build().get()
                .uri("http://todo-service/").retrieve().bodyToFlux(Todo.class);
    }

    private Mono<ResponseEntity<String>> fetchAsMonoEntity() {
        return fetchAsMonoEntity("");
    }

    private Mono<ResponseEntity<String>> fetchAsMonoEntity(HttpMethod method) {
        return fetchAsMonoEntity(method, "", null);
    }

    private Mono<ResponseEntity<String>> fetchAsMonoEntity(String path) {
        return fetchAsMonoEntity(HttpMethod.GET, path, null);
    }

    private Mono<ResponseEntity<String>> fetchAsMonoEntity(HttpMethod httpMethod, String path) {
        return fetchAsMonoEntity(httpMethod, path, null);
    }

    private Mono<ResponseEntity<String>> fetchAsMonoEntityWithBody(HttpMethod httpMethod, String body) {
        return fetchAsMonoEntity(httpMethod, "", body);
    }

    private Mono<ResponseEntity<String>> fetchAsMonoEntity(HttpMethod httpMethod, String path, String body) {
        if (path == null)
            path = "";
        else if (path.startsWith("/"))
            path = path.substring(1);

        WebClient client = rxWebClientBuilder
                .defaultHeader(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
                .build();
        WebClient.RequestHeadersUriSpec<?> readRequest;

        if (httpMethod == HttpMethod.GET || httpMethod == HttpMethod.DELETE) {
            // Requests without a payload
            if (httpMethod == HttpMethod.GET) {
                readRequest = client.get();
            } else {
                readRequest = client.delete();
            }

            // return readRequest.uri("http://todo-service/{path}", path).retrieve().bodyToMono(String.class);
            return readRequest.uri("http://todo-service/{path}", path).exchange().flatMap(r -> r.toEntity(String.class));
        } else if (httpMethod == HttpMethod.POST || httpMethod == HttpMethod.PUT) {

            // Requests with a payload

            WebClient.RequestBodyUriSpec writeRequest;
            if (httpMethod == HttpMethod.POST) {
                writeRequest = client.post();
            } else {
                writeRequest = client.put();
            }

            if (body != null) {
                writeRequest
                        .body(Mono.just(body), String.class);
            }

            writeRequest.header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE);
            return writeRequest.uri("http://todo-service/{path}", path).exchange().flatMap(r -> r.toEntity(String.class));
        }
        throw new IllegalArgumentException("You must provide a valid Http Method");
    }


    @GetMapping("/pending")
    public Mono<ResponseEntity<String>> getPending() {
        return fetchAsMonoEntity("pending");
    }

    @GetMapping("/completed")
    public Mono<ResponseEntity<String>> getCompleted() {
        return fetchAsMonoEntity("/completed");
    }


    @GetMapping("/{id}")
    public Mono<ResponseEntity<String>> getById(@PathVariable("id") String id) {
        return fetchAsMonoEntity(id);
    }

    @PostMapping
    public Mono<ResponseEntity<String>> create(@Valid @RequestBody String todo) {
        return fetchAsMonoEntityWithBody(HttpMethod.POST, todo);
    }

    @PutMapping("/{id}")
    public Mono<ResponseEntity<String>> update(@PathVariable("id") String id, @RequestBody String todoInput) {
        return fetchAsMonoEntity(HttpMethod.PUT, id, todoInput);
    }

    @DeleteMapping("/{id}")
    public Mono<ResponseEntity<String>> delete(@PathVariable("id") String id) {
        return fetchAsMonoEntity(HttpMethod.DELETE, id);
    }

    @DeleteMapping
    public Mono<ResponseEntity<String>> deleteAll() {
        return fetchAsMonoEntity(HttpMethod.DELETE);
    }

}
