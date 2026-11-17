package br.com.minddev.luzvital;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.*;
import org.springframework.stereotype.Component;

@Component
public class Bootstrap implements ApplicationRunner {
  private final AuthService auth;
  private final UsuarioRepository users;
  private final String email, password;

  public Bootstrap(
      AuthService auth,
      UsuarioRepository users,
      @Value("${app.bootstrap.email}") String email,
      @Value("${app.bootstrap.password}") String password) {
    this.auth = auth;
    this.users = users;
    this.email = email;
    this.password = password;
  }

  public void run(ApplicationArguments args) {
    if (!email.isBlank() && users.findByEmail(email.toLowerCase(java.util.Locale.ROOT)).isEmpty()) {
      if (password.length() < 10 || password.length() > 72)
        throw new IllegalArgumentException("BOOTSTRAP_PASSWORD deve ter 10 a 72 caracteres");
      auth.create(new Dtos.UsuarioInput("Gestor inicial", email, password, "GESTOR"));
    }
  }
}
