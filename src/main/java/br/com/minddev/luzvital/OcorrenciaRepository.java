package br.com.minddev.luzvital;

import org.springframework.data.jpa.repository.JpaRepository;

public interface OcorrenciaRepository extends JpaRepository<Ocorrencia, Long> {
  java.util.Optional<Ocorrencia> findBySolicitanteIdAndChave(Long id, String chave);

  org.springframework.data.domain.Page<Ocorrencia> findByPacienteCuidadorId(
      Long id, org.springframework.data.domain.Pageable page);

  java.util.List<Ocorrencia> findByStatus(String status);

  java.util.List<Ocorrencia> findTop20ByEventoPendenteTrueOrderByIdAsc();

  boolean existsByPacienteId(Long id);

  boolean existsByPacienteIdAndStatus(Long id, String status);

  boolean existsBySolicitanteId(Long id);
}
