package br.com.minddev.luzvital;

import jakarta.persistence.*;

@Entity
@Table(name = "aparelhos")
public class Aparelho {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  public Long id;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "paciente_id")
  public Paciente paciente;

  @Column(nullable = false, length = 120)
  public String modelo;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 30)
  public Tipo tipo;

  @Column(nullable = false)
  public Integer potenciaWatts;

  @Column(nullable = false)
  public Integer autonomiaMinutos;

  public enum Tipo {
    VENTILADOR,
    CONCENTRADOR,
    BOMBA,
    OUTRO
  }

  public Long getId() {
    return id;
  }
}
