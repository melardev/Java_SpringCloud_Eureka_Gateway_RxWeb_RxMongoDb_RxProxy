package com.melardev.spring.rest.controllers;

import com.melardev.spring.rest.dtos.responses.ErrorResponse;
import com.melardev.spring.rest.models.Todo;
import com.melardev.spring.rest.repositories.TodosRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import javax.validation.Valid;

@CrossOrigin
@RestController
@RequestMapping("")
public class TodosController {

    @Autowired
    TodosRepository todosRepository;

    @GetMapping
    public Flux<Todo> getAll() {
        return todosRepository.findAllHqlSummary();
    }

    @GetMapping("/pending")
    public Flux<Todo> getPending() {
        return todosRepository.findByCompletedFalse();
    }

    @GetMapping("/completed")
    public Flux<Todo> getCompleted() {
        return todosRepository.findByCompletedIsTrueHql();
    }

    @GetMapping("/{id}")
    public Mono<ResponseEntity<Object>> getById(@PathVariable("id") String id) {
        return this.todosRepository.findById(id)
                .map(ResponseEntity::ok)
                .defaultIfEmpty(new ResponseEntity(new ErrorResponse("Todo not found"), HttpStatus.NOT_FOUND));
    }

    @PostMapping
    public Mono<ResponseEntity<Todo>> create(@Valid @RequestBody Todo todo) {
        return todosRepository.save(todo).map(t -> ResponseEntity.status(HttpStatus.CREATED).body(t));
    }

    @PutMapping("/{id}")
    public Mono<ResponseEntity<Object>> update(@PathVariable("id") String id, @RequestBody Todo todoInput) {
        // Return would be either ResponseEntity<AppResponse> or ResponseEntity<Todo>

        return todosRepository.findById(id)
                .flatMap(t -> {
                    String title = todoInput.getTitle();
                    if (title != null)
                        t.setTitle(title);

                    String description = todoInput.getDescription();
                    if (description != null)
                        t.setDescription(description);

                    t.setCompleted(todoInput.isCompleted());
                    return todosRepository.save(t);
                })
                .map(ResponseEntity::ok)
                .defaultIfEmpty(new ResponseEntity(new ErrorResponse("Not found"), HttpStatus.NOT_FOUND));
    }

    /*
    @DeleteMapping("/{id}")
    public Mono<ResponseEntity<Object>> delete(@PathVariable("id") String id) {
        return todosRepository.deleteById(id)
                .then(Mono.just(new ResponseEntity<>(HttpStatus.NO_CONTENT)))
                // defaultIfEmpty should be never called actually, deleteById returns a Mono of Void
                .defaultIfEmpty(new ResponseEntity<>(new ErrorResponse("Todo not found"), HttpStatus.NOT_FOUND));
    }
    */


    @DeleteMapping("/{id}")
    public Mono<ResponseEntity<Object>> delete(@PathVariable("id") String id) {
        return todosRepository.findById(id)
                .flatMap(t -> todosRepository.delete(t)
                        .then(Mono.just(new ResponseEntity<>(HttpStatus.NO_CONTENT))))
                .defaultIfEmpty(new ResponseEntity<>(new ErrorResponse("Todo not found"), HttpStatus.NOT_FOUND));
    }

    @DeleteMapping
    public Mono<ResponseEntity<Void>> deleteAll() {
        return todosRepository.deleteAll().then(Mono.just(new ResponseEntity<>(HttpStatus.NO_CONTENT)));
    }

}