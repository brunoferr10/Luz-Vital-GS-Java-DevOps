package br.com.minddev.luzvital;

import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.http.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/aparelhos")
@PreAuthorize("hasAnyRole('CUIDADOR','GESTOR')")
public class AparelhoController {
  private final DomainService service;

  public AparelhoController(DomainService service) {
    this.service = service;
  }

  @GetMapping
  public Page<Dtos.AparelhoView> list(
      @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "20") int size) {
    return service.devices(page, size);
  }

  @GetMapping("/{id}")
  public Dtos.AparelhoView get(@PathVariable Long id) {
    return service.getDevice(id);
  }

  @PostMapping
  public ResponseEntity<Dtos.AparelhoView> create(@Valid @RequestBody Dtos.AparelhoInput input) {
    var result = service.saveDevice(null, input);
    return ResponseEntity.created(java.net.URI.create("/api/v1/aparelhos/" + result.id()))
        .body(result);
  }

  @PutMapping("/{id}")
  public Dtos.AparelhoView update(
      @PathVariable Long id, @Valid @RequestBody Dtos.AparelhoInput input) {
    return service.saveDevice(id, input);
  }

  @DeleteMapping("/{id}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void delete(@PathVariable Long id) {
    service.deleteDevice(id);
  }
}
