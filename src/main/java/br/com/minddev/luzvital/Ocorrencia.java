package br.com.minddev.luzvital;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(
    name = "ocorrencias",
    uniqueConstraints = @UniqueConstraint(columnNames = {"solicitante_id", "chave"}))
public class Ocorrencia {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  public Long id;

  @Version public Long versao;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "paciente_id")
  public Paciente paciente;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "solicitante_id")
  public Usuario solicitante;

  @Column(nullable = false, length = 64)
  public String chave;

  @Column(nullable = false)
  public Instant inicio;

  @Column(nullable = false)
  public Integer autonomiaInicial;

  @Column(nullable = false)
  public Integer desempate;

  @Column(nullable = false, length = 20)
  public String status;

  @Column(nullable = false, length = 400)
  public String observacao;

  @Column(nullable = false)
  public Boolean eventoPendente = true;

  @Column(nullable = false)
  public Boolean eventoRecebido = false;

  public Instant encerramento;

  public Long getId() {
    return id;
  }
}
