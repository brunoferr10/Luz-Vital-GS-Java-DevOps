package br.com.minddev.luzvital;

import jakarta.validation.Valid;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.http.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/ocorrencias")
@PreAuthorize("hasAnyRole('CUIDADOR','GESTOR')")
public class OcorrenciaController {
  private final DomainService service;

  public OcorrenciaController(DomainService service) {
    this.service = service;
  }

  @GetMapping
  public Page<Dtos.OcorrenciaView> list(
      @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "20") int size) {
    return service.incidents(page, size);
  }

  @GetMapping("/{id}")
  public Dtos.OcorrenciaView get(@PathVariable Long id) {
    return service.getIncident(id);
  }

  @GetMapping("/fila")
  @PreAuthorize("hasRole('GESTOR')")
  public List<Dtos.OcorrenciaView> queue() {
    return service.queue();
  }

  @PostMapping
  public ResponseEntity<Dtos.OcorrenciaView> create(
      @Valid @RequestBody Dtos.OcorrenciaInput in, @RequestHeader("Idempotency-Key") String key) {
    var o = service.open(in, key);
    return ResponseEntity.created(java.net.URI.create("/api/v1/ocorrencias/" + o.id())).body(o);
  }

  @PutMapping("/{id}")
  @PreAuthorize("hasRole('GESTOR')")
  public Dtos.OcorrenciaView update(
      @PathVariable Long id, @Valid @RequestBody Dtos.OcorrenciaUpdate in) {
    return service.updateIncident(id, in);
  }

  @DeleteMapping("/{id}")
  @PreAuthorize("hasRole('GESTOR')")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void delete(@PathVariable Long id) {
    service.deleteIncident(id);
  }
}
