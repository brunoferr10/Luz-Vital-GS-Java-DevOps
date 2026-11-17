package br.com.minddev.luzvital;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {
  private final AuthService auth;

  public AuthController(AuthService auth) {
    this.auth = auth;
  }

  @PostMapping("/registro")
  @ResponseStatus(HttpStatus.CREATED)
  public Dtos.UsuarioView register(@Valid @RequestBody Dtos.Registro input) {
    return auth.register(input);
  }

  @PostMapping("/login")
  public Dtos.Token login(@Valid @RequestBody Dtos.Login input) {
    return auth.login(input);
  }
}
