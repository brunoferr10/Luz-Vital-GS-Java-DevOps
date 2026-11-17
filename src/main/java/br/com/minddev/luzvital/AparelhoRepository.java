package br.com.minddev.luzvital;

import org.springframework.data.jpa.repository.JpaRepository;

public interface AparelhoRepository extends JpaRepository<Aparelho, Long> {
  java.util.List<Aparelho> findByPacienteId(Long id);

  org.springframework.data.domain.Page<Aparelho> findByPacienteCuidadorId(
      Long id, org.springframework.data.domain.Pageable page);

  boolean existsByPacienteId(Long id);
}
