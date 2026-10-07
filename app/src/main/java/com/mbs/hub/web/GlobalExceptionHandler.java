package com.mbs.hub.web;

import jakarta.validation.ConstraintViolationException;
import java.net.URI;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.stream.Collectors;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

/**
 * RFC 7807 ProblemDetails handler — maps common exceptions to the error
 * contract described in 02 API §6.
 */
@ControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ProblemDetail> handleValidation(MethodArgumentNotValidException ex) {
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST,
            "Request body failed validation.");
        pd.setType(URI.create("https://hub.local/errors/validation"));
        pd.setTitle("Validation failed");
        List<String> errs = ex.getBindingResult().getFieldErrors().stream()
            .map(fe -> fe.getField() + ": " + fe.getDefaultMessage())
            .collect(Collectors.toList());
        pd.setProperty("errors", errs);
        return ResponseEntity.badRequest().body(pd);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ProblemDetail> handleConstraint(ConstraintViolationException ex) {
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, ex.getMessage());
        pd.setType(URI.create("https://hub.local/errors/validation"));
        pd.setTitle("Constraint violated");
        return ResponseEntity.badRequest().body(pd);
    }

    @ExceptionHandler(NoSuchElementException.class)
    public ResponseEntity<ProblemDetail> handleNotFound(NoSuchElementException ex) {
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, ex.getMessage());
        pd.setType(URI.create("https://hub.local/errors/not-found"));
        pd.setTitle("Resource not found");
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(pd);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ProblemDetail> handleBadInput(IllegalArgumentException ex) {
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, ex.getMessage());
        pd.setType(URI.create("https://hub.local/errors/bad-request"));
        pd.setTitle("Bad request");
        return ResponseEntity.badRequest().body(pd);
    }

    /** AC-006.5 / 02 API CV-04: Jira rejected the write → 422 with Jira's message so the UI can roll back. */
    @ExceptionHandler(com.mbs.hub.jira.client.JiraClientException.class)
    public ResponseEntity<ProblemDetail> handleJiraRejected(com.mbs.hub.jira.client.JiraClientException ex) {
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(HttpStatus.UNPROCESSABLE_ENTITY, ex.getMessage());
        pd.setType(URI.create("https://hub.local/errors/jira-rejected"));
        pd.setTitle("Jira rejected the change");
        pd.setProperty("jiraStatus", ex.status());
        return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY).body(pd);
    }

    /** Jira 429 on a write: tell the caller when to retry instead of a bare 500. */
    @ExceptionHandler(com.mbs.hub.jira.client.JiraRateLimitException.class)
    public ResponseEntity<ProblemDetail> handleJiraRateLimited(com.mbs.hub.jira.client.JiraRateLimitException ex) {
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(HttpStatus.SERVICE_UNAVAILABLE, ex.getMessage());
        pd.setType(URI.create("https://hub.local/errors/jira-rate-limited"));
        pd.setTitle("Jira rate limit — retry later");
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .header("Retry-After", String.valueOf(ex.retryAfter().toSeconds()))
                .body(pd);
    }

    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<ProblemDetail> handleConflict(IllegalStateException ex) {
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, ex.getMessage());
        pd.setType(URI.create("https://hub.local/errors/conflict"));
        pd.setTitle("Conflict");
        return ResponseEntity.status(HttpStatus.CONFLICT).body(pd);
    }
}
