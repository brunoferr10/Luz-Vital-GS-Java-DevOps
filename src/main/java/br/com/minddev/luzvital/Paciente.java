package br.com.minddev.luzvital;

import jakarta.persistence.*;

@Entity
@Table(name = "pacientes")
public class Paciente {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  public Long id;

  @Column(nullable = false, length = 120)
  public String nome;

  @Column(nullable = false, length = 200)
  public String endereco;

  @Column(nullable = false, length = 30)
  public String telefone;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "cuidador_id")
  public Usuario cuidador;

  @Column(name = "plano_contingencia", length = 6000)
  public String plano;

  public Long getId() {
    return id;
  }

  public Usuario getCuidador() {
    return cuidador;
  }
}
