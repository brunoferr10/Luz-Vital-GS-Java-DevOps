package br.com.minddev.luzvital;

import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.http.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/pacientes")
@PreAuthorize("hasAnyRole('CUIDADOR','GESTOR')")
public class PacienteController {
  private final DomainService service;

  public PacienteController(DomainService service) {
    this.service = service;
  }

  @GetMapping
  public Page<Dtos.PacienteView> list(
      @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "20") int size) {
    return service.patients(page, size);
  }

  @GetMapping("/{id}")
  public Dtos.PacienteView get(@PathVariable Long id) {
    return service.getPatient(id);
  }

  @PostMapping
  public ResponseEntity<Dtos.PacienteView> create(@Valid @RequestBody Dtos.PacienteInput input) {
    var result = service.savePatient(null, input);
    return ResponseEntity.created(java.net.URI.create("/api/v1/pacientes/" + result.id()))
        .body(result);
  }

  @PutMapping("/{id}")
  public Dtos.PacienteView update(
      @PathVariable Long id, @Valid @RequestBody Dtos.PacienteInput input) {
    return service.savePatient(id, input);
  }

  @DeleteMapping("/{id}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void delete(@PathVariable Long id) {
    service.deletePatient(id);
  }
}
