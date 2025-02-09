package com.unicorn.store.controller;

import com.unicorn.store.exceptions.ResourceNotFoundException;
import com.unicorn.store.model.Unicorn;
import com.unicorn.store.service.UnicornService;
// >= Dockerfile_04
// import io.opentelemetry.api.trace.Span;
// import io.opentelemetry.api.trace.Tracer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

import static org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR;
import static org.springframework.http.HttpStatus.NOT_FOUND;

@RestController
public class UnicornController {

    // >= Dockerfile_04
    // @Autowired
    // private Tracer tracer;
    private final UnicornService unicornService;
    private static final Logger logger = LoggerFactory.getLogger(UnicornController.class);

    public UnicornController(UnicornService unicornService) {
        this.unicornService = unicornService;
    }

    @PostMapping("/unicorns")
    public ResponseEntity<Unicorn> createUnicorn(@RequestBody Unicorn unicorn) {
        // >= Dockerfile_04
        // Span span = tracer.spanBuilder("Create Unicorn").startSpan();
        try {
            var savedUnicorn = unicornService.createUnicorn(unicorn);
            return ResponseEntity.ok(savedUnicorn);
        } catch (Exception e) {
            String errorMsg = "Error creating unicorn";
            // >= Dockerfile_04
            // span.recordException(e);
            logger.error(errorMsg, e);
            throw new ResponseStatusException(INTERNAL_SERVER_ERROR, errorMsg, e);
        } finally {
            // >= Dockerfile_04
            // span.end();
        }
    }

    @GetMapping("/unicorns")
    public ResponseEntity<List<Unicorn>> getAllUnicorns() {
        // Span span = tracer.spanBuilder("Get all Unicorns").startSpan();

        try {
            var savedUnicorns = unicornService.getAllUnicorns();
            return ResponseEntity.ok(savedUnicorns);
        } catch (Exception e) {
            String errorMsg = "Error reading unicorns";
            // span.recordException(e);
            logger.error(errorMsg, e);
            throw new ResponseStatusException(INTERNAL_SERVER_ERROR, errorMsg, e);
        } finally {
            // span.end();
        }
    }

    @PutMapping("/unicorns/{unicornId}")
    public ResponseEntity<Unicorn> updateUnicorn(@RequestBody Unicorn unicorn,
            @PathVariable String unicornId) {
        // Span span = tracer.spanBuilder("Update Unicorn").startSpan();

        try {
            var savedUnicorn = unicornService.updateUnicorn(unicorn, unicornId);
            return ResponseEntity.ok(savedUnicorn);
        } catch (Exception e) {
            String errorMsg = "Error updating unicorn";
            // span.recordException(e);
            logger.error(errorMsg, e);
            throw new ResponseStatusException(INTERNAL_SERVER_ERROR, errorMsg, e);
        } finally {
            // span.end();
        }
    }

    @GetMapping("/unicorns/{unicornId}")
    public ResponseEntity<Unicorn> getUnicorn(@PathVariable String unicornId) {
        // Span span = tracer.spanBuilder("Get Unicorn").startSpan();

        try {
            var unicorn = unicornService.getUnicorn(unicornId);
            return ResponseEntity.ok(unicorn);
        } catch (ResourceNotFoundException e) {
            String errorMsg = "Unicorn not found";
            // span.recordException(e);
            logger.error(errorMsg, e);
            throw new ResponseStatusException(NOT_FOUND, errorMsg, e);
        } finally {
            // span.end();
        }
    }

    @DeleteMapping("/unicorns/{unicornId}")
    public ResponseEntity<String> deleteUnicorn(@PathVariable String unicornId) {
        // Span span = tracer.spanBuilder("Get Unicorn").startSpan();

        try {
            unicornService.deleteUnicorn(unicornId);
            return ResponseEntity.ok().build();
        } catch (ResourceNotFoundException e) {
            String errorMsg = "Unicorn not found";
            logger.error(errorMsg, e);
            // span.recordException(e);
            throw new ResponseStatusException(NOT_FOUND, errorMsg, e);
        } finally {
            // span.end();
        }
    }

    @GetMapping("/health")
    ResponseEntity<String> health() {
        return new ResponseEntity<>("Healthy!", HttpStatus.OK);
    }

    @GetMapping("/")
    ResponseEntity<String> root() {
        return new ResponseEntity<>("OK!", HttpStatus.OK);
    }
}
