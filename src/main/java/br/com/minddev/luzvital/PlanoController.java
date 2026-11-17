package br.com.minddev.luzvital;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/pacientes")
@PreAuthorize("hasAnyRole('CUIDADOR','GESTOR')")
public class PlanoController {
  private final PlanoService service;

  public PlanoController(PlanoService service) {
    this.service = service;
  }

  @PostMapping("/{id}/plano")
  public Dtos.Plano generate(@PathVariable Long id) {
    return service.generate(id);
  }
}
