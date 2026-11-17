package br.com.minddev.luzvital;

import jakarta.persistence.*;

@Entity
@Table(name = "usuarios")
public class Usuario {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  public Long id;

  @Column(nullable = false, length = 120)
  public String nome;

  @Column(nullable = false, unique = true, length = 160)
  public String email;

  @Column(nullable = false, length = 100)
  public String senha;

  @Column(nullable = false, length = 20)
  public String papel;

  public Long getId() {
    return id;
  }

  public String getPapel() {
    return papel;
  }
}
