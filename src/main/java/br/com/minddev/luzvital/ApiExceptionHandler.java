package br.com.minddev.luzvital;

import java.time.Instant;
import java.util.*;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.*;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingRequestHeaderException;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.server.ResponseStatusException;

@RestControllerAdvice
public class ApiExceptionHandler {
  public record Erro(
      int status, String codigo, String mensagem, Instant timestamp, List<String> detalhes) {}

  ResponseEntity<Erro> error(int status, String code, String message, List<String> details) {
    return ResponseEntity.status(status)
        .body(new Erro(status, code, message, Instant.now(), details));
  }

  @ExceptionHandler(MethodArgumentNotValidException.class)
  ResponseEntity<Erro> validation(MethodArgumentNotValidException e) {
    return error(
        400,
        "VALIDACAO",
        "Revise os campos",
        e.getBindingResult().getFieldErrors().stream()
            .map(x -> x.getField() + ": " + x.getDefaultMessage())
            .toList());
  }

  @ExceptionHandler(ResponseStatusException.class)
  ResponseEntity<Erro> status(ResponseStatusException e) {
    return error(e.getStatusCode().value(), "REGRA_NEGOCIO", e.getReason(), List.of());
  }

  @ExceptionHandler({
    DataIntegrityViolationException.class,
    ObjectOptimisticLockingFailureException.class
  })
  ResponseEntity<Erro> conflict(Exception e) {
    return error(
        409,
        "CONFLITO",
        "Registro duplicado, referenciado ou alterado em paralelo. Consulte e tente novamente.",
        List.of());
  }

  @ExceptionHandler(AccessDeniedException.class)
  ResponseEntity<Erro> forbidden(Exception e) {
    return error(403, "ACESSO_NEGADO", "Permissao insuficiente", List.of());
  }

  @ExceptionHandler({
    HttpMessageNotReadableException.class,
    MissingRequestHeaderException.class,
    MethodArgumentTypeMismatchException.class
  })
  ResponseEntity<Erro> malformed(Exception e) {
    return error(400, "REQUISICAO_INVALIDA", "JSON, parametro ou cabecalho invalido", List.of());
  }

  @ExceptionHandler(Exception.class)
  ResponseEntity<Erro> other(Exception e) {
    org.slf4j.LoggerFactory.getLogger(getClass()).error("Erro interno", e);
    return error(500, "ERRO_INTERNO", "Nao foi possivel concluir a operacao", List.of());
  }
}
