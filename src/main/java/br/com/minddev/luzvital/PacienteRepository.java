package br.com.minddev.luzvital;

import org.springframework.data.jpa.repository.JpaRepository;

public interface PacienteRepository extends JpaRepository<Paciente, Long> {
  org.springframework.data.domain.Page<Paciente> findByCuidadorId(
      Long id, org.springframework.data.domain.Pageable page);

  boolean existsByCuidadorId(Long id);
}
