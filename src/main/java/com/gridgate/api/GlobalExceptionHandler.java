package com.gridgate.api;

import com.gridgate.api.RunDialService.RunNotFoundException;
import com.gridgate.api.RunDialService.RunNotReadyException;
import com.gridgate.calle.CalleApiException;
import java.net.URI;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * Centralized exception handler rendering RFC 7807 {@link ProblemDetail} responses.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ProblemDetail> handleValidation(MethodArgumentNotValidException ex) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                HttpStatus.BAD_REQUEST, "Validation failed for request body");
        problem.setTitle("Invalid Request Payload");
        problem.setType(URI.create("https://gridgate.dev/errors/validation-error"));
        problem.setProperty("timestamp", Instant.now());

        Map<String, String> fieldErrors = new HashMap<>();
        for (FieldError error : ex.getBindingResult().getFieldErrors()) {
            fieldErrors.put(error.getField(), error.getDefaultMessage());
        }
        problem.setProperty("invalid_fields", fieldErrors);

        return ResponseEntity.badRequest().body(problem);
    }

    @ExceptionHandler(RunNotFoundException.class)
    public ResponseEntity<ProblemDetail> handleNotFound(RunNotFoundException ex) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, ex.getMessage());
        problem.setTitle("Run Not Found");
        problem.setType(URI.create("https://gridgate.dev/errors/run-not-found"));
        problem.setProperty("run_id", ex.getRunId());
        problem.setProperty("timestamp", Instant.now());
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(problem);
    }

    @ExceptionHandler(RunNotReadyException.class)
    public ResponseEntity<ProblemDetail> handleNotReady(RunNotReadyException ex) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, ex.getMessage());
        problem.setTitle("Run Not Ready");
        problem.setType(URI.create("https://gridgate.dev/errors/run-not-ready"));
        problem.setProperty("error", "run_not_ready");
        problem.setProperty("current_status", ex.getCurrentStatus().name());
        problem.setProperty("timestamp", Instant.now());
        return ResponseEntity.status(HttpStatus.CONFLICT).body(problem);
    }

    @ExceptionHandler(CalleApiException.class)
    public ResponseEntity<ProblemDetail> handleCalleApi(CalleApiException ex) {
        log.error("Upstream CALL-E API error: status={}, message={}", ex.getStatusCode(), ex.getMessage());
        HttpStatus status = ex.getStatusCode() > 0 ? HttpStatus.resolve(ex.getStatusCode()) : HttpStatus.BAD_GATEWAY;
        if (status == null) {
            status = HttpStatus.BAD_GATEWAY;
        }
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(status, ex.getMessage());
        problem.setTitle("CALL-E Telephony API Error");
        problem.setType(URI.create("https://gridgate.dev/errors/calle-api-error"));
        problem.setProperty("upstream_status", ex.getStatusCode());
        problem.setProperty("timestamp", Instant.now());
        return ResponseEntity.status(status).body(problem);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ProblemDetail> handleIllegalArgument(IllegalArgumentException ex) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, ex.getMessage());
        problem.setTitle("Bad Request");
        problem.setType(URI.create("https://gridgate.dev/errors/bad-request"));
        problem.setProperty("timestamp", Instant.now());
        return ResponseEntity.badRequest().body(problem);
    }

    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<ProblemDetail> handleIllegalState(IllegalStateException ex) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, ex.getMessage());
        problem.setTitle("Conflict / Illegal State");
        problem.setType(URI.create("https://gridgate.dev/errors/conflict"));
        problem.setProperty("timestamp", Instant.now());
        return ResponseEntity.status(HttpStatus.CONFLICT).body(problem);
    }
}
