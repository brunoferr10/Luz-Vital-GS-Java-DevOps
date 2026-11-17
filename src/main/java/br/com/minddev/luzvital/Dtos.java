package br.com.minddev.luzvital;

import jakarta.validation.constraints.*;
import java.time.Instant;

public final class Dtos {
  private Dtos() {}

  public record Registro(
      @NotBlank @Size(max = 120) String nome,
      @NotBlank @Email @Size(max = 160) String email,
      @NotBlank @Size(min = 10, max = 72) String senha) {}

  public record Login(@NotBlank @Email String email, @NotBlank String senha) {}

  public record Token(String accessToken, String tokenType, long expiresIn, String papel) {}

  public record UsuarioInput(
      @NotBlank @Size(max = 120) String nome,
      @NotBlank @Email @Size(max = 160) String email,
      @NotBlank @Size(min = 10, max = 72) String senha,
      @Pattern(regexp = "CUIDADOR|GESTOR") @NotBlank String papel) {}

  public record UsuarioView(Long id, String nome, String email, String papel) {}

  public record PacienteInput(
      @NotBlank @Size(max = 120) String nome,
      @NotBlank @Size(max = 200) String endereco,
      @NotBlank @Size(max = 30) String telefone,
      @NotNull @Positive Long cuidadorId) {}

  public record PacienteView(
      Long id,
      String nome,
      String endereco,
      String telefone,
      Long cuidadorId,
      String planoContingencia) {}

  public record AparelhoInput(
      @NotNull @Positive Long pacienteId,
      @NotBlank @Size(max = 120) String modelo,
      @NotNull Aparelho.Tipo tipo,
      @NotNull @Min(1) @Max(100000) Integer potenciaWatts,
      @NotNull @Min(0) @Max(10080) Integer autonomiaMinutos) {}

  public record AparelhoView(
      Long id,
      Long pacienteId,
      String modelo,
      Aparelho.Tipo tipo,
      Integer potenciaWatts,
      Integer autonomiaMinutos) {}

  public record OcorrenciaInput(
      @NotNull @Positive Long pacienteId,
      @NotNull @PastOrPresent Instant inicio,
      @NotBlank @Size(max = 400) String observacao) {}

  public record OcorrenciaUpdate(
      @NotBlank @Pattern(regexp = "ABERTA|ENCERRADA") String status,
      @NotBlank @Size(max = 400) String observacao) {}

  public record OcorrenciaView(
      Long id,
      Long pacienteId,
      Instant inicio,
      String status,
      String observacao,
      long minutosRestantes,
      boolean eventoRecebido,
      Instant encerramento) {}

  public record Plano(String primeirasAcoes, String pedirAjuda, String preparacao, String aviso) {}

  public record Versao(String versao, String mensagem) {}
}
