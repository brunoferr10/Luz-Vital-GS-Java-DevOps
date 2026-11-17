package br.com.minddev.luzvital;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.fasterxml.jackson.databind.*;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ApiIntegrationTest {
  @Autowired MockMvc mvc;
  @Autowired ObjectMapper json;
  @Autowired AuthService auth;
  @Autowired UsuarioRepository users;
  @Autowired PacienteRepository patients;
  @Autowired AparelhoRepository devices;
  @Autowired OcorrenciaRepository incidents;
  String gestor, cuidador, email;
  Long uid;

  @BeforeEach
  void setup() {
    incidents.deleteAll();
    devices.deleteAll();
    patients.deleteAll();
    users.deleteAll();
    var g =
        auth.create(new Dtos.UsuarioInput("Gestor", "g@example.com", "SenhaTeste123!", "GESTOR"));
    var u = auth.register(new Dtos.Registro("Cuidador", "c@example.com", "SenhaTeste123!"));
    uid = u.id();
    email = u.email();
    gestor = auth.login(new Dtos.Login(g.email(), "SenhaTeste123!")).accessToken();
    cuidador = auth.login(new Dtos.Login(u.email(), "SenhaTeste123!")).accessToken();
  }

  ResultActions send(String method, String path, String token, Object body) throws Exception {
    var req =
        switch (method) {
          case "POST" -> post(path);
          case "PUT" -> put(path);
          case "DELETE" -> delete(path);
          default -> get(path);
        };
    if (token != null) req.header("Authorization", "Bearer " + token);
    if (body != null)
      req.contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsBytes(body));
    return mvc.perform(req);
  }

  JsonNode node(ResultActions result) throws Exception {
    return json.readTree(result.andReturn().getResponse().getContentAsString());
  }

  Long patient(String token) throws Exception {
    return node(send(
                "POST",
                "/api/v1/pacientes",
                token,
                new Dtos.PacienteInput("Paciente ficticio", "Rua de teste 10", "11999990000", uid))
            .andExpect(status().isCreated()))
        .get("id")
        .asLong();
  }

  Long device(Long pid, int autonomy, Aparelho.Tipo tipo) throws Exception {
    return node(send(
                "POST",
                "/api/v1/aparelhos",
                cuidador,
                new Dtos.AparelhoInput(pid, "Modelo teste", tipo, 320, autonomy))
            .andExpect(status().isCreated()))
        .get("id")
        .asLong();
  }

  ResultActions occurrence(Long pid, String key, Instant start) throws Exception {
    return mvc.perform(
        post("/api/v1/ocorrencias")
            .header("Authorization", "Bearer " + cuidador)
            .header("Idempotency-Key", key)
            .contentType(MediaType.APPLICATION_JSON)
            .content(
                json.writeValueAsBytes(new Dtos.OcorrenciaInput(pid, start, "Falta de energia"))));
  }

  @Test
  void authenticationAndRoles() throws Exception {
    send("GET", "/api/v1/pacientes", null, null).andExpect(status().isUnauthorized());
    send("POST", "/api/v1/auth/login", null, new Dtos.Login(email, "errada"))
        .andExpect(status().isUnauthorized());
    send("GET", "/api/v1/usuarios", cuidador, null).andExpect(status().isForbidden());
    send("GET", "/api/v1/ocorrencias/fila", cuidador, null).andExpect(status().isForbidden());
    send("GET", "/api/v1/usuarios", gestor, null).andExpect(status().isOk());
    send("GET", "/api/v1/pacientes", "invalid-token", null).andExpect(status().isUnauthorized());
  }

  @Test
  void crudAllFourTablesAndDependencyOrder() throws Exception {
    var u =
        node(
            send(
                    "POST",
                    "/api/v1/usuarios",
                    gestor,
                    new Dtos.UsuarioInput(
                        "Outro", "outro@example.com", "SenhaTeste123!", "CUIDADOR"))
                .andExpect(status().isCreated()));
    long other = u.get("id").asLong();
    send(
            "PUT",
            "/api/v1/usuarios/" + other,
            gestor,
            new Dtos.UsuarioInput(
                "Outro atualizado", "outro@example.com", "SenhaTeste123!", "CUIDADOR"))
        .andExpect(status().isOk());
    send("GET", "/api/v1/usuarios/" + other, gestor, null)
        .andExpect(jsonPath("$.nome").value("Outro atualizado"));
    send("DELETE", "/api/v1/usuarios/" + other, gestor, null).andExpect(status().isNoContent());
    Long p = patient(cuidador);
    Long d = device(p, 60, Aparelho.Tipo.VENTILADOR);
    send("GET", "/api/v1/pacientes/" + p, cuidador, null).andExpect(status().isOk());
    send(
            "PUT",
            "/api/v1/pacientes/" + p,
            cuidador,
            new Dtos.PacienteInput("Atualizado", "Rua teste 20", "11000000000", uid))
        .andExpect(jsonPath("$.nome").value("Atualizado"));
    send("GET", "/api/v1/aparelhos/" + d, cuidador, null).andExpect(status().isOk());
    send(
            "PUT",
            "/api/v1/aparelhos/" + d,
            cuidador,
            new Dtos.AparelhoInput(p, "Modelo atualizado", Aparelho.Tipo.VENTILADOR, 250, 90))
        .andExpect(jsonPath("$.autonomiaMinutos").value(90));
    Long o =
        node(occurrence(p, UUID.randomUUID().toString(), Instant.now().minusSeconds(60))
                .andExpect(status().isCreated()))
            .get("id")
            .asLong();
    send("GET", "/api/v1/ocorrencias/" + o, cuidador, null).andExpect(status().isOk());
    send("DELETE", "/api/v1/pacientes/" + p, gestor, null).andExpect(status().isConflict());
    send("DELETE", "/api/v1/ocorrencias/" + o, gestor, null).andExpect(status().isConflict());
    send(
            "PUT",
            "/api/v1/ocorrencias/" + o,
            gestor,
            new Dtos.OcorrenciaUpdate("ENCERRADA", "Energia restabelecida"))
        .andExpect(jsonPath("$.status").value("ENCERRADA"));
    send("DELETE", "/api/v1/ocorrencias/" + o, gestor, null).andExpect(status().isNoContent());
    send("DELETE", "/api/v1/aparelhos/" + d, cuidador, null).andExpect(status().isNoContent());
    send("DELETE", "/api/v1/pacientes/" + p, cuidador, null).andExpect(status().isNoContent());
    send("GET", "/api/v1/pacientes/" + p, cuidador, null).andExpect(status().isNotFound());
  }

  @Test
  void validatesAndPaginates() throws Exception {
    send("POST", "/api/v1/pacientes", cuidador, new Dtos.PacienteInput("", "", "", uid))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.codigo").value("VALIDACAO"));
    patient(cuidador);
    patient(cuidador);
    send("GET", "/api/v1/pacientes?page=0&size=1", cuidador, null)
        .andExpect(jsonPath("$.content.length()").value(1))
        .andExpect(jsonPath("$.totalElements").value(2));
    send("GET", "/api/v1/pacientes?size=101", cuidador, null).andExpect(status().isBadRequest());
  }

  @Test
  void ownershipIsolation() throws Exception {
    Long p = patient(cuidador);
    Long d = device(p, 60, Aparelho.Tipo.BOMBA);
    var other = auth.register(new Dtos.Registro("Outro", "z@example.com", "SenhaTeste123!"));
    var token = auth.login(new Dtos.Login(other.email(), "SenhaTeste123!")).accessToken();
    send("GET", "/api/v1/pacientes/" + p, token, null).andExpect(status().isForbidden());
    send("GET", "/api/v1/aparelhos/" + d, token, null).andExpect(status().isForbidden());
    send("GET", "/api/v1/pacientes", token, null).andExpect(jsonPath("$.totalElements").value(0));
  }

  @Test
  void idempotencyAndQueueOrdering() throws Exception {
    Long first = patient(cuidador), urgent = patient(cuidador);
    device(first, 90, Aparelho.Tipo.BOMBA);
    device(urgent, 10, Aparelho.Tipo.VENTILADOR);
    Instant start =
        Instant.now().minusSeconds(120).truncatedTo(java.time.temporal.ChronoUnit.MICROS);
    String key = UUID.randomUUID().toString();
    Long id =
        node(occurrence(first, key, start).andExpect(status().isCreated())).get("id").asLong();
    assertEquals(
        id, node(occurrence(first, key, start).andExpect(status().isCreated())).get("id").asLong());
    occurrence(first, key, start.minusSeconds(1)).andExpect(status().isConflict());
    occurrence(first, UUID.randomUUID().toString(), start).andExpect(status().isConflict());
    Long urgentId =
        node(occurrence(urgent, UUID.randomUUID().toString(), start)
                .andExpect(status().isCreated()))
            .get("id")
            .asLong();
    send("GET", "/api/v1/ocorrencias/fila", gestor, null)
        .andExpect(jsonPath("$[0].id").value(urgentId))
        .andExpect(jsonPath("$[0].minutosRestantes").value(8));
  }

  @Test
  void aiDisabledReturnsExplicit503() throws Exception {
    Long p = patient(cuidador);
    device(p, 60, Aparelho.Tipo.CONCENTRADOR);
    send("POST", "/api/v1/pacientes/" + p + "/plano", cuidador, null)
        .andExpect(status().isServiceUnavailable());
  }

  @Test
  void consumerIsIdempotent() throws Exception {
    Long p = patient(cuidador);
    device(p, 60, Aparelho.Tipo.BOMBA);
    Long id =
        node(occurrence(p, UUID.randomUUID().toString(), Instant.now().minusSeconds(10)))
            .get("id")
            .asLong();
    var consumer = new EventConsumer(incidents);
    consumer.consume(new MessagingConfig.EnergiaCaiu(id));
    consumer.consume(new MessagingConfig.EnergiaCaiu(id));
    assertTrue(incidents.findById(id).orElseThrow().eventoRecebido);
  }
}
