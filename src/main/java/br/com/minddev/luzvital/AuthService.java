package br.com.minddev.luzvital;

import java.time.Instant;
import java.util.Locale;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class AuthService {
  private final UsuarioRepository users;
  private final PasswordEncoder passwords;
  private final JwtEncoder encoder;
  private final long minutes;

  public AuthService(
      UsuarioRepository users,
      PasswordEncoder passwords,
      JwtEncoder encoder,
      @Value("${app.token-minutes}") long minutes) {
    this.users = users;
    this.passwords = passwords;
    this.encoder = encoder;
    this.minutes = minutes;
  }

  @Transactional
  public Dtos.UsuarioView register(Dtos.Registro input) {
    return create(new Dtos.UsuarioInput(input.nome(), input.email(), input.senha(), "CUIDADOR"));
  }

  @Transactional
  public Dtos.UsuarioView create(Dtos.UsuarioInput input) {
    var u = new Usuario();
    apply(u, input);
    return view(users.save(u));
  }

  public Dtos.Token login(Dtos.Login input) {
    var u =
        users
            .findByEmail(input.email().toLowerCase(Locale.ROOT))
            .filter(x -> passwords.matches(input.senha(), x.senha))
            .orElseThrow(
                () ->
                    new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Credenciais invalidas"));
    var now = Instant.now();
    var claims =
        JwtClaimsSet.builder()
            .issuer("luz-vital")
            .subject(u.id.toString())
            .issuedAt(now)
            .expiresAt(now.plusSeconds(minutes * 60))
            .claim("papel", u.papel)
            .build();
    String token =
        encoder
            .encode(JwtEncoderParameters.from(JwsHeader.with(MacAlgorithm.HS256).build(), claims))
            .getTokenValue();
    return new Dtos.Token(token, "Bearer", minutes * 60, u.papel);
  }

  void apply(Usuario u, Dtos.UsuarioInput input) {
    if (input.senha().getBytes(java.nio.charset.StandardCharsets.UTF_8).length > 72)
      throw new ResponseStatusException(
          HttpStatus.BAD_REQUEST, "Senha deve ter ate 72 bytes UTF-8");
    u.nome = input.nome();
    u.email = input.email().toLowerCase(Locale.ROOT);
    u.senha = passwords.encode(input.senha());
    u.papel = input.papel();
  }

  static Dtos.UsuarioView view(Usuario u) {
    return new Dtos.UsuarioView(u.id, u.nome, u.email, u.papel);
  }
}
