package br.com.minddev.luzvital;

import java.time.*;
import java.util.*;
import org.springframework.data.domain.*;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@Transactional
public class DomainService {
  private final PacienteRepository patients;
  private final AparelhoRepository devices;
  private final OcorrenciaRepository incidents;
  private final UsuarioRepository users;

  public DomainService(
      PacienteRepository p, AparelhoRepository d, OcorrenciaRepository o, UsuarioRepository u) {
    patients = p;
    devices = d;
    incidents = o;
    users = u;
  }

  Long userId() {
    return Long.valueOf(
        ((Jwt) SecurityContextHolder.getContext().getAuthentication().getPrincipal()).getSubject());
  }

  boolean manager() {
    return SecurityContextHolder.getContext().getAuthentication().getAuthorities().stream()
        .anyMatch(a -> a.getAuthority().equals("ROLE_GESTOR"));
  }

  void own(Long owner) {
    if (!manager() && !owner.equals(userId()))
      throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Registro de outro cuidador");
  }

  static ResponseStatusException missing() {
    return new ResponseStatusException(HttpStatus.NOT_FOUND, "Registro nao encontrado");
  }

  static void conflict(String msg) {
    throw new ResponseStatusException(HttpStatus.CONFLICT, msg);
  }

  Paciente patient(Long id) {
    var p = patients.findById(id).orElseThrow(DomainService::missing);
    own(p.cuidador.getId());
    return p;
  }

  Aparelho device(Long id) {
    var d = devices.findById(id).orElseThrow(DomainService::missing);
    own(d.paciente.getCuidador().getId());
    return d;
  }

  Ocorrencia incident(Long id) {
    var o = incidents.findById(id).orElseThrow(DomainService::missing);
    own(o.paciente.getCuidador().getId());
    return o;
  }

  Dtos.PacienteView pv(Paciente p) {
    return new Dtos.PacienteView(p.id, p.nome, p.endereco, p.telefone, p.cuidador.getId(), p.plano);
  }

  Dtos.AparelhoView dv(Aparelho d) {
    return new Dtos.AparelhoView(
        d.id, d.paciente.getId(), d.modelo, d.tipo, d.potenciaWatts, d.autonomiaMinutos);
  }

  long remaining(Ocorrencia o) {
    Instant now = o.encerramento == null ? Instant.now() : o.encerramento;
    return Math.max(0, o.autonomiaInicial - Duration.between(o.inicio, now).toMinutes());
  }

  Dtos.OcorrenciaView ov(Ocorrencia o) {
    return new Dtos.OcorrenciaView(
        o.id,
        o.paciente.getId(),
        o.inicio,
        o.status,
        o.observacao,
        remaining(o),
        o.eventoRecebido,
        o.encerramento);
  }

  Pageable page(int page, int size) {
    if (page < 0 || size < 1 || size > 100)
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "page >= 0 e size entre 1 e 100");
    return PageRequest.of(page, size, Sort.by("id"));
  }

  public Page<Dtos.PacienteView> patients(int p, int s) {
    return (manager()
            ? patients.findAll(page(p, s))
            : patients.findByCuidadorId(userId(), page(p, s)))
        .map(this::pv);
  }

  public Dtos.PacienteView getPatient(Long id) {
    return pv(patient(id));
  }

  public Dtos.PacienteView savePatient(Long id, Dtos.PacienteInput in) {
    own(in.cuidadorId());
    var u = users.findById(in.cuidadorId()).orElseThrow(DomainService::missing);
    if (!u.papel.equals("CUIDADOR"))
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Responsavel deve ser CUIDADOR");
    var p = id == null ? new Paciente() : patient(id);
    p.nome = in.nome();
    p.endereco = in.endereco();
    p.telefone = in.telefone();
    p.cuidador = u;
    return pv(patients.save(p));
  }

  public void deletePatient(Long id) {
    var p = patient(id);
    if (devices.existsByPacienteId(id) || incidents.existsByPacienteId(id))
      conflict("Exclua primeiro aparelhos e ocorrencias");
    patients.delete(p);
  }

  public Page<Dtos.AparelhoView> devices(int p, int s) {
    return (manager()
            ? devices.findAll(page(p, s))
            : devices.findByPacienteCuidadorId(userId(), page(p, s)))
        .map(this::dv);
  }

  public Dtos.AparelhoView getDevice(Long id) {
    return dv(device(id));
  }

  public Dtos.AparelhoView saveDevice(Long id, Dtos.AparelhoInput in) {
    var p = patient(in.pacienteId());
    var d = id == null ? new Aparelho() : device(id);
    d.paciente = p;
    d.modelo = in.modelo();
    d.tipo = in.tipo();
    d.potenciaWatts = in.potenciaWatts();
    d.autonomiaMinutos = in.autonomiaMinutos();
    return dv(devices.save(d));
  }

  public void deleteDevice(Long id) {
    devices.delete(device(id));
  }

  public Page<Dtos.OcorrenciaView> incidents(int p, int s) {
    return (manager()
            ? incidents.findAll(page(p, s))
            : incidents.findByPacienteCuidadorId(userId(), page(p, s)))
        .map(this::ov);
  }

  public Dtos.OcorrenciaView getIncident(Long id) {
    return ov(incident(id));
  }

  public Dtos.OcorrenciaView open(Dtos.OcorrenciaInput in, String key) {
    if (key == null || !key.matches("[A-Za-z0-9_-]{8,64}"))
      throw new ResponseStatusException(
          HttpStatus.BAD_REQUEST, "Idempotency-Key exige 8 a 64 caracteres alfanumericos, _ ou -");
    var inicio = in.inicio().truncatedTo(java.time.temporal.ChronoUnit.MICROS);
    var p = patient(in.pacienteId());
    var previous = incidents.findBySolicitanteIdAndChave(userId(), key);
    if (previous.isPresent()) {
      var old = previous.get();
      if (!old.paciente.getId().equals(in.pacienteId())
          || !old.inicio.equals(inicio)
          || !old.observacao.equals(in.observacao()))
        conflict("Chave reutilizada com outro conteudo");
      return ov(old);
    }
    if (incidents.existsByPacienteIdAndStatus(p.id, "ABERTA"))
      conflict("Paciente ja possui ocorrencia aberta");
    var ds = devices.findByPacienteId(p.id);
    if (ds.isEmpty())
      throw new ResponseStatusException(
          HttpStatus.BAD_REQUEST, "Cadastre um aparelho antes de abrir ocorrencia");
    var o = new Ocorrencia();
    o.paciente = p;
    o.solicitante = users.findById(userId()).orElseThrow(DomainService::missing);
    o.chave = key;
    o.inicio = inicio;
    o.status = "ABERTA";
    o.observacao = in.observacao();
    o.autonomiaInicial = ds.stream().mapToInt(d -> d.autonomiaMinutos).min().orElse(0);
    o.desempate = ds.stream().anyMatch(d -> d.tipo == Aparelho.Tipo.VENTILADOR) ? 0 : 1;
    return ov(incidents.saveAndFlush(o));
  }

  public Dtos.OcorrenciaView updateIncident(Long id, Dtos.OcorrenciaUpdate in) {
    var o = incident(id);
    if (o.status.equals("ENCERRADA") && in.status().equals("ABERTA"))
      conflict("Ocorrencia encerrada nao pode ser reaberta");
    o.status = in.status();
    o.observacao = in.observacao();
    if (o.status.equals("ENCERRADA") && o.encerramento == null) o.encerramento = Instant.now();
    return ov(incidents.save(o));
  }

  public void deleteIncident(Long id) {
    var o = incident(id);
    if (!o.status.equals("ENCERRADA")) conflict("Encerre a ocorrencia antes de excluir");
    incidents.delete(o);
  }

  public List<Dtos.OcorrenciaView> queue() {
    return incidents.findByStatus("ABERTA").stream()
        .sorted(
            Comparator.comparingLong((Ocorrencia o) -> remaining(o))
                .thenComparingInt(o -> o.desempate)
                .thenComparing(o -> o.inicio))
        .map(this::ov)
        .toList();
  }

  public UsuarioRepository usersRepository() {
    return users;
  }

  public PacienteRepository patientsRepository() {
    return patients;
  }

  public AparelhoRepository devicesRepository() {
    return devices;
  }

  public OcorrenciaRepository incidentsRepository() {
    return incidents;
  }
}
