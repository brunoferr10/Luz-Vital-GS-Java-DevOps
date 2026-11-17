package br.com.minddev.luzvital;

import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.http.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/usuarios")
@PreAuthorize("hasRole('GESTOR')")
@Transactional
public class UsuarioController {
  private final AuthService auth;
  private final DomainService domain;

  public UsuarioController(AuthService auth, DomainService domain) {
    this.auth = auth;
    this.domain = domain;
  }

  @GetMapping
  public Page<Dtos.UsuarioView> list(
      @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "20") int size) {
    return domain.usersRepository().findAll(domain.page(page, size)).map(AuthService::view);
  }

  @GetMapping("/{id}")
  public Dtos.UsuarioView get(@PathVariable Long id) {
    return AuthService.view(
        domain.usersRepository().findById(id).orElseThrow(DomainService::missing));
  }

  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  public Dtos.UsuarioView create(@Valid @RequestBody Dtos.UsuarioInput in) {
    return auth.create(in);
  }

  @PutMapping("/{id}")
  public Dtos.UsuarioView update(@PathVariable Long id, @Valid @RequestBody Dtos.UsuarioInput in) {
    var u = domain.usersRepository().findById(id).orElseThrow(DomainService::missing);
    if (id.equals(domain.userId()) && !in.papel().equals("GESTOR"))
      DomainService.conflict("Nao altere seu proprio papel");
    auth.apply(u, in);
    return AuthService.view(domain.usersRepository().save(u));
  }

  @DeleteMapping("/{id}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void delete(@PathVariable Long id) {
    var u = domain.usersRepository().findById(id).orElseThrow(DomainService::missing);
    if (id.equals(domain.userId())
        || domain.patientsRepository().existsByCuidadorId(id)
        || domain.incidentsRepository().existsBySolicitanteId(id))
      DomainService.conflict("Usuario em uso");
    domain.usersRepository().delete(u);
  }
}
